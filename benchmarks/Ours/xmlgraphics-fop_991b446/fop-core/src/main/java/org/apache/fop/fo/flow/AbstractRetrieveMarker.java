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

import java.util.Iterator;

import org.xml.sax.Locator;

import org.apache.fop.accessibility.StructureTreeElement;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.FObjMixed;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.XMLObj;
import org.apache.fop.fo.flow.table.Table;

/**
 * Abstract base class for the <a href="http://www.w3.org/TR/xsl/#fo_retrieve-marker">
 * <code>fo:retrieve-marker</code></a> and
 * <a href="http://www.w3.org/TR/xsl/#fo_retrieve-table-marker">
 * <code>fo:retrieve-table-marker</code></a> formatting objects.

 */
public abstract class AbstractRetrieveMarker extends FObjMixed {

    private PropertyList propertyList;

    private String retrieveClassName;

    private int position;
    private String positionLabel;
    private int boundary;
    private String boundaryLabel;

    private StructureTreeElement structureTreeElement;

    /**
     * Create a new AbstractRetrieveMarker instance that
     * is a child of the given {@link FONode}
     *
     * @param parent    the parent {@link FONode}
     */
    public AbstractRetrieveMarker(FONode parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     * <p>XSL Content Model: empty
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName)
                throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /**
     * {@inheritDoc}
     * Store a reference to the parent {@link PropertyList}
     * to be used when the retrieve-marker is resolved.
     */
    public void bind(PropertyList pList) throws FOPException {
        super.bind(pList);
        this.retrieveClassName = pList.get(PR_RETRIEVE_CLASS_NAME).getString();
        if (retrieveClassName == null || retrieveClassName.equals("")) {
            missingPropertyError("retrieve-class-name");
        }
        this.propertyList = pList.getParentPropertyList();
    }

    @Override
    public void setStructureTreeElement(StructureTreeElement structureTreeElement) {
        this.structureTreeElement = structureTreeElement;
    }

    @Override
    public StructureTreeElement getStructureTreeElement() {
        return structureTreeElement;
    }

    private PropertyList createPropertyListFor(FObj fo, PropertyList parent) {
        return getBuilderContext().getPropertyListMaker().make(fo, parent);
    }

    private void cloneSingleNode(FONode child, FONode newParent,
                            Marker marker, PropertyList parentPropertyList)
        throws FOPException {

        if (child != null) {
            FONode newChild = child.clone(newParent, true);
            if (child instanceof FObj) {
                Marker.MarkerPropertyList pList;
                PropertyList newPropertyList = createPropertyListFor(
                            (FObj) newChild, parentPropertyList);

                pList = marker.getPropertyListFor(child);
                newChild.processNode(
                        child.getLocalName(),
                        getLocator(),
                        pList,
                        newPropertyList);
                addChildTo(newChild, newParent);
                newChild.startOfNode();
                switch (newChild.getNameId()) {
                case FO_TABLE:
                    Table t = (Table) child;
                    cloneSubtree(t.getColumns().iterator(),
                                 newChild, marker, newPropertyList);
                    cloneSingleNode(t.getTableHeader(),
                                    newChild, marker, newPropertyList);
                    cloneSingleNode(t.getTableFooter(),
                                    newChild, marker, newPropertyList);
                    cloneSubtree(child.getChildNodes(),
                                    newChild, marker, newPropertyList);
                    break;
                case FO_LIST_ITEM:
                    ListItem li = (ListItem) child;
                    cloneSingleNode(li.getLabel(),
                                    newChild, marker, newPropertyList);
                    cloneSingleNode(li.getBody(),
                                    newChild, marker, newPropertyList);
                    break;
                default:
                    cloneSubtree(child.getChildNodes(),
                                    newChild, marker, newPropertyList);
                    break;
                }
                newChild.endOfNode();
            } else if (child instanceof FOText) {
                FOText ft = (FOText) newChild;
                ft.bind(parentPropertyList);
                addChildTo(newChild, newParent);
                if (newParent instanceof AbstractRetrieveMarker) {
                    /*
                     * Otherwise the parent of newChild is a cloned FObjMixed that will
                     * call this FOText's endOfNode when its own endOfNode method is
                     * called.
                     */
                    newChild.endOfNode();
                }
            } else if (child instanceof XMLObj) {
                addChildTo(newChild, newParent);
            }
        }
    }

    /**
     * Clone the FO nodes in the parent iterator,
     * attach the new nodes to the new parent,
     * and map the new nodes to the existing property lists.
     * FOText nodes are also in the new map, with a null value.
     * Clone the subtree by a recursive call to this method.
     * @param parentIter the iterator over the children of the old parent
     * @param newParent the new parent for the cloned nodes
     * @param marker the marker that contains the old property list mapping
     * @param parentPropertyList the parent PropertyList
     * @throws FOPException in case there was an error
     */
    private void cloneSubtree(Iterator parentIter, FONode newParent,
                              Marker marker, PropertyList parentPropertyList)
        throws FOPException {
        if (parentIter != null) {
            FONode child;
            while (parentIter.hasNext()) {
                child = (FONode) parentIter.next();
                cloneSingleNode(child, newParent,
                        marker, parentPropertyList);
            }
        }
    }

    private void cloneFromMarker(Marker marker)
        throws FOPException {
        cloneSubtree(marker.getChildNodes(), this,
                        marker, propertyList);
        handleWhiteSpaceFor(this, null);
    }

    /**
     * Clone the subtree of the given marker
     *
     * @param marker the marker that is to be cloned
     */
    public void bindMarker(Marker marker) {
        // clean up remnants from a possible earlier layout
        if (firstChild != null) {
            currentTextNode = null;
            firstChild = null;
        }
        if (marker.getChildNodes() != null) {
            try {
                restoreFOEventHandlerState();
                cloneFromMarker(marker);
            } catch (FOPException exc) {
                getFOValidationEventProducer().markerCloningFailed(this,
                        marker.getMarkerClassName(), exc, getLocator());
            }
        } else if (log.isDebugEnabled()) {
            log.debug("Empty marker retrieved...");
        }
    }

    protected abstract void restoreFOEventHandlerState();

    /**
     * Return the value for the <code>retrieve-class-name</code>
     * property
     *
     * @return the value for retrieve-class-name
     */
    public String getRetrieveClassName() {
        return this.retrieveClassName;
    }

    protected void setBoundaryLabel(String label) {
        this.boundaryLabel = label;
    }

    protected void setPositionLabel(String label) {
        this.positionLabel = label;
    }

    public String getBoundaryLabel() {
        return this.boundaryLabel;
    }

    public String getPositionLabel() {
        return this.positionLabel;
    }

    protected void setPosition(int position) {
        this.position = position;
    }

    protected void setBoundary(int boundary) {
        this.boundary = boundary;
    }

    public int getPosition() {
        return this.position;
    }

    public int getBoundary() {
        return this.boundary;
    }

    public abstract String getLocalName();

    public abstract int getNameId();

    public void changePositionTo(int position) {
        this.position = position;
    }
}
