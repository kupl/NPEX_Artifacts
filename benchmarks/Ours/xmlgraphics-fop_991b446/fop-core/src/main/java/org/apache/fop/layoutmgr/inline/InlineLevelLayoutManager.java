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

package org.apache.fop.layoutmgr.inline;

import java.util.List;

import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.Position;

/**
 * The interface for LayoutManagers which generate inline areas
 */
public interface InlineLevelLayoutManager extends LayoutManager {

    /**
     * Tell the LM to modify its data, adding a letter space
     * to the word fragment represented by the given elements,
     * and returning the corrected elements
     *
     * @param oldList the elements which must be given one more letter space
     * @return        the new elements replacing the old ones
     */
    List addALetterSpaceTo(List oldList);

    /**
     * Tell the LM to modify its data, adding a letter space
     * to the word fragment represented by the given elements,
     * and returning the corrected elements
     *
     * @param oldList the elements which must be given one more letter space
     * @param depth the depth at which the Positions for this LM in oldList are found
     * @return        the new elements replacing the old ones
     */
List addALetterSpaceTo(List oldList, int depth);

    /**
     * Get the word chars corresponding to the given position.
     *
     * @param pos     the position referring to the needed word chars.
     * @return the word chars
     */
    String getWordChars(Position pos);

    /**
     * Tell the LM to hyphenate a word
     *
     * @param pos the Position referring to the word
     * @param hyphContext  the HyphContext storing hyphenation information
     */
    void hyphenate(Position pos, HyphContext hyphContext);

    /**
     * Tell the LM to apply the changes due to hyphenation
     *
     * @param oldList the list of the old elements the changes refer to
     * @return        true if the LM had to change its data, false otherwise
     */
    boolean applyChanges(List oldList);

    /**
     * Tell the LM to apply the changes due to hyphenation
     *
     * @param oldList the list of the old elements the changes refer to
     * @param depth the depth at which the Positions for this LM in oldList are found
     * @return        true if the LM had to change its data, false otherwise
     */
    boolean applyChanges(List oldList, int depth);

    /**
     * Get a sequence of KnuthElements representing the content
     * of the node assigned to the LM, after changes have been applied
     * @param oldList        the elements to replace
     * @param alignment      the desired text alignment
     * @param depth the depth at which the Positions for this LM in oldList are found
     * @return               the updated list of KnuthElements
     **/
    List getChangedKnuthElements(List oldList, int alignment, int depth);

}
