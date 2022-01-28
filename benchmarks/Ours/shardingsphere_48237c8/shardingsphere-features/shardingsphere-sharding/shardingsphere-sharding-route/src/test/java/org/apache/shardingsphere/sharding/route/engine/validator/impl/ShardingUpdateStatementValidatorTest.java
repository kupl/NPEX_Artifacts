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

package org.apache.shardingsphere.sharding.route.engine.validator.impl;

import org.apache.shardingsphere.infra.exception.ShardingSphereException;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.infra.route.context.RouteResult;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext;
import org.apache.shardingsphere.sql.parser.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.sql.parser.binder.statement.dml.UpdateStatementContext;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.assignment.AssignmentSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.assignment.SetAssignmentSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.AndPredicate;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.PredicateSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.value.PredicateCompareRightValue;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.UpdateStatement;
import org.apache.shardingsphere.sql.parser.sql.value.identifier.IdentifierValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class ShardingUpdateStatementValidatorTest {
    
    @Mock
    private ShardingRule shardingRule;

    @Test(expected = ShardingSphereException.class)
    public void assertValidateUpdateModifyMultiTables() {
        SQLStatementContext<UpdateStatement> sqlStatementContext = new UpdateStatementContext(createUpdateStatement());
        sqlStatementContext.getTablesContext().getTables().addAll(createMultiTablesContext().getTables());
        RouteContext routeContext = new RouteContext(sqlStatementContext, Collections.emptyList(), new RouteResult());
        new ShardingUpdateStatementValidator().preValidate(shardingRule, routeContext, mock(ShardingSphereMetaData.class));
    }

    @Test
    public void assertValidateUpdateWithoutShardingKey() {
        when(shardingRule.isShardingColumn("id", "user")).thenReturn(false);
        RouteContext routeContext = new RouteContext(new UpdateStatementContext(createUpdateStatement()), Collections.emptyList(), new RouteResult());
        new ShardingUpdateStatementValidator().preValidate(shardingRule, routeContext, mock(ShardingSphereMetaData.class));
    }
    
    @Test(expected = ShardingSphereException.class)
    public void assertValidateUpdateWithShardingKey() {
        when(shardingRule.isShardingColumn("id", "user")).thenReturn(true);
        RouteContext routeContext = new RouteContext(new UpdateStatementContext(createUpdateStatement()), Collections.emptyList(), new RouteResult());
        new ShardingUpdateStatementValidator().preValidate(shardingRule, routeContext, mock(ShardingSphereMetaData.class));
    }
    
    @Test
    public void assertValidateUpdateWithoutShardingKeyAndParameters() {
        when(shardingRule.isShardingColumn("id", "user")).thenReturn(false);
        List<Object> parameters = Arrays.asList(1, 1);
        RouteContext routeContext = new RouteContext(new UpdateStatementContext(createUpdateStatement()), parameters, new RouteResult());
        new ShardingUpdateStatementValidator().preValidate(shardingRule, routeContext, mock(ShardingSphereMetaData.class));
    }
    
    @Test
    public void assertValidateUpdateWithShardingKeyAndShardingParameterEquals() {
        when(shardingRule.isShardingColumn("id", "user")).thenReturn(true);
        List<Object> parameters = Arrays.asList(1, 1);
        SQLStatementContext<UpdateStatement> updateStatementContext = new UpdateStatementContext(createUpdateStatementAndParameters(1));
        RouteContext routeContext = new RouteContext(updateStatementContext, parameters, new RouteResult());
        new ShardingUpdateStatementValidator().preValidate(shardingRule, routeContext, mock(ShardingSphereMetaData.class));
    }
    
    @Test(expected = ShardingSphereException.class)
    public void assertValidateUpdateWithShardingKeyAndShardingParameterNotEquals() {
        when(shardingRule.isShardingColumn("id", "user")).thenReturn(true);
        List<Object> parameters = Arrays.asList(1, 1);
        SQLStatementContext<UpdateStatement> updateStatementContext = new UpdateStatementContext(createUpdateStatementAndParameters(2));
        RouteContext routeContext = new RouteContext(updateStatementContext, parameters, new RouteResult());
        new ShardingUpdateStatementValidator().preValidate(shardingRule, routeContext, mock(ShardingSphereMetaData.class));
    }
    
    private UpdateStatement createUpdateStatement() {
        UpdateStatement result = new UpdateStatement();
        result.getTables().add(new SimpleTableSegment(0, 0, new IdentifierValue("user")));
        result.setSetAssignment(
                new SetAssignmentSegment(0, 0, Collections.singletonList(new AssignmentSegment(0, 0, new ColumnSegment(0, 0, new IdentifierValue("id")), new LiteralExpressionSegment(0, 0, "")))));
        return result;
    }
    
    private UpdateStatement createUpdateStatementAndParameters(final Object shardingColumnParameter) {
        UpdateStatement result = new UpdateStatement();
        result.getTables().add(new SimpleTableSegment(0, 0, new IdentifierValue("user")));
        Collection<AssignmentSegment> assignments = Collections.singletonList(
                new AssignmentSegment(0, 0, new ColumnSegment(0, 0, new IdentifierValue("id")), new LiteralExpressionSegment(0, 0, shardingColumnParameter)));
        SetAssignmentSegment setAssignmentSegment = new SetAssignmentSegment(0, 0, assignments);
        result.setSetAssignment(setAssignmentSegment);
        WhereSegment where = new WhereSegment(0, 0);
        AndPredicate andPre = new AndPredicate();
        andPre.getPredicates().add(new PredicateSegment(0, 1,
                new ColumnSegment(0, 0, new IdentifierValue("id")), new PredicateCompareRightValue(0, 0, "=", new ParameterMarkerExpressionSegment(0, 0, 0))));
        where.getAndPredicates().add(andPre);
        result.setWhere(where);
        return result;
    }

    private TablesContext createMultiTablesContext() {
        List<SimpleTableSegment> result = new LinkedList<>();
        result.add(new SimpleTableSegment(0, 0, new IdentifierValue("user")));
        result.add(new SimpleTableSegment(0, 0, new IdentifierValue("order")));
        return new TablesContext(result);
    }
}
