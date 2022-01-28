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

import org.apache.maven.doxia.util.DoxiaUtils;

/**
 * A collection of utility methods for dealing with APT documents.
 *
 * @author ltheussl
 * @since 1.1
 * @version $Id$
 */
public class AptUtils
{

    /**
     * Replace all characters in a text.
     *
     * <pre>
     * AptTools.encodeFragment( null ) = null
     * AptTools.encodeFragment( "" ) = ""
     * AptTools.encodeFragment( "http://www.google.com" ) = "httpwwwgooglecom"
     * </pre>
     *
     * @param text the String to check, may be null.
     * @return the text with only letter and digit, null if null String input.
     * @deprecated This method was used for the original apt format, which
     * removed all non alphanumeric characters from anchors.
     * Use {@link #encodeAnchor(String)} instead.
     */
    public static String encodeFragment( String text )
    {
        if ( text == null )
        {
            return null;
        }

        return linkToKey( text );
    }

    /**
     * Checks if the given string corresponds to an external URI,
     * ie is not a link within the same document nor a link to another
     * document within the same site. This forwards to
     * {@link org.apache.maven.doxia.util.DoxiaUtils#isExternalLink(String)}.
     *
     * @param link The link to check.
     * @return True if DoxiaUtils.isExternalLink(link) returns true.
     *
     * @see org.apache.maven.doxia.util.DoxiaUtils#isExternalLink(String)
     * @see #isInternalLink(String)
     * @see #isLocalLink(String)
     */
    public static boolean isExternalLink( String link )
    {
        return DoxiaUtils.isExternalLink( link );
    }

    /**
     * Checks if the given string corresponds to an internal link,
     * ie it is a link to an anchor within the same document.
     *
     * @param link The link to check.
     * @return True if link is neither an {@link #isExternalLink(String) external}
     * nor a {@link #isLocalLink(String) local} link.
     *
     * @see org.apache.maven.doxia.util.DoxiaUtils#isInternalLink(String)
     * @see #isExternalLink(String)
     * @see #isLocalLink(String)
     */
    public static boolean isInternalLink( String link )
    {
        return ( !isExternalLink( link ) && !isLocalLink( link ) );
    }

    /**
     * Checks if the given string corresponds to a relative link to another document
     * within the same site.
     *
     * @param link The link to check.
     * @return True if the link starts with either "/", "./" or "../".
     *
     * @see org.apache.maven.doxia.util.DoxiaUtils#isLocalLink(String)
     * @see #isExternalLink(String)
     * @see #isInternalLink(String)
     */
    public static boolean isLocalLink( String link )
    {
        return ( link.startsWith( "/" ) || link.startsWith( "./" ) || link.startsWith( "../" ) );
    }

    /**
     * Transforms the given text such that it can be used as a link.
     * All non-LetterOrDigit characters are removed and the remaining
     * characters are transformed to lower-case.
     *
     * @param text The text to transform.
     * @return The text with all non-LetterOrDigit characters removed.
     * @deprecated This method was used for the original apt format, which
     * removed all non alphanumeric characters from anchors.
     * Use {@link #encodeAnchor(String)} instead.
     */
    public static String linkToKey( String text )
    {
        int length = text.length();
        StringBuilder buffer = new StringBuilder( length );

        for ( int i = 0; i < length; ++i )
        {
            char c = text.charAt( i );
            if ( Character.isLetterOrDigit( c ) )
            {
                buffer.append( Character.toLowerCase( c ) );
            }
        }

        return buffer.toString();
    }

    /**
     * Construct a valid anchor. This is a simplified version of
     * {@link org.apache.maven.doxia.util.DoxiaUtils#encodeId(String)}
     * to ensure the anchor is a valid Doxia id.
     * The procedure is identical to the one in
     * {@link org.apache.maven.doxia.util.HtmlTools#encodeId(String)}:
     *
     * <ol>
     *   <li> Trim the id</li>
     *   <li> If the first character is not a letter, prepend the letter 'a'</li>
     *   <li> Any space is replaced with an underscore '_'</li>
     *   <li> Remove any non alphanumeric characters  except ':', '_', '.', '-'.</li>
     * </ol>
     *
     * @param id The id to be encoded.
     * @return The trimmed and encoded id, or null if id is null.
     */
    public static String encodeAnchor( String id )
    {
        if ( id == null )
        {
            return null;
        }

        id = id.trim();

        int length = id.length();
        StringBuilder buffer = new StringBuilder( length );

        for ( int i = 0; i < length; ++i )
        {
            char c = id.charAt( i );

            if ( ( i == 0 ) && ( !Character.isLetter( c ) ) )
            {
                buffer.append( 'a' );
            }

            if ( c == ' ' )
            {
                buffer.append( '_' );
            }
            else if ( ( Character.isLetterOrDigit( c ) ) || ( c == '-' ) || ( c == '_' ) || ( c == ':' )
                            || ( c == '.' ) )
            {
                buffer.append( c );
            }
        }

        return buffer.toString();
    }

    private AptUtils()
    {
        // utility class
    }
}
