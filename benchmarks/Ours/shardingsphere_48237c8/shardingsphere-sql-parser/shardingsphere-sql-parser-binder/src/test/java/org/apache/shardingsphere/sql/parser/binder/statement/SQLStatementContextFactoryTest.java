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

package org.apache.shardingsphere.sql.parser.binder.statement;

import org.apache.shardingsphere.sql.parser.binder.SQLStatementContextFactory;
import org.apache.shardingsphere.sql.parser.binder.metadata.schema.SchemaMetaData;
import org.apache.shardingsphere.sql.parser.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.sql.parser.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.assignment.AssignmentSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.assignment.SetAssignmentSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.pagination.limit.LimitSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.sql.parser.sql.value.identifier.IdentifierValue;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public final class SQLStatementContextFactoryTest {
    
    @Test
    public void assertSQLStatementContextCreatedWhenSQLStatementInstanceOfSelectStatement() {
        ProjectionsSegment projectionsSegment = new ProjectionsSegment(0, 0);
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.setLimit(new LimitSegment(0, 10, null, null));
        selectStatement.setProjections(projectionsSegment);
        SQLStatementContext sqlStatementContext = SQLStatementContextFactory.newInstance(mock(SchemaMetaData.class), null, selectStatement);
        assertNotNull(sqlStatementContext);
        assertTrue(sqlStatementContext instanceof SelectStatementContext);
    }
    
    @Test
    public void assertSQLStatementContextCreatedWhenSQLStatementInstanceOfInsertStatement() {
        InsertStatement insertStatement = new InsertStatement();
        insertStatement.setSetAssignment(new SetAssignmentSegment(0, 0,
                Collections.singleton(new AssignmentSegment(0, 0, new ColumnSegment(0, 0, new IdentifierValue("IdentifierValue")), null))));
        insertStatement.setTable(new SimpleTableSegment(0, 0, new IdentifierValue("tbl")));
        SQLStatementContext sqlStatementContext = SQLStatementContextFactory.newInstance(mock(SchemaMetaData.class), null, insertStatement);
        assertNotNull(sqlStatementContext);
        assertTrue(sqlStatementContext instanceof InsertStatementContext);
    }
    
    @Test
    public void assertSQLStatementContextCreatedWhenSQLStatementNotInstanceOfSelectStatementAndInsertStatement() {
        SQLStatementContext sqlStatementContext = SQLStatementContextFactory.newInstance(mock(SchemaMetaData.class), null, mock(SQLStatement.class));
        assertNotNull(sqlStatementContext);
        assertTrue(sqlStatementContext instanceof CommonSQLStatementContext);
    }
}
