/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.encrypt.merge.dql.impl;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.apache.shardingsphere.encrypt.merge.dql.fixture.EncryptColumnsMergedResultFixture;
import org.apache.shardingsphere.encrypt.merge.dql.fixture.TableAvailableAndSqlStatementContextFixture;
import org.apache.shardingsphere.encrypt.metadata.EncryptColumnMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.column.ColumnMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.schema.SchemaMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.table.TableMetaData;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.TableNameSegment;
import org.apache.shardingsphere.sql.parser.sql.common.value.identifier.IdentifierValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLFeatureNotSupportedException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class EncryptColumnsMergedResultTest {
    
    @Mock
    private TableAvailableAndSqlStatementContextFixture tableAvailableAndSqlStatementContextFixture;
    
    @Mock
    private SchemaMetaData schemaMetaData;
    
    @Mock
    private TableMetaData tableMetaData;
    
    private EncryptColumnsMergedResultFixture encryptColumnsMergedResultFixture;
    
    @SneakyThrows
    @Before
    public void setUp() { 
        Map<String, ColumnMetaData> columns = new HashMap<>();
        EncryptColumnMetaData encryptColumnMetaData = new EncryptColumnMetaData("order", 1, "Integer", false, "status", "status", "status");
        columns.put("", encryptColumnMetaData);
        SimpleTableSegment simpleTableSegment = mock(SimpleTableSegment.class);
        TableNameSegment tableNameSegment = mock(TableNameSegment.class);
        IdentifierValue identifierValue = mock(IdentifierValue.class);
        when(tableAvailableAndSqlStatementContextFixture.getAllTables()).thenReturn(Lists.newArrayList(simpleTableSegment));
        when(simpleTableSegment.getTableName()).thenReturn(tableNameSegment);
        when(tableNameSegment.getIdentifier()).thenReturn(identifierValue);
        String tableName = "t_order";
        when(identifierValue.getValue()).thenReturn(tableName);
        when(schemaMetaData.get(anyString())).thenReturn(tableMetaData);
        when(tableMetaData.getColumns()).thenReturn(columns);
        encryptColumnsMergedResultFixture = spy(new EncryptColumnsMergedResultFixture(tableAvailableAndSqlStatementContextFixture, schemaMetaData));
    }
    
    @SneakyThrows
    @Test
    public void assertHasNextWithEmptyColumnMetaData() {
        when(schemaMetaData.get(anyString())).thenReturn(tableMetaData);
        when(tableMetaData.getColumns()).thenReturn(Collections.emptyMap());
        EncryptColumnsMergedResultFixture encryptColumnsMergedResultFixture = spy(new EncryptColumnsMergedResultFixture(tableAvailableAndSqlStatementContextFixture, schemaMetaData));
        when(encryptColumnsMergedResultFixture.nextValue()).thenReturn(true).thenReturn(false);
        assertThat(encryptColumnsMergedResultFixture.next(), is(true));
    }
    
    @SneakyThrows
    @Test
    public void assertWithoutHasNext() {
        EncryptColumnsMergedResultFixture encryptColumnsMergedResultFixture = spy(new EncryptColumnsMergedResultFixture(tableAvailableAndSqlStatementContextFixture, schemaMetaData));
        when(encryptColumnsMergedResultFixture.nextValue()).thenReturn(false);
        assertThat(encryptColumnsMergedResultFixture.next(), is(false));
    }
    
    @Test
    @SneakyThrows
    public void assertContainerColumnName() {
        Map<String, ColumnMetaData> columns = new HashMap<>();
        EncryptColumnMetaData encryptColumnMetaData = new EncryptColumnMetaData("order", 1, "Integer", false, "status", "status", "status");
        columns.put("", encryptColumnMetaData);
        when(schemaMetaData.get(anyString())).thenReturn(tableMetaData);
        when(tableMetaData.getColumns()).thenReturn(columns);
        EncryptColumnsMergedResultFixture encryptColumnsMergedResultFixture = spy(new EncryptColumnsMergedResultFixture(tableAvailableAndSqlStatementContextFixture, schemaMetaData));
        when(encryptColumnsMergedResultFixture.nextValue()).thenReturn(true).thenReturn(true);
        when(encryptColumnsMergedResultFixture.getOriginalValue(1, String.class)).thenReturn("status").thenReturn("noInThatCollection");
        assertThat(encryptColumnsMergedResultFixture.next(), is(true));
    }
    
    @SneakyThrows
    @Test
    public void assertGetValueWithColumnIndex() {
        Map<String, ColumnMetaData> columns = new HashMap<>();
        EncryptColumnMetaData encryptColumnMetaData = new EncryptColumnMetaData("order", 1, "Integer", false, "status", "status", "status");
        columns.put("key", encryptColumnMetaData);
        when(schemaMetaData.get(anyString())).thenReturn(tableMetaData);
        when(tableMetaData.getColumns()).thenReturn(columns);
        when(encryptColumnsMergedResultFixture.getOriginalValue(1, String.class)).thenReturn("status");
        assertThat(encryptColumnsMergedResultFixture.getValue(1, String.class), is("key"));
    }
    
    @SneakyThrows
    @Test
    public void assertGetValueWithOutColumnIndex() {
        when(encryptColumnsMergedResultFixture.getOriginalValue(2, String.class)).thenReturn("status");
        assertThat(encryptColumnsMergedResultFixture.getValue(2, String.class), is("status"));
    }
    
    @SneakyThrows
    @Test(expected = SQLFeatureNotSupportedException.class)
    public void assertGetCalendarValue() {
        encryptColumnsMergedResultFixture.getCalendarValue(1, String.class, Calendar.getInstance());
    }
    
    @SneakyThrows
    @Test(expected = SQLFeatureNotSupportedException.class)
    public void assertGetInputStream() {
        encryptColumnsMergedResultFixture.getInputStream(1, "whateverString");
    }
}
