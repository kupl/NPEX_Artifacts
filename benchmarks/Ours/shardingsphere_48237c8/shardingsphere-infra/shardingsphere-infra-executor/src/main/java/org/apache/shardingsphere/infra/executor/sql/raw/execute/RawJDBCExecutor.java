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

package org.apache.shardingsphere.infra.executor.sql.raw.execute;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.executor.kernel.ExecutorKernel;
import org.apache.shardingsphere.infra.executor.kernel.InputGroup;
import org.apache.shardingsphere.infra.executor.sql.QueryResult;
import org.apache.shardingsphere.infra.executor.sql.raw.RawSQLExecuteUnit;
import org.apache.shardingsphere.infra.executor.sql.raw.execute.callback.RawSQLExecutorCallback;
import org.apache.shardingsphere.infra.executor.sql.raw.execute.result.ExecuteResult;
import org.apache.shardingsphere.infra.executor.sql.raw.execute.result.query.ExecuteQueryResult;
import org.apache.shardingsphere.infra.executor.sql.raw.execute.result.update.ExecuteUpdateResult;
import org.apache.shardingsphere.infra.executor.sql.resourced.jdbc.executor.ExecutorExceptionHandler;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Raw JDBC executor.
 */
@RequiredArgsConstructor
public final class RawJDBCExecutor {
    
    private final ExecutorKernel executorKernel;
    
    private final boolean serial;
    
    /**
     * Execute query.
     *
     * @param inputGroups input groups
     * @param callback raw SQL execute callback
     * @return Query results
     * @throws SQLException SQL exception
     */
    public List<QueryResult> executeQuery(final Collection<InputGroup<RawSQLExecuteUnit>> inputGroups, final RawSQLExecutorCallback callback) throws SQLException {
        return doExecute(inputGroups, callback).stream().map(each -> ((ExecuteQueryResult) each).getQueryResult()).collect(Collectors.toList());
    }
    
    /**
     * Execute update.
     *
     * @param inputGroups input groups
     * @param callback raw SQL execute callback
     * @return update count
     * @throws SQLException SQL exception
     */
    public int executeUpdate(final Collection<InputGroup<RawSQLExecuteUnit>> inputGroups, final RawSQLExecutorCallback callback) throws SQLException {
        List<Integer> results = doExecute(inputGroups, callback).stream().map(each -> ((ExecuteUpdateResult) each).getUpdateCount()).collect(Collectors.toList());
        // TODO check is need to accumulate
        // TODO refresh metadata
        return accumulate(results);
    }
    
    private int accumulate(final List<Integer> results) {
        int result = 0;
        for (Integer each : results) {
            result += null == each ? 0 : each;
        }
        return result;
    }
    
    /**
     * Execute.
     *
     * @param inputGroups input groups
     * @param callback raw SQL execute callback
     * @return return true if is DQL, false if is DML
     * @throws SQLException SQL exception
     */
    public boolean execute(final Collection<InputGroup<RawSQLExecuteUnit>> inputGroups, final RawSQLExecutorCallback callback) throws SQLException {
        List<ExecuteResult> results = doExecute(inputGroups, callback);
        // TODO refresh metadata
        if (null == results || results.isEmpty() || null == results.get(0)) {
            return false;
        }
        return results.get(0) instanceof ExecuteQueryResult;
    }
    
    @SuppressWarnings("unchecked")
    private <T> List<T> doExecute(final Collection<InputGroup<RawSQLExecuteUnit>> inputGroups, final RawSQLExecutorCallback callback) throws SQLException {
        try {
            return executorKernel.execute((Collection) inputGroups, null, callback, serial);
        } catch (final SQLException ex) {
            ExecutorExceptionHandler.handleException(ex);
            return Collections.emptyList();
        }
    }
}
