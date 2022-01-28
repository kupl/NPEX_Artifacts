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
package org.opensolaris.opengrok.index;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.apache.lucene.util.BytesRef;
import org.opensolaris.opengrok.analysis.AnalyzerGuru;
import org.opensolaris.opengrok.analysis.Ctags;
import org.opensolaris.opengrok.analysis.Definitions;
import org.opensolaris.opengrok.analysis.FileAnalyzer;
import org.opensolaris.opengrok.analysis.FileAnalyzer.Genre;
import org.opensolaris.opengrok.configuration.Project;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.configuration.messages.Message;
import org.opensolaris.opengrok.history.HistoryException;
import org.opensolaris.opengrok.history.HistoryGuru;
import org.opensolaris.opengrok.logger.LoggerFactory;
import org.opensolaris.opengrok.search.QueryBuilder;
import org.opensolaris.opengrok.util.IOUtils;
import org.opensolaris.opengrok.web.Util;

/**
 * This class is used to create / update the index databases. Currently we use
 * one index database per project.
 *
 * @author Trond Norbye
 * @author Lubos Kosco , update for lucene 4.x , 5.x
 */
public class IndexDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexDatabase.class);

    private Project project;
    private FSDirectory indexDirectory;    
    private IndexWriter writer;
    private TermsEnum uidIter;
    private IgnoredNames ignoredNames;
    private Filter includedNames;
    private AnalyzerGuru analyzerGuru;
    private File xrefDir;
    private boolean interrupted;
    private List<IndexChangedListener> listeners;
    private File dirtyFile;
    private final Object lock = new Object();
    private boolean dirty;
    private boolean running;
    private List<String> directories;
    private static final Comparator<File> fileComparator = new Comparator<File>() {
        @Override
        public int compare(File p1, File p2) {
            return p1.getName().compareTo(p2.getName());
        }
    };
    private Ctags ctags;
    private LockFactory lockfact;
    private final BytesRef emptyBR = new BytesRef("");
    
    // Directory where we store indexes
    public static final String INDEX_DIR = "index";
    public static final String XREF_DIR = "xref";

    /**
     * Create a new instance of the Index Database. Use this constructor if you
     * don't use any projects
     *
     * @throws java.io.IOException if an error occurs while creating directories
     */
    public IndexDatabase() throws IOException {
        this(null);
    }

    /**
     * Create a new instance of an Index Database for a given project
     *
     * @param project the project to create the database for
     * @throws java.io.IOException if an error occurs while creating
     * directories
     */
    public IndexDatabase(Project project) throws IOException {
        this.project = project;
        lockfact = SimpleFSLockFactory.INSTANCE;
        initialize();
    }

    /**
     * Update the index database for all of the projects. Print progress to
     * standard out.
     *
     * @param executor An executor to run the job
     * @throws IOException if an error occurs
     */
    public static void updateAll(ExecutorService executor) throws IOException {
        updateAll(executor, null);
    }

    /**
     * Update the index database for all of the projects
     *
     * @param executor An executor to run the job
     * @param listener where to signal the changes to the database
     * @throws IOException if an error occurs
     */
    static void updateAll(ExecutorService executor, IndexChangedListener listener) throws IOException {
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        List<IndexDatabase> dbs = new ArrayList<>();

        if (env.hasProjects()) {
            for (Project project : env.getProjectList()) {
                dbs.add(new IndexDatabase(project));
            }
        } else {
            dbs.add(new IndexDatabase());
        }

        for (IndexDatabase d : dbs) {
            final IndexDatabase db = d;
            if (listener != null) {
                db.addIndexChangedListener(listener);
            }

            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        db.update();
                    } catch (Throwable e) {
                        LOGGER.log(Level.SEVERE, "Problem updating lucene index database: ", e);
                    }
                }
            });
        }
    }

    /**
     * Update the index database for a number of sub-directories
     *
     * @param executor An executor to run the job
     * @param listener where to signal the changes to the database
     * @param paths list of paths to be indexed
     * @throws IOException if an error occurs
     */
    public static void update(ExecutorService executor, IndexChangedListener listener, List<String> paths) throws IOException {
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        List<IndexDatabase> dbs = new ArrayList<>();

        for (String path : paths) {
            Project project = Project.getProject(path);
            if (project == null && env.hasProjects()) {
                LOGGER.log(Level.WARNING, "Could not find a project for \"{0}\"", path);
            } else {
                IndexDatabase db;

                try {
                    if (project == null) {
                        db = new IndexDatabase();
                    } else {
                        db = new IndexDatabase(project);
                    }

                    int idx = dbs.indexOf(db);
                    if (idx != -1) {
                        db = dbs.get(idx);
                    }

                    if (db.addDirectory(path)) {
                        if (idx == -1) {
                            dbs.add(db);
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "Directory does not exist \"{0}\" .", path);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "An error occured while updating index", e);

                }
            }

            for (final IndexDatabase db : dbs) {
                db.addIndexChangedListener(listener);
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            db.update();
                        } catch (Throwable e) {
                            LOGGER.log(Level.SEVERE, "An error occured while updating index", e);
                        }
                    }
                });
            }
        }
    }

    @SuppressWarnings("PMD.CollapsibleIfStatements")
    private void initialize() throws IOException {
        synchronized (this) {
            RuntimeEnvironment env = RuntimeEnvironment.getInstance();
            File indexDir = new File(env.getDataRootFile(), INDEX_DIR);            
            if (project != null) {
                indexDir = new File(indexDir, project.getPath());                
            }

            if (!indexDir.exists() && !indexDir.mkdirs()) {
                // to avoid race conditions, just recheck..
                if (!indexDir.exists()) {
                    throw new FileNotFoundException("Failed to create root directory [" + indexDir.getAbsolutePath() + "]");
                }
            }            

            if (!env.isUsingLuceneLocking()) {
                lockfact = NoLockFactory.INSTANCE;
            }
            indexDirectory = FSDirectory.open(indexDir.toPath(), lockfact);            
            ignoredNames = env.getIgnoredNames();
            includedNames = env.getIncludedNames();
            analyzerGuru = new AnalyzerGuru();
            if (env.isGenerateHtml()) {
                xrefDir = new File(env.getDataRootFile(), XREF_DIR);
            }
            listeners = new ArrayList<>();
            dirtyFile = new File(indexDir, "dirty");
            dirty = dirtyFile.exists();
            directories = new ArrayList<>();
        }
    }

    /**
     * By default the indexer will traverse all directories in the project. If
     * you add directories with this function update will just process the
     * specified directories.
     *
     * @param dir The directory to scan
     * @return <code>true</code> if the file is added, false otherwise
     */
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    public boolean addDirectory(String dir) {
        String directory = dir;
        if (directory.startsWith("\\")) {
            directory = directory.replace('\\', '/');
        } else if (directory.charAt(0) != '/') {
            directory = "/" + directory;
        }
        File file = new File(RuntimeEnvironment.getInstance().getSourceRootFile(), directory);
        if (file.exists()) {
            directories.add(directory);
            return true;
        }
        return false;
    }

    private int getFileCount(File sourceRoot, String dir) throws IOException {
        int file_cnt = 0;
        if (RuntimeEnvironment.getInstance().isPrintProgress()) {
            LOGGER.log(Level.INFO, "Counting files in {0} ...", dir);
            file_cnt = indexDown(sourceRoot, dir, true, 0, 0);
            LOGGER.log(Level.INFO,
                    "Need to process: {0} files for {1}",
                    new Object[]{file_cnt, dir});
        }

        return file_cnt;
    }

    private void markProjectIndexed(Project project) throws IOException {
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();

        // Successfully indexed the project. The message is sent even if
        // the project's isIndexed() is true because it triggers RepositoryInfo
        // refresh.
        if (project != null) {
            if (env.getConfigHost() != null && env.getConfigPort() > 0) {
                Message m = Message.createMessage("project");
                m.addTag(project.getName());
                m.setText("indexed");
                m.write(env.getConfigHost(), env.getConfigPort());
            }

            // Also need to store the correct value in configuration
            // when indexer writes it to a file.
            project.setIndexed(true);
        }
    }

    /**
     * Update the content of this index database
     *
     * @throws IOException if an error occurs
     * @throws HistoryException if an error occurs when accessing the history
     */
    public void update() throws IOException, HistoryException {
        synchronized (lock) {
            if (running) {
                throw new IOException("Indexer already running!");
            }
            running = true;
            interrupted = false;
        }

        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        String ctgs = env.getCtags();
        if (ctgs != null) {
            ctags = new Ctags();
            ctags.setBinary(ctgs);
        }
        if (ctags == null) {
            LOGGER.severe("Unable to run ctags! searching definitions will not work!");
        }

        if (ctags != null) {
            String filename = env.getCTagsExtraOptionsFile();
            if (filename != null) {
                ctags.setCTagsExtraOptionsFile(filename);
            }
        }

        try {
            Analyzer analyzer = AnalyzerGuru.getAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            iwc.setRAMBufferSizeMB(env.getRamBufferSize());
            writer = new IndexWriter(indexDirectory, iwc);
            writer.commit(); // to make sure index exists on the disk

            if (directories.isEmpty()) {
                if (project == null) {
                    directories.add("");
                } else {
                    directories.add(project.getPath());
                }
            }
            
            for (String dir : directories) {
                File sourceRoot;
                if ("".equals(dir)) {
                    sourceRoot = env.getSourceRootFile();
                } else {
                    sourceRoot = new File(env.getSourceRootFile(), dir);
                }

                if (env.isHistoryEnabled()) {
                    HistoryGuru.getInstance().ensureHistoryCacheExists(sourceRoot);
                }

                String startuid = Util.path2uid(dir, "");
                IndexReader reader = DirectoryReader.open(indexDirectory); // open existing index
                Terms terms = null;
                int numDocs = reader.numDocs();
                if (numDocs > 0) {
                    Fields uFields = MultiFields.getFields(reader);//reader.getTermVectors(0);
                    terms = uFields.terms(QueryBuilder.U);
                }
                
                try {
                    if (numDocs > 0) {
                        uidIter = terms.iterator();
                        TermsEnum.SeekStatus stat = uidIter.seekCeil(new BytesRef(startuid)); //init uid                        
                        if (stat == TermsEnum.SeekStatus.END) {
                            uidIter = null;
                            LOGGER.log(Level.WARNING,
                                "Couldn't find a start term for {0}, empty u field?",
                                startuid);
                        }
                    }

                    // The actual indexing happens in indexDown().
                    indexDown(sourceRoot, dir, false, 0,
                            getFileCount(sourceRoot, dir));

                    // Remove data for the trailing terms that indexDown()
                    // did not traverse. These correspond to files that have been
                    // removed and have higher ordering than any present files.
                    while (uidIter != null && uidIter.term() != null
                        && uidIter.term().utf8ToString().startsWith(startuid)) {

                        removeFile(true);
                        BytesRef next = uidIter.next();
                        if (next == null) {
                            uidIter=null;
                        }
                    }

                    markProjectIndexed(project);
                } finally {
                    reader.close();
                }
            }
        } finally {
            if (writer != null) {
                try {
                    writer.prepareCommit();
                    writer.commit();
                    writer.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "An error occured while closing writer", e);
                }
            }

            if (ctags != null) {
                try {
                    ctags.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,
                        "An error occured while closing ctags process", e);
                }
            }

            synchronized (lock) {
                running = false;
            }
        }

        if (!isInterrupted() && isDirty()) {
            if (env.isOptimizeDatabase()) {
                optimize();
            }
            env.setIndexTimestamp();
        }
    }

    /**
     * Optimize all index databases
     *
     * @param executor An executor to run the job
     * @throws IOException if an error occurs
     */
    static void optimizeAll(ExecutorService executor) throws IOException {
        List<IndexDatabase> dbs = new ArrayList<>();
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        if (env.hasProjects()) {
            for (Project project : env.getProjectList()) {
                dbs.add(new IndexDatabase(project));
            }
        } else {
            dbs.add(new IndexDatabase());
        }

        for (IndexDatabase d : dbs) {
            final IndexDatabase db = d;
            if (db.isDirty()) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            db.update();
                        } catch (Throwable e) {
                            LOGGER.log(Level.SEVERE,
                                "Problem updating lucene index database: ", e);
                        }
                    }
                });
            }
        }
    }

    /**
     * Optimize the index database
     */
    public void optimize() {
        synchronized (lock) {
            if (running) {
                LOGGER.warning("Optimize terminated... Someone else is updating / optimizing it!");
                return;
            }
            running = true;
        }
        IndexWriter wrt = null;
        try {
            LOGGER.info("Optimizing the index ... ");
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig conf = new IndexWriterConfig(analyzer);
            conf.setOpenMode(OpenMode.CREATE_OR_APPEND);

            wrt = new IndexWriter(indexDirectory, conf);
            wrt.forceMerge(1); // this is deprecated and not needed anymore            
            LOGGER.info("done");
            synchronized (lock) {
                if (dirtyFile.exists() && !dirtyFile.delete()) {
                    LOGGER.log(Level.FINE, "Failed to remove \"dirty-file\": {0}",
                        dirtyFile.getAbsolutePath());
                }
                dirty = false;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "ERROR: optimizing index: {0}", e);
        } finally {
            if (wrt != null) {
                try {
                    wrt.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,
                        "An error occured while closing writer", e);
                }
            }
            synchronized (lock) {
                running = false;
            }
        }
    }
    
    private boolean isDirty() {
        synchronized (lock) {
            return dirty;
        }
    }

    private void setDirty() {
        synchronized (lock) {
            try {
                if (!dirty) {
                    if (!dirtyFile.createNewFile() && !dirtyFile.exists()) {
                        LOGGER.log(Level.FINE,
                                "Failed to create \"dirty-file\": {0}",
                                dirtyFile.getAbsolutePath());
                    }
                    dirty = true;
                }
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "When creating dirty file: ", e);
            }
        }
    }

    /**
     * Remove xref file for given path
     * @param path path to file under source root
     */
    private void removeXrefFile(String path) {
        File xrefFile;
        if (RuntimeEnvironment.getInstance().isCompressXref()) {
            xrefFile = new File(xrefDir, path + ".gz");
        } else {
            xrefFile = new File(xrefDir, path);
        }
        File parent = xrefFile.getParentFile();

        if (!xrefFile.delete() && xrefFile.exists()) {
            LOGGER.log(Level.INFO, "Failed to remove obsolete xref-file: {0}",
                xrefFile.getAbsolutePath());
        }

        // Remove the parent directory if it's empty.
        if (parent.delete()) {
            LOGGER.log(Level.FINE, "Removed empty xref dir:{0}",
                parent.getAbsolutePath());
        }
    }

    private void removeHistoryFile(String path) {
        HistoryGuru.getInstance().clearCacheFile(path);
    }

    /**
     * Remove a stale file (uidIter.term().text()) from the index database,
     * history cache and xref.
     *
     * @param removeHistory if false, do not remove history cache for this file
     * @throws java.io.IOException if an error occurs
     */
    private void removeFile(boolean removeHistory) throws IOException {
        String path = Util.uid2url(uidIter.term().utf8ToString());

        for (IndexChangedListener listener : listeners) {
            listener.fileRemove(path);
        }

        writer.deleteDocuments(new Term(QueryBuilder.U, uidIter.term()));        
        writer.prepareCommit();
        writer.commit();

        removeXrefFile(path);
        if (removeHistory) {
            removeHistoryFile(path);
        }

        setDirty();
        for (IndexChangedListener listener : listeners) {
            listener.fileRemoved(path);
        }
    }

    /**
     * Add a file to the Lucene index (and generate a xref file)
     *
     * @param file The file to add
     * @param path The path to the file (from source root)
     * @throws java.io.IOException if an error occurs
     */
    private void addFile(File file, String path) throws IOException {
        FileAnalyzer fa;
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            fa = AnalyzerGuru.getAnalyzer(in, path);
        }

        for (IndexChangedListener listener : listeners) {
            listener.fileAdd(path, fa.getClass().getSimpleName());
        }
        fa.setCtags(ctags);
        fa.setProject(Project.getProject(path));
        fa.setScopesEnabled(RuntimeEnvironment.getInstance().isScopesEnabled());
        fa.setFoldingEnabled(RuntimeEnvironment.getInstance().isFoldingEnabled());

        Document doc = new Document();
        try (Writer xrefOut = getXrefWriter(fa, path)) {
            analyzerGuru.populateDocument(doc, file, path, fa, xrefOut);
        } catch (Exception e) {
            LOGGER.log(Level.INFO,
                    "Skipped file ''{0}'' because the analyzer didn''t "
                    + "understand it.",
                    path);
            LOGGER.log(Level.FINE,
                    "Exception from analyzer " + fa.getClass().getName(), e);
            cleanupResources(doc);
            return;
        }

        try {
            writer.addDocument(doc);
        } catch (Throwable t) {
            cleanupResources(doc);
            throw t;
        }

        setDirty();
        for (IndexChangedListener listener : listeners) {
            listener.fileAdded(path, fa.getClass().getSimpleName());
        }
    }

    /**
     * Do a best effort to clean up all resources allocated when populating
     * a Lucene document. On normal execution, these resources should be
     * closed automatically by the index writer once it's done with them, but
     * we may not get that far if something fails.
     *
     * @param doc the document whose resources to clean up
     */
    private void cleanupResources(Document doc) {
        for (IndexableField f : doc) {
            // If the field takes input from a reader, close the reader.
            IOUtils.close(f.readerValue());

            // If the field takes input from a token stream, close the
            // token stream.
            if (f instanceof Field) {
                IOUtils.close(((Field) f).tokenStreamValue());
            }
        }
    }

    /**
     * Check if I should accept this file into the index database
     *
     * @param file the file to check
     * @return true if the file should be included, false otherwise
     */
    private boolean accept(File file) {

        if (!includedNames.isEmpty()
                && // the filter should not affect directory names
                (!(file.isDirectory() || includedNames.match(file)))) {
            return false;
        }

        String absolutePath = file.getAbsolutePath();

        if (ignoredNames.ignore(file)) {
            LOGGER.log(Level.FINER, "ignoring {0}", absolutePath);
            return false;
        }

        if (!file.canRead()) {
            LOGGER.log(Level.WARNING, "Could not read {0}", absolutePath);
            return false;
        }

        try {
            String canonicalPath = file.getCanonicalPath();
            if (!absolutePath.equals(canonicalPath)
                && !acceptSymlink(absolutePath, canonicalPath)) {

                LOGGER.log(Level.FINE, "Skipped symlink ''{0}'' -> ''{1}''",
                    new Object[]{absolutePath, canonicalPath});
                return false;
            }
            //below will only let go files and directories, anything else is considered special and is not added
            if (!file.isFile() && !file.isDirectory()) {
                LOGGER.log(Level.WARNING, "Ignored special file {0}",
                    absolutePath);
                return false;
            }
        } catch (IOException exp) {
            LOGGER.log(Level.WARNING, "Failed to resolve name: {0}",
                absolutePath);
            LOGGER.log(Level.FINE, "Stack Trace: ", exp);
        }

        if (file.isDirectory()) {
            // always accept directories so that their files can be examined
            return true;
        }

        if (HistoryGuru.getInstance().hasHistory(file)) {
            // versioned files should always be accepted
            return true;
        }

        // this is an unversioned file, check if it should be indexed
        return !RuntimeEnvironment.getInstance().isIndexVersionedFilesOnly();
    }

    boolean accept(File parent, File file) {
        try {
            File f1 = parent.getCanonicalFile();
            File f2 = file.getCanonicalFile();
            if (f1.equals(f2)) {
                LOGGER.log(Level.INFO, "Skipping links to itself...: {0} {1}",
                        new Object[]{parent.getAbsolutePath(), file.getAbsolutePath()});
                return false;
            }

            // Now, let's verify that it's not a link back up the chain...
            File t1 = f1;
            while ((t1 = t1.getParentFile()) != null) {
                if (f2.equals(t1)) {
                    LOGGER.log(Level.INFO, "Skipping links to parent...: {0} {1}",
                            new Object[]{parent.getAbsolutePath(), file.getAbsolutePath()});
                    return false;
                }
            }

            return accept(file);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to resolve name: {0} {1}",
                    new Object[]{parent.getAbsolutePath(), file.getAbsolutePath()});
        }
        return false;
    }

    /**
     * Check if I should accept the path containing a symlink
     *
     * @param absolutePath the path with a symlink to check
     * @param canonicalPath the canonical path to the file
     * @return true if the file should be accepted, false otherwise
     */
    private boolean acceptSymlink(String absolutePath, String canonicalPath) throws IOException {
        // Always accept local symlinks
        if (isLocal(canonicalPath)) {
            return true;
        }

        for (String allowedSymlink : RuntimeEnvironment.getInstance().getAllowedSymlinks()) {
            if (absolutePath.startsWith(allowedSymlink)) {
                String allowedTarget = new File(allowedSymlink).getCanonicalPath();
                if (canonicalPath.startsWith(allowedTarget)
                        && absolutePath.substring(allowedSymlink.length()).equals(canonicalPath.substring(allowedTarget.length()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a file is local to the current project. If we don't have
     * projects, check if the file is in the source root.
     *
     * @param path the path to a file
     * @return true if the file is local to the current repository
     */
    private boolean isLocal(String path) {
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        String srcRoot = env.getSourceRootPath();

        boolean local = false;

        if (path.startsWith(srcRoot)) {
            if (env.hasProjects()) {
                String relPath = path.substring(srcRoot.length());
                if (project.equals(Project.getProject(relPath))) {
                    // File is under the current project, so it's local.
                    local = true;
                }
            } else {
                // File is under source root, and we don't have projects, so
                // consider it local.
                local = true;
            }
        }

        return local;
    }

    private void printProgress(int currentCount, int totalCount) {
        if (RuntimeEnvironment.getInstance().isPrintProgress()
            && totalCount > 0 && LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "Progress: {0} ({1}%)",
                    new Object[]{currentCount,
                    (currentCount * 100.0f / totalCount)});
        }
    }

    /**
     * Generate indexes recursively
     *
     * @param dir the root indexDirectory to generate indexes for
     * @param parent path to parent directory
     * @param count_only if true will just traverse the source root and count
     * files
     * @param cur_count current file count during the traversal of the tree
     * @param est_total estimate total files to process
     * @return current file count
     */
    private int indexDown(File dir, String parent, boolean count_only,
        int cur_count, int est_total) throws IOException {

        int lcur_count = cur_count;
        if (isInterrupted()) {
            return lcur_count;
        }

        if (!accept(dir)) {
            return lcur_count;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            LOGGER.log(Level.SEVERE, "Failed to get file listing for: {0}",
                dir.getAbsolutePath());
            return lcur_count;
        }
        Arrays.sort(files, fileComparator);

        for (File file : files) {
            if (accept(dir, file)) {
                String path = parent + '/' + file.getName();

                if (file.isDirectory()) {
                    lcur_count = indexDown(file, path, count_only, lcur_count, est_total);
                } else {
                    lcur_count++;
                    if (count_only) {
                        continue;
                    }

                    printProgress(lcur_count, est_total);

                    if (uidIter != null) {
                        String uid = Util.path2uid(path,
                            DateTools.timeToString(file.lastModified(),
                            DateTools.Resolution.MILLISECOND)); // construct uid for doc
                        BytesRef buid = new BytesRef(uid);
                        // Traverse terms that have smaller UID than the current
                        // file, i.e. given the ordering they positioned before the file
                        // or it is the file that has been modified.
                        while (uidIter != null && uidIter.term() != null 
                                && uidIter.term().compareTo(emptyBR) != 0
                                && uidIter.term().compareTo(buid) < 0) {

                            // If the term's path matches path of currently processed file,
                            // it is clear that the file has been modified and thus
                            // removeFile() will be followed by call to addFile() below.
                            // In such case, instruct removeFile() not to remove history
                            // cache for the file so that incremental history cache
                            // generation works.
                            String termPath = Util.uid2url(uidIter.term().utf8ToString());
                            removeFile(!termPath.equals(path));

                            BytesRef next = uidIter.next();
                            if (next == null) {
                                uidIter = null;
                            }
                        }

                        // If the file was not modified, skip to the next one.
                        if (uidIter != null && uidIter.term() != null
                                && uidIter.term().bytesEquals(buid)) {
                            BytesRef next = uidIter.next(); // keep matching docs
                            if (next == null) {
                                uidIter = null;
                            }
                            continue;
                        }
                    }
                    try {
                        addFile(file, path);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING,
                                "Failed to add file " + file.getAbsolutePath(),
                                e);
                    }
                }
            }
        }

        return lcur_count;
    }

    /**
     * Interrupt the index generation (and the index generation will stop as
     * soon as possible)
     */
    public void interrupt() {
        synchronized (lock) {
            interrupted = true;
        }
    }

    private boolean isInterrupted() {
        synchronized (lock) {
            return interrupted;
        }
    }

    /**
     * Register an object to receive events when modifications is done to the
     * index database.
     *
     * @param listener the object to receive the events
     */
    public void addIndexChangedListener(IndexChangedListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove an object from the lists of objects to receive events when
     * modifications is done to the index database
     *
     * @param listener the object to remove
     */
    public void removeIndexChangedListener(IndexChangedListener listener) {
        listeners.remove(listener);
    }

    /**
     * Get all files in all of the index databases.
     *
     * @throws IOException if an error occurs
     * @return set of files
     */
    public static Set<String> getAllFiles() throws IOException {
        return getAllFiles(null);
    }

    /**
     * List all files in some of the index databases.
     *
     * @param subFiles Subdirectories for the various projects to list the files
     * for (or null or an empty list to dump all projects)
     * @throws IOException if an error occurs
     * @return set of files in the index databases specified by the subFiles parameter
     */
    public static Set<String> getAllFiles(List<String> subFiles) throws IOException {
        Set<String> files = new HashSet<>();
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();

        if (env.hasProjects()) {
            if (subFiles == null || subFiles.isEmpty()) {
                for (Project project : env.getProjectList()) {
                    IndexDatabase db = new IndexDatabase(project);
                    files.addAll(db.getFiles());
                }
            } else {
                for (String path : subFiles) {
                    Project project = Project.getProject(path);
                    if (project == null) {
                        LOGGER.log(Level.WARNING, "Could not find a project for \"{0}\"", path);
                    } else {
                        IndexDatabase db = new IndexDatabase(project);
                        files.addAll(db.getFiles());
                    }
                }
            }
        } else {
            IndexDatabase db = new IndexDatabase();
            files = db.getFiles();
        }

        return files;
    }

    /**
     * Get all files in this index database.
     *
     * @throws IOException If an IO error occurs while reading from the database
     * @return set of files in this index database
     */
    public Set<String> getFiles() throws IOException {
        IndexReader ireader = null;
        TermsEnum iter;
        Terms terms = null;
        Set<String> files = new HashSet<>();

        try {
            ireader = DirectoryReader.open(indexDirectory); // open existing index
            int numDocs = ireader.numDocs();
            if (numDocs > 0) {
                Fields uFields = MultiFields.getFields(ireader);//reader.getTermVectors(0);
                terms = uFields.terms(QueryBuilder.U);
            }
            iter = terms.iterator(); // init uid iterator
            while (iter != null && iter.term() != null) {
                files.add(Util.uid2url(iter.term().utf8ToString()));
                BytesRef next = iter.next();
                if (next == null) {
                    iter = null;
                }
            }
        } finally {
            if (ireader != null) {
                try {
                    ireader.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "An error occured while closing index reader", e);
                }
            }
        }

        return files;
    }

    /**
     * Get number of documents in this index database.
     * @return number of documents
     */
    public int getNumFiles() throws IOException {
        IndexReader ireader = null;
        int numDocs = 0;
        
        try {
            ireader = DirectoryReader.open(indexDirectory); // open existing index
            numDocs = ireader.numDocs();
        } finally {
            if (ireader != null) {
                try {
                    ireader.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "An error occured while closing index reader", e);
                }
            }
        }
        
        return numDocs;
    }

    static void listFrequentTokens() throws IOException {
        listFrequentTokens(null);
    }

    static void listFrequentTokens(List<String> subFiles) throws IOException {
        final int limit = 4;

        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        if (env.hasProjects()) {
            if (subFiles == null || subFiles.isEmpty()) {
                for (Project project : env.getProjectList()) {
                    IndexDatabase db = new IndexDatabase(project);
                    db.listTokens(limit);
                }
            } else {
                for (String path : subFiles) {
                    Project project = Project.getProject(path);
                    if (project == null) {
                        LOGGER.log(Level.WARNING, "Could not find a project for \"{0}\"", path);
                    } else {
                        IndexDatabase db = new IndexDatabase(project);
                        db.listTokens(limit);
                    }
                }
            }
        } else {
            IndexDatabase db = new IndexDatabase();
            db.listTokens(limit);
        }
    }

    public void listTokens(int freq) throws IOException {
        IndexReader ireader = null;
        TermsEnum iter;
        Terms terms = null;         

        try {
            ireader = DirectoryReader.open(indexDirectory);
            int numDocs = ireader.numDocs();
            if (numDocs > 0) {
                Fields uFields = MultiFields.getFields(ireader);//reader.getTermVectors(0);
                terms = uFields.terms(QueryBuilder.DEFS);
            }
            iter = terms.iterator(); // init uid iterator            
            while (iter != null && iter.term() != null) {
                //if (iter.term().field().startsWith("f")) {
                if (iter.docFreq() > 16 && iter.term().utf8ToString().length() > freq) {
                    LOGGER.warning(iter.term().utf8ToString());
                }
                BytesRef next = iter.next();
                if (next==null) {iter=null;}
                /*} else {
                 break;
                 }*/
            }
        } finally {

            if (ireader != null) {
                try {
                    ireader.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "An error occured while closing index reader", e);
                }
            }
        }
    }

    /**
     * Get an indexReader for the Index database where a given file
     *
     * @param path the file to get the database for
     * @return The index database where the file should be located or null if it
     * cannot be located.
     */
    public static IndexReader getIndexReader(String path) {
        IndexReader ret = null;

        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        File indexDir = new File(env.getDataRootFile(), INDEX_DIR);

        if (env.hasProjects()) {
            Project p = Project.getProject(path);
            if (p == null) {
                return null;
            }
            indexDir = new File(indexDir, p.getPath());
        }
        try {
            FSDirectory fdir = FSDirectory.open(indexDir.toPath(), NoLockFactory.INSTANCE);
            if (indexDir.exists() && DirectoryReader.indexExists(fdir)) {
                ret = DirectoryReader.open(fdir);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to open index: {0}", indexDir.getAbsolutePath());
            LOGGER.log(Level.FINE, "Stack Trace: ", ex);
        }
        return ret;
    }

    /**
     * Get the latest definitions for a file from the index.
     *
     * @param file the file whose definitions to find
     * @return definitions for the file, or {@code null} if they could not be
     * found
     * @throws IOException if an error happens when accessing the index
     * @throws ParseException if an error happens when building the Lucene query
     * @throws ClassNotFoundException if the class for the stored definitions
     * instance cannot be found
     */
    public static Definitions getDefinitions(File file)
            throws IOException, ParseException, ClassNotFoundException {
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        String path = env.getPathRelativeToSourceRoot(file);
        //sanitize windows path delimiters
        //in order not to conflict with Lucene escape character
        path=path.replace("\\", "/");

        IndexReader ireader = getIndexReader(path);

        if (ireader == null) {
            // No index, no definitions...
            return null;
        }

        try {
            Query q = new QueryBuilder().setPath(path).build();
            IndexSearcher searcher = new IndexSearcher(ireader);
            TopDocs top = searcher.search(q, 1);
            if (top.totalHits == 0) {
                // No hits, no definitions...
                return null;
            }
            Document doc = searcher.doc(top.scoreDocs[0].doc);
            String foundPath = doc.get(QueryBuilder.PATH);

            // Only use the definitions if we found an exact match.
            if (path.equals(foundPath)) {
                IndexableField tags = doc.getField(QueryBuilder.TAGS);
                if (tags != null) {
                    return Definitions.deserialize(tags.binaryValue().bytes);
                }
            }
        } finally {
            ireader.close();
        }

        // Didn't find any definitions.
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IndexDatabase other = (IndexDatabase) obj;
        if (this.project != other.project && (this.project == null || !this.project.equals(other.project))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (this.project == null ? 0 : this.project.hashCode());
        return hash;
    }

    /**
     * Get a writer to which the xref can be written, or null if no xref
     * should be produced for files of this type.
     */
    private Writer getXrefWriter(FileAnalyzer fa, String path) throws IOException {
        Genre g = fa.getFactory().getGenre();
        if (xrefDir != null && (g == Genre.PLAIN || g == Genre.XREFABLE)) {
            File xrefFile = new File(xrefDir, path);
            // If mkdirs() returns false, the failure is most likely
            // because the file already exists. But to check for the
            // file first and only add it if it doesn't exists would
            // only increase the file IO...
            if (!xrefFile.getParentFile().mkdirs()) {
                assert xrefFile.getParentFile().exists();
            }

            RuntimeEnvironment env = RuntimeEnvironment.getInstance();

            boolean compressed = env.isCompressXref();
            File file = new File(xrefDir, path + (compressed ? ".gz" : ""));
            return new BufferedWriter(new OutputStreamWriter(
                    compressed ?
                        new GZIPOutputStream(new FileOutputStream(file)) :
                        new FileOutputStream(file)));
        }

        // no Xref for this analyzer
        return null;
    }
}
