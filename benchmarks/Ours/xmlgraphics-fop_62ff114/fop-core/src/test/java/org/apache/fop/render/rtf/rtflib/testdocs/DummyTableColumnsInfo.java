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


/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz bdelacretaz@codeconsult.ch and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

package org.apache.fop.render.rtf.rtflib.testdocs;

import org.apache.fop.render.rtf.rtflib.rtfdoc.ITableColumnsInfo;

/** ITableColumnsInfo that does nothing, used in testodcs package
 *  to create documents without worrying about nested tables handling.
 *  Might need to be replaced by more complete version in some sample
 *  documents created by this package.
 */

class DummyTableColumnsInfo implements ITableColumnsInfo {

    public float getColumnWidth() {
        return INVALID_COLUMN_WIDTH;
    }

    public void selectFirstColumn() {
    }

    public int getNumberOfColumns() {
        return 0;
    }

    public int getColumnIndex() {
        return 0;
    }

    public void selectNextColumn() {
    }

    /* (non-Javadoc)
     * @see org.apache.fop.render.rtf.rtflib.rtfdoc.ITableColumnsInfo#getFirstSpanningCol()
     */
    public boolean getFirstSpanningCol() {
        // TODO Auto-generated method stub
        return false;
    }

}
