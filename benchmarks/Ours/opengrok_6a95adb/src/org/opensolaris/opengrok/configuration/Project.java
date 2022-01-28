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
  * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.opensolaris.opengrok.util.ClassUtil;

/**
 * Placeholder for the information that builds up a project
 */
public class Project implements Comparable<Project>, Nameable, Serializable {

    private static final long serialVersionUID = 1L;

    static {
        ClassUtil.remarkTransientFields(Project.class);
    }

    private String path;

    /**
     * This variable is very important, since it's used as the project
     * identifier all over xrefs and webapp.
     */
    private String name;

    /**
     * Size of tabs in this project. Used for displaying the xrefs correctly in
     * projects with non-standard tab size.
     */
    private int tabSize;

    /**
     * A flag if the navigate window should be opened by default when browsing
     * the source code of this project.
     */
    private boolean navigateWindowEnabled = false;

    /**
     * This marks the project as (not)ready before initial index is done.
     * this is to avoid all/multi-project searches referencing this project
     * from failing.
     */
    private boolean indexed = false;

    /**
     * Set of groups which match this project.
     */
    private transient Set<Group> groups = new TreeSet<>();

    // This empty constructor is needed for serialization.
    public Project() {
    }

    public Project(String name) {
        this.name = name;
    }

    public Project(String name, String path) {
        this.name = name;
        this.path = path;
    }

    /**
     * Get a textual name of this project
     *
     * @return a textual name of the project
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the path (relative from source root) where this project is located
     *
     * @return the relative path
     */
    public String getPath() {
        return path;
    }

    public boolean isIndexed() {
        return indexed;
    }

    /**
     * Get the project id
     *
     * @return the id of the project
     */
    public String getId() {
        return path;
    }

    /**
     * Get the tab size for this project, if tab size has been set.
     *
     * @return tab size if set, 0 otherwise
     * @see #hasTabSizeSetting()
     */
    public int getTabSize() {
        return tabSize;
    }

    /**
     * Set a textual name of this project, preferably don't use " , " in the
     * name, since it's used as delimiter for more projects
     *
     * XXX we should not allow setting project name after it has been
     * constructed because it is probably part of HashMap.
     *
     * @param name a textual name of the project
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the path (relative from source root) this project is located It seems
     * that you should ALWAYS prefix the path with current file.separator ,
     * current environment should always have it set up
     *
     * @param path the relative path from source sroot where this project is
     * located.
     */
    public void setPath(String path) {
        this.path = path;
    }

    public void setIndexed(boolean flag) {
        this.indexed = flag;
    }

    /**
     * Set tab size for this project. Used for expanding tabs to spaces in
     * xrefs.
     *
     * @param tabSize the size of tabs in this project
     */
    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
    }

    /**
     * Has this project an explicit tab size setting?
     *
     * @return {@code true} if the tab size has been set for this project, or
     * {@code false} if it hasn't and the default should be used
     */
    public boolean hasTabSizeSetting() {
        return tabSize > 0;
    }

    /**
     * Indicate whether the navigate window should be opened by default when
     * browsing a source code from this project.
     *
     * @return true if yes; false otherwise
     */
    public boolean isNavigateWindowEnabled() {
        return navigateWindowEnabled;
    }

    /**
     * Set the value of navigateWindowEnabled
     *
     * @param navigateWindowEnabled new value of navigateWindowEnabled
     */
    public void setNavigateWindowEnabled(boolean navigateWindowEnabled) {
        this.navigateWindowEnabled = navigateWindowEnabled;
    }

    /**
     * Return groups where this project belongs
     *
     * @return set of groups|empty if none
     */
    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    /**
     * Adds a group where this project belongs
     *
     * @param group group to add
     */
    public void addGroup(Group group) {
        while (group != null) {
            this.groups.add(group);
            group = group.getParent();
        }
    }

    /**
     * Get the project for a specific file
     *
     * @param path the file to lookup (relative to source root)
     * @return the project that this file belongs to (or null if the file
     * doesn't belong to a project)
     */
    public static Project getProject(String path) {
        // Try to match each project path as prefix of the given path.
        final RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        if (env.hasProjects()) {
            final String lpath = path.replace(File.separatorChar, '/');
            for (Project p : env.getProjectList()) {
                String pp = p.getPath();
                // Check if the project's path is a prefix of the given
                // path. It has to be an exact match, or the project's path
                // must be immediately followed by a separator. "/foo" is
                // a prefix for "/foo" and "/foo/bar", but not for "/foof".
                if (lpath.startsWith(pp)
                        && (pp.length() == lpath.length()
                        || lpath.charAt(pp.length()) == '/')) {
                    return p;
                }
            }
        }

        return null;
    }

    /**
     * Get the project for a specific file
     *
     * @param file the file to lookup
     * @return the project that this file belongs to (or null if the file
     * doesn't belong to a project)
     */
    public static Project getProject(File file) {
        Project ret = null;
        try {
            ret = getProject(RuntimeEnvironment.getInstance().getPathRelativeToSourceRoot(file));
        } catch (FileNotFoundException e) { // NOPMD
            // ignore if not under source root
        } catch (IOException e) { // NOPMD
            // problem has already been logged, just return null
        }
        return ret;
    }

    /**
     * Returns project object by its name, used in webapp to figure out which
     * project is to be searched
     *
     * @param name name of the project
     * @return project that fits the name
     */
    public static Project getByName(String name) {
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        if (env.hasProjects()) {
            Project proj;
            if ((proj = env.getProjects().get(name)) != null) {
                return (proj);
            }
        }
        return null;
    }

    @Override
    public int compareTo(Project p2) {
        return getName().toUpperCase(Locale.getDefault()).compareTo(p2.getName().toUpperCase(Locale.getDefault()));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + (this.name == null ? 0 : this.name.toUpperCase(Locale.getDefault()).hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Project other = (Project) obj;
        return !(this.name != other.name
                && (this.name == null
                || !this.name.toUpperCase(Locale.getDefault()).equals(other.name.toUpperCase(Locale.getDefault()))));
    }
}
