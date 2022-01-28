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
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.history;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.opensolaris.opengrok.configuration.Configuration.RemoteSCM;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.index.IgnoredNames;
import org.opensolaris.opengrok.logger.LoggerFactory;
import org.opensolaris.opengrok.util.Statistics;

/**
 * The HistoryGuru is used to implement an transparent layer to the various
 * source control systems.
 *
 * @author Chandan
 */
public final class HistoryGuru {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryGuru.class);

    /**
     * The one and only instance of the HistoryGuru
     */
    private static final HistoryGuru INSTANCE = new HistoryGuru();

    /**
     * The history cache to use
     */
    private final HistoryCache historyCache;

    /**
     * map of repositories, with {@code DirectoryName} as key
     */
    private Map<String, Repository> repositories = new ConcurrentHashMap<>();

    private final int scanningDepth;
    
    /**
     * Creates a new instance of HistoryGuru, and try to set the default source
     * control system.
     */
    private HistoryGuru() {
        HistoryCache cache = null;
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        scanningDepth = env.getScanningDepth();

        if (env.useHistoryCache()) {
            cache = new FileHistoryCache();

            try {
                cache.initialize();
            } catch (HistoryException he) {
                LOGGER.log(Level.WARNING,
                        "Failed to initialize the history cache", he);
                // Failed to initialize, run without a history cache
                cache = null;
            }
        }
        historyCache = cache;
    }

    /**
     * Get the one and only instance of the HistoryGuru
     *
     * @return the one and only HistoryGuru instance
     */
    public static HistoryGuru getInstance() {
        return INSTANCE;
    }

    /**
     * Return whether or not a cache should be used for the history log.
     *
     * @return {@code true} if the history cache has been enabled and
     * initialized, {@code false} otherwise
     */
    private boolean useCache() {
        return historyCache != null;
    }

    /**
     * Get a string with information about the history cache.
     *
     * @return a free form text string describing the history cache instance
     * @throws HistoryException if an error occurred while getting the info
     */
    public String getCacheInfo() throws HistoryException {
        return historyCache == null ? "No cache" : historyCache.getInfo();
    }

    /**
     * Annotate the specified revision of a file.
     *
     * @param file the file to annotate
     * @param rev the revision to annotate (<code>null</code> means BASE)
     * @return file annotation, or <code>null</code> if the
     * <code>HistoryParser</code> does not support annotation
     * @throws IOException
     */
    public Annotation annotate(File file, String rev) throws IOException {
        Annotation ret = null;

        Repository repo = getRepository(file);
        if (repo != null) {
            ret = repo.annotate(file, rev);
            History hist = null;
            try {
                hist = repo.getHistory(file);
            } catch (HistoryException ex) {
                LOGGER.log(Level.FINEST,
                        "Cannot get messages for tooltip: ", ex);
            }
            if (hist != null && ret != null) {
                Set<String> revs = ret.getRevisions();
                int revsMatched = 0;
             // !!! cannot do this because of not matching rev ids (keys)
                // first is the most recent one, so we need the position of "rev"
                // until the end of the list
                //if (hent.indexOf(rev)>0) {
                //     hent = hent.subList(hent.indexOf(rev), hent.size());
                //}
                for (HistoryEntry he : hist.getHistoryEntries()) {
                    String hist_rev = he.getRevision();
                    String short_rev = repo.getRevisionForAnnotate(hist_rev);
                    if (revs.contains(short_rev)) {
                        ret.addDesc(short_rev, "changeset: " + he.getRevision()
                                + "\nsummary: " + he.getMessage() + "\nuser: "
                                + he.getAuthor() + "\ndate: " + he.getDate());
                         // History entries are coming from recent to older,
                         // file version should be from oldest to newer.
                        ret.addFileVersion(short_rev, revs.size() - revsMatched);
                        revsMatched++;
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Get the appropriate history reader for given file.
     *
     * @param file The file to get the history reader for
     * @throws HistoryException If an error occurs while getting the history
     * @return A HistorReader that may be used to read out history data for a
     * named file
     */
    public HistoryReader getHistoryReader(File file) throws HistoryException {
        History history = getHistory(file, false);
        return history == null ? null : new HistoryReader(history);
    }

    /**
     * Get the history for the specified file.
     *
     * @param file the file to get the history for
     * @return history for the file
     * @throws HistoryException on error when accessing the history
     */
    public History getHistory(File file) throws HistoryException {
        return getHistory(file, true, false);
    }

    public History getHistory(File file, boolean withFiles) throws HistoryException {
        return getHistory(file, true, false);
    }

    /**
     * Get history for the specified file (called from the web app).
     *
     * @param file the file to get the history for
     * @return history for the file
     * @throws HistoryException on error when accessing the history
     */
    public History getHistoryUI(File file) throws HistoryException {
        return getHistory(file, true, true);
    }

    /**
     * Get the history for the specified file.
     *
     * @param file the file to get the history for
     * @param withFiles whether or not the returned history should contain a
     * list of files touched by each changeset (the file list may be skipped if
     * false, but it doesn't have to)
     * @param ui called from the webapp
     * @return history for the file
     * @throws HistoryException on error when accessing the history
     */
    public History getHistory(File file, boolean withFiles, boolean ui)
            throws HistoryException {
        final File dir = file.isDirectory() ? file : file.getParentFile();
        final Repository repo = getRepository(dir);

        History history = null;
        RemoteSCM rscm = RuntimeEnvironment.getInstance().getRemoteScmSupported();
        boolean doRemote = (ui && (rscm == RemoteSCM.UIONLY))
                || (rscm == RemoteSCM.ON)
                || (ui || ((rscm == RemoteSCM.DIRBASED) && (repo != null) && repo.hasHistoryForDirectories()));

        if (repo != null && repo.isWorking() && repo.fileHasHistory(file)
                && (!repo.isRemote() || doRemote)) {

            if (useCache() && historyCache.supportsRepository(repo)) {
                history = historyCache.get(file, repo, withFiles);
            } else {
                history = repo.getHistory(file);
            }
        }

        return history;
    }

    /**
     * Get a named revision of the specified file.
     *
     * @param parent The directory containing the file
     * @param basename The name of the file
     * @param rev The revision to get
     * @return An InputStream containing the named revision of the file.
     */
    public InputStream getRevision(String parent, String basename, String rev) {
        InputStream ret = null;

        Repository rep = getRepository(new File(parent));
        if (rep != null) {
            ret = rep.getHistoryGet(parent, basename, rev);
        }
        return ret;
    }

    /**
     * Does this directory contain files with source control information?
     *
     * @param file The name of the directory
     * @return true if the files in this directory have associated revision
     * history
     */
    public boolean hasHistory(File file) {
        Repository repo = getRepository(file);

        if (repo == null) {
            return false;
        }

        // This should return true for Annotate view.
        return repo.isWorking() && repo.fileHasHistory(file)
                && ((RuntimeEnvironment.getInstance().getRemoteScmSupported() == RemoteSCM.ON)
                || (RuntimeEnvironment.getInstance().getRemoteScmSupported() == RemoteSCM.UIONLY)
                || (RuntimeEnvironment.getInstance().getRemoteScmSupported() == RemoteSCM.DIRBASED)
                || !repo.isRemote());
    }

    /**
     * Check if we can annotate the specified file.
     *
     * @param file the file to check
     * @return <code>true</code> if the file is under version control and the
     * version control system supports annotation
     */
    public boolean hasAnnotation(File file) {
        if (!file.isDirectory()) {
            Repository repo = getRepository(file);
            if (repo != null && repo.isWorking()) {
                return repo.fileHasAnnotation(file);
            }
        }

        return false;
    }

    /**
     * Get the last modified times for all files and subdirectories in the
     * specified directory.
     *
     * @param directory the directory whose files to check
     * @return a map from file names to modification times for the files that
     * the history cache has information about
     * @throws org.opensolaris.opengrok.history.HistoryException
     */
    public Map<String, Date> getLastModifiedTimes(File directory)
            throws HistoryException {
        
        Repository repository = getRepository(directory);
        
        if (repository != null && useCache()) {
            return historyCache.getLastModifiedTimes(directory, repository);
        }
        
        return Collections.emptyMap();
    }

    /**
     * recursively search for repositories with a depth limit, add those found
     * to the internally used map.
     *
     * @param files list of files to check if they contain a repository
     * @param ignoredNames what files to ignore
     * @param recursiveSearch whether to use recursive search
     * @param depth current depth - using global scanningDepth - one can limit
     * this to improve scanning performance
     * @return collection of added repositories
     */
    private Collection<RepositoryInfo> addRepositories(File[] files,
            IgnoredNames ignoredNames, boolean recursiveSearch, int depth) {

        List<RepositoryInfo> repoList = new ArrayList<>();

        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }

            String path;
            try {
                path = file.getCanonicalPath();
                File skipRepository = new File(path, ".opengrok_skip_history");
                // Should potential repository be ignored?
                if (skipRepository.exists()) {
                    LOGGER.log(Level.INFO,
                        "Skipping history cache creation for {0} and its subdirectories",
                        file.getAbsolutePath());
                    continue;
                }

                Repository repository = null;
                try {
                    repository = RepositoryFactory.getRepository(file);
                } catch (InstantiationException ie) {
                    LOGGER.log(Level.WARNING, "Could not create repository for '"
                            + file + "', could not instantiate the repository.", ie);
                } catch (IllegalAccessException iae) {
                    LOGGER.log(Level.WARNING, "Could not create repository for '"
                            + file + "', missing access rights.", iae);
                }
                if (repository == null) {
                    // Not a repository, search its sub-dirs.
                    if (!ignoredNames.ignore(file)) {
                        File subFiles[] = file.listFiles();
                        if (subFiles == null) {
                            LOGGER.log(Level.WARNING,
                                    "Failed to get sub directories for ''{0}'', " +
                                    "check access permissions.",
                                    file.getAbsolutePath());
                        } else if (depth <= scanningDepth) {
                            repoList.addAll(HistoryGuru.this.addRepositories(subFiles, ignoredNames,
                                    recursiveSearch, depth + 1));
                        }
                    }
                } else {
                    repository.setDirectoryName(path);
                    if (RuntimeEnvironment.getInstance().isVerbose()) {
                        LOGGER.log(Level.CONFIG, "Adding <{0}> repository: <{1}>",
                                new Object[]{repository.getClass().getName(), path});
                    }

                    repoList.add(new RepositoryInfo(repository));
                    repositories.put(repository.getDirectoryName(), repository);

                    // @TODO: Search only for one type of repository - the one found here
                    if (recursiveSearch && repository.supportsSubRepositories()) {
                        File subFiles[] = file.listFiles();
                        if (subFiles == null) {
                            LOGGER.log(Level.WARNING,
                                    "Failed to get sub directories for ''{0}'', check access permissions.",
                                    file.getAbsolutePath());
                        } else if (depth <= scanningDepth) {
                            // Search only one level down - if not: too much
                            // stat'ing for huge Mercurial repositories
                            repoList.addAll(HistoryGuru.this.addRepositories(subFiles, ignoredNames,
                                    false, depth + 1));
                        }
                    }
                }
            } catch (IOException exp) {
                LOGGER.log(Level.WARNING,
                        "Failed to get canonical path for {0}: {1}",
                        new Object[]{file.getAbsolutePath(), exp.getMessage()});
                LOGGER.log(Level.WARNING, "Repository will be ignored...", exp);
            }
        }
        
        return repoList;
    }

    private Collection<RepositoryInfo>
        addRepositories(File[] files, Collection<RepositoryInfo> repos,
                IgnoredNames ignoredNames, int depth) {

        return HistoryGuru.this.addRepositories(files, ignoredNames, true, depth);
    }

    /**
     * Recursively search for repositories in given directories, add those found
     * to the internally used repository map.
     *
     * @param files list of directories to check if they contain a repository
     * @param ignoredNames what files to ignore
     * @return collection of added repositories
     */
    public Collection<RepositoryInfo> addRepositories(File[] files,
            IgnoredNames ignoredNames) {

        return HistoryGuru.this.addRepositories(files, ignoredNames, true, 0);
    }

    /**
     * Recursively search for repositories in given directories, add those found
     * to the internally used repository map.
     * 
     * @param repos collection of repository paths
     * @param ignoredNames what files to ignore
     * @return collection of added repositories
     */
    public Collection<RepositoryInfo> addRepositories(Collection<String> repos,
            IgnoredNames ignoredNames) {

        return HistoryGuru.this.addRepositories(repos.stream().
                map(r -> new File(r)).
                collect(Collectors.toList()).toArray(new File[0]), ignoredNames);
    }
    
    /**
     * Get collection of repositories used internally by HistoryGuru.
     * @return collection of repositories
     */
    public Collection<RepositoryInfo> getRepositories() {
        return repositories.values().stream().
                map(ri -> new RepositoryInfo(ri)).collect(Collectors.toSet());
    }
    
    /**
     * Update the source contents in all repositories.
     */
    public void updateRepositories() {
        boolean verbose = RuntimeEnvironment.getInstance().isVerbose();

        for (Map.Entry<String, Repository> entry : repositories.entrySet()) {
            Repository repository = entry.getValue();

            String path = entry.getKey();
            String type = repository.getClass().getSimpleName();

            if (repository.isWorking()) {
                if (verbose) {
                    LOGGER.info(String.format("Update %s repository in %s",
                            type, path));
                }

                try {
                    repository.update();
                } catch (UnsupportedOperationException e) {
                    LOGGER.warning(String.format("Skipping update of %s repository"
                            + " in %s: Not implemented", type, path));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "An error occured while updating "
                            + path + " (" + type + ")", e);
                }
            } else {
                LOGGER.warning(String.format("Skipping update of %s repository in "
                        + "%s: Missing SCM dependencies?", type, path));
            }
        }
    }

    /**
     * Update the source contents in given repositories.
     *
     * @param paths A list of files/directories to update
     */
    public void updateRepositories(Collection<String> paths) {
        boolean verbose = RuntimeEnvironment.getInstance().isVerbose();

        List<Repository> repos = getReposFromString(paths);

        for (Repository repository : repos) {
            String type = repository.getClass().getSimpleName();

            if (repository.isWorking()) {
                if (verbose) {
                    LOGGER.info(String.format("Update %s repository in %s", type,
                            repository.getDirectoryName()));
                }

                try {
                    repository.update();
                } catch (UnsupportedOperationException e) {
                    LOGGER.warning(String.format("Skipping update of %s repository"
                            + " in %s: Not implemented", type,
                            repository.getDirectoryName()));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "An error occured while updating "
                            + repository.getDirectoryName() + " (" + type + ")", e);
                }
            } else {
                LOGGER.warning(String.format("Skipping update of %s repository in"
                        + " %s: Missing SCM dependencies?", type,
                        repository.getDirectoryName()));
            }
        }
    }

    private void createCache(Repository repository, String sinceRevision) {
        String path = repository.getDirectoryName();
        String type = repository.getClass().getSimpleName();

        if (repository.isWorking()) {
            boolean verbose = RuntimeEnvironment.getInstance().isVerbose();
            Statistics elapsed = new Statistics();

            if (verbose) {
                LOGGER.log(Level.INFO, "Creating historycache for {0} ({1})",
                        new Object[]{path, type});
            }

            try {
                repository.createCache(historyCache, sinceRevision);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                        "An error occured while creating cache for " + path + " ("
                        + type + ")", e);
            }

            if (verbose) {
                elapsed.report(LOGGER, "Done historycache for " + path);
            }
        } else {
            LOGGER.log(Level.WARNING,
                    "Skipping creation of historycache of {0} repository in {1}: Missing SCM dependencies?",
                    new Object[]{type, path});
        }
    }

    private void createCacheReal(Collection<Repository> repositories) {
        Statistics elapsed = new Statistics();
        ExecutorService executor = RuntimeEnvironment.getHistoryExecutor();
        // Since we know each repository object from the repositories
        // collection is unique, we can abuse HashMap to create a list of
        // repository,revision tuples with repository as key (as the revision
        // string does not have to be unique - surely it is not unique
        // for the initial index case).
        HashMap<Repository, String> repos2process = new HashMap<>();

        // Collect the list of <latestRev,repo> pairs first so that we
        // do not have to deal with latch decrementing in the cycle below.
        for (final Repository repo : repositories) {
            final String latestRev;

            try {
                latestRev = historyCache.getLatestCachedRevision(repo);
                repos2process.put(repo, latestRev);
            } catch (HistoryException he) {
                LOGGER.log(Level.WARNING,
                        String.format(
                                "Failed to retrieve latest cached revision for %s",
                                repo.getDirectoryName()), he);
            }
        }

        LOGGER.log(Level.INFO, "Creating historycache for {0} repositories",
                repos2process.size());
        final CountDownLatch latch = new CountDownLatch(repos2process.size());
        for (final Map.Entry<Repository, String> entry : repos2process.entrySet()) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        createCache(entry.getKey(), entry.getValue());
                    } catch (Exception ex) {
                        // We want to catch any exception since we are in thread.
                        LOGGER.log(Level.WARNING,
                                "createCacheReal() got exception{0}", ex);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        /*
         * Wait until the history of all repositories is done. This is necessary
         * since the next phase of generating index will need the history to
         * be ready as it is recorded in Lucene index.
         */
        try {
            latch.await();
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE,
                    "latch exception{0}", ex);
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                // Wait forever
                executor.awaitTermination(999, TimeUnit.DAYS);
            } catch (InterruptedException exp) {
                LOGGER.log(Level.WARNING,
                        "Received interrupt while waiting for executor to finish", exp);
            }
        }
        RuntimeEnvironment.freeHistoryExecutor();
        try {
            /* Thread pool for handling renamed files needs to be destroyed too. */
            RuntimeEnvironment.destroyRenamedHistoryExecutor();
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE,
                    "destroying of renamed thread pool failed", ex);
        }

        // The cache has been populated. Now, optimize how it is stored on
        // disk to enhance performance and save space.
        try {
            historyCache.optimize();
        } catch (HistoryException he) {
            LOGGER.log(Level.WARNING,
                    "Failed optimizing the history cache database", he);
        }
        elapsed.report(LOGGER, "Done historycache for all repositories");
        historyCache.setHistoryIndexDone();
    }

    /**
     * Create history cache for selected repositories.
     * For this to work the repositories have to be already present in the
     * internal map, e.g. via {@code setRepositories()} or {@code addRepositories()}.
     * 
     * @param repositories list of repository paths
     */
    public void createCache(Collection<String> repositories) {
        if (!useCache()) {
            return;
        }
        createCacheReal(getReposFromString(repositories));
    }

    /**
     * Remove history data for a list of repositories.
     * Note that this just deals with the data, the map used by HistoryGuru
     * will be left intact.
     * 
     * @param repositories list of repository paths relative to source root
     * @return list of repository paths that were found and their history data removed
     * @throws HistoryException 
     */
    public List<String> clearCache(Collection<String> repositories) throws HistoryException {
        List<String> clearedRepos = new ArrayList<>();
        HistoryCache cache = historyCache;

        if (!useCache()) {
            return clearedRepos;
        }

        for (Repository r : getReposFromString(repositories)) {
            try {
                cache.clear(r);
                clearedRepos.add(r.getDirectoryName());
                LOGGER.log(Level.INFO,
                        "History cache for {0} cleared.", r.getDirectoryName());
            } catch (HistoryException e) {
                LOGGER.log(Level.WARNING,
                        "Clearing history cache for repository {0} failed: {1}",
                        new Object[]{r.getDirectoryName(), e.getLocalizedMessage()});
            }
        }

        return clearedRepos;
    }

    /**
     * Clear entry for single file from history cache.
     * @param path path to the file relative to the source root
     */
    public void clearCacheFile(String path) {
        if (!useCache()) {
            return;
        }

        historyCache.clearFile(path);
    }

    /**
     * Remove history data for a list of repositories. Those that are
     * successfully cleared are removed from the internal list of repositories.
     *
     * @param repositories list of repository paths relative to source root
     * @throws HistoryException 
     */
    public void removeCache(Collection<String> repositories) throws HistoryException {
        if (!useCache()) {
            return;
        }

        List<String> repos = clearCache(repositories);
        removeRepositories(repos);
    }

    /**
     * Create the history cache for all of the repositories
     */
    public void createCache() {
        if (!useCache()) {
            return;
        }

        createCacheReal(repositories.values());
    }

    /**
     * Lookup repositories from list of repository paths.
     * @param repositories paths to repositories relative to source root
     * @return list of repositories
     */
    private List<Repository> getReposFromString(Collection<String> repositories) {
        ArrayList<Repository> repos = new ArrayList<>();
        File srcRoot = RuntimeEnvironment.getInstance().getSourceRootFile();
        
        for (String file : repositories) {
            File f = new File(srcRoot, file);
            Repository r = getRepository(f);
            if (r == null) {
                LOGGER.log(Level.WARNING, "Could not locate a repository for {0}",
                        f.getAbsolutePath());
            } else if (!repos.contains(r)) {
                repos.add(r);
            }
        }
        
        return repos;
    }

    /**
     * Ensure that we have a directory in the cache. If it's not there, fetch
     * its history and populate the cache. If it's already there, and the cache
     * is able to tell how recent it is, attempt to update it to the most recent
     * revision.
     *
     * @param file the root path to test
     * @throws HistoryException if an error occurs while accessing the history
     * cache
     */
    public void ensureHistoryCacheExists(File file) throws HistoryException {
        if (!useCache()) {
            return;
        }

        Repository repository = getRepository(file);

        if (repository == null) {
            // no repository -> no history :(
            return;
        }

        String sinceRevision = null;

        if (historyCache.hasCacheForDirectory(file, repository)) {
            sinceRevision = historyCache.getLatestCachedRevision(repository);
            if (sinceRevision == null) {
                // Cache already exists, but we don't know how recent it is,
                // so don't do anything.
                return;
            }
        }

        // Create cache from the beginning if it doesn't exist, or update it
        // incrementally otherwise.
        createCache(getRepository(file), sinceRevision);
    }

    protected Repository getRepository(File path) {
        File file;

        try {
            file = path.getCanonicalFile();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to get canonical path for " + path, e);
            return null;
        }

        while (file != null) {
            Repository r = repositories.get(file.getAbsolutePath());
            if (r != null) {
                return r;
            }
            file = file.getParentFile();
        }

        return null;
    }

    /**
     * Remove list of repositories from the list maintained in the HistoryGuru.
     * This is much less heavyweight than {@code invalidateRepositories()}
     * since it just removes items from the map.
     * @param repos repository paths
     */
    public void removeRepositories(Collection<String> repos) {
        for (String repo : repos) {
            repositories.remove(repo);
        }
    }

    /**set
     * Set list of known repositories which match the list of directories.
     * @param repos list of repositories
     * @param dirs list of directories that might correspond to the repositories
     */
    public void invalidateRepositories(Collection<? extends RepositoryInfo> repos, List<String> dirs) {
        if (repos != null && !repos.isEmpty() && dirs != null && !dirs.isEmpty()) {
            List<RepositoryInfo> newrepos = new ArrayList<>();
            for (RepositoryInfo i : repos) {
                for (String dir : dirs) {
                    Path dirPath = new File(dir).toPath();
                    Path iPath = new File(i.getDirectoryName()).toPath();
                    if (iPath.startsWith(dirPath)) {
                        newrepos.add(i);
                    }
                }
            }
            repos = newrepos;
        }
        
        invalidateRepositories(repos);
    }
    
    /**
     * Go through the list of specified repositories and determine if they
     * are valid. Those that make it through will form the new HistoryGuru
     * internal map. This means this method should be used only if dealing
     * with whole collection of repositories.
     * <br>
     * The caller is expected to reflect the new list via {@code getRepositories()}.
     * <br>
     * The processing is done via thread pool since the operation
     * is expensive (see {@code RepositoryFactory.getRepository()}).
     *
     * @param repos collection of repositories to invalidate. 
     * If null or empty, the internal map of repositories will be cleared.
     */
    public void invalidateRepositories(Collection<? extends RepositoryInfo> repos) {
        if (repos == null || repos.isEmpty()) {
            repositories.clear();
            return;
        }
        
        Map<String, Repository> newrepos =
            Collections.synchronizedMap(new HashMap<>(repos.size()));
        Statistics elapsed = new Statistics();
        boolean verbose = RuntimeEnvironment.getInstance().isVerbose();
        
        if (verbose) {
            LOGGER.log(Level.FINE, "invalidating {0} repositories", repos.size());
        }

        /*
         * getRepository() below does various checks of the repository
         * which involves executing commands and I/O so make the checks
         * run in parallel to speed up the process.
         */
        final CountDownLatch latch = new CountDownLatch(repos.size());
        final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                    thread.setName("invalidate-repos-" + thread.getId());
                    return thread;
                }
        });

        for (RepositoryInfo rinfo : repos) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Repository r = RepositoryFactory.getRepository(rinfo);
                        if (r == null) {
                            LOGGER.log(Level.WARNING,
                                    "Failed to instantiate internal repository data for {0} in {1}",
                                    new Object[]{rinfo.getType(), rinfo.getDirectoryName()});
                        } else {
                            newrepos.put(r.getDirectoryName(), r);
                        }
                    } catch (Exception ex) {
                        // We want to catch any exception since we are in thread.
                        LOGGER.log(Level.WARNING, "Could not create " + rinfo.getType()
                            + " for '" + rinfo.getDirectoryName(), ex);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        // Wait until all repositories are validated.
        try {
            latch.await();
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "latch exception{0}", ex);
        }
        executor.shutdown();

        repositories.clear();
        repositories.putAll(newrepos);

        if (verbose) {
            elapsed.report(LOGGER, "done invalidating repositories");
        }
    }
}
