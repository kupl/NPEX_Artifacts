package org.apache.maven.doxia.module.rtf;

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
 * @version $Id$
 */
class RomanNumerals
{
    private static final int[] NUMBERS = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

    private static final String[] UPPER_CASE_LETTERS =
        {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private static final String[] LOWER_CASE_LETTERS =
        {"m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i"};

    // -----------------------------------------------------------------------

    static String toString( int n )
    {
        return toString( n, false );
    }

    static String toString( int n, boolean lowerCase )
    {
        StringBuilder roman = new StringBuilder();
        String[] letters = lowerCase ? LOWER_CASE_LETTERS : UPPER_CASE_LETTERS;

        for ( int i = 0; i < NUMBERS.length; ++i )
        {
            while ( n >= NUMBERS[i] )
            {
                roman.append( letters[i] );
                n -= NUMBERS[i];
            }
        }

        return roman.toString();
    }
}
