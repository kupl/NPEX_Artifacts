/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.osgi;

import java.util.HashMap;
import java.util.Map;

import org.junit.rules.ExternalResource;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * @author msicker
 * @version 1.0.0
 */
public class OsgiRule extends ExternalResource {

    private final FrameworkFactory factory;
    private Framework framework;

    public OsgiRule(final FrameworkFactory factory) {
        this.factory = factory;
    }

    public Framework getFramework() {
        return framework;
    }

    @Override
    protected void before() throws Throwable {
        final Map<String, String> configMap = new HashMap<>(2);
        // Cleans framework before first init. Subsequent init invocations do not clean framework.
        configMap.put("org.osgi.framework.storage.clean", "onFirstInit");
        // Delegates loading of endorsed libraries to JVM classloader
        // config.put("org.osgi.framework.bootdelegation", "javax.*,org.w3c.*,org.xml.*");
        framework = factory.newFramework(configMap);
        framework.init();
        framework.start();
    }

    @Override
    protected void after() {
        if (framework != null) {
            try {
                framework.stop();
            } catch (final BundleException e) {
                throw new RuntimeException(e);
            } finally {
                framework = null;
            }
        }
    }
}
