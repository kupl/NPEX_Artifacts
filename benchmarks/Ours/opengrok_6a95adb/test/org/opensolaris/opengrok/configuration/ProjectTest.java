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

package org.opensolaris.opengrok.configuration;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import junit.framework.AssertionFailedError;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProjectTest {
    /**
     * Test that a {@code Project} instance can be encoded and decoded
     * without errors. Bug #3077.
     */
    @Test
    public void testEncodeDecode() {
        // Create an exception listener to detect errors while encoding and
        // decoding
        final LinkedList<Exception> exceptions = new LinkedList<Exception>();
        ExceptionListener listener = new ExceptionListener() {
            public void exceptionThrown(Exception e) {
                exceptions.addLast(e);
            }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLEncoder enc = new XMLEncoder(out);
        enc.setExceptionListener(listener);
        Project p1 = new Project("foo");
        enc.writeObject(p1);
        enc.close();

        // verify that the write didn't fail
        if (!exceptions.isEmpty()) {
            AssertionFailedError afe = new AssertionFailedError(
                    "Got " + exceptions.size() + " exception(s)");
            // Can only chain one of the exceptions. Take the first one.
            afe.initCause(exceptions.getFirst());
            throw afe;
        }

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        XMLDecoder dec = new XMLDecoder(in, null, listener);
        Project p2 = (Project) dec.readObject();
        assertNotNull(p2);
        dec.close();

        // verify that the read didn't fail
        if (!exceptions.isEmpty()) {
            AssertionFailedError afe = new AssertionFailedError(
                    "Got " + exceptions.size() + " exception(s)");
            // Can only chain one of the exceptions. Take the first one.
            afe.initCause(exceptions.getFirst());
            throw afe;
        }
    }

    /**
     * Test project matching.
     */
    @Test
    public void testGetProject() {
        // Create 2 projects, one being prefix of the other.
        Project foo = new Project("Project foo", "/foo");
        Project bar = new Project("Project foo-bar", "/foo-bar");

        // Make the runtime environment aware of these two projects.
        HashMap<String,Project> projects = new HashMap<>();
        projects.put("foo", foo);
        projects.put("bar", bar);
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        env.setProjectsEnabled(true);
        env.setProjects(projects);

        // The matching of project name to project should be exact.
        assertEquals(foo, Project.getProject("/foo"));
        assertEquals(bar, Project.getProject("/foo-bar"));
        assertEquals(foo, Project.getProject("/foo/blah.c"));
        assertEquals(bar, Project.getProject("/foo-bar/ha.c"));
        assertNull(Project.getProject("/foof"));
        assertNull(Project.getProject("/foof/ha.c"));
    }

    /**
     * Test getProjectDescriptions().
     */
    @Test
    public void testGetProjectDescriptions() {
        // Create 2 projects.
        Project foo = new Project("foo", "/foo");
        Project bar = new Project("bar", "/bar");

        // Make the runtime environment aware of these two projects.
        HashMap<String,Project> projects = new HashMap<>();
        projects.put("foo", foo);
        projects.put("bar", bar);
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        env.setProjects(projects);

        List<String> descs = env.getProjectNames();
        assertTrue(descs.contains("foo"));
        assertTrue(descs.contains("bar"));
        assertFalse(descs.contains("foobar"));
        assertEquals(2, descs.size());
    }
}
