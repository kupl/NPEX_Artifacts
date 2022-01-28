package org.apache.maven.doxia.module.itext;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.Writer;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;
import org.apache.maven.doxia.sink.impl.AbstractTextSinkFactory;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;

/**
 * IText implementation of the Sink factory.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 1.0
 */
@Component( role = SinkFactory.class, hint = "itext" )
public class ITextSinkFactory
    extends AbstractTextSinkFactory
{
    /** {@inheritDoc} */
    protected Sink createSink( Writer writer, String encoding )
    {
        return new ITextSink( writer, encoding );
    }

    /**
     * createSink.
     *
     * @param writer a {@link java.io.Writer} object.
     * @return a {@link org.apache.maven.doxia.sink.Sink} object.
     */
    public Sink createSink( Writer writer )
    {
        // TODO: should this method be deprecated?
        return createSink( writer, WriterFactory.UTF_8 );
    }

    /**
     * Create a <code>Sink</code> into a PrettyPrintXMLWriter.
     *
     * @param xmlWriter not null XML writer to write the result.
     * @return a <code>Sink</code> instance.
     */
    public Sink createSink( PrettyPrintXMLWriter xmlWriter )
    {
        if ( xmlWriter == null )
        {
            throw new IllegalArgumentException( "xmlWriter could not be null." );
        }

        return new ITextSink( xmlWriter );
    }
}
