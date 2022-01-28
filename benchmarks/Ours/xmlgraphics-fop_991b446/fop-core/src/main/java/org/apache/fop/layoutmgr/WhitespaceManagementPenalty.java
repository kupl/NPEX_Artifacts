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

package org.apache.fop.layoutmgr;

import java.util.ArrayList;
import java.util.List;

import org.apache.fop.layoutmgr.MultiSwitchLayoutManager.WhitespaceManagementPosition;

/**
 * A special penalty used to specify content having multiple variants. At most
 * only one variant will be inserted into the final document. If none of the
 * variants fit into the remaining space on the current page, the dynamic
 * content will be completely ignored.
 */
public class WhitespaceManagementPenalty extends KnuthPenalty {

    public class Variant {

        public final List<ListElement> knuthList;
        public final int width;
        private final KnuthPenalty penalty;

        public Variant(List<ListElement> knuthList, int width) {
            this.knuthList = knuthList;
            this.width = width;
            this.penalty = new KnuthPenalty(width, 0, false, null, false);
        }

        public KnuthElement getPenalty() {
            return penalty;
        }

        public WhitespaceManagementPenalty getWhitespaceManagementPenalty() {
            return WhitespaceManagementPenalty.this;
        }

    }

    private final WhitespaceManagementPosition whitespaceManagementPosition;
    private final List<Variant> variantList;

    public WhitespaceManagementPenalty(WhitespaceManagementPosition pos) {
        super(0, 0, false, pos, false);
        this.whitespaceManagementPosition = pos;
        variantList = new ArrayList<Variant>();
    }

    public void addVariant(Variant variant) {
        variantList.add(variant);
    }

    public void setActiveVariant(Variant bestVariant) {
        whitespaceManagementPosition.setKnuthList(bestVariant.knuthList);
    }

    public boolean hasActiveVariant() {
        return whitespaceManagementPosition.getKnuthList() != null;
    }

    public List<Variant> getVariants() {
        return variantList;
    }

    @Override
    public String toString() {
        String str = super.toString();
        StringBuffer buffer = new StringBuffer(64);
        buffer.append(" number of variants = " + variantList.size());
        return str + buffer;
    }

}
