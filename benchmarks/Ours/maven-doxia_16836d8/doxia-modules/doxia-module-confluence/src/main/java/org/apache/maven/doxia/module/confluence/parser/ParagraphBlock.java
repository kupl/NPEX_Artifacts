package org.apache.maven.doxia.module.confluence.parser;

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

import org.apache.maven.doxia.sink.Sink;

import java.util.List;

/**
 * @author Juan F. Codagnone
 * @version $Id$
 */
class ParagraphBlock
    extends AbstractFatherBlock
{

    private boolean generateParagraphTags = true;

    ParagraphBlock( List<Block> blocks )
    {
        super( blocks );
    }

    ParagraphBlock( List<Block> blocks, boolean generateParagraphTags )
    {
        super( blocks );
        this.generateParagraphTags = generateParagraphTags;
    }

    /** {@inheritDoc} */
    public  void before(  Sink sink )
    {
        if ( this.generateParagraphTags )
        {
            sink.paragraph();
        }
    }

    /** {@inheritDoc} */
    public  void after(  Sink sink )
    {
        if ( this.generateParagraphTags )
        {
            sink.paragraph_();
        }
    }
}
