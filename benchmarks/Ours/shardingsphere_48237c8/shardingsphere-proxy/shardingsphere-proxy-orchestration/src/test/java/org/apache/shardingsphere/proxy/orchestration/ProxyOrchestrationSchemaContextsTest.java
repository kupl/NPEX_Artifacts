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

package org.apache.shardingsphere.proxy.orchestration;

import lombok.SneakyThrows;
import org.apache.shardingsphere.infra.auth.Authentication;
import org.apache.shardingsphere.infra.auth.ProxyUser;
import org.apache.shardingsphere.infra.config.properties.ConfigurationProperties;
import org.apache.shardingsphere.infra.config.properties.ConfigurationPropertyKey;
import org.apache.shardingsphere.infra.database.type.dialect.MySQLDatabaseType;
import org.apache.shardingsphere.kernel.context.SchemaContext;
import org.apache.shardingsphere.kernel.context.SchemaContexts;
import org.apache.shardingsphere.kernel.context.impl.StandardSchemaContexts;
import org.apache.shardingsphere.kernel.context.runtime.RuntimeContext;
import org.apache.shardingsphere.kernel.context.schema.ShardingSphereSchema;
import org.apache.shardingsphere.orchestration.core.common.event.auth.AuthenticationChangedEvent;
import org.apache.shardingsphere.orchestration.core.common.event.props.PropertiesChangedEvent;
import org.apache.shardingsphere.orchestration.core.common.eventbus.OrchestrationEventBus;
import org.apache.shardingsphere.orchestration.core.facade.OrchestrationFacade;
import org.apache.shardingsphere.orchestration.core.registry.RegistryCenter;
import org.apache.shardingsphere.orchestration.core.registry.event.CircuitStateChangedEvent;
import org.apache.shardingsphere.proxy.backend.schema.ProxySchemaContexts;
import org.apache.shardingsphere.proxy.orchestration.schema.ProxyOrchestrationSchemaContexts;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class ProxyOrchestrationSchemaContextsTest {
    
    @Mock
    private OrchestrationFacade orchestrationFacade;
    
    @Before
    @SneakyThrows(ReflectiveOperationException.class)
    public void setUp() {
        Field field = ProxySchemaContexts.getInstance().getClass().getDeclaredField("schemaContexts");
        field.setAccessible(true);
        field.set(ProxySchemaContexts.getInstance(), getProxyOrchestrationSchemaContexts());
    }
    
    private Map<String, SchemaContext> getSchemaContextMap() {
        Map<String, SchemaContext> result = new HashMap<>(10);
        for (int i = 0; i < 10; i++) {
            String name = "schema_" + i;
            ShardingSphereSchema schema = mock(ShardingSphereSchema.class);
            RuntimeContext runtimeContext = mock(RuntimeContext.class);
            result.put(name, new SchemaContext(name, schema, runtimeContext));
        }
        return result;
    }
    
    private ProxyOrchestrationSchemaContexts getProxyOrchestrationSchemaContexts() {
        when(orchestrationFacade.getRegistryCenter()).thenReturn(mock(RegistryCenter.class));
        ProxyOrchestrationSchemaContexts result = new ProxyOrchestrationSchemaContexts(new StandardSchemaContexts(), orchestrationFacade);
        SchemaContexts schemaContexts =
                new StandardSchemaContexts(getSchemaContextMap(), new Authentication(), new ConfigurationProperties(new Properties()), new MySQLDatabaseType());
        result.getSchemaContexts().putAll(schemaContexts.getSchemaContexts());
        return result;
    }
    
    @Test
    public void assertRenewProperties() {
        assertTrue(ProxySchemaContexts.getInstance().getSchemaContexts().getProps().getProps().isEmpty());
        Properties props = new Properties();
        props.setProperty(ConfigurationPropertyKey.SQL_SHOW.getKey(), Boolean.TRUE.toString());
        OrchestrationEventBus.getInstance().post(new PropertiesChangedEvent(props));
        assertFalse(ProxySchemaContexts.getInstance().getSchemaContexts().getProps().getProps().isEmpty());
    }
    
    @Test
    public void assertRenewAuthentication() {
        ProxyUser proxyUser = new ProxyUser("root", Collections.singleton("db1"));
        Authentication authentication = new Authentication();
        authentication.getUsers().put("root", proxyUser);
        OrchestrationEventBus.getInstance().post(new AuthenticationChangedEvent(authentication));
        assertThat(ProxySchemaContexts.getInstance().getSchemaContexts().getAuthentication().getUsers().keySet().iterator().next(), is("root"));
        assertThat(ProxySchemaContexts.getInstance().getSchemaContexts().getAuthentication().getUsers().get("root").getPassword(), is("root"));
        assertThat(ProxySchemaContexts.getInstance().getSchemaContexts().getAuthentication().getUsers().get("root").getAuthorizedSchemas().iterator().next(), is("db1"));
    }
    
    @Test
    public void assertRenewCircuitState() {
        assertFalse(ProxySchemaContexts.getInstance().getSchemaContexts().isCircuitBreak());
        OrchestrationEventBus.getInstance().post(new CircuitStateChangedEvent(true));
        assertTrue(ProxySchemaContexts.getInstance().getSchemaContexts().isCircuitBreak());
        OrchestrationEventBus.getInstance().post(new CircuitStateChangedEvent(false));
    }
}
