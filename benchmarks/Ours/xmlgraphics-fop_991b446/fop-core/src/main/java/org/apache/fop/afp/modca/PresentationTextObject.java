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

package org.apache.fop.afp.modca;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.fop.afp.AFPLineDataInfo;
import org.apache.fop.afp.AFPTextDataInfo;
import org.apache.fop.afp.ptoca.LineDataInfoProducer;
import org.apache.fop.afp.ptoca.PtocaBuilder;
import org.apache.fop.afp.ptoca.PtocaProducer;
import org.apache.fop.afp.ptoca.TextDataInfoProducer;

/**
 * The Presentation Text object is the data object used in document processing
 * environments for representing text which has been prepared for presentation.
 * Text, as used here, means an ordered string of characters, such as graphic
 * symbols, numbers, and letters, that are suitable for the specific purpose of
 * representing coherent information. Text which has been prepared for
 * presentation has been reduced to a primitive form through explicit
 * specification of the characters and their placement in the presentation
 * space. Control sequences which designate specific control functions may be
 * embedded within the text. These functions extend the primitive form by
 * applying specific characteristics to the text when it is presented. The
 * collection of the graphic characters and control codes is called Presentation
 * Text, and the object that contains the Presentation Text is called the
 * PresentationText object.
 * <p>
 * The content for this object can be created using {@link PtocaBuilder}.
 */
public class PresentationTextObject extends AbstractNamedAFPObject {

    /**
     * The current presentation text data
     */
    private PresentationTextData currentPresentationTextData;

    /**
     * The presentation text data list
     */
    private List<PresentationTextData> presentationTextDataList;

    private DefaultBuilder builder = new DefaultBuilder();

    /**
     * Construct a new PresentationTextObject for the specified name argument,
     * the name should be an 8 character identifier.
     *
     * @param name the name of this presentation object
     */
    public PresentationTextObject(String name) {
        super(name);
    }

    /**
     * Create the presentation text data for the byte array of data.
     *
     * @param textDataInfo
     *            The afp text data
     * @throws UnsupportedEncodingException thrown if character encoding is not supported
     */
    public void createTextData(AFPTextDataInfo textDataInfo) throws UnsupportedEncodingException {
        createControlSequences(new TextDataInfoProducer(textDataInfo));
    }

    /**
     * Creates a chain of control sequences using a producer.
     * @param producer the producer
     * @throws UnsupportedEncodingException thrown if character encoding is not supported
     */
    public boolean createControlSequences(PtocaProducer producer)
                throws UnsupportedEncodingException {
        if (currentPresentationTextData == null) {
            startPresentationTextData();
        }
        try {
            if (getBytesAvailable() != null && getBytesAvailable() < getSize(producer)) {
                return false;
            }
            producer.produce(builder);
        } catch (UnsupportedEncodingException e) {
            endPresentationTextData();
            throw e;
        } catch (IOException ioe) {
            endPresentationTextData();
            handleUnexpectedIOError(ioe);
        }
        return true;
    }

    private int getSize(PtocaProducer producer) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PtocaBuilder pb = new PtocaBuilder() {
            protected OutputStream getOutputStreamForControlSequence(int length) {
                return bos;
            }
        };
        producer.produce(pb);
        return bos.size();
    }

    private class DefaultBuilder extends PtocaBuilder {
        public Integer getBytesAvailable() {
            if (currentPresentationTextData == null) {
                return null;
            }
            return currentPresentationTextData.getBytesAvailable();
        }

        protected OutputStream getOutputStreamForControlSequence(int length) {
            if (length > currentPresentationTextData.getBytesAvailable()) {
                endPresentationTextData();
                startPresentationTextData();
            }
            return currentPresentationTextData.getOutputStream();
        }
    }

    /**
     * Drawing of lines using the starting and ending coordinates, thickness and
     * orientation arguments.
     *
     * @param lineDataInfo the line data information.
     */
    public boolean createLineData(AFPLineDataInfo lineDataInfo) {
        try {
            return createControlSequences(new LineDataInfoProducer(lineDataInfo));
        } catch (UnsupportedEncodingException e) {
            handleUnexpectedIOError(e); //Won't happen for lines
        }
        return false;
    }

    /**
     * Helper method to mark the start of the presentation text data
     */
    private void startPresentationTextData() {
        if (presentationTextDataList == null) {
            presentationTextDataList = new java.util.ArrayList<PresentationTextData>();
        }
        if (presentationTextDataList.size() == 0) {
            currentPresentationTextData = new PresentationTextData(true);
        } else {
            currentPresentationTextData = new PresentationTextData();
        }
        presentationTextDataList.add(currentPresentationTextData);
    }

    /**
     * Helper method to mark the end of the presentation text data
     */
    private void endPresentationTextData() {
        this.currentPresentationTextData = null;
    }

    /** {@inheritDoc} */
    protected void writeStart(OutputStream os) throws IOException {
        byte[] data = new byte[17];
        copySF(data, Type.BEGIN, Category.PRESENTATION_TEXT);
        os.write(data);
    }

    /** {@inheritDoc} */
    protected void writeContent(OutputStream os) throws IOException {
        writeObjects(this.presentationTextDataList, os);
    }

    /** {@inheritDoc} */
    protected void writeEnd(OutputStream os) throws IOException {
        byte[] data = new byte[17];
        copySF(data, Type.END, Category.PRESENTATION_TEXT);
        os.write(data);
    }

    /**
     * A control sequence is a sequence of bytes that specifies a control
     * function. A control sequence consists of a control sequence introducer
     * and zero or more parameters. The control sequence can extend multiple
     * presentation text data objects, but must eventually be terminated. This
     * method terminates the control sequence.
     */
    public void endControlSequence() {
        if (currentPresentationTextData == null) {
            startPresentationTextData();
        }
        try {
            builder.endChainedControlSequence();
        } catch (IOException ioe) {
            endPresentationTextData();
            handleUnexpectedIOError(ioe);
            //Should not occur since we're writing to byte arrays
        }
    }

    private void handleUnexpectedIOError(IOException ioe) {
        //"Unexpected" since we're currently dealing with ByteArrayOutputStreams here.
        throw new RuntimeException("Unexpected I/O error: " + ioe.getMessage(), ioe);
    }

    /** {@inheritDoc} */
    public String toString() {
        if (presentationTextDataList != null) {
            return presentationTextDataList.toString();
        }
        return super.toString();
    }

    public Integer getBytesAvailable() {
        return builder.getBytesAvailable();
    }
}
