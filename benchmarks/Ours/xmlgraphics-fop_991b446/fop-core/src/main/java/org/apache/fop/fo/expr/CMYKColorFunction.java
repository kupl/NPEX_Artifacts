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

package org.apache.fop.fo.expr;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.properties.ColorProperty;
import org.apache.fop.fo.properties.Property;

/**
 * Implements the cmyk() function.
 */
class CMYKColorFunction extends FunctionBase {

    /** {@inheritDoc} */
    public int getRequiredArgsCount() {
        return 4;
    }

    /** {@inheritDoc} */
    public Property eval(Property[] args, PropertyInfo pInfo) throws PropertyException {
        StringBuffer sb = new StringBuffer();
        sb.append("cmyk(" + args[0] + "," + args[1] + "," + args[2] + "," + args[3] + ")");
        FOUserAgent ua = (pInfo == null)
                ? null
                : (pInfo.getFO() == null ? null : pInfo.getFO().getUserAgent());
        return ColorProperty.getInstance(ua, sb.toString());
    }


}
