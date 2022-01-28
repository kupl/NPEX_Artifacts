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

package org.apache.shardingsphere.scaling.core.job;

import lombok.Getter;
import lombok.Setter;
import org.apache.shardingsphere.scaling.core.config.ScalingConfiguration;
import org.apache.shardingsphere.scaling.core.config.SyncConfiguration;
import org.apache.shardingsphere.scaling.core.job.check.DataConsistencyChecker;
import org.apache.shardingsphere.scaling.core.job.task.ScalingTask;
import org.apache.shardingsphere.scaling.core.schedule.SyncTaskControlStatus;
import org.apache.shardingsphere.scaling.core.utils.SyncConfigurationUtil;
import org.apache.shardingsphere.sharding.algorithm.keygen.SnowflakeKeyGenerateAlgorithm;

import java.util.LinkedList;
import java.util.List;

/**
 * Sharding scaling out job.
 */
@Getter
@Setter
public final class ShardingScalingJob {
    
    private static SnowflakeKeyGenerateAlgorithm idAutoIncreaseGenerator;
    
    private long jobId;
    
    private int shardingItem;
    
    private final transient List<SyncConfiguration> syncConfigs = new LinkedList<>();
    
    private final transient List<ScalingTask> inventoryDataTasks = new LinkedList<>();
    
    private final transient List<ScalingTask> incrementalDataTasks = new LinkedList<>();
    
    private transient ScalingConfiguration scalingConfig;
    
    private transient DataConsistencyChecker dataConsistencyChecker;
    
    private String status = SyncTaskControlStatus.RUNNING.name();
    
    public ShardingScalingJob() {
        initIdAutoIncreaseGenerator();
        jobId = (Long) idAutoIncreaseGenerator.generateKey();
    }
    
    public ShardingScalingJob(final ScalingConfiguration scalingConfig) {
        this();
        this.scalingConfig = scalingConfig;
        jobId = null != scalingConfig.getJobConfiguration().getJobId() ? scalingConfig.getJobConfiguration().getJobId() : jobId;
        shardingItem = scalingConfig.getJobConfiguration().getShardingItem();
        syncConfigs.addAll(SyncConfigurationUtil.toSyncConfigs(scalingConfig));
    }
    
    private static void initIdAutoIncreaseGenerator() {
        if (null != idAutoIncreaseGenerator) {
            return;
        }
        synchronized (ShardingScalingJob.class) {
            if (null != idAutoIncreaseGenerator) {
                return;
            }
            idAutoIncreaseGenerator = new SnowflakeKeyGenerateAlgorithm();
            idAutoIncreaseGenerator.init();
        }
    }
}
