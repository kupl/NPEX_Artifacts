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

package org.apache.fop.afp.fonts;

import org.apache.fop.afp.fonts.CharactersetEncoder.DefaultEncoder;
import org.apache.fop.afp.fonts.CharactersetEncoder.EbcdicDoubleByteLineDataEncoder;

/**
 * An enumeration of AFP characterset types.
 */
public enum CharacterSetType {
    /** Double byte character sets; these do NOT have the shift-in;shift-out operators */
    DOUBLE_BYTE {
        @Override
        CharactersetEncoder getEncoder(String encoding) {
            return new DefaultEncoder(encoding, true);
        }
    },
    /** Double byte character sets; these can have the shift-in;shift-out operators */
    DOUBLE_BYTE_LINE_DATA {
        @Override
        CharactersetEncoder getEncoder(String encoding) {
            return new EbcdicDoubleByteLineDataEncoder(encoding);
        }
    },
    SINGLE_BYTE {
        @Override
        CharactersetEncoder getEncoder(String encoding) {
            return new DefaultEncoder(encoding, false);
        }
    };

    /**
     * Returns the character-set encoder
     *
     * @param encoding
     * @return
     */
    abstract CharactersetEncoder getEncoder(String encoding);
}
