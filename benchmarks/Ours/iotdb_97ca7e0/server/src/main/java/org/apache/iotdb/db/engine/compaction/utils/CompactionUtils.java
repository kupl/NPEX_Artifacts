/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.engine.compaction.utils;

import static org.apache.iotdb.db.utils.MergeUtils.writeTVPair;

import com.google.common.util.concurrent.RateLimiter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.apache.iotdb.db.conf.IoTDBDescriptor;
import org.apache.iotdb.db.engine.merge.manage.MergeManager;
import org.apache.iotdb.db.engine.storagegroup.TsFileResource;
import org.apache.iotdb.tsfile.file.metadata.ChunkMetadata;
import org.apache.iotdb.tsfile.read.TimeValuePair;
import org.apache.iotdb.tsfile.read.TsFileSequenceReader;
import org.apache.iotdb.tsfile.read.common.Chunk;
import org.apache.iotdb.tsfile.read.reader.BatchDataIterator;
import org.apache.iotdb.tsfile.read.reader.IChunkReader;
import org.apache.iotdb.tsfile.read.reader.IPointReader;
import org.apache.iotdb.tsfile.read.reader.chunk.ChunkReaderByTimestamp;
import org.apache.iotdb.tsfile.utils.Pair;
import org.apache.iotdb.tsfile.write.chunk.ChunkWriterImpl;
import org.apache.iotdb.tsfile.write.chunk.IChunkWriter;
import org.apache.iotdb.tsfile.write.schema.MeasurementSchema;
import org.apache.iotdb.tsfile.write.writer.RestorableTsFileIOWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompactionUtils {

  private static final Logger logger = LoggerFactory.getLogger(CompactionUtils.class);
  private static final int MERGE_PAGE_POINT_NUM = IoTDBDescriptor.getInstance().getConfig()
      .getMergePagePointNumberThreshold();

  private CompactionUtils() {
    throw new IllegalStateException("Utility class");
  }

  private static Pair<ChunkMetadata, Chunk> readByAppendMerge(
      Map<TsFileSequenceReader, List<ChunkMetadata>> readerChunkMetadataMap) throws IOException {
    ChunkMetadata newChunkMetadata = null;
    Chunk newChunk = null;
    for (Entry<TsFileSequenceReader, List<ChunkMetadata>> entry : readerChunkMetadataMap
        .entrySet()) {
      for (ChunkMetadata chunkMetadata : entry.getValue()) {
        Chunk chunk = entry.getKey().readMemChunk(chunkMetadata);
        if (newChunkMetadata == null) {
          newChunkMetadata = chunkMetadata;
          newChunk = chunk;
        } else {
          newChunkMetadata.mergeChunkMetadata(chunkMetadata);
          newChunk.mergeChunk(chunk);
        }
      }
    }
    return new Pair<>(newChunkMetadata, newChunk);
  }

  private static long readByDeserializeMerge(
      Map<TsFileSequenceReader, List<ChunkMetadata>> readerChunkMetadataMap, long maxVersion,
      Map<Long, TimeValuePair> timeValuePairMap)
      throws IOException {
    for (Entry<TsFileSequenceReader, List<ChunkMetadata>> entry : readerChunkMetadataMap
        .entrySet()) {
      TsFileSequenceReader reader = entry.getKey();
      List<ChunkMetadata> chunkMetadataList = entry.getValue();
      for (ChunkMetadata chunkMetadata : chunkMetadataList) {
        maxVersion = Math.max(chunkMetadata.getVersion(), maxVersion);
        IChunkReader chunkReader = new ChunkReaderByTimestamp(
            reader.readMemChunk(chunkMetadata));
        while (chunkReader.hasNextSatisfiedPage()) {
          IPointReader iPointReader = new BatchDataIterator(
              chunkReader.nextPageData());
          while (iPointReader.hasNextTimeValuePair()) {
            TimeValuePair timeValuePair = iPointReader.nextTimeValuePair();
            timeValuePairMap.put(timeValuePair.getTimestamp(), timeValuePair);
          }
        }
      }
    }
    return maxVersion;
  }

  private static long writeByAppendMerge(long maxVersion, String device,
      RateLimiter compactionWriteRateLimiter,
      Map<TsFileSequenceReader, List<ChunkMetadata>> readerChunkMetadatasMap,
      TsFileResource targetResource, RestorableTsFileIOWriter writer) throws IOException {
    Pair<ChunkMetadata, Chunk> chunkPair = readByAppendMerge(readerChunkMetadatasMap);
    ChunkMetadata newChunkMetadata = chunkPair.left;
    Chunk newChunk = chunkPair.right;
    if (newChunkMetadata != null && newChunk != null) {
      maxVersion = Math.max(newChunkMetadata.getVersion(), maxVersion);
      // wait for limit write
      MergeManager.mergeRateLimiterAcquire(compactionWriteRateLimiter,
          (long) newChunk.getHeader().getDataSize() + newChunk.getData().position());
      writer.writeChunk(newChunk, newChunkMetadata);
      targetResource.updateStartTime(device, newChunkMetadata.getStartTime());
      targetResource.updateEndTime(device, newChunkMetadata.getEndTime());
    }
    return maxVersion;
  }

  private static long writeByDeserializeMerge(long maxVersion, String device,
      RateLimiter compactionRateLimiter,
      Entry<String, Map<TsFileSequenceReader, List<ChunkMetadata>>> entry,
      TsFileResource targetResource, RestorableTsFileIOWriter writer) throws IOException {
    Map<Long, TimeValuePair> timeValuePairMap = new TreeMap<>();
    maxVersion = readByDeserializeMerge(entry.getValue(), maxVersion, timeValuePairMap);
    Iterator<List<ChunkMetadata>> chunkMetadataListIterator = entry.getValue().values()
        .iterator();
    if (!chunkMetadataListIterator.hasNext()) {
      return maxVersion;
    }
    List<ChunkMetadata> chunkMetadataList = chunkMetadataListIterator.next();
    if (chunkMetadataList.isEmpty()) {
      return maxVersion;
    }
    IChunkWriter chunkWriter = new ChunkWriterImpl(
        new MeasurementSchema(entry.getKey(), chunkMetadataList.get(0).getDataType()));
    for (TimeValuePair timeValuePair : timeValuePairMap.values()) {
      writeTVPair(timeValuePair, chunkWriter);
      targetResource.updateStartTime(device, timeValuePair.getTimestamp());
      targetResource.updateEndTime(device, timeValuePair.getTimestamp());
    }
    // wait for limit write
    MergeManager
        .mergeRateLimiterAcquire(compactionRateLimiter, chunkWriter.getCurrentChunkSize());
    chunkWriter.writeToFileWriter(writer);
    return maxVersion;
  }

  private static Set<String> getTsFileDevicesSet(List<TsFileResource> subLevelResources,
      Map<String, TsFileSequenceReader> tsFileSequenceReaderMap, String storageGroup)
      throws IOException {
    Set<String> tsFileDevicesSet = new HashSet<>();
    for (TsFileResource levelResource : subLevelResources) {
      TsFileSequenceReader reader = buildReaderFromTsFileResource(levelResource,
          tsFileSequenceReaderMap,
          storageGroup);
      if (reader == null) {
        continue;
      }
      tsFileDevicesSet.addAll(reader.getAllDevices());
    }
    return tsFileDevicesSet;
  }

  /**
   * @param targetResource the target resource to be merged to
   * @param tsFileResources the source resource to be merged
   * @param storageGroup the storage group name
   * @param compactionLogger the logger
   * @param devices the devices to be skipped(used by recover)
   */
  @SuppressWarnings("squid:S3776") // Suppress high Cognitive Complexity warning
  public static void merge(TsFileResource targetResource,
      List<TsFileResource> tsFileResources, String storageGroup,
      CompactionLogger compactionLogger,
      Set<String> devices, boolean sequence) throws IOException {
    RestorableTsFileIOWriter writer = new RestorableTsFileIOWriter(targetResource.getTsFile());
    Map<String, TsFileSequenceReader> tsFileSequenceReaderMap = new HashMap<>();
    RateLimiter compactionWriteRateLimiter = MergeManager.getINSTANCE().getMergeWriteRateLimiter();
    Set<String> tsFileDevicesMap = getTsFileDevicesSet(tsFileResources, tsFileSequenceReaderMap,
        storageGroup);
    for (String device : tsFileDevicesMap) {
      if (devices.contains(device)) {
        continue;
      }
      writer.startChunkGroup(device);
      // sort chunkMeta by measurement
      Map<String, Map<TsFileSequenceReader, List<ChunkMetadata>>> measurementChunkMetadataMap = new HashMap<>();
      for (TsFileResource levelResource : tsFileResources) {
        TsFileSequenceReader reader = buildReaderFromTsFileResource(levelResource,
            tsFileSequenceReaderMap, storageGroup);
        if (reader == null) {
          continue;
        }
        Map<String, List<ChunkMetadata>> chunkMetadataMap = reader
            .readChunkMetadataInDevice(device);
        for (Entry<String, List<ChunkMetadata>> entry : chunkMetadataMap.entrySet()) {
          for (ChunkMetadata chunkMetadata : entry.getValue()) {
            Map<TsFileSequenceReader, List<ChunkMetadata>> readerChunkMetadataMap;
            String measurementUid = chunkMetadata.getMeasurementUid();
            if (measurementChunkMetadataMap.containsKey(measurementUid)) {
              readerChunkMetadataMap = measurementChunkMetadataMap.get(measurementUid);
            } else {
              readerChunkMetadataMap = new LinkedHashMap<>();
            }
            List<ChunkMetadata> chunkMetadataList;
            if (readerChunkMetadataMap.containsKey(reader)) {
              chunkMetadataList = readerChunkMetadataMap.get(reader);
            } else {
              chunkMetadataList = new ArrayList<>();
            }
            chunkMetadataList.add(chunkMetadata);
            readerChunkMetadataMap.put(reader, chunkMetadataList);
            measurementChunkMetadataMap
                .put(chunkMetadata.getMeasurementUid(), readerChunkMetadataMap);
          }
        }
      }
      if (!sequence) {
        long maxVersion = Long.MIN_VALUE;
        for (Entry<String, Map<TsFileSequenceReader, List<ChunkMetadata>>> entry : measurementChunkMetadataMap
            .entrySet()) {
          maxVersion = writeByDeserializeMerge(maxVersion, device, compactionWriteRateLimiter,
              entry,
              targetResource, writer);
        }
        writer.endChunkGroup();
        writer.writeVersion(maxVersion);
      } else {
        long maxVersion = Long.MIN_VALUE;
        for (Entry<String, Map<TsFileSequenceReader, List<ChunkMetadata>>> entry : measurementChunkMetadataMap
            .entrySet()) {
          Map<TsFileSequenceReader, List<ChunkMetadata>> readerChunkMetadatasMap = entry.getValue();
          boolean isPageEnoughLarge = true;
          for (List<ChunkMetadata> chunkMetadatas : readerChunkMetadatasMap.values()) {
            for (ChunkMetadata chunkMetadata : chunkMetadatas) {
              if (chunkMetadata.getNumOfPoints() < MERGE_PAGE_POINT_NUM) {
                isPageEnoughLarge = false;
                break;
              }
            }
          }
          if (isPageEnoughLarge) {
            logger.debug("{} [Compaction] page enough large, use append merge", storageGroup);
            // append page in chunks, so we do not have to deserialize a chunk
            maxVersion = writeByAppendMerge(maxVersion, device, compactionWriteRateLimiter,
                readerChunkMetadatasMap, targetResource, writer);
          } else {
            logger
                .debug("{} [Compaction] page too small, use deserialize merge", storageGroup);
            // we have to deserialize chunks to merge pages
            maxVersion = writeByDeserializeMerge(maxVersion, device, compactionWriteRateLimiter,
                entry, targetResource, writer);
          }
        }
        writer.endChunkGroup();
        writer.writeVersion(maxVersion);
      }
      if (compactionLogger != null) {
        compactionLogger.logDevice(device, writer.getPos());
      }
    }

    for (TsFileSequenceReader reader : tsFileSequenceReaderMap.values()) {
      reader.close();
    }
    Set<Long> historicalVersions = new HashSet<>();
    for (TsFileResource tsFileResource : tsFileResources) {
      historicalVersions.addAll(tsFileResource.getHistoricalVersions());
    }
    targetResource.setHistoricalVersions(historicalVersions);
    targetResource.serialize();
    writer.endFile();
    targetResource.close();
  }

  private static TsFileSequenceReader buildReaderFromTsFileResource(TsFileResource levelResource,
      Map<String, TsFileSequenceReader> tsFileSequenceReaderMap, String storageGroup) {
    return tsFileSequenceReaderMap.computeIfAbsent(levelResource.getTsFile().getAbsolutePath(),
        path -> {
          try {
            if (levelResource.getTsFile().exists()) {
              return new TsFileSequenceReader(path);
            } else {
              logger.info("{} tsfile does not exist", path);
              return null;
            }
          } catch (IOException e) {
            logger.error(
                "Storage group {}, flush recover meets error. reader create failed.",
                storageGroup, e);
            return null;
          }
        });
  }
}