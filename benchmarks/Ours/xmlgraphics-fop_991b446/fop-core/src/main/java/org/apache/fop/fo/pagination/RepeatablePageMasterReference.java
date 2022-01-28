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

package org.apache.fop.fo.pagination;

// XML
import org.xml.sax.Locator;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.layoutmgr.BlockLevelEventProducer;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_repeatable-page-master-reference">
 * <code>fo:repeatable-page-master-reference</code></a> object.
 * This handles a reference with a specified number of repeating
 * instances of the referenced page master (may have no limit).
 */
public class RepeatablePageMasterReference extends FObj
    implements SubSequenceSpecifier {

    // The value of properties relevant for fo:repeatable-page-master-reference.
    private String masterReference;
    // The simple page master referenced
    private SimplePageMaster master;
    private Property maximumRepeats;
    // End of property values

    private static final int INFINITE = -1;

    private int numberConsumed;

    /**
     * Base constructor
     *
     * @param parent {@link FONode} that is the parent of this object
     */
    public RepeatablePageMasterReference(FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    public void bind(PropertyList pList) throws FOPException {
        masterReference = pList.get(PR_MASTER_REFERENCE).getString();
        maximumRepeats = pList.get(PR_MAXIMUM_REPEATS);

        if (masterReference == null || masterReference.equals("")) {
            missingPropertyError("master-reference");
        }
    }

    /** {@inheritDoc} */
    public void startOfNode() throws FOPException {
        PageSequenceMaster pageSequenceMaster = (PageSequenceMaster) parent;

        if (masterReference == null) {
            missingPropertyError("master-reference");
        } else {
            pageSequenceMaster.addSubsequenceSpecifier(this);
        }
    }

    /**
     * {@inheritDoc}
     * <br>XSL Content Model: empty
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName)
        throws ValidationException {
        invalidChildError(loc, nsURI, localName);
    }

    /** {@inheritDoc} */
    public SimplePageMaster getNextPageMaster(boolean isOddPage,
                                        boolean isFirstPage,
                                        boolean isLastPage,
                                        boolean isEmptyPage) {
        if (getMaximumRepeats() != INFINITE && numberConsumed >= getMaximumRepeats()) {
           return null;
        }
        numberConsumed++;
        return master;
    }

    public SimplePageMaster getLastPageMaster(boolean isOddPage, boolean isFirstPage, boolean isEmptyPage,
                                              BlockLevelEventProducer blockLevelEventProducer) {
        return getNextPageMaster(isOddPage, isFirstPage, true, isEmptyPage);
    }

    /**
     * Get the value of the <code>maximum-repeats</code> property.
     * @return the "maximum-repeats" property
     */
    public int getMaximumRepeats() {
        if (maximumRepeats.getEnum() == EN_NO_LIMIT) {
            return INFINITE;
        } else {
            int mr = maximumRepeats.getNumeric().getValue();
            if (mr < 0) {
                log.debug("negative maximum-repeats: "
                        + this.maximumRepeats);
                mr = 0;
            }
            return mr;
        }
    }

    /** {@inheritDoc} */
    public void reset() {
        this.numberConsumed = 0;
    }


    /** {@inheritDoc} */
    public boolean goToPrevious() {
        if (numberConsumed == 0) {
            return false;
        } else {
            numberConsumed--;
            return true;
        }
    }

    /** {@inheritDoc} */
    public boolean hasPagePositionLast() {
        return false;
    }

    /** {@inheritDoc} */
    public boolean hasPagePositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "repeatable-page-master-reference";
    }

    /**
     * {@inheritDoc}
     * @return {@link org.apache.fop.fo.Constants#FO_REPEATABLE_PAGE_MASTER_REFERENCE}
     */
    public int getNameId() {
        return FO_REPEATABLE_PAGE_MASTER_REFERENCE;
    }


    /** {@inheritDoc} */
    public void resolveReferences(LayoutMasterSet layoutMasterSet) throws ValidationException {
        master = layoutMasterSet.getSimplePageMaster(masterReference);
        if (master == null) {
            BlockLevelEventProducer.Provider.get(
                getUserAgent().getEventBroadcaster())
                .noMatchingPageMaster(this, parent.getName(), masterReference, getLocator());
        }

    }

    /** {@inheritDoc} */
    public boolean canProcess(String flowName) {
        assert master != null;
        return master.getRegion(FO_REGION_BODY).getRegionName().equals(flowName);
    }

    /** {@inheritDoc} */
    public boolean isInfinite() {
        return getMaximumRepeats() == INFINITE;
    }

    /** {@inheritDoc} */
    public boolean isReusable() {
        return false;
    }

}
