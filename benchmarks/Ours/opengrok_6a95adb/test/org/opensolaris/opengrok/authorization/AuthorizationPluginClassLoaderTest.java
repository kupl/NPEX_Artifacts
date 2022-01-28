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
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.authorization;

import java.io.File;
import java.net.URL;
import org.junit.Assert;
import org.junit.Test;
import org.opensolaris.opengrok.configuration.Group;
import org.opensolaris.opengrok.configuration.Project;
import org.opensolaris.opengrok.web.DummyHttpServletRequest;

public class AuthorizationPluginClassLoaderTest {

    private File pluginDirectory;

    public AuthorizationPluginClassLoaderTest() {
        URL resource = AuthorizationPluginClassLoaderTest.class.getResource("plugins.jar");
        pluginDirectory = new File(resource.getFile()).getParentFile();
    }

    @Test
    public void testProhibitedPackages() {
        AuthorizationPluginClassLoader instance = new AuthorizationPluginClassLoader(null);

        try {
            instance.loadClass("java.lang.plugin.MyPlugin");
            Assert.fail("Should produce SecurityException");
        } catch (ClassNotFoundException ex) {
            Assert.fail("Should not produce ClassNotFoundException");
        } catch (SecurityException ex) {
        }

        try {
            instance.loadClass("javax.servlet.HttpServletRequest");
            Assert.fail("Should produce SecurityException");
        } catch (ClassNotFoundException ex) {
            Assert.fail("Should not produce ClassNotFoundException");
        } catch (SecurityException ex) {
        }

        try {
            instance.loadClass("org.w3c.plugin.MyPlugin");
            Assert.fail("Should produce SecurityException");
        } catch (ClassNotFoundException ex) {
            Assert.fail("Should not produce ClassNotFoundException");
        } catch (SecurityException ex) {
        }

        try {
            instance.loadClass("org.xml.plugin.MyPlugin");
            Assert.fail("Should produce SecurityException");
        } catch (ClassNotFoundException ex) {
            Assert.fail("Should not produce ClassNotFoundException");
        } catch (SecurityException ex) {
        }

        try {
            instance.loadClass("org.omg.plugin.MyPlugin");
            Assert.fail("Should produce SecurityException");
        } catch (ClassNotFoundException ex) {
            Assert.fail("Should not produce ClassNotFoundException");
        } catch (SecurityException ex) {
        }

        try {
            instance.loadClass("sun.org.plugin.MyPlugin");
            Assert.fail("Should produce SecurityException");
        } catch (ClassNotFoundException ex) {
            Assert.fail("Should not produce ClassNotFoundException");
        } catch (SecurityException ex) {
        }
    }

    @Test
    public void testProhibitedNames() {
        AuthorizationPluginClassLoader instance = new AuthorizationPluginClassLoader(null);

        try {
            instance.loadClass("org.opensolaris.opengrok.configuration.Group");
        } catch (ClassNotFoundException ex) {
            Assert.fail("Should not produce ClassNotFoundException");
        } catch (SecurityException ex) {
            Assert.fail("Should not produce SecurityException");
        } catch (Throwable e) {
        }

        try {
            instance.loadClass("org.opensolaris.opengrok.configuration.Project");
        } catch (ClassNotFoundException ex) {
            Assert.fail("Should not produce ClassNotFoundException");
        } catch (SecurityException ex) {
            Assert.fail("Should not produce SecurityException");
        } catch (Throwable e) {
        }

        try {
            instance.loadClass("org.opensolaris.opengrok.authorization.IAuthorizationPlugin");
        } catch (ClassNotFoundException ex) {
            Assert.fail("Should not produce ClassNotFoundException");
        } catch (SecurityException ex) {
            Assert.fail("Should not produce SecurityException");
        } catch (Throwable e) {
        }

        try {
            instance.loadClass("org.opensolaris.opengrok.configuration.RuntimeEnvironment");
            Assert.fail("Should produce SecurityException");
        } catch (ClassNotFoundException ex) {
            Assert.fail("Should not produce ClassNotFoundException");
        } catch (SecurityException ex) {
        } catch (Throwable e) {
        }
    }

    @Test
    public void testNonExistingPlugin() {
        AuthorizationPluginClassLoader instance
                = new AuthorizationPluginClassLoader(pluginDirectory);

        Class clazz = loadClass(instance, "org.sample.plugin.NoPlugin", true);
    }

    @Test
    public void testFalsePlugin() {
        AuthorizationPluginClassLoader instance
                = new AuthorizationPluginClassLoader(pluginDirectory);

        Class clazz = loadClass(instance, "org.sample.plugin.FalsePlugin");

        IAuthorizationPlugin plugin = getNewInstance(clazz);

        Group g = new Group("group1");
        Project p = new Project("project1");

        Assert.assertFalse(
                plugin.isAllowed(new DummyHttpServletRequest(), g)
        );
        Assert.assertFalse(
                plugin.isAllowed(new DummyHttpServletRequest(), p)
        );
    }

    @Test
    public void testTruePlugin() {
        AuthorizationPluginClassLoader instance
                = new AuthorizationPluginClassLoader(pluginDirectory);

        Class clazz = loadClass(instance, "org.sample.plugin.TruePlugin");

        IAuthorizationPlugin plugin = getNewInstance(clazz);

        Group g = new Group("group1");
        Project p = new Project("project1");

        Assert.assertTrue(
                plugin.isAllowed(new DummyHttpServletRequest(), g)
        );
        Assert.assertTrue(
                plugin.isAllowed(new DummyHttpServletRequest(), p)
        );
    }

    private IAuthorizationPlugin getNewInstance(Class c) {
        IAuthorizationPlugin plugin = null;
        try {
            plugin = (IAuthorizationPlugin) c.newInstance();
        } catch (InstantiationException ex) {
            Assert.fail("Should not produce InstantiationException");
        } catch (IllegalAccessException ex) {
            Assert.fail("Should not produce IllegalAccessException");
        } catch (Exception ex) {
            Assert.fail("Should not produce any exception");
        }
        return plugin;
    }

    private Class loadClass(AuthorizationPluginClassLoader loader, String name) {
        return loadClass(loader, name, false);
    }

    private Class loadClass(AuthorizationPluginClassLoader loader, String name, boolean shouldFail) {
        Class clazz = null;
        try {
            clazz = loader.loadClass(name);
            if (shouldFail) {
                Assert.fail("Should produce some exception");
            }
        } catch (ClassNotFoundException ex) {
            if (!shouldFail) {
                Assert.fail("Should not produce ClassNotFoundException");
            }
        } catch (SecurityException ex) {
            if (!shouldFail) {
                Assert.fail("Should not produce SecurityException");
            }
        } catch (Exception ex) {
            if (!shouldFail) {
                Assert.fail("Should not produce any exception");
            }
        }
        return clazz;
    }

}
