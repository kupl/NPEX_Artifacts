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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.logger.LoggerFactory;

/**
 * This is a factory class for the different repositories.
 *
 * @author austvik
 */
public final class RepositoryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryFactory.class);

    private static final Repository repositories[] = {
        new BitKeeperRepository(),
        new MercurialRepository(),
        new AccuRevRepository(),
        new BazaarRepository(),
        new GitRepository(),
        new MonotoneRepository(),
        new SubversionRepository(),
        new SCCSRepository(),
        new RazorRepository(),
        new ClearCaseRepository(),
        new PerforceRepository(),
        new RCSRepository(),
        new CVSRepository(),
        new RepoRepository(),
        new SSCMRepository(),};

    private RepositoryFactory() {
        // Factory class, should not be constructed
    }

    /**
     * Get a list of all available repository handlers.
     *
     * @return a list which contains none-{@code null} values, only.
     */
    public static List<Class<? extends Repository>> getRepositoryClasses() {
        ArrayList<Class<? extends Repository>> list
                = new ArrayList<>(repositories.length);
        for (int i = repositories.length - 1; i >= 0; i--) {
            list.add(repositories[i].getClass());
        }
        
        return list;
    }

    /**
     * Returns a repository for the given file, or null if no repository was
     * found.
     *
     * Note that the operations performed by this method take quite a long time
     * thanks to external commands being executed. For that reason, when run
     * on multiple files, it should be parallelized, e.g. like it is done in
     * {@code invalidateRepositories()}.
     *
     * @param file File that might contain a repository
     * @return Correct repository for the given file
     * @throws java.lang.InstantiationException in case we cannot create the
     * repository object
     * @throws java.lang.IllegalAccessException in case no permissions
     * to repository file
     */
    public static Repository getRepository(File file) throws InstantiationException, IllegalAccessException {
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        Repository repo = null;

        for (Repository rep : repositories) {
            if (rep.isRepositoryFor(file)) {
                repo = rep.getClass().newInstance();
                try {
                    repo.setDirectoryName(file.getCanonicalPath());
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE,
                            "Failed to get canonical path name for "
                            + file.getAbsolutePath(), e);
                }

                if (!repo.isWorking()) {
                    LOGGER.log(Level.WARNING,
                            "{0} not working (missing binaries?): {1}",
                            new Object[]{
                                repo.getClass().getSimpleName(),
                                file.getPath()
                            });
                }

                if (repo.getType() == null || repo.getType().length() == 0) {
                    repo.setType(repo.getClass().getSimpleName());
                }

                if (repo.getParent() == null || repo.getParent().length() == 0) {
                    try {
                        repo.setParent(repo.determineParent());
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING,
                                "Failed to get parent for {0}: {1}",
                                new Object[]{file.getAbsolutePath(), ex});
                    }
                }

                if (repo.getBranch() == null || repo.getBranch().length() == 0) {
                    try {
                        repo.setBranch(repo.determineBranch());
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING,
                                "Failed to get branch for {0}: {1}",
                                new Object[]{file.getAbsolutePath(), ex});
                    }
                }

                if (repo.getCurrentVersion() == null || repo.getCurrentVersion().length() == 0) {
                    try {
                        repo.setCurrentVersion(repo.determineCurrentVersion());
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING,
                                "Failed to determineCurrentVersion for {0}: {1}",
                                new Object[]{file.getAbsolutePath(), ex});
                    }
                }

                // If this repository displays tags only for files changed by tagged
                // revision, we need to prepare list of all tags in advance.
                if (env.isTagsEnabled() && repo.hasFileBasedTags()) {
                    repo.buildTagList(file);
                }

                break;
            }
        }
        
        return repo;
    }

    /**
     * Returns a repository for the given file, or null if no repository was
     * found.
     *
     * @param info Information about the repository
     * @return Correct repository for the given file
     * @throws java.lang.InstantiationException in case we cannot create the
     * repository object
     * @throws java.lang.IllegalAccessException in case no permissions to repository
     */
    public static Repository getRepository(RepositoryInfo info) throws InstantiationException, IllegalAccessException {
        return getRepository(new File(info.getDirectoryName()));
    }

    /**
     * Go through all repository types and add items to lists of ignored
     * files/directories. This way per-repository ignored entries are set
     * inside repository classes rather than globally in IgnoredFiles/Dirs.
     * Should be called after {@code setConfiguration()}.
     * 
     * @param env runtime environment
     */
    public static void initializeIgnoredNames(RuntimeEnvironment env) {
        for (Repository repo : repositories) {
            for (String file : repo.getIgnoredFiles()) {
                env.getIgnoredNames().add("f:" + file);
            }

            for (String dir : repo.getIgnoredDirs()) {
                env.getIgnoredNames().add("d:" + dir);
            }
        }
    }
}
