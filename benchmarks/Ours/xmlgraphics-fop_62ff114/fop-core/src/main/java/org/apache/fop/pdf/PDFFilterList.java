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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class represents a list of PDF filters to be applied when serializing
 * the output of a PDF object.
 */
public class PDFFilterList {

    /** Key for the default filter */
    public static final String DEFAULT_FILTER = "default";
    /** Key for the filter used for normal content*/
    public static final String CONTENT_FILTER = "content";
    /** Key for the filter used for precompressed content */
    public static final String PRECOMPRESSED_FILTER = "precompressed";
    /** Key for the filter used for images */
    public static final String IMAGE_FILTER = "image";
    /** Key for the filter used for JPEG images */
    public static final String JPEG_FILTER = "jpeg";
    /** Key for the filter used for TIFF images */
    public static final String TIFF_FILTER = "tiff";
    /** Key for the filter used for fonts */
    public static final String FONT_FILTER = "font";
    /** Key for the filter used for metadata */
    public static final String METADATA_FILTER = "metadata";

    private List<PDFFilter> filters = new java.util.ArrayList<PDFFilter>();

    private boolean ignoreASCIIFilters;

    private boolean disableAllFilters;

    /**
     * Default constructor.
     * <p>
     * The flag for ignoring ASCII filters defaults to false.
     */
    public PDFFilterList() {
        //nop
    }

    /**
     * Use this descriptor if you want to have ASCII filters (such as ASCIIHex
     * and ASCII85) ignored, for example, when encryption is active.
     * @param ignoreASCIIFilters true if ASCII filters should be ignored
     */
    public PDFFilterList(boolean ignoreASCIIFilters) {
        this.ignoreASCIIFilters = ignoreASCIIFilters;
    }

    /**
     * Used to disable all filters.
     * @param value true if all filters shall be disabled
     */
    public void setDisableAllFilters(boolean value) {
        this.disableAllFilters = value;
    }

    /**
     * Returns true if all filters are disabled.
     * @return true if all filters are disabled
     */
    public boolean isDisableAllFilters() {
        return this.disableAllFilters;
    }

    /**
     * Indicates whether the filter list is already initialized.
     * @return true if more there are filters present
     */
    public boolean isInitialized() {
        return this.filters.size() > 0;
    }

    /**
     * Add a filter for compression of the stream. Filters are
     * applied in the order they are added. This should always be a
     * new instance of the particular filter of choice. The applied
     * flag in the filter is marked true after it has been applied to the
     * data.
     * @param filter filter to add
     */
    public void addFilter(PDFFilter filter) {
        if (filter != null) {
            if (this.ignoreASCIIFilters && filter.isASCIIFilter()) {
                return; //ignore ASCII filter
            }
            filters.add(filter);
        }
    }

    /**
     * Add a filter for compression of the stream by name.
     * @param filterType name of the filter to add
     */
    public void addFilter(String filterType) {
        if (filterType == null) {
            return;
        }
        if (filterType.equals("flate")) {
            addFilter(new FlateFilter());
        } else if (filterType.equals("null")) {
            addFilter(new NullFilter());
        } else if (filterType.equals("ascii-85")) {
            if (this.ignoreASCIIFilters) {
                return; //ignore ASCII filter
            }
            addFilter(new ASCII85Filter());
        } else if (filterType.equals("ascii-hex")) {
            if (this.ignoreASCIIFilters) {
                return; //ignore ASCII filter
            }
            addFilter(new ASCIIHexFilter());
        } else if (filterType.equals("")) {
            return;
        } else {
            throw new IllegalArgumentException(
                "Unsupported filter type in stream-filter-list: " + filterType);
        }
    }

    /**
     * Checks the filter list for the filter and adds it in the correct
     * place if necessary.
     * @param pdfFilter the filter to check / add
     */
    public void ensureFilterInPlace(PDFFilter pdfFilter) {
        if (this.filters.size() == 0) {
            addFilter(pdfFilter);
        } else {
            if (!(this.filters.get(0).equals(pdfFilter))) {
                this.filters.add(0, pdfFilter);
            }
        }
    }

    /**
     * Adds the default filters to this stream.
     * @param filters Map of filters
     * @param type which filter list to modify
     */
    public void addDefaultFilters(Map filters, String type) {
        if (METADATA_FILTER.equals(type)) {
            //XMP metadata should not be embedded in clear-text
            addFilter(new NullFilter());
            return;
        }
        List filterset = null;
        if (filters != null) {
            filterset = (List)filters.get(type);
            if (filterset == null) {
                filterset = (List)filters.get(DEFAULT_FILTER);
            }
        }
        if (filterset == null || filterset.size() == 0) {
            if (JPEG_FILTER.equals(type)) {
                //JPEG is already well compressed
                addFilter(new NullFilter());
            } else if (TIFF_FILTER.equals(type)) {
                //CCITT-encoded images are already well compressed
                addFilter(new NullFilter());
            } else if (PRECOMPRESSED_FILTER.equals(type)) {
                //precompressed content doesn't need further compression
                addFilter(new NullFilter());
            } else {
                // built-in default to flate
                addFilter(new FlateFilter());
            }
        } else {
            for (Object aFilterset : filterset) {
                String v = (String) aFilterset;
                addFilter(v);
            }
        }
    }

    List<PDFFilter> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    /**
     * Apply the filters to the data
     * in the order given and return the /Filter and /DecodeParms
     * entries for the stream dictionary. If the filters have already
     * been applied to the data (either externally, or internally)
     * then the dictionary entries are built and returned.
     * @return a String representing the filter list
     */
    protected String buildFilterDictEntries() {
        if (filters.size() > 0) {
            List names = new java.util.ArrayList();
            List parms = new java.util.ArrayList();

            int nonNullParams = populateNamesAndParms(names, parms);

            // now build up the filter entries for the dictionary
            return buildFilterEntries(names)
                    + (nonNullParams > 0 ? buildDecodeParms(parms) : "");
        }
        return "";

    }

    /**
     * Apply the filters to the data
     * in the order given and add the /Filter and /DecodeParms
     * entries to the stream dictionary. If the filters have already
     * been applied to the data (either externally, or internally)
     * then the dictionary entries added.
     * @param dict the PDFDictionary to set the entries on
     */
    protected void putFilterDictEntries(PDFDictionary dict) {
        if (filters.size() > 0) {
            List names = new java.util.ArrayList();
            List parms = new java.util.ArrayList();

            populateNamesAndParms(names, parms);

            // now build up the filter entries for the dictionary
            putFilterEntries(dict, names);
            putDecodeParams(dict, parms);
        }
    }

    private int populateNamesAndParms(List names, List parms) {
        // run the filters
        int nonNullParams = 0;
        for (PDFFilter filter : filters) {
            // place the names in our local vector in reverse order
            if (filter.getName().length() > 0) {
                names.add(0, filter.getName());
                PDFObject param = filter.getDecodeParms();
                if (param != null) {
                    parms.add(0, param);
                    nonNullParams++;
                } else {
                    parms.add(0, null);
                }
            }
        }
        return nonNullParams;
    }

    private String buildFilterEntries(List names) {
        int filterCount = 0;
        StringBuffer sb = new StringBuffer(64);
        for (Object name1 : names) {
            final String name = (String) name1;
            if (name.length() > 0) {
                filterCount++;
                sb.append(name);
                sb.append(" ");
            }
        }
        if (filterCount > 0) {
            if (filterCount > 1) {
                return "/Filter [ " + sb + "]";
            } else {
                return "/Filter " + sb;
            }
        } else {
            return "";
        }
    }

    private void putFilterEntries(PDFDictionary dict, List names) {
        PDFArray array = new PDFArray(dict);
        for (Object name1 : names) {
            final String name = (String) name1;
            if (name.length() > 0) {
                array.add(new PDFName(name));
            }
        }
        if (array.length() > 0) {
            if (array.length() > 1) {
                dict.put("Filter", array);
            } else {
                dict.put("Filter", array.get(0));
            }
        }
    }

    private String buildDecodeParms(List parms) {
        StringBuffer sb = new StringBuffer();
        boolean needParmsEntry = false;
        sb.append("\n/DecodeParms ");

        if (parms.size() > 1) {
            sb.append("[ ");
        }
        for (Object parm : parms) {
            String s = (String) parm;
            if (s != null) {
                sb.append(s);
                needParmsEntry = true;
            } else {
                sb.append("null");
            }
            sb.append(" ");
        }
        if (parms.size() > 1) {
            sb.append("]");
        }
        if (needParmsEntry) {
            return sb.toString();
        } else {
            return "";
        }
    }

    private void putDecodeParams(PDFDictionary dict, List parms) {
        boolean needParmsEntry = false;
        PDFArray array = new PDFArray(dict);
        for (Object obj : parms) {
            if (obj != null) {
                array.add(obj);
                needParmsEntry = true;
            } else {
                array.add(null);
            }
        }
        if (array.length() > 0 & needParmsEntry) {
            if (array.length() > 1) {
                dict.put("DecodeParms", array);
            } else {
                dict.put("DecodeParms", array.get(0));
            }
        }
    }

    /**
     * Applies all registered filters as necessary. The method returns an
     * OutputStream which will receive the filtered contents.
     * @param stream raw data output stream
     * @return OutputStream filtered output stream
     * @throws IOException In case of an I/O problem
     */
    public OutputStream applyFilters(OutputStream stream) throws IOException {
        OutputStream out = stream;
        if (!isDisableAllFilters()) {
            for (int count = filters.size() - 1; count >= 0; count--) {
                PDFFilter filter = filters.get(count);
                out = filter.applyFilter(out);
            }
        }
        return out;
    }
}
