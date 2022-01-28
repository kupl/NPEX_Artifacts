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

package org.apache.shardingsphere.sql.parser.binder.statement.dml;

import lombok.Getter;
import lombok.ToString;
import org.apache.shardingsphere.sql.parser.binder.segment.table.TablesContext;
import org.apache.shardingsphere.sql.parser.binder.statement.CommonSQLStatementContext;
import org.apache.shardingsphere.sql.parser.binder.type.TableAvailable;
import org.apache.shardingsphere.sql.parser.binder.type.WhereAvailable;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.statement.dml.UpdateStatement;
import org.apache.shardingsphere.sql.parser.sql.common.extractor.TableExtractor;

import java.util.Collection;
import java.util.Optional;

/**
 * Update SQL statement context.
 */
@Getter
@ToString(callSuper = true)
public final class UpdateStatementContext extends CommonSQLStatementContext<UpdateStatement> implements TableAvailable, WhereAvailable {
    
    private final TablesContext tablesContext;
    
    public UpdateStatementContext(final UpdateStatement sqlStatement) {
        super(sqlStatement);
        TableExtractor tableExtractor = new TableExtractor();
        tableExtractor.extractTablesFromUpdate(sqlStatement);
        tablesContext = new TablesContext(tableExtractor.getRewriteTables());
    }
    
    @Override
    public Collection<SimpleTableSegment> getAllTables() {
        TableExtractor tableExtractor = new TableExtractor();
        tableExtractor.extractTablesFromUpdate(getSqlStatement());
        return tableExtractor.getRewriteTables();
    }
    
    @Override
    public Optional<WhereSegment> getWhere() {
        return getSqlStatement().getWhere();
    }
}
