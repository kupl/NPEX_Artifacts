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

package org.apache.fop.render.rtf.rtflib.rtfdoc;

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

import java.io.IOException;
import java.io.Writer;

/**
 * <p>Common code for RtfAfter and RtfBefore.</p>
 *
 * <p>This work was authored by Andreas Lambert (andreas.lambert@cronidesoft.com),
 * Christopher Scott (scottc@westinghouse.com), and
 * Christoph Zahm (zahm@jnet.ch) [support for tables in headers/footers].</p>
 */

abstract class RtfAfterBeforeBase
extends RtfContainer
implements IRtfParagraphContainer, IRtfExternalGraphicContainer, IRtfTableContainer,
        IRtfTextrunContainer {
    private RtfParagraph para;
    private RtfExternalGraphic externalGraphic;
    private RtfTable table;

    RtfAfterBeforeBase(RtfSection parent, Writer w, RtfAttributes attrs) throws IOException {
        super((RtfContainer)parent, w, attrs);
    }

    public RtfParagraph newParagraph() throws IOException {
        closeAll();
        para = new RtfParagraph(this, writer);
        return para;
    }

    public RtfParagraph newParagraph(RtfAttributes attrs) throws IOException {
        closeAll();
        para = new RtfParagraph(this, writer, attrs);
        return para;
    }

    public RtfExternalGraphic newImage() throws IOException {
        closeAll();
        externalGraphic = new RtfExternalGraphic(this, writer);
        return externalGraphic;
    }

    private void closeCurrentParagraph() throws IOException {
        if (para != null) {
            para.close();
        }
    }

    private void closeCurrentExternalGraphic() throws IOException {
        if (externalGraphic != null) {
            externalGraphic.close();
        }
    }

    private void closeCurrentTable() throws IOException {
        if (table != null) {
            table.close();
        }
    }

    protected void writeRtfPrefix() throws IOException {
        writeGroupMark(true);
        writeMyAttributes();
    }

    /** must be implemented to write the header or footer attributes */
    protected abstract void writeMyAttributes() throws IOException;

    protected void writeRtfSuffix() throws IOException {
        writeGroupMark(false);
    }

    public RtfAttributes getAttributes() {
        return attrib;
    }

    public void closeAll() throws IOException {
        closeCurrentParagraph();
        closeCurrentExternalGraphic();
        closeCurrentTable();
    }

    /** close current table if any and start a new one
     * @param tc added by Boris Poudérous on july 2002 in order to process
     *  number-columns-spanned attribute
     */
    public RtfTable newTable(RtfAttributes attrs, ITableColumnsInfo tc) throws IOException {
        closeAll();
        table = new RtfTable(this, writer, attrs, tc);
        return table;
    }

    /** close current table if any and start a new one  */
    public RtfTable newTable(ITableColumnsInfo tc) throws IOException {
        closeAll();
        table = new RtfTable(this, writer, tc);
        return table;
    }

    public RtfTextrun getTextrun()
    throws IOException {
        return RtfTextrun.getTextrun(this, writer, null);
    }
}
