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

import lombok.Getter;
import org.apache.shardingsphere.infra.config.RuleConfiguration;

import java.util.Collection;
import java.util.Collections;

/**
 * Consensus replication logic table rule configuration.
 */
@Getter
public final class ConsensusReplicationLogicTableRuleConfiguration implements RuleConfiguration {
    
    private final String logicTable;
    
    private final Collection<ConsensusReplicationActualTableRuleConfiguration> replicaGroups;
    
    public ConsensusReplicationLogicTableRuleConfiguration(final String logicTable, final Collection<ConsensusReplicationActualTableRuleConfiguration> replicaGroups) {
        this.logicTable = logicTable;
        if (null != replicaGroups) {
            this.replicaGroups = replicaGroups;
        } else {
            this.replicaGroups = Collections.emptyList();
        }
    }
}
