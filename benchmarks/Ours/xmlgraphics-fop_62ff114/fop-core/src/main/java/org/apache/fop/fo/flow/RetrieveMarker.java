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

package org.apache.fop.fo.flow;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_retrieve-marker">
 * <code>fo:retrieve-marker</code></a> formatting object.
 * This will create a layout manager that will retrieve
 * a marker based on the information.
 */
public class RetrieveMarker extends AbstractRetrieveMarker {

    /**
     * Create a new RetrieveMarker instance that is a
     * child of the given {@link FONode}.
     *
     * @param parent the parent {@link FONode}
     */
    public RetrieveMarker(FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     * <i>NOTE: An <code>fo:retrieve-marker</code> is only permitted as a descendant
     * of an <code>fo:static-content</code>.</i>
     */
    public void processNode(String elementName,
                            Locator locator,
                            Attributes attlist,
                            PropertyList pList)
            throws FOPException {
        if (findAncestor(FO_STATIC_CONTENT) < 0) {
            invalidChildError(locator, getParent().getName(), FO_URI, getLocalName(),
                "rule.retrieveMarkerDescendantOfStaticContent");
        } else {
            super.processNode(elementName, locator, attlist, pList);
        }
    }

    /** {@inheritDoc} */
    public void bind(PropertyList pList) throws FOPException {
        super.bind(pList);
        setPosition(pList.get(PR_RETRIEVE_POSITION).getEnum());
        setPositionLabel((String) pList.get(PR_RETRIEVE_POSITION).getObject());
        setBoundary(pList.get(PR_RETRIEVE_BOUNDARY).getEnum());
        setBoundaryLabel((String) pList.get(PR_RETRIEVE_BOUNDARY).getObject());
    }

    @Override
    public void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startRetrieveMarker(this);
    }

    @Override
    public void endOfNode() throws FOPException {
        super.endOfNode();
        getFOEventHandler().endRetrieveMarker(this);
    }

    /**
     * Return the value for the <code>retrieve-position</code>
     * property
     * @return  the value for retrieve-position-within-table; one of
     *              {@link org.apache.fop.fo.Constants#EN_FSWP},
     *              {@link org.apache.fop.fo.Constants#EN_FIC},
     *              {@link org.apache.fop.fo.Constants#EN_LSWP},
     *              {@link org.apache.fop.fo.Constants#EN_LEWP}.
     */
    public int getRetrievePosition() {
        return getPosition();
    }

    /**
     * Return the value for the <code>retrieve-boundary</code>
     * property
     * @return  the value for retrieve-boundary; one of
     *              {@link org.apache.fop.fo.Constants#EN_PAGE},
     *              {@link org.apache.fop.fo.Constants#EN_PAGE_SEQUENCE},
     *              {@link org.apache.fop.fo.Constants#EN_DOCUMENT}.
     */
    public int getRetrieveBoundary() {
        return getBoundary();
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "retrieve-marker";
    }

    /**
     * {@inheritDoc}
     * @return {@link org.apache.fop.fo.Constants#FO_RETRIEVE_MARKER}
     */
    public int getNameId() {
        return FO_RETRIEVE_MARKER;
    }

    @Override
    protected void restoreFOEventHandlerState() {
        getFOEventHandler().restoreState(this);
    }

}
