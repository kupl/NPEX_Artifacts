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

package org.apache.shardingsphere.sharding.route.engine.type.unconfigured;

import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.apache.shardingsphere.sql.parser.binder.metadata.schema.SchemaMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.table.TableMetaData;
import org.apache.shardingsphere.infra.route.context.RouteMapper;
import org.apache.shardingsphere.infra.route.context.RouteResult;
import org.apache.shardingsphere.infra.route.context.RouteUnit;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class ShardingUnconfiguredTablesRoutingEngineTest {
    
    private ShardingUnconfiguredTablesRoutingEngine shardingDefaultDatabaseRoutingEngine;
    
    @Before
    public void setUp() {
        Map<String, TableMetaData> tableMetaDataMap = new HashMap<>(2, 1);
        tableMetaDataMap.put("t_order", new TableMetaData(Collections.emptyList(), Collections.emptyList()));
        tableMetaDataMap.put("t_order_item", new TableMetaData(Collections.emptyList(), Collections.emptyList()));
        Map<String, SchemaMetaData> unconfiguredSchemaMetaDataMap = new HashMap<>(1, 1);
        unconfiguredSchemaMetaDataMap.put("ds_0", new SchemaMetaData(tableMetaDataMap));
        shardingDefaultDatabaseRoutingEngine = new ShardingUnconfiguredTablesRoutingEngine(Arrays.asList("t_order", "t_order_item"), unconfiguredSchemaMetaDataMap);
    }
    
    @Test
    public void assertRoute() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        ShardingRule shardingRule = new ShardingRule(shardingRuleConfig, Arrays.asList("ds_0", "ds_1"));
        RouteResult routeResult = shardingDefaultDatabaseRoutingEngine.route(shardingRule);
        List<RouteUnit> routeUnits = new ArrayList<>(routeResult.getRouteUnits());
        assertThat(routeResult, instanceOf(RouteResult.class));
        assertThat(routeResult.getRouteUnits().size(), is(1));
        assertThat(routeUnits.get(0).getDataSourceMapper().getActualName(), is("ds_0"));
        assertThat(routeUnits.get(0).getTableMappers().size(), is(2));
        Iterator<RouteMapper> tableMappers = routeUnits.get(0).getTableMappers().iterator();
        RouteMapper tableMapper0 = tableMappers.next();
        assertThat(tableMapper0.getActualName(), is("t_order"));
        assertThat(tableMapper0.getLogicName(), is("t_order"));
        RouteMapper tableMapper1 = tableMappers.next();
        assertThat(tableMapper1.getActualName(), is("t_order_item"));
        assertThat(tableMapper1.getLogicName(), is("t_order_item"));
    }
}
