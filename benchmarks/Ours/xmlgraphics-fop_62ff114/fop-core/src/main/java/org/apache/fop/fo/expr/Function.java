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

import org.apache.fop.datatypes.PercentBase;
import org.apache.fop.fo.properties.Property;

/**
 * Interface for managing XSL-FO Functions
 */
public interface Function {

    /**
     * @return the number of required (non-optional) arguments that must be specified
     * in the argument list
     */
    int getRequiredArgsCount();

    /**
     * @return the number of non-required (optional) arguments that may be specified
     * in the argument list, which, if specified, must follow the required arguments
     */
    int getOptionalArgsCount();

    /**
     * @param index of optional argument
     * @param pi property information instance that applies to property being evaluated
     * @return the default property value for the optional argument at INDEX, where
     * INDEX is with respect to optional arguments; i.e., the first optional argument
     * position is index 0; if no default for a given index, then null is returned
     * @throws PropertyException if index is greater than or equal to optional args count
     */
    Property getOptionalArgDefault(int index, PropertyInfo pi) throws PropertyException;

    /**
     * Determine if function allows variable arguments. If it does, then they must appear
     * after required and optional arguments, and all optional arguments must be specified.
     * @return true if function permits additional variable number of arguments after
     * required and (completely specified) optional arguments
     */
    boolean hasVariableArgs();

    /**
     * @return the basis for percentage calculations
     */
    PercentBase getPercentBase();

    /**
     * Evaluate the function
     * @param args an array of Properties that should be evaluated
     * @param pi property information instance that applies to property being evaluated
     * @return the Property satisfying the function
     * @throws PropertyException for problems when evaluating the function
     */
    Property eval(Property[] args, PropertyInfo pi) throws PropertyException;

}
