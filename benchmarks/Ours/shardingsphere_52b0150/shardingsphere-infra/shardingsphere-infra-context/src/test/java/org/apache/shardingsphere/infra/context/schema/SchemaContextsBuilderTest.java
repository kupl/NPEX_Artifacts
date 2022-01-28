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

package org.apache.shardingsphere.infra.context.schema;

import org.apache.shardingsphere.infra.config.properties.ConfigurationPropertyKey;
import org.apache.shardingsphere.infra.context.fixture.FixtureRule;
import org.apache.shardingsphere.infra.context.fixture.FixtureRuleConfiguration;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.database.type.DatabaseTypes;
import org.apache.shardingsphere.jdbc.test.MockedDataSource;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public final class SchemaContextsBuilderTest {
    
    @Test
    public void assertBuildWithoutConfiguration() throws SQLException {
        DatabaseType databaseType = DatabaseTypes.getActualDatabaseType("FixtureDB");
        SchemaContexts actual = new SchemaContextsBuilder(databaseType, Collections.emptyMap(), Collections.emptyMap(), null).build();
        assertThat(actual.getDatabaseType(), CoreMatchers.is(databaseType));
        assertTrue(actual.getSchemaContextMap().isEmpty());
        assertTrue(actual.getAuthentication().getUsers().isEmpty());
        assertTrue(actual.getProps().getProps().isEmpty());
        assertFalse(actual.isCircuitBreak());
    }
    
    @Test
    public void assertBuildWithConfigurationsButWithoutDataSource() throws SQLException {
        DatabaseType databaseType = DatabaseTypes.getActualDatabaseType("FixtureDB");
        Properties props = new Properties();
        props.setProperty(ConfigurationPropertyKey.EXECUTOR_SIZE.getKey(), "1");
        SchemaContexts actual = new SchemaContextsBuilder(databaseType, Collections.singletonMap("logic_db", Collections.emptyMap()), 
                Collections.singletonMap("logic_db", Collections.singleton(new FixtureRuleConfiguration())), props).build();
        assertThat(actual.getDatabaseType(), CoreMatchers.is(databaseType));
        assertRules(actual);
        assertTrue(actual.getSchemaContextMap().get("logic_db").getSchema().getDataSources().isEmpty());
        assertTrue(actual.getAuthentication().getUsers().isEmpty());
        assertThat(actual.getProps().getProps().size(), CoreMatchers.is(1));
        assertThat(actual.getProps().getValue(ConfigurationPropertyKey.EXECUTOR_SIZE), CoreMatchers.is(1));
        assertFalse(actual.isCircuitBreak());
    }
    
    @Test
    public void assertBuildWithConfigurationsAndDataSources() throws SQLException {
        DatabaseType databaseType = DatabaseTypes.getActualDatabaseType("FixtureDB");
        Properties props = new Properties();
        props.setProperty(ConfigurationPropertyKey.EXECUTOR_SIZE.getKey(), "1");
        SchemaContexts actual = new SchemaContextsBuilder(databaseType, Collections.singletonMap("logic_db", Collections.singletonMap("ds", new MockedDataSource())),
                Collections.singletonMap("logic_db", Collections.singleton(new FixtureRuleConfiguration())), props).build();
        assertThat(actual.getDatabaseType(), CoreMatchers.is(databaseType));
        assertRules(actual);
        assertDataSources(actual);
        assertTrue(actual.getAuthentication().getUsers().isEmpty());
        assertThat(actual.getProps().getProps().size(), CoreMatchers.is(1));
        assertThat(actual.getProps().getValue(ConfigurationPropertyKey.EXECUTOR_SIZE), CoreMatchers.is(1));
        assertFalse(actual.isCircuitBreak());
    }
    
    private void assertRules(final SchemaContexts actual) {
        assertThat(actual.getSchemaContextMap().get("logic_db").getSchema().getRules().size(), CoreMatchers.is(1));
        assertThat(actual.getSchemaContextMap().get("logic_db").getSchema().getRules().iterator().next(), CoreMatchers.instanceOf(FixtureRule.class));
    }
    
    private void assertDataSources(final SchemaContexts actual) {
        assertThat(actual.getSchemaContextMap().get("logic_db").getSchema().getDataSources().size(), CoreMatchers.is(1));
        assertThat(actual.getSchemaContextMap().get("logic_db").getSchema().getDataSources().get("ds"), CoreMatchers.instanceOf(MockedDataSource.class));
    }
}
