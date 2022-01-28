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

package org.apache.shardingsphere.sql.parser.binder.segment.select.pagination;

import com.google.common.collect.Lists;
import org.apache.shardingsphere.sql.parser.sql.constant.OrderDirection;
import org.apache.shardingsphere.sql.parser.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.GroupBySegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.OrderBySegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.IndexOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.pagination.PaginationValueSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.pagination.limit.NumberLiteralLimitValueSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.pagination.limit.ParameterMarkerLimitValueSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class PaginationContextTest {
    
    @Test
    public void assertSegmentWithNullOffsetSegment() {
        PaginationValueSegment rowCountSegment = getRowCountSegment();
        PaginationContext paginationContext = new PaginationContext(null, rowCountSegment, getParameters());
        assertTrue(paginationContext.isHasPagination());
        assertNull(paginationContext.getOffsetSegment().orElse(null));
        assertThat(paginationContext.getRowCountSegment().orElse(null), is(rowCountSegment));
    }
    
    @Test
    public void assertGetSegmentWithRowCountSegment() {
        PaginationValueSegment offsetSegment = getOffsetSegment();
        PaginationContext paginationContext = new PaginationContext(offsetSegment, null, getParameters());
        assertTrue(paginationContext.isHasPagination());
        assertThat(paginationContext.getOffsetSegment().orElse(null), is(offsetSegment));
        assertNull(paginationContext.getRowCountSegment().orElse(null));
    }
    
    @Test
    public void assertGetActualOffset() {
        assertThat(new PaginationContext(getOffsetSegment(), getRowCountSegment(), getParameters()).getActualOffset(), is(30L));
    }
    
    @Test
    public void assertGetActualOffsetWithNumberLiteralPaginationValueSegment() {
        assertThat(new PaginationContext(getOffsetSegmentWithNumberLiteralPaginationValueSegment(), 
                getRowCountSegmentWithNumberLiteralPaginationValueSegment(), getParameters()).getActualOffset(), is(30L));
    }
    
    @Test
    public void assertGetActualOffsetWithNullOffsetSegment() {
        assertThat(new PaginationContext(null, getRowCountSegment(), getParameters()).getActualOffset(), is(0L));
    }
    
    @Test
    public void assertGetActualRowCount() {
        assertThat(new PaginationContext(getOffsetSegment(), getRowCountSegment(), getParameters()).getActualRowCount().orElse(null), is(20L));
    }
    
    @Test
    public void assertGetActualRowCountWithNumberLiteralPaginationValueSegment() {
        assertThat(new PaginationContext(getOffsetSegmentWithNumberLiteralPaginationValueSegment(),
            getRowCountSegmentWithNumberLiteralPaginationValueSegment(), getParameters()).getActualRowCount().orElse(null), is(20L));
    }
    
    @Test
    public void assertGetActualRowCountWithNullRowCountSegment() {
        assertNull(new PaginationContext(getOffsetSegment(), null, getParameters()).getActualRowCount().orElse(null));
    }
    
    private PaginationValueSegment getOffsetSegmentWithNumberLiteralPaginationValueSegment() {
        return new NumberLiteralLimitValueSegment(28, 30, 30);
    }
    
    private PaginationValueSegment getRowCountSegmentWithNumberLiteralPaginationValueSegment() {
        return new NumberLiteralLimitValueSegment(32, 34, 20);
    }
    
    @Test
    public void assertGetOffsetParameterIndex() {
        assertThat(new PaginationContext(getOffsetSegment(), getRowCountSegment(), getParameters()).getOffsetParameterIndex().orElse(null), is(0));
    }
    
    @Test
    public void assertGetRowCountParameterIndex() {
        assertThat(new PaginationContext(getOffsetSegment(), getRowCountSegment(), getParameters()).getRowCountParameterIndex().orElse(null), is(1));
    }
    
    private PaginationValueSegment getOffsetSegment() {
        return new ParameterMarkerLimitValueSegment(28, 30, 0);
    }
    
    private PaginationValueSegment getRowCountSegment() {
        return new ParameterMarkerLimitValueSegment(32, 34, 1);
    }
    
    private List<Object> getParameters() {
        return Lists.newArrayList(30, 20);
    }
    
    @Test
    public void assertGetRevisedOffset() {
        assertThat(new PaginationContext(getOffsetSegment(), getRowCountSegment(), getParameters()).getRevisedOffset(), is(0L));
    }
    
    @Test
    public void getRevisedRowCount() {
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.setProjections(new ProjectionsSegment(0, 0));
        SelectStatementContext selectStatementContext = new SelectStatementContext(null, Collections.emptyList(), selectStatement);
        assertThat(new PaginationContext(getOffsetSegment(), getRowCountSegment(), getParameters()).getRevisedRowCount(selectStatementContext), is(50L));
    }
    
    @Test
    public void getRevisedRowCountWithMax() {
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.setProjections(new ProjectionsSegment(0, 0));
        selectStatement.setGroupBy(new GroupBySegment(0, 0, Collections.singletonList(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.ASC, OrderDirection.DESC))));
        selectStatement.setOrderBy(new OrderBySegment(0, 0, Collections.singletonList(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.DESC, OrderDirection.DESC))));
        SelectStatementContext selectStatementContext = new SelectStatementContext(null, Collections.emptyList(), selectStatement);
        assertThat(new PaginationContext(getOffsetSegment(), getRowCountSegment(), getParameters()).getRevisedRowCount(selectStatementContext), is((long) Integer.MAX_VALUE));
    }
}
