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

package org.apache.shardingsphere.driver.executor;

import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.context.metadata.MetaDataContexts;
import org.apache.shardingsphere.infra.executor.kernel.model.ExecutionGroup;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.ConnectionMode;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.ExecutorExceptionHandler;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutionUnit;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutor;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutorCallback;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResult;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.jdbc.MemoryJDBCQueryResult;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.jdbc.StreamJDBCQueryResult;
import org.apache.shardingsphere.infra.route.context.RouteUnit;
import org.apache.shardingsphere.infra.rule.type.DataNodeContainedRule;
import org.apache.shardingsphere.sql.parser.sql.common.statement.SQLStatement;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Prepared statement executor.
 */
public final class PreparedStatementExecutor extends AbstractStatementExecutor {
    
    public PreparedStatementExecutor(final Map<String, DataSource> dataSourceMap, final MetaDataContexts metaDataContexts, final JDBCExecutor jdbcExecutor) {
        super(dataSourceMap, metaDataContexts, jdbcExecutor);
    }
    
    @Override
    public List<QueryResult> executeQuery(final Collection<ExecutionGroup<JDBCExecutionUnit>> executionGroups) throws SQLException {
        boolean isExceptionThrown = ExecutorExceptionHandler.isExceptionThrown();
        JDBCExecutorCallback<QueryResult> callback = createJDBCExecutorCallbackWithQueryResult(isExceptionThrown);
        return getJdbcExecutor().execute(executionGroups, callback);
    }
    
    private JDBCExecutorCallback<QueryResult> createJDBCExecutorCallbackWithQueryResult(final boolean isExceptionThrown) {
        return new JDBCExecutorCallback<QueryResult>(getMetaDataContexts().getDatabaseType(), isExceptionThrown) {
            
            @Override
            protected QueryResult executeSQL(final String sql, final Statement statement, final ConnectionMode connectionMode) throws SQLException { 
                return createQueryResult(statement, connectionMode);
            }
            
            private QueryResult createQueryResult(final Statement statement, final ConnectionMode connectionMode) throws SQLException {
                PreparedStatement preparedStatement = (PreparedStatement) statement;
                ResultSet resultSet = preparedStatement.executeQuery();
                return ConnectionMode.MEMORY_STRICTLY == connectionMode ? new StreamJDBCQueryResult(resultSet) : new MemoryJDBCQueryResult(resultSet);
            }
        };
    }
    
    @Override
    public int executeUpdate(final Collection<ExecutionGroup<JDBCExecutionUnit>> executionGroups, 
                             final SQLStatementContext<?> sqlStatementContext, final Collection<RouteUnit> routeUnits) throws SQLException {
        boolean isExceptionThrown = ExecutorExceptionHandler.isExceptionThrown();
        JDBCExecutorCallback<Integer> callback = createJDBCExecutorCallbackWithInteger(isExceptionThrown);
        List<Integer> results = getJdbcExecutor().execute(executionGroups, callback);
        refreshSchema(getMetaDataContexts().getDefaultMetaData(), sqlStatementContext.getSqlStatement(), routeUnits);
        return isNeedAccumulate(getMetaDataContexts().getDefaultMetaData().getRuleMetaData().getRules().stream().filter(
            rule -> rule instanceof DataNodeContainedRule).collect(Collectors.toList()), sqlStatementContext) ? accumulate(results) : results.get(0);
    }
    
    private JDBCExecutorCallback<Integer> createJDBCExecutorCallbackWithInteger(final boolean isExceptionThrown) {
        return new JDBCExecutorCallback<Integer>(getMetaDataContexts().getDatabaseType(), isExceptionThrown) {
            
            @Override
            protected Integer executeSQL(final String sql, final Statement statement, final ConnectionMode connectionMode) throws SQLException {
                return ((PreparedStatement) statement).executeUpdate();
            }
        };
    }
    
    @Override
    public boolean execute(final Collection<ExecutionGroup<JDBCExecutionUnit>> executionGroups, final SQLStatement sqlStatement, final Collection<RouteUnit> routeUnits) throws SQLException {
        boolean isExceptionThrown = ExecutorExceptionHandler.isExceptionThrown();
        JDBCExecutorCallback<Boolean> callback = createJDBCExecutorCallbackWithBoolean(isExceptionThrown);
        return executeAndRefreshMetaData(executionGroups, sqlStatement, routeUnits, callback);
    }
    
    private JDBCExecutorCallback<Boolean> createJDBCExecutorCallbackWithBoolean(final boolean isExceptionThrown) {
        return new JDBCExecutorCallback<Boolean>(getMetaDataContexts().getDatabaseType(), isExceptionThrown) {
                    
            @Override
            protected Boolean executeSQL(final String sql, final Statement statement, final ConnectionMode connectionMode) throws SQLException {
                return ((PreparedStatement) statement).execute();
            }
        };
    }
}
