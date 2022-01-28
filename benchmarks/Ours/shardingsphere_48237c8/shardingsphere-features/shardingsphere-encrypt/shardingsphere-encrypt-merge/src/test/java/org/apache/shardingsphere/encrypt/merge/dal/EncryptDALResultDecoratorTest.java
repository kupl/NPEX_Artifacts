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

package org.apache.shardingsphere.encrypt.merge.dal;

import org.apache.shardingsphere.encrypt.merge.dal.impl.DecoratedEncryptColumnsMergedResult;
import org.apache.shardingsphere.encrypt.merge.dal.impl.MergedEncryptColumnsMergedResult;
import org.apache.shardingsphere.infra.executor.sql.QueryResult;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.infra.merge.result.impl.transparent.TransparentMergedResult;
import org.apache.shardingsphere.sql.parser.binder.metadata.schema.SchemaMetaData;
import org.apache.shardingsphere.sql.parser.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.sql.parser.binder.statement.dal.DescribeStatementContext;
import org.apache.shardingsphere.sql.parser.binder.statement.dal.ShowColumnsStatementContext;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.table.TableNameSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.dal.dialect.mysql.DescribeStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.dal.dialect.mysql.ShowColumnsStatement;
import org.apache.shardingsphere.sql.parser.sql.value.identifier.IdentifierValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class EncryptDALResultDecoratorTest {
    
    @Mock
    private SchemaMetaData schemaMetaData;

    @Mock
    private SQLStatementContext sqlStatementContext;

    @Test
    public void assertMergedResultWithDescribeStatement() {
        schemaMetaData = mock(SchemaMetaData.class);
        sqlStatementContext = getDescribeStatementContext();
        EncryptDALResultDecorator encryptDALResultDecorator = new EncryptDALResultDecorator();
        MergedResult mergedResult = encryptDALResultDecorator.decorate(mock(QueryResult.class), sqlStatementContext, schemaMetaData);
        assertThat(mergedResult, instanceOf(MergedEncryptColumnsMergedResult.class));
        mergedResult = encryptDALResultDecorator.decorate(mock(MergedResult.class), sqlStatementContext, schemaMetaData);
        assertThat(mergedResult, instanceOf(DecoratedEncryptColumnsMergedResult.class));
    }

    @Test
    public void assertMergedResultWithShowColumnsStatement() {
        schemaMetaData = mock(SchemaMetaData.class);
        sqlStatementContext = getShowColumnsStatementContext();
        EncryptDALResultDecorator encryptDALResultDecorator = new EncryptDALResultDecorator();
        MergedResult mergedResult = encryptDALResultDecorator.decorate(mock(QueryResult.class), sqlStatementContext, schemaMetaData);
        assertThat(mergedResult, instanceOf(MergedEncryptColumnsMergedResult.class));
        mergedResult = encryptDALResultDecorator.decorate(mock(MergedResult.class), sqlStatementContext, schemaMetaData);
        assertThat(mergedResult, instanceOf(DecoratedEncryptColumnsMergedResult.class));
    }

    @Test
    public void assertMergedResultWithOtherStatement() {
        schemaMetaData = mock(SchemaMetaData.class);
        sqlStatementContext = mock(SQLStatementContext.class);
        EncryptDALResultDecorator encryptDALResultDecorator = new EncryptDALResultDecorator();
        MergedResult mergedResult = encryptDALResultDecorator.decorate(mock(QueryResult.class), sqlStatementContext, schemaMetaData);
        assertThat(mergedResult, instanceOf(TransparentMergedResult.class));
        mergedResult = encryptDALResultDecorator.decorate(mock(MergedResult.class), sqlStatementContext, schemaMetaData);
        assertThat(mergedResult, instanceOf(MergedResult.class));
    }

    private SQLStatementContext getDescribeStatementContext() {
        DescribeStatementContext sqlStatementContext = mock(DescribeStatementContext.class);
        SimpleTableSegment simpleTableSegment = getSimpleTableSegment();
        when(sqlStatementContext.getAllTables()).thenReturn(Collections.singletonList(simpleTableSegment));
        when(sqlStatementContext.getSqlStatement()).thenReturn(mock(DescribeStatement.class));
        return sqlStatementContext;
    }

    private SQLStatementContext getShowColumnsStatementContext() {
        ShowColumnsStatementContext sqlStatementContext = mock(ShowColumnsStatementContext.class);
        SimpleTableSegment simpleTableSegment = getSimpleTableSegment();
        when(sqlStatementContext.getAllTables()).thenReturn(Collections.singletonList(simpleTableSegment));
        when(sqlStatementContext.getSqlStatement()).thenReturn(mock(ShowColumnsStatement.class));
        return sqlStatementContext;
    }

    private SimpleTableSegment getSimpleTableSegment() {
        IdentifierValue identifierValue = new IdentifierValue("test");
        TableNameSegment tableNameSegment = new TableNameSegment(1, 4, identifierValue);
        SimpleTableSegment simpleTableSegment = new SimpleTableSegment(tableNameSegment);
        return simpleTableSegment;
    }

}
