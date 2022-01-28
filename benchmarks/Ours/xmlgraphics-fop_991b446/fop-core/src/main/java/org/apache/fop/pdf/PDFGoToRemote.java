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

package org.apache.fop.pdf;

/**
 * Class representing a /GoToR object.
 */
public class PDFGoToRemote extends PDFAction {

    /**
     * the file specification
     */
    private PDFReference pdfFileSpec;
    private int pageReference;
    private String destination;
    private boolean newWindow;

    /**
     * Create an GoToR object.
     *
     * @param pdfFileSpec the fileSpec associated with the action
     * @param newWindow boolean indicating whether the target should be
     *                  displayed in a new window
     */
    public PDFGoToRemote(PDFFileSpec pdfFileSpec, boolean newWindow) {
        /* generic creation of object */
        super();

        this.pdfFileSpec = pdfFileSpec.makeReference();
        this.newWindow = newWindow;
    }

    /**
     * Create an GoToR object.
     *
     * @param pdfFileSpec the fileSpec associated with the action
     * @param page a page reference within the remote document
     * @param newWindow boolean indicating whether the target should be
     *                  displayed in a new window
     */
    public PDFGoToRemote(PDFFileSpec pdfFileSpec, int page, boolean newWindow) {
        this(pdfFileSpec.makeReference(), page, newWindow);
    }

    /**
     * Create an GoToR object.
     *
     * @param pdfFileSpec the fileSpec associated with the action
     * @param page a page reference within the remote document
     * @param newWindow boolean indicating whether the target should be
     *                  displayed in a new window
     */
    public PDFGoToRemote(PDFReference pdfFileSpec, int page, boolean newWindow) {
        super();

        this.pdfFileSpec = pdfFileSpec;
        this.pageReference = page;
        this.newWindow = newWindow;
    }

    /**
     * create an GoToR object.
     *
     * @param pdfFileSpec the fileSpec associated with the action
     * @param dest a named destination within the remote document
     * @param newWindow boolean indicating whether the target should be
     *                  displayed in a new window
     */
    public PDFGoToRemote(PDFFileSpec pdfFileSpec, String dest, boolean newWindow) {
        /* generic creation of object */
        super();

        this.pdfFileSpec = pdfFileSpec.makeReference();
        this.destination = dest;
        this.newWindow = newWindow;
    }

    /**
     * return the action string which will reference this object
     *
     * @return the action String
     */
    public String getAction() {
        return this.referencePDF();
    }

    /**
     * {@inheritDoc}
     */
    public String toPDFString() {
        StringBuffer sb = new StringBuffer(64);
        sb.append("<<\n/S /GoToR\n/F ");
        sb.append(pdfFileSpec.toString());
        sb.append("\n");

        if (destination != null) {
            sb.append("/D (").append(this.destination).append(")");
        } else {
            sb.append("/D [ ").append(this.pageReference).append(" /XYZ null null null ]");
        }

        if (newWindow) {
            sb.append("/NewWindow true");
        }

        sb.append("\n>>");

        return sb.toString();
    }


    /*
     * example
     * 28 0 obj
     * <<
     * /S /GoToR
     * /F 29 0 R
     * /D [ 0 /XYZ -6 797 null ]
     * >>
     * endobj
     */

    /** {@inheritDoc} */
    protected boolean contentEquals(PDFObject obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFGoToRemote)) {
            return false;
        }

        PDFGoToRemote remote = (PDFGoToRemote)obj;

        if (!remote.pdfFileSpec.toString().equals(pdfFileSpec.toString())) {
            return false;
        }

        if (destination != null) {
            if (!destination.equals(remote.destination)) {
                return false;
            }
        } else {
            if (pageReference != remote.pageReference) {
                return false;
            }
        }

        return (this.newWindow == remote.newWindow);
    }
}

