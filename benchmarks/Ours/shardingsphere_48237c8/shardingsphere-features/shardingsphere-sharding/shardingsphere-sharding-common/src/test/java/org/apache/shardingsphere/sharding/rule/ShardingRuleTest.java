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

package org.apache.shardingsphere.sharding.rule;

import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.exception.ShardingSphereConfigurationException;
import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.keygen.KeyGenerateStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.NoneShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.algorithm.keygen.SnowflakeKeyGenerateAlgorithm;
import org.apache.shardingsphere.sharding.algorithm.keygen.fixture.IncrementKeyGenerateAlgorithm;
import org.apache.shardingsphere.sharding.algorithm.sharding.inline.InlineShardingAlgorithm;
import org.apache.shardingsphere.sharding.strategy.ShardingStrategy;
import org.apache.shardingsphere.sharding.strategy.none.NoneShardingStrategy;
import org.apache.shardingsphere.sharding.strategy.standard.StandardShardingStrategy;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ShardingRuleTest {
    
    @Test(expected = IllegalArgumentException.class)
    public void assertNewShardingRuleWithEmptyDataSourceNames() {
        new ShardingRule(new ShardingRuleConfiguration(), Collections.emptyList());
    }
    
    @Test
    public void assertNewShardingRuleWithMaximumConfiguration() {
        ShardingRule actual = createMaximumShardingRule();
        assertThat(actual.getTableRules().size(), is(2));
        assertThat(actual.getBindingTableRules().size(), is(1));
        assertThat(actual.getBindingTableRules().iterator().next().getTableRules().size(), is(2));
        assertThat(actual.getBroadcastTables(), is(Collections.singletonList("BROADCAST_TABLE")));
        assertThat(actual.getDefaultDatabaseShardingStrategy(), instanceOf(StandardShardingStrategy.class));
        assertThat(actual.getDefaultTableShardingStrategy(), instanceOf(StandardShardingStrategy.class));
        assertThat(actual.getDefaultKeyGenerateAlgorithm(), instanceOf(IncrementKeyGenerateAlgorithm.class));
    }
    
    @Test
    public void assertNewShardingRuleWithMinimumConfiguration() {
        ShardingRule actual = createMinimumShardingRule();
        assertThat(actual.getTableRules().size(), is(1));
        assertTrue(actual.getBindingTableRules().isEmpty());
        assertTrue(actual.getBroadcastTables().isEmpty());
        assertThat(actual.getDefaultDatabaseShardingStrategy(), instanceOf(NoneShardingStrategy.class));
        assertThat(actual.getDefaultTableShardingStrategy(), instanceOf(NoneShardingStrategy.class));
        assertThat(actual.getDefaultKeyGenerateAlgorithm(), instanceOf(SnowflakeKeyGenerateAlgorithm.class));
    }
    
    @Test
    public void assertFindTableRule() {
        assertTrue(createMaximumShardingRule().findTableRule("logic_Table").isPresent());
    }
    
    @Test
    public void assertNotFindTableRule() {
        assertFalse(createMaximumShardingRule().findTableRule("other_Table").isPresent());
    }
    
    @Test
    public void assertFindTableRuleByActualTable() {
        assertTrue(createMaximumShardingRule().findTableRuleByActualTable("table_0").isPresent());
    }
    
    @Test
    public void assertNotFindTableRuleByActualTable() {
        assertFalse(createMaximumShardingRule().findTableRuleByActualTable("table_3").isPresent());
    }
    
    @Test
    public void assertFindLogicTableByActualTable() {
        assertTrue(createMaximumShardingRule().findLogicTableByActualTable("table_0").isPresent());
    }
    
    @Test
    public void assertNotFindLogicTableByActualTable() {
        assertFalse(createMaximumShardingRule().findLogicTableByActualTable("table_3").isPresent());
    }
    
    @Test
    public void assertGetTableRuleWithShardingTable() {
        TableRule actual = createMaximumShardingRule().getTableRule("Logic_Table");
        assertThat(actual.getLogicTable(), is("logic_table"));
    }
    
    @Test
    public void assertGetTableRuleWithBroadcastTable() {
        TableRule actual = createMaximumShardingRule().getTableRule("Broadcast_Table");
        assertThat(actual.getLogicTable(), is("broadcast_table"));
    }
    
    @Test(expected = ShardingSphereConfigurationException.class)
    public void assertGetTableRuleFailure() {
        createMinimumShardingRule().getTableRule("New_Table");
    }
    
    @Test
    public void assertGetDatabaseShardingStrategyFromTableRule() {
        TableRule tableRule = mock(TableRule.class);
        when(tableRule.getDatabaseShardingStrategy()).thenReturn(new NoneShardingStrategy());
        assertThat(createMaximumShardingRule().getDatabaseShardingStrategy(tableRule), instanceOf(NoneShardingStrategy.class));
    }
    
    @Test
    public void assertGetDatabaseShardingStrategyFromDefault() {
        ShardingStrategy actual = createMaximumShardingRule().getDatabaseShardingStrategy(mock(TableRule.class));
        assertThat(actual, instanceOf(StandardShardingStrategy.class));
        assertThat(actual.getShardingColumns().iterator().next(), is("ds_id"));
    }
    
    @Test
    public void assertGetTableShardingStrategyFromTableRule() {
        TableRule tableRule = mock(TableRule.class);
        when(tableRule.getTableShardingStrategy()).thenReturn(new NoneShardingStrategy());
        assertThat(createMaximumShardingRule().getTableShardingStrategy(tableRule), instanceOf(NoneShardingStrategy.class));
    }
    
    @Test
    public void assertGetTableShardingStrategyFromDefault() {
        ShardingStrategy actual = createMaximumShardingRule().getTableShardingStrategy(mock(TableRule.class));
        assertThat(actual, instanceOf(StandardShardingStrategy.class));
        assertThat(actual.getShardingColumns().iterator().next(), is("table_id"));
    }
    
    @Test
    public void assertIsAllBindingTableWhenLogicTablesIsEmpty() {
        assertFalse(createMaximumShardingRule().isAllBindingTables(Collections.emptyList()));
    }
    
    @Test
    public void assertIsNotAllBindingTable() {
        assertFalse(createMaximumShardingRule().isAllBindingTables(Collections.singletonList("new_Table")));
        assertFalse(createMaximumShardingRule().isAllBindingTables(Arrays.asList("logic_Table", "new_Table")));
    }
    
    @Test
    public void assertIsAllBindingTable() {
        assertTrue(createMaximumShardingRule().isAllBindingTables(Collections.singletonList("logic_Table")));
        assertTrue(createMaximumShardingRule().isAllBindingTables(Collections.singletonList("logic_table")));
        assertTrue(createMaximumShardingRule().isAllBindingTables(Collections.singletonList("sub_Logic_Table")));
        assertTrue(createMaximumShardingRule().isAllBindingTables(Collections.singletonList("sub_logic_table")));
        assertTrue(createMaximumShardingRule().isAllBindingTables(Arrays.asList("logic_Table", "sub_Logic_Table")));
        assertTrue(createMaximumShardingRule().isAllBindingTables(Arrays.asList("logic_table", "sub_logic_Table")));
        assertFalse(createMaximumShardingRule().isAllBindingTables(Arrays.asList("logic_table", "sub_logic_Table", "new_table")));
        assertFalse(createMaximumShardingRule().isAllBindingTables(Collections.emptyList()));
        assertFalse(createMaximumShardingRule().isAllBindingTables(Collections.singletonList("new_Table")));
    }
    
    @Test
    public void assertGetBindingTableRuleForNotConfig() {
        assertFalse(createMinimumShardingRule().findBindingTableRule("logic_Table").isPresent());
    }
    
    @Test
    public void assertGetBindingTableRuleForNotFound() {
        assertFalse(createMaximumShardingRule().findBindingTableRule("new_Table").isPresent());
    }
    
    @Test
    public void assertGetBindingTableRuleForFound() {
        ShardingRule actual = createMaximumShardingRule();
        assertTrue(actual.findBindingTableRule("logic_Table").isPresent());
        assertThat(actual.findBindingTableRule("logic_Table").get().getTableRules().size(), is(2));
    }
    
    @Test
    public void assertIsAllBroadcastTableWhenLogicTablesIsEmpty() {
        assertFalse(createMaximumShardingRule().isAllBroadcastTables(Collections.emptyList()));
    }
    
    @Test
    public void assertIsAllBroadcastTable() {
        assertTrue(createMaximumShardingRule().isAllBroadcastTables(Collections.singletonList("Broadcast_Table")));
    }
    
    @Test
    public void assertIsNotAllBroadcastTable() {
        assertFalse(createMaximumShardingRule().isAllBroadcastTables(Arrays.asList("broadcast_table", "other_table")));
    }
    
    @Test
    public void assertIsBroadcastTable() {
        assertTrue(createMaximumShardingRule().isBroadcastTable("Broadcast_Table"));
    }
    
    @Test
    public void assertIsNotBroadcastTable() {
        assertFalse(createMaximumShardingRule().isBroadcastTable("other_table"));
    }
    
    @Test
    public void assertIsShardingColumnForDefaultDatabaseShardingStrategy() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTables().add(createTableRuleConfigWithAllStrategies());
        shardingRuleConfig.setDefaultDatabaseShardingStrategy(new StandardShardingStrategyConfiguration("column", "STANDARD_TEST"));
        shardingRuleConfig.getShardingAlgorithms().put("standard", new ShardingSphereAlgorithmConfiguration("STANDARD_TEST", new Properties()));
        assertTrue(new ShardingRule(shardingRuleConfig, createDataSourceNames()).isShardingColumn("column", "LOGIC_TABLE"));
    }
    
    @Test
    public void assertIsShardingColumnForDefaultTableShardingStrategy() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTables().add(createTableRuleConfigWithAllStrategies());
        shardingRuleConfig.setDefaultTableShardingStrategy(new StandardShardingStrategyConfiguration("column", "STANDARD_TEST"));
        shardingRuleConfig.getShardingAlgorithms().put("standard", new ShardingSphereAlgorithmConfiguration("STANDARD_TEST", new Properties()));
        assertTrue(new ShardingRule(shardingRuleConfig, createDataSourceNames()).isShardingColumn("column", "LOGIC_TABLE"));
    }
    
    @Test
    public void assertIsShardingColumnForDatabaseShardingStrategy() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTables().add(createTableRuleConfigWithAllStrategies());
        shardingRuleConfig.getShardingAlgorithms().put("standard", new ShardingSphereAlgorithmConfiguration("STANDARD_TEST", new Properties()));
        assertTrue(new ShardingRule(shardingRuleConfig, createDataSourceNames()).isShardingColumn("column", "logic_Table"));
    }
    
    @Test
    public void assertIsShardingColumnForTableShardingStrategy() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTables().add(createTableRuleConfigWithTableStrategies());
        shardingRuleConfig.getShardingAlgorithms().put("standard", new ShardingSphereAlgorithmConfiguration("STANDARD_TEST", new Properties()));
        assertTrue(new ShardingRule(shardingRuleConfig, createDataSourceNames()).isShardingColumn("column", "logic_Table"));
    }
    
    @Test
    public void assertIsNotShardingColumn() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTables().add(createTableRuleConfigWithAllStrategies());
        assertFalse(new ShardingRule(shardingRuleConfig, createDataSourceNames()).isShardingColumn("column", "other_Table"));
    }
    
    @Test
    public void assertFindGenerateKeyColumn() {
        assertTrue(createMaximumShardingRule().findGenerateKeyColumnName("logic_table").isPresent());
    }
    
    @Test
    public void assertNotFindGenerateKeyColumn() {
        assertFalse(createMinimumShardingRule().findGenerateKeyColumnName("sub_logic_table").isPresent());
    }
    
    @Test(expected = ShardingSphereConfigurationException.class)
    public void assertGenerateKeyFailure() {
        createMaximumShardingRule().generateKey("table_0");
    }
    
    @Test
    public void assertGenerateKeyWithDefaultKeyGenerator() {
        assertThat(createMinimumShardingRule().generateKey("logic_table"), instanceOf(Long.class));
    }
    
    @Test
    public void assertGenerateKeyWithKeyGenerator() {
        assertThat(createMaximumShardingRule().generateKey("logic_table"), instanceOf(Integer.class));
    }
    
    @Test
    public void assertGetDataNodeByLogicTable() {
        assertThat(createMaximumShardingRule().getDataNode("logic_table"), is(new DataNode("ds_0.table_0")));
    }
    
    @Test
    public void assertGetDataNodeByDataSourceAndLogicTable() {
        assertThat(createMaximumShardingRule().getDataNode("ds_1", "logic_table"), is(new DataNode("ds_1.table_0")));
    }
    
    @Test(expected = ShardingSphereConfigurationException.class)
    public void assertGetDataNodeByLogicTableFailureWithDataSourceName() {
        createMaximumShardingRule().getDataNode("ds_3", "logic_table");
    }
    
    @Test
    public void assertGetShardingLogicTableNames() {
        ShardingRule actual = createMaximumShardingRule();
        assertThat(actual.getShardingLogicTableNames(Arrays.asList("LOGIC_TABLE", "BROADCAST_TABLE")), is(Collections.singletonList("LOGIC_TABLE")));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void assertConstructShardingRuleWithNullShardingRuleConfiguration() {
        new ShardingRule((ShardingRuleConfiguration) null, createDataSourceNames());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void assertConstructShardingRuleWithNullDataSourceNames() {
        ShardingRuleConfiguration shardingRuleConfiguration = new ShardingRuleConfiguration();
        ShardingTableRuleConfiguration shardingTableRuleConfiguration = createTableRuleConfiguration("LOGIC_TABLE", "ms_ds_${0..1}.table_${0..2}");
        shardingRuleConfiguration.getTables().add(shardingTableRuleConfiguration);
        new ShardingRule(shardingRuleConfiguration, null);
    }
    
    @Test
    public void assertTableRuleExists() {
        assertTrue(createMaximumShardingRule().tableRuleExists(Collections.singletonList("logic_table")));
    }
    
    @Test
    public void assertTableRuleExistsForMultipleTables() {
        assertTrue(createMaximumShardingRule().tableRuleExists(Arrays.asList("logic_table", "table_0")));
    }
    
    @Test
    public void assertTableRuleNotExists() {
        assertFalse(createMinimumShardingRule().tableRuleExists(Collections.singletonList("table_0")));
    }
    
    private ShardingRule createMaximumShardingRule() {
        ShardingRuleConfiguration shardingRuleConfiguration = new ShardingRuleConfiguration();
        ShardingTableRuleConfiguration shardingTableRuleConfiguration = createTableRuleConfiguration("LOGIC_TABLE", "ds_${0..1}.table_${0..2}");
        shardingTableRuleConfiguration.setKeyGenerateStrategy(new KeyGenerateStrategyConfiguration("id", "increment"));
        ShardingTableRuleConfiguration subTableRuleConfiguration = createTableRuleConfiguration("SUB_LOGIC_TABLE", "ds_${0..1}.sub_table_${0..2}");
        shardingRuleConfiguration.getTables().add(shardingTableRuleConfiguration);
        shardingRuleConfiguration.getTables().add(subTableRuleConfiguration);
        shardingRuleConfiguration.getBindingTableGroups().add(shardingTableRuleConfiguration.getLogicTable() + "," + subTableRuleConfiguration.getLogicTable());
        shardingRuleConfiguration.getBroadcastTables().add("BROADCAST_TABLE");
        InlineShardingAlgorithm shardingAlgorithmDB = new InlineShardingAlgorithm();
        Properties props = new Properties();
        props.setProperty("algorithm.expression", "ds_%{ds_id % 2}");
        shardingAlgorithmDB.setProps(props);
        shardingRuleConfiguration.setDefaultDatabaseShardingStrategy(new StandardShardingStrategyConfiguration("ds_id", "standard"));
        InlineShardingAlgorithm shardingAlgorithmTBL = new InlineShardingAlgorithm();
        props = new Properties();
        props.setProperty("algorithm.expression", "table_%{table_id % 2}");
        shardingAlgorithmTBL.setProps(props);
        shardingRuleConfiguration.setDefaultTableShardingStrategy(new StandardShardingStrategyConfiguration("table_id", "standard"));
        shardingRuleConfiguration.setDefaultKeyGenerateStrategy(new KeyGenerateStrategyConfiguration("id", "default"));
        shardingRuleConfiguration.getShardingAlgorithms().put("standard", new ShardingSphereAlgorithmConfiguration("STANDARD_TEST", new Properties()));
        shardingRuleConfiguration.getKeyGenerators().put("increment", new ShardingSphereAlgorithmConfiguration("INCREMENT", new Properties()));
        shardingRuleConfiguration.getKeyGenerators().put("default", new ShardingSphereAlgorithmConfiguration("INCREMENT", new Properties()));
        return new ShardingRule(shardingRuleConfiguration, createDataSourceNames());
    }
    
    private ShardingRule createMinimumShardingRule() {
        ShardingRuleConfiguration shardingRuleConfiguration = new ShardingRuleConfiguration();
        ShardingTableRuleConfiguration shardingTableRuleConfiguration = createTableRuleConfiguration("LOGIC_TABLE", "ds_${0..1}.table_${0..2}");
        shardingRuleConfiguration.getTables().add(shardingTableRuleConfiguration);
        return new ShardingRule(shardingRuleConfiguration, createDataSourceNames());
    }
    
    private ShardingTableRuleConfiguration createTableRuleConfiguration(final String logicTableName, final String actualDataNodes) {
        return new ShardingTableRuleConfiguration(logicTableName, actualDataNodes);
    }
    
    private Collection<String> createDataSourceNames() {
        return Arrays.asList("ds_0", "ds_1");
    }
    
    private ShardingTableRuleConfiguration createTableRuleConfigWithAllStrategies() {
        ShardingTableRuleConfiguration result = new ShardingTableRuleConfiguration("LOGIC_TABLE", "ds_${0..1}.table_${0..2}");
        result.setDatabaseShardingStrategy(new StandardShardingStrategyConfiguration("column", "standard"));
        result.setTableShardingStrategy(new NoneShardingStrategyConfiguration());
        return result;
    }
    
    private ShardingTableRuleConfiguration createTableRuleConfigWithTableStrategies() {
        ShardingTableRuleConfiguration result = new ShardingTableRuleConfiguration("LOGIC_TABLE", "ds_${0..1}.table_${0..2}");
        result.setTableShardingStrategy(new StandardShardingStrategyConfiguration("column", "standard"));
        return result;
    }
}
