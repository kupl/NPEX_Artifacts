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

package org.apache.iotdb.db.engine.compaction.no;

import static org.apache.iotdb.db.conf.IoTDBConstant.FILE_NAME_SEPARATOR;
import static org.apache.iotdb.tsfile.common.constant.TsFileConstant.TSFILE_SUFFIX;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.apache.iotdb.db.engine.storagegroup.TsFileResource;
import org.apache.iotdb.db.engine.compaction.TsFileManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoCompactionTsFileManagement extends TsFileManagement {

  private static final Logger logger = LoggerFactory.getLogger(NoCompactionTsFileManagement.class);
  // includes sealed and unsealed sequence TsFiles
  private TreeSet<TsFileResource> sequenceFileTreeSet = new TreeSet<>(
      (o1, o2) -> {
        int rangeCompare = Long.compare(Long.parseLong(o1.getTsFile().getParentFile().getName()),
            Long.parseLong(o2.getTsFile().getParentFile().getName()));
        return rangeCompare == 0 ? compareFileName(o1.getTsFile(), o2.getTsFile()) : rangeCompare;
      });

  // includes sealed and unsealed unSequence TsFiles
  private List<TsFileResource> unSequenceFileList = new ArrayList<>();

  public NoCompactionTsFileManagement(String storageGroupName, String storageGroupDir) {
    super(storageGroupName, storageGroupDir);
  }

  // ({systemTime}-{versionNum}-{mergeNum}.tsfile)
  public static int compareFileName(File o1, File o2) {
    String[] items1 = o1.getName().replace(TSFILE_SUFFIX, "")
        .split(FILE_NAME_SEPARATOR);
    String[] items2 = o2.getName().replace(TSFILE_SUFFIX, "")
        .split(FILE_NAME_SEPARATOR);
    long ver1 = Long.parseLong(items1[0]);
    long ver2 = Long.parseLong(items2[0]);
    int cmp = Long.compare(ver1, ver2);
    if (cmp == 0) {
      return Long.compare(Long.parseLong(items1[1]), Long.parseLong(items2[1]));
    } else {
      return cmp;
    }
  }

  @Override
  public List<TsFileResource> getStableTsFileList(boolean sequence) {
    return getTsFileList(sequence);
  }

  @Override
  public List<TsFileResource> getTsFileList(boolean sequence) {
    if (sequence) {
      return new ArrayList<>(sequenceFileTreeSet);
    } else {
      return unSequenceFileList;
    }
  }

  @Override
  public Iterator<TsFileResource> getIterator(boolean sequence) {
    return getTsFileList(sequence).iterator();
  }

  @Override
  public void remove(TsFileResource tsFileResource, boolean sequence) {
    if (sequence) {
      sequenceFileTreeSet.remove(tsFileResource);
    } else {
      unSequenceFileList.remove(tsFileResource);
    }
  }

  @Override
  public void removeAll(List<TsFileResource> tsFileResourceList, boolean sequence) {
    if (sequence) {
      sequenceFileTreeSet.removeAll(tsFileResourceList);
    } else {
      unSequenceFileList.removeAll(tsFileResourceList);
    }
  }

  @Override
  public void add(TsFileResource tsFileResource, boolean sequence) {
    if (sequence) {
      sequenceFileTreeSet.add(tsFileResource);
    } else {
      unSequenceFileList.add(tsFileResource);
    }
  }

  @Override
  public void addAll(List<TsFileResource> tsFileResourceList, boolean sequence) {
    if (sequence) {
      sequenceFileTreeSet.addAll(tsFileResourceList);
    } else {
      unSequenceFileList.addAll(tsFileResourceList);
    }
  }

  @Override
  public boolean contains(TsFileResource tsFileResource, boolean sequence) {
    if (sequence) {
      return sequenceFileTreeSet.contains(tsFileResource);
    } else {
      return unSequenceFileList.contains(tsFileResource);
    }
  }

  @Override
  public void clear() {
    sequenceFileTreeSet.clear();
    unSequenceFileList.clear();
  }

  @Override
  public boolean isEmpty(boolean sequence) {
    if (sequence) {
      return sequenceFileTreeSet.isEmpty();
    } else {
      return unSequenceFileList.isEmpty();
    }
  }

  @Override
  public int size(boolean sequence) {
    if (sequence) {
      return sequenceFileTreeSet.size();
    } else {
      return unSequenceFileList.size();
    }
  }

  @Override
  public void recover() {
    logger.info("{} no recover logic", storageGroupName);
  }

  @Override
  public void forkCurrentFileList(long timePartition) {
    logger.info("{} do not need fork", storageGroupName);
  }

  @Override
  protected void merge(long timePartition) {
    logger.info("{} no merge logic", storageGroupName);
  }
}
