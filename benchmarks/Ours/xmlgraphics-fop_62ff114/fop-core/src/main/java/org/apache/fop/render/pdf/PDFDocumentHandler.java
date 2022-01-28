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

package org.apache.fop.render.pdf;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.xmlgraphics.xmp.Metadata;

import org.apache.fop.accessibility.StructureTreeEventHandler;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.fo.extensions.xmp.XMPMetadata;
import org.apache.fop.pdf.PDFAnnotList;
import org.apache.fop.pdf.PDFArray;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFPage;
import org.apache.fop.pdf.PDFReference;
import org.apache.fop.pdf.PDFResources;
import org.apache.fop.pdf.PDFStream;
import org.apache.fop.render.extensions.prepress.PageBoundaries;
import org.apache.fop.render.extensions.prepress.PageScale;
import org.apache.fop.render.intermediate.AbstractBinaryWritingIFDocumentHandler;
import org.apache.fop.render.intermediate.IFContext;
import org.apache.fop.render.intermediate.IFDocumentHandlerConfigurator;
import org.apache.fop.render.intermediate.IFDocumentNavigationHandler;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFPainter;
import org.apache.fop.render.pdf.PDFRendererConfig.PDFRendererConfigParser;
import org.apache.fop.render.pdf.extensions.PDFDictionaryAttachment;
import org.apache.fop.render.pdf.extensions.PDFEmbeddedFileAttachment;

/**
 * {@link org.apache.fop.render.intermediate.IFDocumentHandler} implementation that produces PDF.
 */
public class PDFDocumentHandler extends AbstractBinaryWritingIFDocumentHandler {

    /** logging instance */
    private static Log log = LogFactory.getLog(PDFDocumentHandler.class);

    private boolean accessEnabled;

    private PDFLogicalStructureHandler logicalStructureHandler;

    private PDFStructureTreeBuilder structureTreeBuilder;

    /** the PDF Document being created */
    private PDFDocument pdfDoc;

    /**
     * Utility class which enables all sorts of features that are not directly connected to the
     * normal rendering process.
     */
    private final PDFRenderingUtil pdfUtil;

    /** the /Resources object of the PDF document being created */
    private PDFResources pdfResources;

    /** The current content generator */
    private PDFContentGenerator generator;

    /** the current page to add annotations to */
    private PDFPage currentPage;

    /** the current page's PDF reference */
    private PageReference currentPageRef;

    /** Used for bookmarks/outlines. */
    private Map<Integer, PageReference> pageReferences = new HashMap<Integer, PageReference>();

    private final PDFDocumentNavigationHandler documentNavigationHandler
            = new PDFDocumentNavigationHandler(this);

    private Map<Integer, PDFArray> pageNumbers = new HashMap<Integer, PDFArray>();
    private Map<String, PDFReference> contents = new HashMap<String, PDFReference>();

    /**
     * Default constructor.
     */
    public PDFDocumentHandler(IFContext context) {
        super(context);
        this.pdfUtil = new PDFRenderingUtil(context.getUserAgent());
    }

    /** {@inheritDoc} */
    public boolean supportsPagesOutOfOrder() {
        return !accessEnabled;
    }

    /** {@inheritDoc} */
    public String getMimeType() {
        return MimeConstants.MIME_PDF;
    }

    /** {@inheritDoc} */
    public IFDocumentHandlerConfigurator getConfigurator() {
        return new PDFRendererConfigurator(getUserAgent(), new PDFRendererConfigParser());
    }

    /** {@inheritDoc} */
    public IFDocumentNavigationHandler getDocumentNavigationHandler() {
        return this.documentNavigationHandler;
    }

    void mergeRendererOptionsConfig(PDFRendererOptionsConfig config) {
        pdfUtil.mergeRendererOptionsConfig(config);
    }

    PDFLogicalStructureHandler getLogicalStructureHandler() {
        return logicalStructureHandler;
    }

    PDFDocument getPDFDocument() {
        return pdfDoc;
    }

    PDFPage getCurrentPage() {
        return currentPage;
    }

    PageReference getCurrentPageRef() {
        return currentPageRef;
    }

    PDFContentGenerator getGenerator() {
        return generator;
    }

    /** {@inheritDoc} */
    public void startDocument() throws IFException {
        super.startDocument();
        try {
            this.pdfDoc = pdfUtil.setupPDFDocument(this.outputStream);
            this.accessEnabled = getUserAgent().isAccessibilityEnabled();
            if (accessEnabled) {
                setupAccessibility();
            }
        } catch (IOException e) {
            throw new IFException("I/O error in startDocument()", e);
        }
    }

    private void setupAccessibility() {
        pdfDoc.getRoot().makeTagged();
        logicalStructureHandler = new PDFLogicalStructureHandler(pdfDoc);
        // TODO this is ugly. All the necessary information should be available
        // at creation time in order to enforce immutability
        structureTreeBuilder.setPdfFactory(pdfDoc.getFactory());
        structureTreeBuilder.setLogicalStructureHandler(logicalStructureHandler);
        structureTreeBuilder.setEventBroadcaster(getUserAgent().getEventBroadcaster());
    }

    /** {@inheritDoc} */
    public void endDocumentHeader() throws IFException {
        pdfUtil.generateDefaultXMPMetadata();
    }

    /** {@inheritDoc} */
    public void endDocument() throws IFException {
        pdfDoc.getResources().addFonts(pdfDoc, fontInfo);
        try {
            if (pdfDoc.isLinearizationEnabled()) {
                generator.flushPDFDoc();
            } else {
                pdfDoc.outputTrailer(this.outputStream);
            }
            this.pdfDoc = null;

            pdfResources = null;
            this.generator = null;
            currentPage = null;
        } catch (IOException ioe) {
            throw new IFException("I/O error in endDocument()", ioe);
        }
        super.endDocument();
    }

    /** {@inheritDoc} */
    public void startPageSequence(String id) throws IFException {
        //nop
    }

    /** {@inheritDoc} */
    public void endPageSequence() throws IFException {
        //nop
    }

    /** {@inheritDoc} */
    public void startPage(int index, String name, String pageMasterName, Dimension size)
                throws IFException {
        this.pdfResources = this.pdfDoc.getResources();

        PageBoundaries boundaries = new PageBoundaries(size, getContext().getForeignAttributes());

        Rectangle trimBox = boundaries.getTrimBox();
        Rectangle bleedBox = boundaries.getBleedBox();
        Rectangle mediaBox = boundaries.getMediaBox();
        Rectangle cropBox = boundaries.getCropBox();

        // set scale attributes
        double scaleX = 1;
        double scaleY = 1;
        String scale = (String) getContext().getForeignAttribute(
                PageScale.EXT_PAGE_SCALE);
        Point2D scales = PageScale.getScale(scale);
        if (scales != null) {
            scaleX = scales.getX();
            scaleY = scales.getY();
        }

        //PDF uses the lower left as origin, need to transform from FOP's internal coord system
        AffineTransform boxTransform = new AffineTransform(
                scaleX / 1000, 0, 0, -scaleY / 1000, 0, scaleY * size.getHeight() / 1000);

        this.currentPage = this.pdfDoc.getFactory().makePage(
                this.pdfResources,
                index,
                toPDFCoordSystem(mediaBox, boxTransform),
                toPDFCoordSystem(cropBox, boxTransform),
                toPDFCoordSystem(bleedBox, boxTransform),
                toPDFCoordSystem(trimBox, boxTransform));
        if (pdfDoc.getProfile().isPDFVTActive()) {
            pdfDoc.getFactory().makeDPart(currentPage, pageMasterName);
        }
        if (accessEnabled) {
            logicalStructureHandler.startPage(currentPage);
        }

        pdfUtil.generatePageLabel(index, name);

        currentPageRef = new PageReference(currentPage, size);
        this.pageReferences.put(index, currentPageRef);

        this.generator = new PDFContentGenerator(this.pdfDoc, this.outputStream, this.currentPage, getContext());
        // Transform the PDF's default coordinate system (0,0 at lower left) to the PDFPainter's
        AffineTransform basicPageTransform = new AffineTransform(1, 0, 0, -1, 0,
                (scaleY * size.height) / 1000f);
        basicPageTransform.scale(scaleX, scaleY);
        generator.saveGraphicsState();
        generator.concatenate(basicPageTransform);
    }

    private Rectangle2D toPDFCoordSystem(Rectangle box, AffineTransform transform) {
        return transform.createTransformedShape(box).getBounds2D();
    }

    /** {@inheritDoc} */
    public IFPainter startPageContent() throws IFException {
        return new PDFPainter(this, logicalStructureHandler);
    }

    /** {@inheritDoc} */
    public void endPageContent() throws IFException {
        generator.restoreGraphicsState();
        //for top-level transform to change the default coordinate system
    }

    /** {@inheritDoc} */
    public void endPage() throws IFException {
        if (accessEnabled) {
            logicalStructureHandler.endPage();
        }
        try {
            this.documentNavigationHandler.commit();
            setUpContents();
            PDFAnnotList annots = currentPage.getAnnotations();
            if (annots != null) {
                this.pdfDoc.addObject(annots);
            }
            this.pdfDoc.addObject(currentPage);

            if (!pdfDoc.isLinearizationEnabled()) {
                this.generator.flushPDFDoc();
                this.generator = null;
            }
        } catch (IOException ioe) {
            throw new IFException("I/O error in endPage()", ioe);
        }
    }

    private void setUpContents() throws IOException {
        PDFStream stream = generator.getStream();
        String hash = stream.streamHashCode();
        if (!contents.containsKey(hash)) {
            pdfDoc.registerObject(stream);
            PDFReference ref = new PDFReference(stream);
            contents.put(hash, ref);
        }
        currentPage.setContents(contents.get(hash));
    }

    /** {@inheritDoc} */
    public void handleExtensionObject(Object extension) throws IFException {
        if (extension instanceof XMPMetadata) {
            pdfUtil.renderXMPMetadata((XMPMetadata) extension);
        } else if (extension instanceof Metadata) {
            XMPMetadata wrapper = new XMPMetadata(((Metadata) extension));
            pdfUtil.renderXMPMetadata(wrapper);
        } else if (extension instanceof PDFEmbeddedFileAttachment) {
            PDFEmbeddedFileAttachment embeddedFile
                = (PDFEmbeddedFileAttachment)extension;
            try {
                pdfUtil.addEmbeddedFile(embeddedFile);
            } catch (IOException ioe) {
                throw new IFException("Error adding embedded file: " + embeddedFile.getSrc(), ioe);
            }
        } else if (extension instanceof PDFDictionaryAttachment) {
            pdfUtil.renderDictionaryExtension((PDFDictionaryAttachment) extension, currentPage);
        } else if (extension != null) {
            log.debug("Don't know how to handle extension object. Ignoring: "
                    + extension + " (" + extension.getClass().getName() + ")");
        } else {
            log.debug("Ignoring null extension object.");
        }
    }

    /** {@inheritDoc} */
    public void setDocumentLocale(Locale locale) {
        pdfDoc.getRoot().setLanguage(locale);
    }

    PageReference getPageReference(int pageIndex) {
        return this.pageReferences.get(pageIndex);
    }

    static final class PageReference {

        private final PDFReference pageRef;
        private final Dimension pageDimension;

        private PageReference(PDFPage page, Dimension dim) {
            this.pageRef = page.makeReference();
            this.pageDimension = new Dimension(dim);
        }

        public PDFReference getPageRef() {
            return this.pageRef;
        }

        public Dimension getPageDimension() {
            return this.pageDimension;
        }
    }

    @Override
    public StructureTreeEventHandler getStructureTreeEventHandler() {
        if (structureTreeBuilder == null) {
            structureTreeBuilder = new PDFStructureTreeBuilder();
        }
        return structureTreeBuilder;
    }

    public Map<Integer, PDFArray> getPageNumbers() {
        return pageNumbers;
    }
}
