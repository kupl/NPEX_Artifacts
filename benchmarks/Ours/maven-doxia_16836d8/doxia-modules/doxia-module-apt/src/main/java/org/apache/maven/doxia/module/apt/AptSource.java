package org.apache.maven.doxia.module.apt;

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

/**
 * An interface to read apt source documents.
 *
 * @version $Id$
 */
public interface AptSource
{
    /**
     * Returns a line of the apt source document.
     *
     * @return a line of the apt source.
     * @throws org.apache.maven.doxia.module.apt.AptParseException if the document can't be parsed.
     */
    String getNextLine()
        throws AptParseException;

    /**
     * Returns the name the apt source document.
     *
     * @return the name the apt source document.
     */
    String getName();

    /**
     * Gets the current line number while parsing the document.
     *
     * @return the line number.
     */
    int getLineNumber();
}

