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

package org.apache.shardingsphere.proxy.backend.text.admin;

import org.apache.shardingsphere.infra.auth.Authentication;
import org.apache.shardingsphere.infra.auth.ProxyUser;
import org.apache.shardingsphere.infra.config.properties.ConfigurationProperties;
import org.apache.shardingsphere.infra.context.schema.SchemaContext;
import org.apache.shardingsphere.infra.context.schema.impl.StandardSchemaContexts;
import org.apache.shardingsphere.infra.database.type.dialect.MySQLDatabaseType;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.connection.BackendConnection;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.exception.UnknownDatabaseException;
import org.apache.shardingsphere.proxy.backend.response.BackendResponse;
import org.apache.shardingsphere.proxy.backend.response.update.UpdateResponse;
import org.apache.shardingsphere.sql.parser.sql.dialect.statement.mysql.dal.MySQLUseStatement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class UseDatabaseBackendHandlerTest {
    
    private static final String SCHEMA_PATTERN = "schema_%s";
    
    private BackendConnection backendConnection;
    
    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        backendConnection = mock(BackendConnection.class);
        when(backendConnection.getUsername()).thenReturn("root");
        Field schemaContexts = ProxyContext.getInstance().getClass().getDeclaredField("schemaContexts");
        schemaContexts.setAccessible(true);
        schemaContexts.set(ProxyContext.getInstance(),
                new StandardSchemaContexts(getSchemaContextMap(), getAuthentication(), new ConfigurationProperties(new Properties()), new MySQLDatabaseType()));
    }
    
    private Map<String, SchemaContext> getSchemaContextMap() {
        Map<String, SchemaContext> result = new HashMap<>(10);
        for (int i = 0; i < 10; i++) {
            result.put(String.format(SCHEMA_PATTERN, i), mock(SchemaContext.class));
        }
        return result;
    }
    
    private Authentication getAuthentication() {
        ProxyUser proxyUser = new ProxyUser("root", Arrays.asList(String.format(SCHEMA_PATTERN, 0), String.format(SCHEMA_PATTERN, 1)));
        Authentication result = new Authentication();
        result.getUsers().put("root", proxyUser);
        return result;
    }
    
    @Test
    public void assertExecuteUseStatementBackendHandler() {
        MySQLUseStatement useStatement = mock(MySQLUseStatement.class);
        when(useStatement.getSchema()).thenReturn(String.format(SCHEMA_PATTERN, 0));
        UseDatabaseBackendHandler useSchemaBackendHandler = new UseDatabaseBackendHandler(useStatement, backendConnection);
        BackendResponse actual = useSchemaBackendHandler.execute();
        verify(backendConnection).setCurrentSchema(anyString());
        assertThat(actual, instanceOf(UpdateResponse.class));
    }
    
    @Test(expected = UnknownDatabaseException.class)
    public void assertExecuteUseStatementNotExist() {
        MySQLUseStatement useStatement = mock(MySQLUseStatement.class);
        when(useStatement.getSchema()).thenReturn("not_exist");
        UseDatabaseBackendHandler useSchemaBackendHandler = new UseDatabaseBackendHandler(useStatement, backendConnection);
        useSchemaBackendHandler.execute();
    }
}
