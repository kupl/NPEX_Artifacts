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

import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.properties.Property;

/**
 * Class for managing the "min" Number Function. See Sec. 5.10.1 in the XSL-FO
 * standard.
 */
public class MinFunction extends FunctionBase {

    /** {@inheritDoc} */
    public int getRequiredArgsCount() {
        return 2;
    }

    /** {@inheritDoc} */
    public Property eval(Property[] args, PropertyInfo pInfo) throws PropertyException {
        Numeric n1 = args[0].getNumeric();
        Numeric n2 = args[1].getNumeric();
        if (n1 == null || n2 == null) {
            throw new PropertyException("Non numeric operands to min function");
        }
        return (Property) NumericOp.min(n1, n2);
       }

}

