/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.mapper.internal;

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.Converter;

public class ConverterAdapter<A> implements Adapter<A, String> {
    private final Converter<A> converter;

    public ConverterAdapter(final Converter<A> converter) {
        this.converter = converter;
    }

    public Converter<A> getConverter() {
        return converter;
    }

    @Override
    public A to(final String s) {
        return converter.fromString(s);
    }

    @Override
    public String from(final A a) {
        return converter.toString(a);
    }
}
