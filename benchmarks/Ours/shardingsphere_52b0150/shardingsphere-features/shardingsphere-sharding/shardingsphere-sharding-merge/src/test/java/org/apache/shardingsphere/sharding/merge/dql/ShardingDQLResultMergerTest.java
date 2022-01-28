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

package org.apache.shardingsphere.sharding.merge.dql;

import com.google.common.collect.ImmutableMap;
import org.apache.shardingsphere.infra.database.type.DatabaseTypes;
import org.apache.shardingsphere.infra.executor.sql.QueryResult;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.sharding.merge.dql.groupby.GroupByMemoryMergedResult;
import org.apache.shardingsphere.sharding.merge.dql.groupby.GroupByStreamMergedResult;
import org.apache.shardingsphere.sharding.merge.dql.iterator.IteratorStreamMergedResult;
import org.apache.shardingsphere.sharding.merge.dql.orderby.OrderByStreamMergedResult;
import org.apache.shardingsphere.sharding.merge.dql.pagination.LimitDecoratorMergedResult;
import org.apache.shardingsphere.sharding.merge.dql.pagination.RowNumberDecoratorMergedResult;
import org.apache.shardingsphere.sharding.merge.dql.pagination.TopAndRowNumberDecoratorMergedResult;
import org.apache.shardingsphere.sql.parser.binder.metadata.column.ColumnMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.schema.SchemaMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.table.TableMetaData;
import org.apache.shardingsphere.sql.parser.binder.segment.select.groupby.GroupByContext;
import org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByContext;
import org.apache.shardingsphere.sql.parser.binder.segment.select.orderby.OrderByItem;
import org.apache.shardingsphere.sql.parser.binder.segment.select.pagination.PaginationContext;
import org.apache.shardingsphere.sql.parser.binder.segment.select.projection.ProjectionsContext;
import org.apache.shardingsphere.sql.parser.binder.segment.select.projection.impl.AggregationProjection;
import org.apache.shardingsphere.sql.parser.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.sql.parser.sql.common.constant.AggregationType;
import org.apache.shardingsphere.sql.parser.sql.common.constant.OrderDirection;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.order.item.IndexOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.pagination.limit.NumberLiteralLimitValueSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.pagination.rownum.NumberLiteralRowNumberValueSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.statement.dml.SelectStatement;
import org.apache.shardingsphere.sql.parser.sql.common.value.identifier.IdentifierValue;
import org.apache.shardingsphere.sql.parser.sql.dialect.statement.mysql.dml.MySQLSelectStatement;
import org.apache.shardingsphere.sql.parser.sql.dialect.statement.oracle.dml.OracleSelectStatement;
import org.apache.shardingsphere.sql.parser.sql.dialect.statement.sqlserver.dml.SQLServerSelectStatement;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ShardingDQLResultMergerTest {
    
    @Test
    public void assertBuildIteratorStreamMergedResult() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("MySQL"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new MySQLSelectStatement()),
                new GroupByContext(Collections.emptyList(), 0), new OrderByContext(Collections.emptyList(), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), new PaginationContext(null, null, Collections.emptyList()));
        assertThat(resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData()), instanceOf(IteratorStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildIteratorStreamMergedResultWithLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("MySQL"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new MySQLSelectStatement()),
                new GroupByContext(Collections.emptyList(), 0), new OrderByContext(Collections.emptyList(), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralLimitValueSegment(0, 0, 1), null, Collections.emptyList()));
        assertThat(resultMerger.merge(Collections.singletonList(createQueryResult()), selectStatementContext, createSchemaMetaData()), instanceOf(IteratorStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildIteratorStreamMergedResultWithMySQLLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("MySQL"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new MySQLSelectStatement()),
                new GroupByContext(Collections.emptyList(), 0), new OrderByContext(Collections.emptyList(), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralLimitValueSegment(0, 0, 1), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(LimitDecoratorMergedResult.class));
        assertThat(((LimitDecoratorMergedResult) actual).getMergedResult(), instanceOf(IteratorStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildIteratorStreamMergedResultWithOracleLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("Oracle"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new OracleSelectStatement()),
                new GroupByContext(Collections.emptyList(), 0), new OrderByContext(Collections.emptyList(), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralRowNumberValueSegment(0, 0, 1, true), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(RowNumberDecoratorMergedResult.class));
        assertThat(((RowNumberDecoratorMergedResult) actual).getMergedResult(), instanceOf(IteratorStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildIteratorStreamMergedResultWithSQLServerLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("SQLServer"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new SQLServerSelectStatement()),
                new GroupByContext(Collections.emptyList(), 0), new OrderByContext(Collections.emptyList(), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralLimitValueSegment(0, 0, 1), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(TopAndRowNumberDecoratorMergedResult.class));
        assertThat(((TopAndRowNumberDecoratorMergedResult) actual).getMergedResult(), instanceOf(IteratorStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildOrderByStreamMergedResult() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("MySQL"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(
                createSelectStatement(new MySQLSelectStatement()), new GroupByContext(Collections.emptyList(), 0),
                new OrderByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), new PaginationContext(null, null, Collections.emptyList()));
        assertThat(resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData()), instanceOf(OrderByStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildOrderByStreamMergedResultWithMySQLLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("MySQL"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(
                createSelectStatement(new MySQLSelectStatement()), new GroupByContext(Collections.emptyList(), 0),
                new OrderByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralLimitValueSegment(0, 0, 1), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(LimitDecoratorMergedResult.class));
        assertThat(((LimitDecoratorMergedResult) actual).getMergedResult(), instanceOf(OrderByStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildOrderByStreamMergedResultWithOracleLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("Oracle"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(
                createSelectStatement(new OracleSelectStatement()), new GroupByContext(Collections.emptyList(), 0),
                new OrderByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralRowNumberValueSegment(0, 0, 1, true), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(RowNumberDecoratorMergedResult.class));
        assertThat(((RowNumberDecoratorMergedResult) actual).getMergedResult(), instanceOf(OrderByStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildOrderByStreamMergedResultWithSQLServerLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("SQLServer"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(
                createSelectStatement(new SQLServerSelectStatement()), new GroupByContext(Collections.emptyList(), 0),
                new OrderByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralRowNumberValueSegment(0, 0, 1, true), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(TopAndRowNumberDecoratorMergedResult.class));
        assertThat(((TopAndRowNumberDecoratorMergedResult) actual).getMergedResult(), instanceOf(OrderByStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByStreamMergedResult() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("MySQL"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new MySQLSelectStatement()),
                new GroupByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), 0),
                new OrderByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), new PaginationContext(null, null, Collections.emptyList()));
        assertThat(resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData()), instanceOf(GroupByStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByStreamMergedResultWithMySQLLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("MySQL"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new MySQLSelectStatement()),
                new GroupByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), 0),
                new OrderByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralLimitValueSegment(0, 0, 1), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(LimitDecoratorMergedResult.class));
        assertThat(((LimitDecoratorMergedResult) actual).getMergedResult(), instanceOf(GroupByStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByStreamMergedResultWithOracleLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("Oracle"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new OracleSelectStatement()),
                new GroupByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), 0),
                new OrderByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralRowNumberValueSegment(0, 0, 1, true), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(RowNumberDecoratorMergedResult.class));
        assertThat(((RowNumberDecoratorMergedResult) actual).getMergedResult(), instanceOf(GroupByStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByStreamMergedResultWithSQLServerLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("SQLServer"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new SQLServerSelectStatement()),
                new GroupByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), 0),
                new OrderByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralRowNumberValueSegment(0, 0, 1, true), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(TopAndRowNumberDecoratorMergedResult.class));
        assertThat(((TopAndRowNumberDecoratorMergedResult) actual).getMergedResult(), instanceOf(GroupByStreamMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByMemoryMergedResult() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("MySQL"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new MySQLSelectStatement()),
                new GroupByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), 0), 
                new OrderByContext(Collections.emptyList(), false), new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(null, null, Collections.emptyList()));
        assertThat(resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData()), instanceOf(GroupByMemoryMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByMemoryMergedResultWithMySQLLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("MySQL"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new MySQLSelectStatement()),
                new GroupByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), 0), 
                new OrderByContext(Collections.emptyList(), false), new ProjectionsContext(0, 0, false, Collections.emptyList()),
                new PaginationContext(new NumberLiteralLimitValueSegment(0, 0, 1), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(LimitDecoratorMergedResult.class));
        assertThat(((LimitDecoratorMergedResult) actual).getMergedResult(), instanceOf(GroupByMemoryMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByMemoryMergedResultWithOracleLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("Oracle"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new OracleSelectStatement()),
                new GroupByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC))), 0),
                new OrderByContext(Collections.singletonList(new OrderByItem(new IndexOrderByItemSegment(0, 0, 2, OrderDirection.DESC, OrderDirection.ASC))), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralRowNumberValueSegment(0, 0, 1, true), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(RowNumberDecoratorMergedResult.class));
        assertThat(((RowNumberDecoratorMergedResult) actual).getMergedResult(), instanceOf(GroupByMemoryMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByMemoryMergedResultWithSQLServerLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("SQLServer"));
        SelectStatementContext selectStatementContext = new SelectStatementContext(createSelectStatement(new SQLServerSelectStatement()),
                new GroupByContext(Arrays.asList(
                        new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.ASC)), 
                        new OrderByItem(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.ASC, OrderDirection.ASC))), 0), new OrderByContext(Collections.emptyList(), false),
                new ProjectionsContext(0, 0, false, Collections.emptyList()), 
                new PaginationContext(new NumberLiteralRowNumberValueSegment(0, 0, 1, true), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(TopAndRowNumberDecoratorMergedResult.class));
        assertThat(((TopAndRowNumberDecoratorMergedResult) actual).getMergedResult(), instanceOf(GroupByMemoryMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByMemoryMergedResultWithAggregationOnly() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("MySQL"));
        ProjectionsContext projectionsContext = new ProjectionsContext(
                0, 0, false, Collections.singletonList(new AggregationProjection(AggregationType.COUNT, "(*)", null)));
        SelectStatementContext selectStatementContext = new SelectStatementContext(
                createSelectStatement(new MySQLSelectStatement()), new GroupByContext(Collections.emptyList(), 0), new OrderByContext(Collections.emptyList(), false),
                projectionsContext, new PaginationContext(null, null, Collections.emptyList()));
        assertThat(resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData()), instanceOf(GroupByMemoryMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByMemoryMergedResultWithAggregationOnlyWithMySQLLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("MySQL"));
        ProjectionsContext projectionsContext = new ProjectionsContext(
                0, 0, false, Collections.singletonList(new AggregationProjection(AggregationType.COUNT, "(*)", null)));
        SelectStatementContext selectStatementContext = new SelectStatementContext(
                createSelectStatement(new MySQLSelectStatement()), new GroupByContext(Collections.emptyList(), 0), new OrderByContext(Collections.emptyList(), false),
                projectionsContext, new PaginationContext(new NumberLiteralLimitValueSegment(0, 0, 1), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(LimitDecoratorMergedResult.class));
        assertThat(((LimitDecoratorMergedResult) actual).getMergedResult(), instanceOf(GroupByMemoryMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByMemoryMergedResultWithAggregationOnlyWithOracleLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("Oracle"));
        ProjectionsContext projectionsContext = new ProjectionsContext(
                0, 0, false, Collections.singletonList(new AggregationProjection(AggregationType.COUNT, "(*)", null)));
        SelectStatementContext selectStatementContext = new SelectStatementContext(
                createSelectStatement(new OracleSelectStatement()), new GroupByContext(Collections.emptyList(), 0), new OrderByContext(Collections.emptyList(), false),
                        projectionsContext, new PaginationContext(new NumberLiteralRowNumberValueSegment(0, 0, 1, true), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(RowNumberDecoratorMergedResult.class));
        assertThat(((RowNumberDecoratorMergedResult) actual).getMergedResult(), instanceOf(GroupByMemoryMergedResult.class));
    }
    
    @Test
    public void assertBuildGroupByMemoryMergedResultWithAggregationOnlyWithSQLServerLimit() throws SQLException {
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypes.getActualDatabaseType("SQLServer"));
        ProjectionsContext projectionsContext = new ProjectionsContext(
                0, 0, false, Collections.singletonList(new AggregationProjection(AggregationType.COUNT, "(*)", null)));
        SelectStatementContext selectStatementContext = new SelectStatementContext(
                createSelectStatement(new SQLServerSelectStatement()), new GroupByContext(Collections.emptyList(), 0), new OrderByContext(Collections.emptyList(), false),
                projectionsContext, new PaginationContext(new NumberLiteralRowNumberValueSegment(0, 0, 1, true), null, Collections.emptyList()));
        MergedResult actual = resultMerger.merge(createQueryResults(), selectStatementContext, createSchemaMetaData());
        assertThat(actual, instanceOf(TopAndRowNumberDecoratorMergedResult.class));
        assertThat(((TopAndRowNumberDecoratorMergedResult) actual).getMergedResult(), instanceOf(GroupByMemoryMergedResult.class));
    }
    
    private List<QueryResult> createQueryResults() throws SQLException {
        List<QueryResult> result = new LinkedList<>();
        QueryResult queryResult = createQueryResult();
        result.add(queryResult);
        result.add(mock(QueryResult.class));
        result.add(mock(QueryResult.class));
        result.add(mock(QueryResult.class));
        return result;
    }
    
    private QueryResult createQueryResult() throws SQLException {
        QueryResult result = mock(QueryResult.class);
        when(result.getColumnCount()).thenReturn(1);
        when(result.getColumnLabel(1)).thenReturn("count(*)");
        when(result.getValue(1, Object.class)).thenReturn(0);
        return result;
    }
    
    private SchemaMetaData createSchemaMetaData() {
        ColumnMetaData columnMetaData1 = new ColumnMetaData("col1", 0, "dataType", false, false, false);
        ColumnMetaData columnMetaData2 = new ColumnMetaData("col2", 0, "dataType", false, false, false);
        ColumnMetaData columnMetaData3 = new ColumnMetaData("col3", 0, "dataType", false, false, false);
        TableMetaData tableMetaData = new TableMetaData(Arrays.asList(columnMetaData1, columnMetaData2, columnMetaData3), Collections.emptyList());
        return new SchemaMetaData(ImmutableMap.of("tbl", tableMetaData));
    }
    
    private SelectStatement createSelectStatement(final SelectStatement result) {
        SimpleTableSegment tableSegment = new SimpleTableSegment(10, 13, new IdentifierValue("tbl"));
        result.setFrom(tableSegment);
        ProjectionsSegment projectionsSegment = new ProjectionsSegment(0, 0);
        result.setProjections(projectionsSegment);
        return result;
    }
}
