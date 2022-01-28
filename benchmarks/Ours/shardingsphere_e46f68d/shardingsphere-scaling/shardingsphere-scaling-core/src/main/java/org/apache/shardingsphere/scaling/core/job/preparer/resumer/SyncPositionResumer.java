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

package org.apache.shardingsphere.scaling.core.job.preparer.resumer;

import org.apache.shardingsphere.scaling.core.config.DumperConfiguration;
import org.apache.shardingsphere.scaling.core.config.InventoryDumperConfiguration;
import org.apache.shardingsphere.scaling.core.config.SyncConfiguration;
import org.apache.shardingsphere.scaling.core.datasource.DataSourceManager;
import org.apache.shardingsphere.scaling.core.job.ShardingScalingJob;
import org.apache.shardingsphere.scaling.core.job.position.PositionManager;
import org.apache.shardingsphere.scaling.core.job.position.resume.ResumeBreakPointManager;
import org.apache.shardingsphere.scaling.core.job.preparer.utils.JobPrepareUtil;
import org.apache.shardingsphere.scaling.core.job.task.DefaultSyncTaskFactory;
import org.apache.shardingsphere.scaling.core.job.task.ScalingTask;
import org.apache.shardingsphere.scaling.core.job.task.SyncTaskFactory;
import org.apache.shardingsphere.scaling.core.job.task.inventory.InventoryDataScalingTaskGroup;
import org.apache.shardingsphere.scaling.core.metadata.MetaDataManager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Synchronize position resumer.
 */
public final class SyncPositionResumer {
    
    private final SyncTaskFactory syncTaskFactory = new DefaultSyncTaskFactory();
    
    /**
     * Resume position from resume from break-point manager.
     *
     * @param shardingScalingJob sharding scaling job
     * @param dataSourceManager dataSource manager
     * @param resumeBreakPointManager resume from break-point manager
     */
    public void resumePosition(final ShardingScalingJob shardingScalingJob, final DataSourceManager dataSourceManager, final ResumeBreakPointManager resumeBreakPointManager) {
        resumeInventoryPosition(shardingScalingJob, dataSourceManager, resumeBreakPointManager);
        resumeIncrementalPosition(shardingScalingJob, resumeBreakPointManager);
    }
    
    private void resumeInventoryPosition(final ShardingScalingJob shardingScalingJob, final DataSourceManager dataSourceManager, final ResumeBreakPointManager resumeBreakPointManager) {
        List<ScalingTask> allInventoryDataTasks = getAllInventoryDataTasks(shardingScalingJob, dataSourceManager, resumeBreakPointManager);
        for (Collection<ScalingTask> each : JobPrepareUtil.groupInventoryDataTasks(shardingScalingJob.getSyncConfigs().get(0).getConcurrency(), allInventoryDataTasks)) {
            shardingScalingJob.getInventoryDataTasks().add(syncTaskFactory.createInventoryDataSyncTaskGroup(each));
        }
    }
    
    private List<ScalingTask> getAllInventoryDataTasks(final ShardingScalingJob shardingScalingJob, 
                                                                          final DataSourceManager dataSourceManager, final ResumeBreakPointManager resumeBreakPointManager) {
        List<ScalingTask> result = new LinkedList<>();
        for (SyncConfiguration each : shardingScalingJob.getSyncConfigs()) {
            MetaDataManager metaDataManager = new MetaDataManager(dataSourceManager.getDataSource(each.getDumperConfig().getDataSourceConfig()));
            for (Entry<String, PositionManager> entry : getInventoryPositionMap(each.getDumperConfig(), resumeBreakPointManager).entrySet()) {
                result.add(syncTaskFactory.createInventoryDataSyncTask(newInventoryDumperConfig(each.getDumperConfig(), metaDataManager, entry), each.getImporterConfig()));
            }
        }
        return result;
    }
    
    private InventoryDumperConfiguration newInventoryDumperConfig(final DumperConfiguration dumperConfig, final MetaDataManager metaDataManager, final Entry<String, PositionManager> entry) {
        String[] splitTable = entry.getKey().split("#");
        InventoryDumperConfiguration result = new InventoryDumperConfiguration(dumperConfig);
        result.setTableName(splitTable[0].split("\\.")[1]);
        result.setPositionManager(entry.getValue());
        if (2 == splitTable.length) {
            result.setSpiltNum(Integer.parseInt(splitTable[1]));
        }
        result.setPrimaryKey(metaDataManager.getTableMetaData(result.getTableName()).getPrimaryKeyColumns().get(0));
        return result;
    }
    
    private Map<String, PositionManager> getInventoryPositionMap(final DumperConfiguration dumperConfig, final ResumeBreakPointManager resumeBreakPointManager) {
        Pattern pattern = Pattern.compile(String.format("%s\\.\\w+(#\\d+)?", dumperConfig.getDataSourceName()));
        return resumeBreakPointManager.getInventoryPositionManagerMap().entrySet().stream()
                .filter(entry -> pattern.matcher(entry.getKey()).find())
                .collect(Collectors.toMap(Entry::getKey, Map.Entry::getValue, (oldValue, currentValue) -> oldValue, LinkedHashMap::new));
    }

    private void resumeIncrementalPosition(final ShardingScalingJob shardingScalingJob, final ResumeBreakPointManager resumeBreakPointManager) {
        for (SyncConfiguration each : shardingScalingJob.getSyncConfigs()) {
            each.getDumperConfig().setPositionManager(resumeBreakPointManager.getIncrementalPositionManagerMap().get(each.getDumperConfig().getDataSourceName()));
            shardingScalingJob.getIncrementalDataTasks().add(syncTaskFactory.createIncrementalDataSyncTask(each.getConcurrency(), each.getDumperConfig(), each.getImporterConfig()));
        }
    }
    
    /**
     * Persist position.
     *
     * @param shardingScalingJob sharding scaling job
     * @param resumeBreakPointManager resume from break-point manager
     */
    public void persistPosition(final ShardingScalingJob shardingScalingJob, final ResumeBreakPointManager resumeBreakPointManager) {
        persistIncrementalPosition(shardingScalingJob.getIncrementalDataTasks(), resumeBreakPointManager);
        persistInventoryPosition(shardingScalingJob.getInventoryDataTasks(), resumeBreakPointManager);
    }
    
    private void persistInventoryPosition(final List<ScalingTask> inventoryDataTasks, final ResumeBreakPointManager resumeBreakPointManager) {
        for (ScalingTask each : inventoryDataTasks) {
            if (each instanceof InventoryDataScalingTaskGroup) {
                putInventoryDataScalingTask(((InventoryDataScalingTaskGroup) each).getScalingTasks(), resumeBreakPointManager);
            }
        }
        resumeBreakPointManager.persistInventoryPosition();
    }
    
    private void putInventoryDataScalingTask(final Collection<ScalingTask> scalingTasks, final ResumeBreakPointManager resumeBreakPointManager) {
        for (ScalingTask each : scalingTasks) {
            resumeBreakPointManager.getInventoryPositionManagerMap().put(each.getTaskId(), each.getPositionManager());
        }
    }
    
    private void persistIncrementalPosition(final List<ScalingTask> incrementalDataTasks, final ResumeBreakPointManager resumeBreakPointManager) {
        for (ScalingTask each : incrementalDataTasks) {
            resumeBreakPointManager.getIncrementalPositionManagerMap().put(each.getTaskId(), each.getPositionManager());
        }
        resumeBreakPointManager.persistIncrementalPosition();
    }
}
