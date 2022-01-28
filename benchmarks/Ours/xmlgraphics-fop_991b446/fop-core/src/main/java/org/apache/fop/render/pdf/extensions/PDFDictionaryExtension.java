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

package org.apache.fop.render.pdf.extensions;

import java.util.List;
import java.util.Map;

// CSOFF: LineLengthCheck

public class PDFDictionaryExtension extends PDFCollectionExtension {

    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_PAGE_NUMBERS = "page-numbers";

    private static final long serialVersionUID = -1L;

    private PDFDictionaryType dictionaryType;
    private Map<String, String> properties;
    private List<PDFCollectionEntryExtension> entries;

    PDFDictionaryExtension() {
        this(PDFDictionaryType.Dictionary);
    }

    PDFDictionaryExtension(PDFDictionaryType dictionaryType) {
        super(PDFObjectType.Dictionary);
        this.dictionaryType = dictionaryType;
        this.properties = new java.util.HashMap<String, String>();
        this.entries = new java.util.ArrayList<PDFCollectionEntryExtension>();
    }

    @Override
    public void setValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getValue() {
        return getEntries();
    }

    public PDFDictionaryType getDictionaryType() {
        return dictionaryType;
    }

    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public void addEntry(PDFCollectionEntryExtension entry) {
        if ((entry.getKey() == null) || (entry.getKey().length() == 0)) {
            throw new IllegalArgumentException("pdf:dictionary key is empty");
        } else {
            entries.add(entry);
        }
    }

    public List<PDFCollectionEntryExtension> getEntries() {
        return entries;
    }

    public PDFCollectionEntryExtension findEntry(String key) {
        for (PDFCollectionEntryExtension entry : entries) {
            String entryKey = entry.getKey();
            if ((entryKey != null) && entryKey.equals(key)) {
                return entry;
            }
        }
        return null;
    }

    public Object findEntryValue(String key) {
        for (PDFCollectionEntryExtension entry : entries) {
            String entryKey = entry.getKey();
            if ((entryKey != null) && entryKey.equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public PDFCollectionEntryExtension getLastEntry() {
        if (entries.size() > 0) {
            return entries.get(entries.size() - 1);
        } else {
            return null;
        }
    }

    public boolean usesIDAttribute() {
        return dictionaryType.usesIDAttribute();
    }

    @Override
    public String getElementName() {
        return dictionaryType.elementName();
    }

}
