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

import java.io.OutputStream;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.xmlgraphics.util.QName;

import org.apache.fop.accessibility.fo.FO2StructureTreeConverter;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.fo.ElementMapping.Maker;
import org.apache.fop.fo.extensions.ExtensionElementMapping;
import org.apache.fop.fo.pagination.Root;
import org.apache.fop.render.pdf.extensions.PDFElementMapping;
import org.apache.fop.util.ContentHandlerFactory;
import org.apache.fop.util.ContentHandlerFactory.ObjectBuiltListener;
import org.apache.fop.util.ContentHandlerFactory.ObjectSource;

/**
 * SAX Handler that passes parsed data to the various
 * FO objects, where they can be used either to build
 * an FO Tree, or used by Structure Renderers to build
 * other data structures.
 */
public class FOTreeBuilder extends DefaultHandler {

    /** logging instance */
    private static final Log LOG = LogFactory.getLog(FOTreeBuilder.class);

    /** The registry for ElementMapping instances */
    protected ElementMappingRegistry elementMappingRegistry;

    /** The root of the formatting object tree */
    protected Root rootFObj;

    /** Main DefaultHandler that handles the FO namespace. */
    protected MainFOHandler mainFOHandler;

    /** Current delegate ContentHandler to receive the SAX events */
    protected ContentHandler delegate;

    /** Provides information used during tree building stage. */
    private FOTreeBuilderContext builderContext;

    /** The object that handles formatting and rendering to a stream */
    private FOEventHandler foEventHandler;

    /** The SAX locator object managing the line and column counters */
    private Locator locator;

    /** The user agent for this processing run. */
    private FOUserAgent userAgent;

    private boolean used;
    private boolean empty = true;

    private int depth;
    private boolean errorinstart;

    /**
     * <code>FOTreeBuilder</code> constructor
     *
     * @param outputFormat the MIME type of the output format to use (ex. "application/pdf").
     * @param foUserAgent   the {@link FOUserAgent} in effect for this process
     * @param stream    the <code>OutputStream</code> to direct the results to
     * @throws FOPException if the <code>FOTreeBuilder</code> cannot be properly created
     */
    public FOTreeBuilder(
                String outputFormat,
                FOUserAgent foUserAgent,
                OutputStream stream)
            throws FOPException {

        this.userAgent = foUserAgent;
        this.elementMappingRegistry = userAgent.getElementMappingRegistry();
        //This creates either an AreaTreeHandler and ultimately a Renderer, or
        //one of the RTF-, MIF- etc. Handlers.
        foEventHandler = foUserAgent.getRendererFactory().createFOEventHandler(
                foUserAgent, outputFormat, stream);
        if (userAgent.isAccessibilityEnabled()) {
            foEventHandler = new FO2StructureTreeConverter(
                    foUserAgent.getStructureTreeEventHandler(), foEventHandler);
        }
        builderContext = new FOTreeBuilderContext();
        builderContext.setPropertyListMaker(new PropertyListMaker() {
            public PropertyList make(FObj fobj, PropertyList parentPropertyList) {
                return new StaticPropertyList(fobj, parentPropertyList);
            }
        });
    }

    /** {@inheritDoc} */
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    /**
     * @return a {@link Locator} instance if it is available and not disabled
     */
    protected Locator getEffectiveLocator() {
        return (userAgent.isLocatorEnabled() ? this.locator : null);
    }

    /** {@inheritDoc} */
    public void characters(char[] data, int start, int length)
                throws SAXException {
        delegate.characters(data, start, length);
    }

    /** {@inheritDoc} */
    public void startDocument() throws SAXException {
        if (used) {
            throw new IllegalStateException("FOTreeBuilder (and the Fop class) cannot be reused."
                    + " Please instantiate a new instance.");
        }

        used = true;
        empty = true;
        rootFObj = null;    // allows FOTreeBuilder to be reused
        if (LOG.isDebugEnabled()) {
            LOG.debug("Building formatting object tree");
        }
        foEventHandler.startDocument();
        this.mainFOHandler = new MainFOHandler();
        this.mainFOHandler.startDocument();
        this.delegate = this.mainFOHandler;
    }

    /** {@inheritDoc} */
    public void endDocument() throws SAXException {
        this.delegate.endDocument();
        if (this.rootFObj == null && empty) {
            FOValidationEventProducer eventProducer
                = FOValidationEventProducer.Provider.get(userAgent.getEventBroadcaster());
            eventProducer.emptyDocument(this);
        }
        rootFObj = null;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Parsing of document complete");
        }
        foEventHandler.endDocument();
    }

    /** {@inheritDoc} */
    public void startElement(String namespaceURI, String localName, String rawName,
                             Attributes attlist) throws SAXException {
        this.depth++;
        errorinstart = false;
        try {
            delegate.startElement(namespaceURI, localName, rawName, attlist);
        } catch (SAXException e) {
            errorinstart = true;
            throw e;
        }
    }

    /** {@inheritDoc} */
    public void endElement(String uri, String localName, String rawName)
                throws SAXException {
        if (!errorinstart) {
            this.delegate.endElement(uri, localName, rawName);
            this.depth--;
            if (depth == 0) {
                if (delegate != mainFOHandler) {
                    //Return from sub-handler back to main handler
                    delegate.endDocument();
                    delegate = mainFOHandler;
                    delegate.endElement(uri, localName, rawName);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public void warning(SAXParseException e) {
        LOG.warn(e.getLocalizedMessage());
    }

    /** {@inheritDoc} */
    public void error(SAXParseException e) {
        LOG.error(e.toString());
    }

    /** {@inheritDoc} */
    public void fatalError(SAXParseException e) throws SAXException {
        LOG.error(e.toString());
        throw e;
    }

    /**
     * Provides access to the underlying {@link FOEventHandler} object.
     *
     * @return the FOEventHandler object
     */
    public FOEventHandler getEventHandler() {
        return foEventHandler;
    }

    /**
     * Returns the results of the rendering process. Information includes
     * the total number of pages generated and the number of pages per
     * page-sequence.
     *
     * @return the results of the rendering process.
     */
    public FormattingResults getResults() {
        return getEventHandler().getResults();
    }

    /**
     * Main <code>DefaultHandler</code> implementation which builds the FO tree.
     */
    private class MainFOHandler extends DefaultHandler {

        /** Current formatting object being handled */
        protected FONode currentFObj;

        /** Current propertyList for the node being handled */
        protected PropertyList currentPropertyList;

        /** Current marker nesting-depth */
        private int nestedMarkerDepth;

        /** {@inheritDoc} */
        public void startElement(String namespaceURI, String localName, String rawName,
                                 Attributes attlist) throws SAXException {

            /* the node found in the FO document */
            FONode foNode;
            PropertyList propertyList = null;

            // Check to ensure first node encountered is an fo:root
            if (rootFObj == null) {
                empty = false;
                if (!namespaceURI.equals(FOElementMapping.URI)
                        || !localName.equals("root")) {
                    FOValidationEventProducer eventProducer
                        = FOValidationEventProducer.Provider.get(
                                userAgent.getEventBroadcaster());
                    eventProducer.invalidFORoot(this, FONode.getNodeString(namespaceURI, localName),
                            getEffectiveLocator());
                }
            } else { // check that incoming node is valid for currentFObj
                if (currentFObj.getNamespaceURI().equals(FOElementMapping.URI)
                    || currentFObj.getNamespaceURI().equals(ExtensionElementMapping.URI)
                    || currentFObj.getNamespaceURI().equals(PDFElementMapping.NAMESPACE)) {
                    currentFObj.validateChildNode(locator, namespaceURI, localName);
                }
            }

            ElementMapping.Maker fobjMaker = findFOMaker(namespaceURI, localName);

            try {
                foNode = fobjMaker.make(currentFObj);
                if (rootFObj == null) {
                    rootFObj = (Root) foNode;
                    rootFObj.setBuilderContext(builderContext);
                    rootFObj.setFOEventHandler(foEventHandler);
                }
                propertyList = foNode.createPropertyList(
                                    currentPropertyList, foEventHandler);
                foNode.processNode(localName, getEffectiveLocator(),
                                    attlist, propertyList);
                if (foNode.getNameId() == Constants.FO_MARKER) {
                    if (builderContext.inMarker()) {
                        nestedMarkerDepth++;
                    } else {
                        builderContext.switchMarkerContext(true);
                    }
                }
                if (foNode.getNameId() == Constants.FO_PAGE_SEQUENCE) {
                    builderContext.getXMLWhiteSpaceHandler().reset();
                }
            } catch (IllegalArgumentException e) {
                throw new SAXException(e);
            }

            ContentHandlerFactory chFactory = foNode.getContentHandlerFactory();
            if (chFactory != null) {
                ContentHandler subHandler = chFactory.createContentHandler();
                if (subHandler instanceof ObjectSource
                        && foNode instanceof ObjectBuiltListener) {
                    ((ObjectSource) subHandler).setObjectBuiltListener(
                            (ObjectBuiltListener) foNode);
                }

                subHandler.startDocument();
                subHandler.startElement(namespaceURI, localName,
                        rawName, attlist);
                depth = 1;
                delegate = subHandler;
            }

            if (currentFObj != null) {
                currentFObj.addChildNode(foNode);
            }

            currentFObj = foNode;
            if (propertyList != null && !builderContext.inMarker()) {
                currentPropertyList = propertyList;
            }

            // fo:characters can potentially be removed during
            // white-space handling.
            // Do not notify the FOEventHandler.
            if (currentFObj.getNameId() != Constants.FO_CHARACTER
                    && (!builderContext.inMarker() || currentFObj.getNameId() == Constants.FO_MARKER)) {
                currentFObj.startOfNode();
            }
        }

        /** {@inheritDoc} */
        public void endElement(String uri, String localName, String rawName)
                    throws SAXException {
            if (currentFObj == null) {
                throw new SAXException(
                        "endElement() called for " + rawName
                            + " where there is no current element.");
            } else if (!currentFObj.getLocalName().equals(localName)
                    || !currentFObj.getNamespaceURI().equals(uri)) {
                throw new SAXException("Mismatch: " + currentFObj.getLocalName()
                        + " (" + currentFObj.getNamespaceURI()
                        + ") vs. " + localName + " (" + uri + ")");
            }

            // fo:characters can potentially be removed during
            // white-space handling.
            // Do not notify the FOEventHandler.
            if (currentFObj.getNameId() != Constants.FO_CHARACTER
                    && (!builderContext.inMarker() || currentFObj.getNameId() == Constants.FO_MARKER)) {
                currentFObj.endOfNode();
            }

            if (currentPropertyList != null
                    && currentPropertyList.getFObj() == currentFObj
                    && !builderContext.inMarker()) {
                currentPropertyList = currentPropertyList.getParentPropertyList();
            }

            if (currentFObj.getNameId() == Constants.FO_MARKER) {
                if (nestedMarkerDepth == 0) {
                    builderContext.switchMarkerContext(false);
                } else {
                    nestedMarkerDepth--;
                }
            }

            if (currentFObj.getParent() == null) {
                LOG.debug("endElement for top-level " + currentFObj.getName());
            }

            currentFObj = currentFObj.getParent();
        }

        /** {@inheritDoc} */
        public void characters(char[] data, int start, int length)
            throws FOPException {
            if (currentFObj != null) {
                currentFObj.characters(data, start, length,
                        currentPropertyList, getEffectiveLocator());
            }
        }

        /** {@inheritDoc} */
        public void endDocument() throws SAXException {
            currentFObj = null;
        }

        /**
         * Finds the {@link Maker} used to create {@link FONode} objects of a particular type
         *
         * @param namespaceURI URI for the namespace of the element
         * @param localName name of the Element
         * @return the ElementMapping.Maker that can create an FO object for this element
         * @throws FOPException if a Maker could not be found for a bound namespace.
         */
        private Maker findFOMaker(String namespaceURI, String localName) throws FOPException {
            Maker maker = elementMappingRegistry.findFOMaker(namespaceURI, localName, locator);
            if (maker instanceof UnknownXMLObj.Maker) {
                FOValidationEventProducer eventProducer
                    = FOValidationEventProducer.Provider.get(
                        userAgent.getEventBroadcaster());
                String name = (currentFObj != null ? currentFObj.getName()
                                                   : "{" + namespaceURI + "}" + localName);
                eventProducer.unknownFormattingObject(this, name,
                        new QName(namespaceURI, localName),
                        getEffectiveLocator());
            }
            return maker;
        }

    }
}

