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

package org.apache.fop.render.rtf.rtflib.rtfdoc;

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz bdelacretaz@codeconsult.ch and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Converts java Strings according to RTF conventions.</p>
 *
 * <p>This work was authored by Bertrand Delacretaz (bdelacretaz@codeconsult.ch).</p>
 */

public final class RtfStringConverter {

    private static final RtfStringConverter INSTANCE = new RtfStringConverter();
    private static final Map SPECIAL_CHARS;
    private static final Character DBLQUOTE = '\"';
    private static final Character QUOTE = '\'';
    private static final Character SPACE = ' ';

    /** List of characters to escape with corresponding replacement strings */
    static {
        SPECIAL_CHARS = new HashMap();
        SPECIAL_CHARS.put('\t', "tab");
        SPECIAL_CHARS.put('\n', "line");
        SPECIAL_CHARS.put('\'', "rquote");
        SPECIAL_CHARS.put('\"', "rdblquote");
        SPECIAL_CHARS.put('\\', "\\");
        SPECIAL_CHARS.put('{', "{");
        SPECIAL_CHARS.put('}', "}");
    }

    /** singleton pattern */
    private RtfStringConverter() {
    }

    /**
     * use this to get an object of this class
     * @return the singleton instance
     */
    public static RtfStringConverter getInstance() {
        return INSTANCE;
    }

    /**
     * Write given String to given Writer, converting characters as required by
     * RTF spec
     * @param w Writer
     * @param str String to be written
     * @throws IOException for I/O problems
     */
    public void writeRtfString(Writer w, String str) throws IOException {
        if (str == null) {
            return;
        }
        w.write(escape(str));
    }

    /**
     * Escapes a String as required by the RTF spec.
     * @param str String to be escaped
     * @return the escaped string
     */
    public String escape(String str) {
        if (str == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer(Math.max(16, str.length()));
        // TODO: could be made more efficient (binary lookup, etc.)
        for (int i = 0; i < str.length(); i++) {
            final Character c = str.charAt(i);
            Character d;
            String replacement;
            if (i != 0) {
                d = str.charAt(i - 1);
            } else {
                d = SPACE;
            }

            //This section modified by Chris Scott
            //add "smart" quote recognition
            if (c.equals((Object)DBLQUOTE) && d.equals((Object)SPACE)) {
                replacement = "ldblquote";
            } else if (c.equals((Object)QUOTE) && d.equals((Object)SPACE)) {
                replacement = "lquote";
            } else {
                replacement = (String)SPECIAL_CHARS.get(c);
            }

            if (replacement != null) {
                // RTF-escaped char
                sb.append('\\');
                sb.append(replacement);
                sb.append(' ');
            } else if (c > 127) {
                // write unicode representation - contributed by Michel Jacobson
                // <jacobson@idf.ext.jussieu.fr>
                sb.append("\\u");
                sb.append(Integer.toString((int) c));
                sb.append("\\\'3f");
            } else {
                // plain char that is understood by RTF natively
                sb.append(c.charValue());
            }
        }
        return sb.toString();
    }

}
