/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fo;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.DOMImplementation;
import org.xml.sax.Locator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.xmlgraphics.util.Service;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.fo.ElementMapping.Maker;

/**
 * This class keeps track of all configured ElementMapping implementations which are responsible
 * for properly handling all kinds of different XML namespaces.
 */
public class ElementMappingRegistry {

    /** logging instance */
    private static final Log LOG = LogFactory.getLog(ElementMappingRegistry.class);

    /**
     * Table mapping element names to the makers of objects
     * representing formatting objects.
     */
    protected Map<String, Map<String, Maker>> fobjTable
    = new java.util.HashMap<String, Map<String, Maker>>();

    /**
     * Map of mapped namespaces and their associated ElementMapping instances.
     */
    protected Map<String, ElementMapping> namespaces
    = new java.util.HashMap<String, ElementMapping>();

    /**
     * Main constructor. Adds all default element mapping as well as detects ElementMapping
     * through the Service discovery.
     * @param factory the Fop Factory
     */
    public ElementMappingRegistry(FopFactory factory) {
        // Add standard element mappings
        setupDefaultMappings();
    }

    /**
     * Sets all the element and property list mappings to their default values.
     */
    private void setupDefaultMappings() {
        // add mappings from available services
        Iterator<String> providers = Service.providerNames(ElementMapping.class);
        if (providers != null) {
            while (providers.hasNext()) {
                String mapping = providers.next();
                try {
                    addElementMapping(mapping);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Error while adding element mapping", e);
                }

            }
        }
    }

    /**
     * Add the element mapping with the given class name.
     * @param mappingClassName the class name representing the element mapping.
     * @throws IllegalArgumentException if there was not such element mapping.
     */
    public void addElementMapping(String mappingClassName)
                throws IllegalArgumentException {
        try {
            ElementMapping mapping
                = (ElementMapping)Class.forName(mappingClassName).getDeclaredConstructor().newInstance();
            addElementMapping(mapping);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find "
                                               + mappingClassName);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate "
                                               + mappingClassName);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Could not access "
                                               + mappingClassName);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(mappingClassName
                                               + " is not an ElementMapping");
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Add the element mapping.
     * @param mapping the element mapping instance
     */
    public void addElementMapping(ElementMapping mapping) {
        this.fobjTable.put(mapping.getNamespaceURI(), mapping.getTable());
        this.namespaces.put(mapping.getNamespaceURI().intern(), mapping);
    }

    /**
     * Finds the Maker used to create node objects of a particular type
     * @param namespaceURI URI for the namespace of the element
     * @param localName name of the Element
     * @param locator the Locator instance for context information
     * @return the ElementMapping.Maker that can create an FO object for this element
     * @throws FOPException if a Maker could not be found for a bound namespace.
     */
    public Maker findFOMaker(String namespaceURI, String localName, Locator locator)
                throws FOPException {
        Map<String, Maker> table = fobjTable.get(namespaceURI);
        Maker fobjMaker = null;
        if (table != null) {
            fobjMaker = table.get(localName);
            // try default
            if (fobjMaker == null) {
                fobjMaker = table.get(ElementMapping.DEFAULT);
            }
        }

        if (fobjMaker == null) {
            if (namespaces.containsKey(namespaceURI.intern())) {
                  throw new FOPException(FONode.errorText(locator)
                      + "No element mapping definition found for "
                      + FONode.getNodeString(namespaceURI, localName), locator);
            } else {
                fobjMaker = new UnknownXMLObj.Maker(namespaceURI);
            }
        }
        return fobjMaker;
    }

    /**
     * Tries to determine the DOMImplementation that is used to handled a particular namespace.
     * The method may return null for namespaces that don't result in a DOM. It is mostly used
     * in namespaces occurring in foreign objects.
     * @param namespaceURI the namespace URI
     * @return the handling DOMImplementation, or null if not applicable
     */
    public DOMImplementation getDOMImplementationForNamespace(String namespaceURI) {
        ElementMapping mapping = this.namespaces.get(namespaceURI);
        if (mapping == null) {
            return null;
        } else {
            return mapping.getDOMImplementation();
        }
    }

    /**
     * Returns an ElementMapping class for a namespace URI if there is one.
     * @param namespaceURI the namespace URI
     * @return the requested ElementMapping or null, if no ElementMapping for the namespace is
     *         available.
     */
    public ElementMapping getElementMapping(String namespaceURI) {
        return this.namespaces.get(namespaceURI);
    }

    /**
     * Indicates whether a namespace is known to FOP.
     * @param namespaceURI the namespace URI
     * @return true if the namespace is known.
     */
    public boolean isKnownNamespace(String namespaceURI) {
        return this.namespaces.containsKey(namespaceURI);
    }
}
