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

package org.apache.shardingsphere.replication.primaryreplica.spring.boot;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.config.RuleConfiguration;
import org.apache.shardingsphere.replication.primaryreplica.algorithm.config.AlgorithmProvidedPrimaryReplicaReplicationRuleConfiguration;
import org.apache.shardingsphere.replication.primaryreplica.spi.ReplicaLoadBalanceAlgorithm;
import org.apache.shardingsphere.replication.primaryreplica.spring.boot.algorithm.PrimaryReplicaReplicationAlgorithmProvidedBeanRegistry;
import org.apache.shardingsphere.replication.primaryreplica.spring.boot.condition.PrimaryReplicaReplicationSpringBootCondition;
import org.apache.shardingsphere.replication.primaryreplica.spring.boot.rule.YamlPrimaryReplicaReplicationRuleSpringBootConfiguration;
import org.apache.shardingsphere.replication.primaryreplica.yaml.config.YamlPrimaryReplicaReplicationRuleConfiguration;
import org.apache.shardingsphere.replication.primaryreplica.yaml.swapper.PrimaryReplicaReplicationRuleAlgorithmProviderConfigurationYamlSwapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Rule spring boot configuration for primary-replica replication.
 */
@Configuration
@EnableConfigurationProperties(YamlPrimaryReplicaReplicationRuleSpringBootConfiguration.class)
@ConditionalOnClass(YamlPrimaryReplicaReplicationRuleConfiguration.class)
@Conditional(PrimaryReplicaReplicationSpringBootCondition.class)
@RequiredArgsConstructor
public class PrimaryReplicaReplicationRuleSpringbootConfiguration {
    
    private final PrimaryReplicaReplicationRuleAlgorithmProviderConfigurationYamlSwapper swapper = new PrimaryReplicaReplicationRuleAlgorithmProviderConfigurationYamlSwapper();
    
    private final YamlPrimaryReplicaReplicationRuleSpringBootConfiguration yamlConfig;
    
    /**
     * Primary-replica replication rule configuration for spring boot.
     *
     * @param loadBalanceAlgorithms load balance algorithms
     * @return Primary-replica replication rule configuration
     */
    @Bean
    public RuleConfiguration primaryReplicaReplicationRuleConfiguration(final ObjectProvider<Map<String, ReplicaLoadBalanceAlgorithm>> loadBalanceAlgorithms) {
        AlgorithmProvidedPrimaryReplicaReplicationRuleConfiguration result = swapper.swapToObject(yamlConfig.getPrimaryReplicaReplication());
        Map<String, ReplicaLoadBalanceAlgorithm> balanceAlgorithmMap = Optional.ofNullable(loadBalanceAlgorithms.getIfAvailable()).orElse(Collections.emptyMap());
        result.setLoadBalanceAlgorithms(balanceAlgorithmMap);
        return result;
    }
    
    /**
     * Primary-replica replication algorithm provided bean registry.
     *
     * @param environment environment
     * @return Primary-replica replication algorithm provided bean registry
     */
    @Bean
    public static PrimaryReplicaReplicationAlgorithmProvidedBeanRegistry primaryReplicaReplicationAlgorithmProvidedBeanRegistry(final Environment environment) {
        return new PrimaryReplicaReplicationAlgorithmProvidedBeanRegistry(environment);
    }
}
