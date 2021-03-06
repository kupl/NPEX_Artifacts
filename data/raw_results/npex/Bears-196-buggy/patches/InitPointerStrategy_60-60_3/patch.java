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
package org.apache.tamaya.spisupport;

import org.apache.tamaya.ConfigException;
import org.apache.tamaya.spi.ConversionContext;
import org.apache.tamaya.spi.PropertyConverter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converter, converting from String to the given enum type.
 */
public class EnumConverter<T> implements PropertyConverter<T> {
    private final Logger LOG = Logger.getLogger(EnumConverter.class.getName());
    private Class<T> enumType;
    private Method factory;

    public EnumConverter(Class<T> enumType) {
        if (!Enum.class.isAssignableFrom(enumType)) {
            throw new IllegalArgumentException("Not an Enum: " + enumType.getName());
        }
        this.enumType = Objects.requireNonNull(enumType);
        try {
            this.factory = enumType.getMethod("valueOf", String.class);
        } catch (NoSuchMethodException e) {
            throw new ConfigException("Uncovertible enum type without createValue method found, please provide a custom " +
                    "PropertyConverter for: " + enumType.getName());
        }
    }

    @Override
public T convert(java.lang.String value, org.apache.tamaya.spi.ConversionContext ctx) {
    ctx.addSupportedFormats(getClass(), "<enumValue>");
    try {
        return ((T) (factory.invoke(null, value)));
    } catch (java.lang.reflect.InvocationTargetException | java.lang.IllegalAccessException e) {
        /* NPEX_PATCH_BEGINS */
        if (value == null) {
            value = "";
        }
        LOG.log(java.util.logging.Level.FINEST, (("Invalid enum createValue '" + value) + "' for ") + enumType.getName(), e);
    }
    try {
        return ((T) (factory.invoke(null, value.toUpperCase(java.util.Locale.ENGLISH))));
    } catch (java.lang.reflect.InvocationTargetException | java.lang.IllegalAccessException e) {
        LOG.log(java.util.logging.Level.FINEST, (("Invalid enum createValue '" + value) + "' for ") + enumType.getName(), e);
    }
    return null;
}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnumConverter)) return false;
        EnumConverter<?> that = (EnumConverter<?>) o;
        return Objects.equals(enumType, that.enumType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enumType);
    }
}
