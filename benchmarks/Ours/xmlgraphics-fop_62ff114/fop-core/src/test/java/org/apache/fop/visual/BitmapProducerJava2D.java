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

package org.apache.fop.visual;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.configuration.Configurable;
import org.apache.fop.configuration.Configuration;
import org.apache.fop.configuration.ConfigurationException;
import org.apache.fop.util.DefaultErrorListener;

/**
 * BitmapProducer implementation that uses the Java2DRenderer to create bitmaps.
 * <p>
 * Here's what the configuration element looks like for the class:
 * <p>
 * <pre>
 * <producer classname="org.apache.fop.visual.BitmapProducerJava2D">
 *   <delete-temp-files>false</delete-temp-files>
 * </producer>
 * </pre>
 * <p>
 * The "delete-temp-files" element is optional and defaults to true.
 */
public class BitmapProducerJava2D extends AbstractBitmapProducer implements Configurable {

    // configure fopFactory as desired
    private final FopFactory fopFactory;

    private boolean deleteTempFiles;

    public BitmapProducerJava2D(URI baseUri) {
        super(baseUri);
        fopFactory = FopFactory.newInstance(baseUri);
    }

    public void configure(Configuration cfg) throws ConfigurationException {
        this.deleteTempFiles = cfg.getChild("delete-temp-files").getValueAsBoolean(true);
    }

    /** @see org.apache.fop.visual.BitmapProducer */
    public BufferedImage produce(File src, int index, ProducerContext context) {
        try {
            FOUserAgent userAgent = fopFactory.newFOUserAgent();
            userAgent.setTargetResolution(context.getTargetResolution());

            File outputFile = new File(context.getTargetDir(),
                    src.getName() + "." + index + ".java2d.png");
            OutputStream out = new FileOutputStream(outputFile);
            out = new BufferedOutputStream(out);
            try {
                Fop fop = fopFactory.newFop(MimeConstants.MIME_PNG, userAgent, out);
                SAXResult res = new SAXResult(fop.getDefaultHandler());

                Transformer transformer = getTransformer(context);
                transformer.setErrorListener(new DefaultErrorListener(log));
                transformer.transform(new StreamSource(src), res);
            } finally {
                IOUtils.closeQuietly(out);
            }

            BufferedImage img = BitmapComparator.getImage(outputFile);
            if (deleteTempFiles) {
                if (!outputFile.delete()) {
                    log.warn("Cannot delete " + outputFile);
                    outputFile.deleteOnExit();
                }
            }
            return img;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);
            return null;
        }
    }

}
