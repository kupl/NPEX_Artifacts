/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.history;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.logger.LoggerFactory;
import org.opensolaris.opengrok.util.Executor;

/**
 * Access to an AccuRev repository (here an actual user workspace)
 *
 * AccuRev requires that a user logs into their system before it can be used. So
 * on the machine acting as the OpenGrok server, some valid user has to be
 * permanently logged in. (accurev login -n &lt;user&gt;)
 *
 * It appears that the file path that is given to all these methods is the
 * complete path to the file which includes the path to the root of the source
 * location. This means that when using the -P option of OpenGrok to make all
 * the directories pointed to by the source root to be seen as separate projects
 * is not all as it would seem. The History GURU always starts building the
 * history cache using the source root. Well there is NO HISTORY for anything at
 * the source root because it is not part of an actual AccuRev depot. The
 * directories within the source root directory represent the work areas of
 * AccuRev and it is those areas where history can be obtained. This
 * implementation allows those directories to be symbolic links to the actual
 * workspaces.
 *
 * Other assumptions:
 *
 * There is only one associated AccuRev depot associated with workspaces.
 *
 * @author Steven Haehn
 */
public class AccuRevRepository extends Repository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccuRevRepository.class);

    private static final long serialVersionUID = 1L;
    /**
     * The property name used to obtain the client command for this repository.
     */
    public static final String CMD_PROPERTY_KEY
            = "org.opensolaris.opengrok.history.AccuRev";
    /**
     * The command to use to access the repository if none was given explicitly
     */
    public static final String CMD_FALLBACK = "accurev";
    private static final Pattern annotationPattern
            = Pattern.compile("^\\s+(\\d+.\\d+)\\s+(\\w+)");   // version, user
    private static final Pattern depotPattern
            = Pattern.compile("^Depot:\\s+(\\w+)");
    private static final Pattern parentPattern
            = Pattern.compile("^Basis:\\s+(\\w+)");
    private static final Pattern workspaceRootPattern
            = Pattern.compile("Top:\\s+(.+)$");
    private static final RuntimeEnvironment env = RuntimeEnvironment.getInstance();

    private String depotName = null;
    private String parent = null;
    private String wsRoot = null;
    private String relRoot = "";
    
    /**  
     * This will be /./ on Unix and \.\ on Windows 
     */
    private static final String depotRoot 
            = String.format( "%s.%s", File.separator, File.separator );

    public AccuRevRepository() {
        type = "AccuRev";
        datePatterns = new String[]{
            "yyyy/MM/dd hh:mm:ss"
        };
        ensureCommand(CMD_PROPERTY_KEY, CMD_FALLBACK);
    }

    @Override
    public Annotation annotate(File file, String rev) throws IOException {

        Annotation a = new Annotation(file.getName());

        ArrayList<String> cmd = new ArrayList<>();

        // Do not use absolute paths because symbolic links will cause havoc.
        String path = getDepotRelativePath( file );

        cmd.add(RepoCommand);
        cmd.add("annotate");
        cmd.add("-fvu");      // version & user

        if (rev != null) {
            cmd.add("-v");
            cmd.add(rev.trim());
        }

        cmd.add(path);

        Executor executor = new Executor(cmd, file.getParentFile());
        executor.exec();
        try (BufferedReader reader
                = new BufferedReader(executor.getOutputReader())) {
            String line;
            int lineno = 0;
            try {
                while ((line = reader.readLine()) != null) {
                    ++lineno;
                    Matcher matcher = annotationPattern.matcher(line);

                    if (matcher.find()) {
                        // On Windows machines version shows up as
                        // <number>\<number>. To get search annotation
                        // to work properly, need to flip '\' to '/'.
                        // This is a noop on Unix boxes.
                        String version = matcher.group(1).replace('\\', '/');
                        String author  = matcher.group(2);
                        a.addLine(version, author, true);
                    } else {
                        LOGGER.log(Level.SEVERE,
                                "Did not find annotation in line {0}: [{1}]",
                                new Object[]{lineno, line});
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE,
                        "Could not read annotations for " + file, e);
            }
        }

        return a;
    }

    /**
     * Get an executor to be used for retrieving the history log for the given
     * file. (used by AccuRevHistoryParser).
     *
     * @param file file for which history is to be retrieved.
     * @return An Executor ready to be started
     */
    Executor getHistoryLogExecutor(File file) throws IOException {

        // Do not use absolute paths because symbolic links will cause havoc.
        String path = getDepotRelativePath( file );
        
        ArrayList<String> cmd = new ArrayList<>();

        cmd.add(RepoCommand);
        cmd.add("hist");

        if (!file.isDirectory()) {
            cmd.add("-k");
            cmd.add("keep");  // get a list of all 'real' file versions
        }

        cmd.add(path);
        
        File workingDirectory = file.isDirectory() ? file : file.getParentFile();
        
        return new Executor(cmd, workingDirectory);
    }

    @Override
    InputStream getHistoryGet(String parent, String basename, String rev) {

        ArrayList<String> cmd = new ArrayList<>();
        InputStream inputStream = null;
        File directory = new File(parent);

        /*
         * Only way to guarantee getting the contents of a file is to fire off
         * an AccuRev 'stat'us command to get the element ID number for the
         * subsequent 'cat' command. (Element ID's are unique for a file, unless
         * evil twins are present) This is because it is possible that the file
         * may have been moved to a different place in the depot. The 'stat'
         * command will produce a line with the format:
         *
         * <filePath> <elementID> <virtualVersion> (<realVersion>) (<status>)
         *
         *  /./myFile e:17715 CP.73_Depot/2 (3220/2) (backed)
         */
        cmd.add(RepoCommand);
        cmd.add("stat");
        cmd.add("-fe");
        cmd.add(basename);
        Executor executor = new Executor(cmd, directory);
        executor.exec();

        String elementID = null;

        try (BufferedReader info = new BufferedReader(executor.getOutputReader())) {
            String line = info.readLine();
            String[] statInfo = line.split("\\s+");
            elementID = statInfo[1].substring(2); // skip over 'e:'

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
                    "Could not obtain status for {0}", basename);
        }

        if (elementID != null) {
            /*
             *  This really gets the contents of the file.
             */
            cmd.clear();
            cmd.add(RepoCommand);
            cmd.add("cat");
            cmd.add("-v");
            cmd.add(rev.trim());
            cmd.add("-e");
            cmd.add(elementID);

            executor = new Executor(cmd, directory);
            executor.exec();

            inputStream
                    = new ByteArrayInputStream(executor.getOutputString().getBytes());
        }

        return inputStream;
    }

    @Override
    public void update() throws IOException {      
        File directory = new File(getDirectoryName());
        List<String> cmd = new ArrayList<>();
        
        cmd.add(RepoCommand);
        cmd.add("update");

        Executor executor = new Executor(cmd, directory);
        if (executor.exec() != 0) {
            throw new IOException(executor.getErrorString());
        }
    }

    @Override
    boolean fileHasHistory(File file) {
        return true;
    }

    @Override
    public boolean fileHasAnnotation(File file) {
        return true;
    }

    /**
     * Expecting data of the form:
     *
     *   Principal:      shaehn
     *   Host:           waskly
     *   Server name:    lean.machine.com
     *   Port:           5050
     *   DB Encoding:    Unicode
     *   ACCUREV_BIN:    C:\Program Files (x86)\AccuRev\bin
     *   Client time:    2017/08/02 13:30:31 Eastern Daylight Time (1501695031)
     *   Server time:    2017/08/02 13:30:54 Eastern Daylight Time (1501695054)
     *   Depot:          bread_and_butter
     *   Workspace/ref:  BABS_2_shaehn
     *   Basis:          BABS2 
     *   Top:            C:\Users\shaehn\workspaces\BABS_2
     *
     *   Output would be similar on Unix boxes, but with '/' appearing
     *   in path names instead of '\'. The 'Basis' (BABS2) is the parent
     *   stream of the user workspace (BABS_2_shaehn). The 'Top' is the
     *   path to the root of the user workspace/repository. The elements
     *   below 'Server time' will be missing when current working directory
     *   is not within a known AccuRev workspace/repository.
     */
    private boolean getAccuRevInfo( File wsPath ) {
    
        ArrayList<String> cmd = new ArrayList<>();
        boolean status  = false;
        Path given = Paths.get(wsPath.toString());
        Path realWsPath = null;
        
        try {
            // This helps overcome symbolic link issues so that
            // Accurev will report the desired information.
            // Otherwise it claims:
            // "You are not in a directory associated with a workspace"
            realWsPath = given.toRealPath();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
                    "Could not determine real path for {0}", wsPath);
        }
        
        cmd.add(RepoCommand);
        cmd.add("info");

        Executor executor = new Executor(cmd, realWsPath.toFile());
        executor.exec();

        try (BufferedReader info = new BufferedReader(executor.getOutputReader())) {
            String line;
            while ((line = info.readLine()) != null) {

                if (line.contains("not logged in")) {
                    LOGGER.log(
                            Level.SEVERE, "Not logged into AccuRev server");
                    break;
                }

                if (line.startsWith("Depot")) {
                    Matcher depotMatch  = depotPattern.matcher(line);
                    if (depotMatch.find()) {
                        depotName = depotMatch.group(1);
                        status = true;
                    }
                }

                else if (line.startsWith("Basis")) {
                    Matcher parentMatch = parentPattern.matcher(line);
                    if (parentMatch.find()) {
                        parent = parentMatch.group(1);
                    }
                }
                
                else if (line.startsWith("Top")) {
                    Matcher workspaceRoot = workspaceRootPattern.matcher(line);
                    if (workspaceRoot.find()) {
                        wsRoot = workspaceRoot.group(1);
                        // Normally, the source root path and the workspace root
                        // are the same, but if the source root has been extended
                        // into the actual AccuRev workspace, there is going to
                        // be a residual relative path needed to construct
                        // depot relative names.
                        //
                        //  Rare but possible:
                        //   srcRoot: C:\Users\shaehn\workspaces\BABS_2\tools -
                        //   wsRoot:  C:\Users\shaehn\workspaces\BABS_2
                        //
                        //  Gives: \tools for relRoot
                        //
                        // Assuming that the given name is to the root of the
                        // AccuRev workspace, check to see if it happens to be
                        // a symbolic link (which means its path name will differ
                        // from the path known by Accurev)
 
                        if (Files.isSymbolicLink(given)) {
                            LOGGER.log(Level.INFO,"{0} is symbolic link.", wsPath);
                            
                            // When we know that the two paths DO NOT point to the
                            // same place (that is, the given path is deeper into
                            // the repository workspace), then need to get the
                            // real path pointed to by the symbolic link so that
                            // the relative root fragment can be extracted.
                            if (! Files.isSameFile(given, Paths.get(wsRoot))) {
                                String linkedTo = Files.readSymbolicLink(given).toRealPath().toString();
                                if (linkedTo.regionMatches(0, wsRoot, 0, wsRoot.length())) {
                                    relRoot = linkedTo.substring(wsRoot.length());
                                }
                            }
                        } else {
                            // The source root and the workspace root will both
                            // be canonical paths. There will be a non-empty
                            // relative root whenever the source root is longer
                            // than the workspace root known to AccuRev.
                            String srcRoot = env.getSourceRootPath();
                            if (srcRoot.length() > wsRoot.length()) {
                                relRoot = srcRoot.substring(wsRoot.length());
                            }
                        }
                        
                        if (relRoot.length() > 0) {
                            LOGGER.log(Level.INFO,"Source root relative to workspace root by: {0}", relRoot);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
                    "Could not find AccuRev repository for {0}", wsPath);
        }
        
        return status;
    }

    /**
     * Check if a given path is associated with an AccuRev workspace
     *
     * The AccuRev 'info' command provides a Depot name when in a known
     * workspace. Otherwise, the Depot name will be missing.
     *
     * @param wsPath The presumed path to an AccuRev workspace directory.
     * @return true if the given path is in the depot, false otherwise
     */
    private boolean isInAccuRevDepot(File wsPath) {

        // Once depot name is determined, always assume inside depot.
        boolean status = (depotName != null);

        if (! status && isWorking()) {
            status = getAccuRevInfo( wsPath );
        }

        return status;
    }

    /**
     * Obtain a depot relative name
     * for a given repository element path. For example,
     * when the repository root is "/home/shaehn/workspaces/BABS_2" then
     *
     * given file path: /home/shaehn/workspaces/BABS_2/tools
     * depot relative:  /./tools 
     * 
     * Using depot relative names instead of absolute file paths solves
     * the problems encountered when symbolic links are made for repository
     * root paths. For example, when the following path
     * 
     *  /home/shaehn/active/src/BABS is a symbolic link to
     *  /home/shaehn/workspaces/BABS_2 then 
     * 
     * given file path: /home/shaehn/active/src/BABS/tools
     * depot relative:  /./tools 
     * 
     * @param file path to repository element
     * @return a depot relative file element path
     */
    public String getDepotRelativePath(File file) {

        String path = depotRoot;
        try {
            // This should turn any symbolically linked paths into the real thing...
            Path realPath = Paths.get(file.toString()).toRealPath();
            // ... so that removing the workspace root will give the depot relative path
            //     (Note realPath should always be starting with wsRoot.)
            String relativePath = realPath.toString().substring(wsRoot.length());
            
            if (relativePath.length() > 0) {
                path = Paths.get(depotRoot, relativePath).toString();
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Unable to determine depot relative path for {0}",
                    file.getPath());
        }

        return path;
    }

    @Override
    boolean isRepositoryFor(File sourceHome) {

        if (sourceHome.isDirectory()) {
            return isInAccuRevDepot(sourceHome);
        }

        return false;
    }

    @Override
    public boolean isWorking() {

        if (working == null) {

            working = checkCmd(RepoCommand, "info");
        }

        return working;
    }

    @Override
    boolean hasHistoryForDirectories() {
        return true;
    }

    @Override
    History getHistory(File file) throws HistoryException {
        return new AccuRevHistoryParser().parse(file, this);
    }

    @Override
    String determineParent() throws IOException {
        getAccuRevInfo(new File(getDirectoryName()));
        return parent;
    }

    @Override
    String determineBranch() {
        return null;
    }
}
