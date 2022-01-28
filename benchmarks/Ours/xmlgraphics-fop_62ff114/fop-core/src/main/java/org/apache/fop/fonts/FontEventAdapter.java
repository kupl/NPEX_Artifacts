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

package org.apache.fop.fonts;

import org.apache.fop.events.EventBroadcaster;

/**
 * Event listener interface for font-related events. This interface extends FontEventListener
 * and EventProducer for integration into FOP's event subsystem.
 */
public class FontEventAdapter implements FontEventListener {

    private final EventBroadcaster eventBroadcaster;

    private FontEventProducer eventProducer;

    /**
     * Creates a new FontEventAdapter.
     * @param broadcaster the event broadcaster to send the generated events to
     */
    public FontEventAdapter(EventBroadcaster broadcaster) {
        this.eventBroadcaster = broadcaster;
    }

    private FontEventProducer getEventProducer() {
        if (eventProducer == null) {
            eventProducer = FontEventProducer.Provider.get(eventBroadcaster);
        }
        return eventProducer;
    }

    /** {@inheritDoc} */
    public void fontSubstituted(Object source, FontTriplet requested, FontTriplet effective) {
        getEventProducer().fontSubstituted(source, requested, effective);
    }

    /** {@inheritDoc} */
    public void fontLoadingErrorAtAutoDetection(Object source, String fontURL, Exception e) {
        getEventProducer().fontLoadingErrorAtAutoDetection(source, fontURL, e);
    }

    /** {@inheritDoc} */
    public void glyphNotAvailable(Object source, char ch, String fontName) {
        getEventProducer().glyphNotAvailable(source, ch, fontName);
    }

    /** {@inheritDoc} */
    public void fontDirectoryNotFound(Object source, String dir) {
        getEventProducer().fontDirectoryNotFound(source, dir);
    }

    /** {@inheritDoc} */
    public void svgTextStrokedAsShapes(Object source, String fontFamily) {
        getEventProducer().svgTextStrokedAsShapes(source, fontFamily);
    }

    /** {@inheritDoc} */
    public void fontFeatureNotSuppprted(Object source, String feature, String onlySupportedIn) {
        getEventProducer().fontFeatureNotSuppprted(source, feature, onlySupportedIn);
    }

}
