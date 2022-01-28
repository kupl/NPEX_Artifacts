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

package org.apache.shardingsphere.scaling.core.job.task.incremental;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.scaling.core.config.DumperConfiguration;
import org.apache.shardingsphere.scaling.core.config.ImporterConfiguration;
import org.apache.shardingsphere.scaling.core.config.ScalingContext;
import org.apache.shardingsphere.scaling.core.datasource.DataSourceManager;
import org.apache.shardingsphere.scaling.core.exception.SyncTaskExecuteException;
import org.apache.shardingsphere.scaling.core.execute.engine.ExecuteCallback;
import org.apache.shardingsphere.scaling.core.execute.executor.AbstractShardingScalingExecutor;
import org.apache.shardingsphere.scaling.core.execute.executor.channel.DistributionChannel;
import org.apache.shardingsphere.scaling.core.execute.executor.dumper.Dumper;
import org.apache.shardingsphere.scaling.core.execute.executor.dumper.DumperFactory;
import org.apache.shardingsphere.scaling.core.execute.executor.importer.Importer;
import org.apache.shardingsphere.scaling.core.execute.executor.importer.ImporterFactory;
import org.apache.shardingsphere.scaling.core.execute.executor.record.Record;
import org.apache.shardingsphere.scaling.core.job.SyncProgress;
import org.apache.shardingsphere.scaling.core.job.position.IncrementalPosition;
import org.apache.shardingsphere.scaling.core.job.task.ScalingTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Incremental data execute task.
 */
@Slf4j
public final class IncrementalDataScalingTask extends AbstractShardingScalingExecutor<IncrementalPosition> implements ScalingTask<IncrementalPosition> {
    
    private final int concurrency;
    
    private final DumperConfiguration dumperConfiguration;
    
    private final ImporterConfiguration importerConfiguration;
    
    private final DataSourceManager dataSourceManager;
    
    private Dumper dumper;
    
    private long delayMillisecond;
    
    @SuppressWarnings("unchecked")
    public IncrementalDataScalingTask(final int concurrency, final DumperConfiguration dumperConfiguration, final ImporterConfiguration importerConfiguration) {
        this.concurrency = concurrency;
        this.dumperConfiguration = dumperConfiguration;
        this.importerConfiguration = importerConfiguration;
        dataSourceManager = new DataSourceManager();
        setTaskId(dumperConfiguration.getDataSourceName());
        setPositionManager(dumperConfiguration.getPositionManager());
    }
    
    @Override
    public void start() {
        dumper = DumperFactory.newInstanceLogDumper(dumperConfiguration, getPositionManager().getPosition());
        Collection<Importer> importers = instanceImporters();
        instanceChannel(importers);
        Future<?> future = ScalingContext.getInstance().getTaskExecuteEngine().submitAll(importers, new ExecuteCallback() {
            
            @Override
            public void onSuccess() {
            }
            
            @Override
            public void onFailure(final Throwable throwable) {
                log.error("get an error when migrating the increment data", throwable);
                dumper.stop();
            }
        });
        dumper.start();
        waitForResult(future);
        dataSourceManager.close();
    }
    
    private List<Importer> instanceImporters() {
        List<Importer> result = new ArrayList<>(concurrency);
        for (int i = 0; i < concurrency; i++) {
            result.add(ImporterFactory.newInstance(importerConfiguration, dataSourceManager));
        }
        return result;
    }
    
    private void instanceChannel(final Collection<Importer> importers) {
        DistributionChannel channel = new DistributionChannel(importers.size(), records -> {
            Record lastHandledRecord = records.get(records.size() - 1);
            if (lastHandledRecord.getPosition() instanceof IncrementalPosition) {
                getPositionManager().setPosition((IncrementalPosition) lastHandledRecord.getPosition());
            }
            delayMillisecond = System.currentTimeMillis() - lastHandledRecord.getCommitTime();
        });
        dumper.setChannel(channel);
        for (Importer each : importers) {
            each.setChannel(channel);
        }
    }
    
    private void waitForResult(final Future<?> future) {
        try {
            future.get();
        } catch (final InterruptedException ignored) {
        } catch (final ExecutionException ex) {
            throw new SyncTaskExecuteException(String.format("Task %s execute failed ", getTaskId()), ex.getCause());
        }
    }
    
    @Override
    public void stop() {
        if (null != dumper) {
            dumper.stop();
            dumper = null;
        }
    }
    
    @Override
    public SyncProgress getProgress() {
        return new IncrementalDataSyncTaskProgress(getTaskId(), delayMillisecond, getPositionManager().getPosition());
    }
    
}
