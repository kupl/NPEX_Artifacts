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

import java.awt.geom.Point2D;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Length;
import org.apache.fop.util.ContentHandlerFactory.ObjectBuiltListener;
import org.apache.fop.util.XMLConstants;

/**
 * Abstract class modelling generic, non-XSL-FO XML objects. Such objects are
 * stored in a DOM.
 */
public abstract class XMLObj extends FONode implements ObjectBuiltListener {

    // temp reference for attributes
    private Attributes attr;

    /** DOM element representing this node */
    protected Element element;

    /** DOM document containing this node */
    protected Document doc;

    /** Name of the node */
    protected String name;

    /**
     * Base constructor
     *
     * @param parent {@link FONode} that is the parent of this object
     */
    public XMLObj(FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     * <br>Here, blocks XSL-FO's from having non-FO parents.
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName)
        throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** {@inheritDoc} */
    public void processNode(String elementName, Locator locator,
        Attributes attlist, PropertyList propertyList) throws FOPException {
            setLocator(locator);
            name = elementName;
            attr = attlist;
    }

    /**
     * @return DOM document representing this foreign XML
     */
    public Document getDOMDocument() {
        return doc;
    }

    /**
     * Returns the dimensions of the generated area in pts.
     *
     * @param view Point2D instance to receive the dimensions
     * @return the requested dimensions in pts.
     */
    public Point2D getDimension(Point2D view) {
         return null;
    }

    /**
     * Retrieve the intrinsic alignment-adjust of the child element.
     *
     * @return the intrinsic alignment-adjust.
     */
    public Length getIntrinsicAlignmentAdjust() {
        return null;
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return name;
    }

    private static HashMap ns = new HashMap();
    static {
        ns.put(XMLConstants.XLINK_PREFIX, XMLConstants.XLINK_NAMESPACE);
    }

    /**
     * Add an element to the DOM document
     * @param doc DOM document to which to add an element
     * @param parent the parent element of the element that is being added
     */
    public void addElement(Document doc, Element parent) {
        this.doc = doc;
        element = doc.createElementNS(getNamespaceURI(), name);

        setAttributes(element, attr);
        attr = null;
        parent.appendChild(element);
    }

    private static void setAttributes(Element element, Attributes attr) {
        for (int count = 0; count < attr.getLength(); count++) {
            String rf = attr.getValue(count);
            String qname = attr.getQName(count);
            int idx = qname.indexOf(":");
            if (idx == -1) {
                element.setAttribute(qname, rf);
            } else {
                String pref = qname.substring(0, idx);
                String tail = qname.substring(idx + 1);
                if (pref.equals(XMLConstants.XMLNS_PREFIX)) {
                    ns.put(tail, rf);
                } else {
                    element.setAttributeNS((String)ns.get(pref), tail, rf);
                }
            }
        }
    }

    /**
     * Add the top-level element to the DOM document
     *
     * @param doc DOM document
     * @param svgRoot non-XSL-FO element to be added as the root of this document
     */
    public void buildTopLevel(Document doc, Element svgRoot) {
        // build up the info for the top level element
        setAttributes(element, attr);
    }

    /**
     * Create an empty DOM document
     *
     * @return DOM document
     */
    public Document createBasicDocument() {
        doc = null;

        element = null;
        try {
            DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            fact.setNamespaceAware(true);
            doc = fact.newDocumentBuilder().newDocument();
            Element el = doc.createElementNS(getNamespaceURI(), name);
            doc.appendChild(el);

            element = doc.getDocumentElement();
            buildTopLevel(doc, element);
            if (!element.hasAttributeNS(
                    XMLConstants.XMLNS_NAMESPACE_URI, XMLConstants.XMLNS_PREFIX)) {
                element.setAttributeNS(XMLConstants.XMLNS_NAMESPACE_URI, XMLConstants.XMLNS_PREFIX,
                                getNamespaceURI());
            }

        } catch (Exception e) {
            //TODO this is ugly because there may be subsequent failures like NPEs
            log.error("Error while trying to instantiate a DOM Document", e);
        }
        return doc;
    }

    /** {@inheritDoc} */
    protected void addChildNode(FONode child) {
        if (child instanceof XMLObj) {
            ((XMLObj)child).addElement(doc, element);
        } else {
            // in theory someone might want to embed some defined
            // xml (eg. fo) inside the foreign xml
            // they could use a different namespace
            log.debug("Invalid element: " + child.getName() + " inside foreign xml markup");
        }
    }

    /** {@inheritDoc} */
    protected void characters(char[] data, int start, int length,
                                 PropertyList pList, Locator locator) throws FOPException {
        super.characters(data, start, length, pList, locator);
        String str = new String(data, start, length);
        org.w3c.dom.Text text = doc.createTextNode(str);
        element.appendChild(text);
    }

    /** {@inheritDoc} */
    public void notifyObjectBuilt(Object obj) {
        this.doc = (Document)obj;
        this.element = this.doc.getDocumentElement();
    }

}

