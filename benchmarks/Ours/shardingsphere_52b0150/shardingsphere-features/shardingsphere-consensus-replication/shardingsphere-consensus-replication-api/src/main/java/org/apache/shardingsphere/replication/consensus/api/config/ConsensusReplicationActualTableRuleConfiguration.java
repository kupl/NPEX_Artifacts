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

package org.apache.shardingsphere.replication.consensus.api.config;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import org.apache.shardingsphere.infra.config.RuleConfiguration;

/**
 * Consensus replication actual table rule configuration.
 */
@Getter
public final class ConsensusReplicationActualTableRuleConfiguration implements RuleConfiguration {
    
    private final String physicsTable;
    
    private final String replicaGroupId;
    
    private final String replicaPeers;
    
    private final String dataSourceName;
    
    public ConsensusReplicationActualTableRuleConfiguration(final String physicsTable, final String replicaGroupId, final String replicaPeers, final String dataSourceName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(physicsTable), "physicsTable is required.");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(replicaGroupId), "replicaGroupId is required.");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(replicaPeers), "replicaPeers is required.");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(dataSourceName), "dataSourceName is required.");
        this.physicsTable = physicsTable;
        this.replicaGroupId = replicaGroupId;
        this.replicaPeers = replicaPeers;
        this.dataSourceName = dataSourceName;
    }
}
