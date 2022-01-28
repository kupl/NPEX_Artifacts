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

package org.apache.shardingsphere.replication.primaryreplica.spring.namespace;

import org.apache.shardingsphere.replication.primaryreplica.algorithm.RandomReplicaLoadBalanceAlgorithm;
import org.apache.shardingsphere.replication.primaryreplica.algorithm.config.AlgorithmProvidedPrimaryReplicaReplicationRuleConfiguration;
import org.apache.shardingsphere.replication.primaryreplica.api.config.rule.PrimaryReplicaReplicationDataSourceRuleConfiguration;
import org.apache.shardingsphere.replication.primaryreplica.spi.ReplicaLoadBalanceAlgorithm;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@ContextConfiguration(locations = "classpath:META-INF/spring/primary-replica-replication-application-context.xml")
public final class PrimaryReplicaReplicationSpringNamespaceTest extends AbstractJUnit4SpringContextTests {
    
    @Resource
    private ReplicaLoadBalanceAlgorithm randomLoadbalancer;
    
    @Resource
    private AlgorithmProvidedPrimaryReplicaReplicationRuleConfiguration defaultRule;
    
    @Resource
    private AlgorithmProvidedPrimaryReplicaReplicationRuleConfiguration randomRule;
    
    @Test
    public void assertRandomLoadbalancer() {
        assertThat(randomLoadbalancer.getType(), is("RANDOM"));
    }
    
    @Test
    public void assertDefaultDataSource() {
        assertLoadBalancers(defaultRule.getLoadBalanceAlgorithms());
        assertThat(defaultRule.getDataSources().size(), is(1));
        assertDefaultDataSourceRule(defaultRule.getDataSources().iterator().next());
    }
    
    private void assertLoadBalancers(final Map<String, ReplicaLoadBalanceAlgorithm> loadBalances) {
        assertThat(loadBalances.size(), is(1));
        assertThat(loadBalances.get("randomLoadbalancer"), instanceOf(RandomReplicaLoadBalanceAlgorithm.class));
    }
    
    private void assertDefaultDataSourceRule(final PrimaryReplicaReplicationDataSourceRuleConfiguration dataSourceRuleConfig) {
        assertThat(dataSourceRuleConfig.getName(), is("default_ds"));
        assertThat(dataSourceRuleConfig.getPrimaryDataSourceName(), is("primary_ds"));
        assertThat(dataSourceRuleConfig.getReplicaDataSourceNames(), is(Arrays.asList("replica_ds_0", "replica_ds_1")));
        assertThat(dataSourceRuleConfig.getLoadBalancerName(), is(""));
    }
    
    @Test
    public void assertRandomDataSource() {
        assertLoadBalancers(randomRule.getLoadBalanceAlgorithms());
        assertThat(randomRule.getDataSources().size(), is(1));
        assertRandomDataSourceRule(randomRule.getDataSources().iterator().next());
    }
    
    private void assertRandomDataSourceRule(final PrimaryReplicaReplicationDataSourceRuleConfiguration dataSourceRuleConfig) {
        assertThat(dataSourceRuleConfig.getName(), is("random_ds"));
        assertThat(dataSourceRuleConfig.getPrimaryDataSourceName(), is("primary_ds"));
        assertThat(dataSourceRuleConfig.getReplicaDataSourceNames(), is(Arrays.asList("replica_ds_0", "replica_ds_1")));
        assertThat(dataSourceRuleConfig.getLoadBalancerName(), is("randomLoadbalancer"));
    }
}
