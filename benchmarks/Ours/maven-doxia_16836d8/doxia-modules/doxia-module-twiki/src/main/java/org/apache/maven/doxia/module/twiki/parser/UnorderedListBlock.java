package org.apache.maven.doxia.module.twiki.parser;

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

/**
 * <pre>
 *    - item1
 *    - item2
 *        - item2.1
 *        ...
 * </pre>
 *
 * @author Juan F. Codagnone
 * @version $Id$
 */
class UnorderedListBlock
    extends ListBlock
{
    /**
     * Creates the UnorderedListBlock.
     *
     * @param blocks list of list items
     * @throws IllegalArgumentException if listItemBlocks is <code>null</code>
     */
    UnorderedListBlock( final ListItemBlock[] blocks )
    {
        super( blocks );
    }

    /** {@inheritDoc} */
    final void before( final Sink sink )
    {
        sink.list();
    }

    /** {@inheritDoc} */
    final void after( final Sink sink )
    {
        sink.list_();
    }
}
