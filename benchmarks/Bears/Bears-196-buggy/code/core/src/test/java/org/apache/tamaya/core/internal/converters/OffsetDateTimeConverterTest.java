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
package org.apache.tamaya.core.internal.converters;

import org.apache.tamaya.spi.ConversionContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.OffsetDateTime;
import org.apache.tamaya.TypeLiteral;

import static org.assertj.core.api.Assertions.*;

/**
 * Created by atsti on 02.10.2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class OffsetDateTimeConverterTest {

    @Mock
    ConversionContext context;

    @Test
    public void convert() throws Exception {
        OffsetDateTimeConverter conv = new OffsetDateTimeConverter();
        OffsetDateTime value = conv.convert("2007-12-03T10:15:30+01:00", context);
        assertThat(OffsetDateTime.parse("2007-12-03T10:15:30+01:00")).isEqualTo(value);
        value = conv.convert("foo", context);
        assertThat(value).isNull();
    }

    @Test
    public void equalsAndHashcode() throws Exception {
        OffsetDateTimeConverter conv1 = new OffsetDateTimeConverter();
        OffsetDateTimeConverter conv2 = new OffsetDateTimeConverter();
        assertThat(conv2).isEqualTo(conv1);
        assertThat(conv2.hashCode()).isEqualTo(conv1.hashCode());
        assertThat(conv2).isNotEqualTo(new OffsetTimeConverter());
        assertThat(conv2).isNotEqualTo(null);
    }

    @Test
    public void callToConvertAddsMoreSupportedFormatsToTheContext() throws Exception {
        ConversionContext context = new ConversionContext.Builder(TypeLiteral.of(OffsetDateTime.class)).build();

        OffsetDateTimeConverter converter = new OffsetDateTimeConverter();
        converter.convert("", context);


        assertThat(context.getSupportedFormats().toString().contains(" (OffsetDateTimeConverter)")).isTrue();
    }

    @Test
    public void testHashCode() {
        OffsetDateTimeConverter instance = new OffsetDateTimeConverter();
        assertThat(instance.hashCode()).isEqualTo(OffsetDateTimeConverter.class.hashCode());
    }
}
