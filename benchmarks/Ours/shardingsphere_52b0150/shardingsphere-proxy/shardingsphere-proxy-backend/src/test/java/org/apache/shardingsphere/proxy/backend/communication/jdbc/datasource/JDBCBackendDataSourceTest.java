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

package org.apache.shardingsphere.proxy.backend.communication.jdbc.datasource;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.shardingsphere.infra.auth.Authentication;
import org.apache.shardingsphere.infra.config.properties.ConfigurationProperties;
import org.apache.shardingsphere.infra.context.schema.SchemaContext;
import org.apache.shardingsphere.infra.context.schema.impl.StandardSchemaContexts;
import org.apache.shardingsphere.infra.context.schema.runtime.RuntimeContext;
import org.apache.shardingsphere.infra.context.schema.ShardingSphereSchema;
import org.apache.shardingsphere.infra.database.type.dialect.MySQLDatabaseType;
import org.apache.shardingsphere.infra.executor.sql.ConnectionMode;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.datasource.fixture.CallTimeRecordDataSource;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.transaction.ShardingTransactionManagerEngine;
import org.apache.shardingsphere.transaction.context.TransactionContexts;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class JDBCBackendDataSourceTest {
    
    private static final String DATA_SOURCE_PATTERN = "ds_%s";
    
    @Before
    public void setUp() {
        setSchemaContexts();
        setTransactionContexts();
    }
    
    @SneakyThrows(ReflectiveOperationException.class)
    private void setSchemaContexts() {
        Field schemaContexts = ProxyContext.getInstance().getClass().getDeclaredField("schemaContexts");
        schemaContexts.setAccessible(true);
        schemaContexts.set(ProxyContext.getInstance(),
                new StandardSchemaContexts(createSchemaContextMap(), new Authentication(), new ConfigurationProperties(new Properties()), new MySQLDatabaseType()));
    }
    
    private Map<String, SchemaContext> createSchemaContextMap() {
        SchemaContext schemaContext = mock(SchemaContext.class);
        ShardingSphereSchema shardingSphereSchema = mock(ShardingSphereSchema.class);
        RuntimeContext runtimeContext = mock(RuntimeContext.class);
        when(shardingSphereSchema.getDataSources()).thenReturn(mockDataSources(2));
        when(schemaContext.getName()).thenReturn("schema");
        when(schemaContext.getSchema()).thenReturn(shardingSphereSchema);
        when(schemaContext.getRuntimeContext()).thenReturn(runtimeContext);
        return Collections.singletonMap("schema", schemaContext);
    }
    
    @SneakyThrows(ReflectiveOperationException.class)
    private void setTransactionContexts() {
        Field transactionContexts = ProxyContext.getInstance().getClass().getDeclaredField("transactionContexts");
        transactionContexts.setAccessible(true);
        transactionContexts.set(ProxyContext.getInstance(), createTransactionContexts());
    }
    
    private TransactionContexts createTransactionContexts() {
        TransactionContexts result = mock(TransactionContexts.class, RETURNS_DEEP_STUBS);
        ShardingTransactionManagerEngine transactionManagerEngine = mock(ShardingTransactionManagerEngine.class);
        when(result.getEngines().get("schema")).thenReturn(transactionManagerEngine);
        return result;
    }
    
    private Map<String, DataSource> mockDataSources(final int size) {
        Map<String, DataSource> result = new HashMap<>(size, 1);
        for (int i = 0; i < size; i++) {
            result.put(String.format(DATA_SOURCE_PATTERN, i), new CallTimeRecordDataSource());
        }
        return result;
    }
    
    @Test
    public void assertGetConnectionFixedOne() throws SQLException {
        Connection actual = ProxyContext.getInstance().getBackendDataSource().getConnection("schema", String.format(DATA_SOURCE_PATTERN, 1));
        assertThat(actual, instanceOf(Connection.class));
    }
    
    @Test
    public void assertGetConnectionsSucceed() throws SQLException {
        List<Connection> actual = ProxyContext.getInstance().getBackendDataSource().getConnections("schema", String.format(DATA_SOURCE_PATTERN, 1), 5, ConnectionMode.MEMORY_STRICTLY);
        assertThat(actual.size(), is(5));
    }
    
    @Test(expected = SQLException.class)
    public void assertGetConnectionsFailed() throws SQLException {
        ProxyContext.getInstance().getBackendDataSource().getConnections("schema", String.format(DATA_SOURCE_PATTERN, 1), 6, ConnectionMode.MEMORY_STRICTLY);
    }
    
    @Test
    public void assertGetConnectionsByMultiThread() {
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        Collection<Future<List<Connection>>> futures = new LinkedList<>();
        for (int i = 0; i < 200; i++) {
            futures.add(executorService.submit(new CallableTask(String.format(DATA_SOURCE_PATTERN, 1), 6, ConnectionMode.MEMORY_STRICTLY)));
        }
        Collection<Connection> actual = new LinkedList<>();
        for (Future<List<Connection>> each : futures) {
            try {
                actual.addAll(each.get());
            } catch (final InterruptedException | ExecutionException ex) {
                assertThat(ex.getMessage(), containsString("Could't get 6 connections one time, partition succeed connection(5) have released!"));
            }
        }
        assertTrue(actual.isEmpty());
        executorService.shutdown();
    }
    
    @RequiredArgsConstructor
    private static class CallableTask implements Callable<List<Connection>> {
        
        private final String datasourceName;
        
        private final int connectionSize;
    
        private final ConnectionMode connectionMode;
        
        @Override
        public List<Connection> call() throws SQLException {
            return ProxyContext.getInstance().getBackendDataSource().getConnections("schema", datasourceName, connectionSize, connectionMode);
        }
    }
}
