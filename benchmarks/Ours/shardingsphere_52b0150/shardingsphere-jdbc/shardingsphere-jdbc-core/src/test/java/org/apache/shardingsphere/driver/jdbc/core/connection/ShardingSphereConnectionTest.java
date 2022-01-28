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

package org.apache.shardingsphere.driver.jdbc.core.connection;

import org.apache.shardingsphere.driver.jdbc.core.fixture.BASEShardingTransactionManagerFixture;
import org.apache.shardingsphere.driver.jdbc.core.fixture.XAShardingTransactionManagerFixture;
import org.apache.shardingsphere.infra.context.schema.SchemaContext;
import org.apache.shardingsphere.infra.context.schema.SchemaContexts;
import org.apache.shardingsphere.infra.context.schema.impl.StandardSchemaContexts;
import org.apache.shardingsphere.infra.context.schema.runtime.RuntimeContext;
import org.apache.shardingsphere.infra.context.schema.ShardingSphereSchema;
import org.apache.shardingsphere.infra.database.type.DatabaseTypes;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.transaction.ShardingTransactionManagerEngine;
import org.apache.shardingsphere.transaction.context.TransactionContexts;
import org.apache.shardingsphere.transaction.core.TransactionOperationType;
import org.apache.shardingsphere.transaction.core.TransactionType;
import org.apache.shardingsphere.transaction.core.TransactionTypeHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ShardingSphereConnectionTest {
    
    private static Map<String, DataSource> dataSourceMap = new HashMap<>();
    
    private ShardingSphereConnection connection;
    
    private SchemaContexts schemaContexts;
    
    private TransactionContexts transactionContexts;
    
    @BeforeClass
    public static void init() throws SQLException {
        DataSource primaryDataSource = mockDataSource();
        DataSource replicaDataSource = mockDataSource();
        dataSourceMap = new HashMap<>(2, 1);
        dataSourceMap.put("test_primary_ds", primaryDataSource);
        dataSourceMap.put("test_replica_ds", replicaDataSource);
    }
    
    private static DataSource mockDataSource() throws SQLException {
        DataSource result = mock(DataSource.class);
        when(result.getConnection()).thenReturn(mock(Connection.class));
        return result;
    }
    
    @Before
    public void setUp() {
        schemaContexts = mock(StandardSchemaContexts.class);
        SchemaContext schemaContext = mock(SchemaContext.class);
        ShardingSphereSchema schema = mock(ShardingSphereSchema.class);
        RuntimeContext runtimeContext = mock(RuntimeContext.class);
        when(schemaContexts.getDefaultSchemaContext()).thenReturn(schemaContext);
        when(schemaContext.getSchema()).thenReturn(schema);
        when(schemaContexts.getDatabaseType()).thenReturn(DatabaseTypes.getActualDatabaseType("H2"));
        when(schemaContext.getRuntimeContext()).thenReturn(runtimeContext);
        transactionContexts = mock(TransactionContexts.class);
        when(transactionContexts.getDefaultTransactionManagerEngine()).thenReturn(new ShardingTransactionManagerEngine());
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTables().add(new ShardingTableRuleConfiguration("test"));
        connection = new ShardingSphereConnection(dataSourceMap, schemaContexts, transactionContexts, TransactionType.LOCAL);
    }
    
    @After
    public void clear() {
        try {
            connection.close();
            TransactionTypeHolder.clear();
            XAShardingTransactionManagerFixture.getINVOCATIONS().clear();
            BASEShardingTransactionManagerFixture.getINVOCATIONS().clear();
        } catch (final SQLException ignored) {
        }
    }
    
    @Test
    public void assertGetConnectionFromCache() throws SQLException {
        assertThat(connection.getConnection("test_primary_ds"), is(connection.getConnection("test_primary_ds")));
    }
    
    @Test(expected = IllegalStateException.class)
    public void assertGetConnectionFailure() throws SQLException {
        connection.getConnection("not_exist");
    }
    
    @Test
    public void assertXATransactionOperation() throws SQLException {
        connection = new ShardingSphereConnection(dataSourceMap, schemaContexts, transactionContexts, TransactionType.XA);
        connection.setAutoCommit(false);
        assertTrue(XAShardingTransactionManagerFixture.getINVOCATIONS().contains(TransactionOperationType.BEGIN));
        connection.commit();
        assertTrue(XAShardingTransactionManagerFixture.getINVOCATIONS().contains(TransactionOperationType.COMMIT));
        connection.rollback();
        assertTrue(XAShardingTransactionManagerFixture.getINVOCATIONS().contains(TransactionOperationType.ROLLBACK));
    }
    
    @Test
    public void assertBASETransactionOperation() throws SQLException {
        connection = new ShardingSphereConnection(dataSourceMap, schemaContexts, transactionContexts, TransactionType.BASE);
        connection.setAutoCommit(false);
        assertTrue(BASEShardingTransactionManagerFixture.getINVOCATIONS().contains(TransactionOperationType.BEGIN));
        connection.commit();
        assertTrue(BASEShardingTransactionManagerFixture.getINVOCATIONS().contains(TransactionOperationType.COMMIT));
        connection.rollback();
        assertTrue(BASEShardingTransactionManagerFixture.getINVOCATIONS().contains(TransactionOperationType.ROLLBACK));
    }
    
    @Test
    public void assertIsValid() throws SQLException {
        Connection primaryConnection = mock(Connection.class);
        Connection upReplicaConnection = mock(Connection.class);
        Connection downReplicaConnection = mock(Connection.class);
        when(primaryConnection.isValid(anyInt())).thenReturn(true);
        when(upReplicaConnection.isValid(anyInt())).thenReturn(true);
        when(downReplicaConnection.isValid(anyInt())).thenReturn(false);
        connection.getCachedConnections().put("test_primary", primaryConnection);
        connection.getCachedConnections().put("test_replica_up", upReplicaConnection);
        assertTrue(connection.isValid(0));
        connection.getCachedConnections().put("test_replica_down", downReplicaConnection);
        assertFalse(connection.isValid(0));
    }
}
