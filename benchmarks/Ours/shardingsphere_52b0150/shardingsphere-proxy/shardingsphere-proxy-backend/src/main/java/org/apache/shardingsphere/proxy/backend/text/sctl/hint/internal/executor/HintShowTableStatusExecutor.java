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

package org.apache.shardingsphere.proxy.backend.text.sctl.hint.internal.executor;

import com.google.common.base.Joiner;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.executor.sql.raw.execute.result.query.QueryHeader;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.connection.BackendConnection;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.text.sctl.hint.internal.command.HintShowTableStatusCommand;
import org.apache.shardingsphere.proxy.backend.text.sctl.hint.internal.result.HintShowTableStatusResult;
import org.apache.shardingsphere.sharding.merge.dal.common.MultipleLocalDataMergedResult;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hint show table status command executor.
 */
@RequiredArgsConstructor
public final class HintShowTableStatusExecutor extends AbstractHintQueryExecutor<HintShowTableStatusCommand> {
    
    private final BackendConnection backendConnection;
    
    @Override
    protected List<QueryHeader> createQueryHeaders() {
        List<QueryHeader> result = new ArrayList<>(3);
        result.add(new QueryHeader("", "", "table_name", "", 255, Types.CHAR, 0, false, false, false, false));
        result.add(new QueryHeader("", "", "database_sharding_values", "", 255, Types.CHAR, 0, false, false, false, false));
        result.add(new QueryHeader("", "", "table_sharding_values", "", 255, Types.CHAR, 0, false, false, false, false));
        return result;
    }
    
    @Override
    protected MergedResult createMergedResult() {
        Map<String, HintShowTableStatusResult> results = new HashMap<>();
        Collection<String> tableNames = 
                ProxyContext.getInstance().getSchema(backendConnection.getSchemaName()).getSchema().getMetaData().getRuleSchemaMetaData().getConfiguredSchemaMetaData().getAllTableNames();
        for (String each : tableNames) {
            if (HintManager.isDatabaseShardingOnly()) {
                fillShardingValues(results, each, HintManager.getDatabaseShardingValues(), Collections.emptyList());
            } else {
                fillShardingValues(results, each, HintManager.getDatabaseShardingValues(each), HintManager.getTableShardingValues(each));
            }
        }
        return convertToMergedResult(results.values());
    }
    
    private void fillShardingValues(final Map<String, HintShowTableStatusResult> results, final String logicTable,
                                    final Collection<Comparable<?>> databaseShardingValues, final Collection<Comparable<?>> tableShardingValues) {
        if (!results.containsKey(logicTable)) {
            results.put(logicTable, new HintShowTableStatusResult(logicTable));
        }
        for (Comparable<?> each : databaseShardingValues) {
            results.get(logicTable).getDatabaseShardingValues().add(each.toString());
        }
        for (Comparable<?> each : tableShardingValues) {
            results.get(logicTable).getTableShardingValues().add(each.toString());
        }
    }
    
    private MergedResult convertToMergedResult(final Collection<HintShowTableStatusResult> hintShowTableStatusResults) {
        Collection<List<Object>> values = new ArrayList<>(hintShowTableStatusResults.size());
        for (HintShowTableStatusResult each : hintShowTableStatusResults) {
            values.add(createRow(each));
        }
        return new MultipleLocalDataMergedResult(values);
    }
    
    private List<Object> createRow(final HintShowTableStatusResult hintShowTableStatusResult) {
        List<Object> row = new ArrayList<>(3);
        row.add(hintShowTableStatusResult.getLogicTable());
        row.add(Joiner.on(",").join(hintShowTableStatusResult.getDatabaseShardingValues()));
        row.add(Joiner.on(",").join(hintShowTableStatusResult.getTableShardingValues()));
        return row;
    }
}
