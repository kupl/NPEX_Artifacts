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

package org.apache.shardingsphere.infra.metadata.refresh.impl;

import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.refresh.MetaDataRefreshStrategy;
import org.apache.shardingsphere.infra.metadata.refresh.TableMetaDataLoaderCallback;
import org.apache.shardingsphere.sql.parser.binder.metadata.schema.SchemaMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.table.TableMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.table.TableMetaDataLoader;
import org.apache.shardingsphere.sql.parser.binder.statement.ddl.CreateTableStatementContext;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Create table statement meta data refresh strategy.
 */
public final class CreateTableStatementMetaDataRefreshStrategy implements MetaDataRefreshStrategy<CreateTableStatementContext> {
    
    @Override
    public void refreshMetaData(final ShardingSphereMetaData metaData, final DatabaseType databaseType,
                                final Map<String, DataSource> dataSourceMap, final CreateTableStatementContext sqlStatementContext, final TableMetaDataLoaderCallback callback) throws SQLException {
        String tableName = sqlStatementContext.getSqlStatement().getTable().getTableName().getIdentifier().getValue();
        Optional<TableMetaData> tableMetaData = callback.load(tableName);
        if (tableMetaData.isPresent()) {
            metaData.getSchema().getConfiguredSchemaMetaData().put(tableName, tableMetaData.get());
        } else {
            refreshUnconfiguredMetaData(metaData, databaseType, dataSourceMap, tableName);
        }
    }
    
    private void refreshUnconfiguredMetaData(final ShardingSphereMetaData metaData, 
                                             final DatabaseType databaseType, final Map<String, DataSource> dataSourceMap, final String tableName) throws SQLException {
        for (Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            Optional<TableMetaData> tableMetaData = TableMetaDataLoader.load(entry.getValue(), tableName, databaseType.getName());
            if (tableMetaData.isPresent()) {
                refreshUnconfiguredMetaData(metaData, tableName, entry.getKey(), tableMetaData.get());
                return;
            }
        }
    }
    
    private void refreshUnconfiguredMetaData(final ShardingSphereMetaData metaData, final String tableName, final String dataSourceName, final TableMetaData tableMetaData) {
        SchemaMetaData schemaMetaData = metaData.getSchema().getUnconfiguredSchemaMetaDataMap().get(dataSourceName);
        if (null == schemaMetaData) {
            Map<String, TableMetaData> tables = new HashMap<>(1, 1);
            tables.put(tableName, tableMetaData);
            metaData.getSchema().getUnconfiguredSchemaMetaDataMap().put(dataSourceName, new SchemaMetaData(tables));
        } else {
            schemaMetaData.put(tableName, tableMetaData);
        }
    }
}
