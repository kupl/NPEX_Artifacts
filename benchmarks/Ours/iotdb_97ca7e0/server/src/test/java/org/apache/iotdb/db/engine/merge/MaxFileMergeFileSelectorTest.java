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

package org.apache.iotdb.db.engine.merge;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import org.apache.iotdb.db.engine.merge.manage.MergeResource;
import org.apache.iotdb.db.engine.merge.selector.MaxFileMergeFileSelector;
import org.apache.iotdb.db.engine.merge.selector.IMergeFileSelector;
import org.apache.iotdb.db.engine.storagegroup.TsFileResource;
import org.apache.iotdb.db.exception.MergeException;
import org.junit.Test;

public class MaxFileMergeFileSelectorTest extends MergeTest {

  @Test
  public void testFullSelection() throws MergeException, IOException {
    MergeResource resource = new MergeResource(seqResources, unseqResources);
    IMergeFileSelector mergeFileSelector = new MaxFileMergeFileSelector(resource, Long.MAX_VALUE);
    List[] result = mergeFileSelector.select();
    List<TsFileResource> seqSelected = result[0];
    List<TsFileResource> unseqSelected = result[1];
    assertEquals(seqResources, seqSelected);
    assertEquals(unseqResources, unseqSelected);
    resource.clear();

    resource = new MergeResource(seqResources.subList(0, 1), unseqResources);
    mergeFileSelector = new MaxFileMergeFileSelector(resource, Long.MAX_VALUE);
    result = mergeFileSelector.select();
    seqSelected = result[0];
    unseqSelected = result[1];
    assertEquals(seqResources.subList(0, 1), seqSelected);
    assertEquals(unseqResources, unseqSelected);
    resource.clear();

    resource = new MergeResource(seqResources, unseqResources.subList(0, 1));
    mergeFileSelector = new MaxFileMergeFileSelector(resource, Long.MAX_VALUE);
    result = mergeFileSelector.select();
    seqSelected = result[0];
    unseqSelected = result[1];
    assertEquals(seqResources.subList(0, 1), seqSelected);
    assertEquals(unseqResources.subList(0, 1), unseqSelected);
    resource.clear();
  }

  @Test
  public void testNonSelection() throws MergeException, IOException {
    MergeResource resource = new MergeResource(seqResources, unseqResources);
    IMergeFileSelector mergeFileSelector = new MaxFileMergeFileSelector(resource, 1);
    List[] result = mergeFileSelector.select();
    assertEquals(0, result.length);
    resource.clear();
  }

  @Test
  public void testRestrictedSelection() throws MergeException, IOException {
    MergeResource resource = new MergeResource(seqResources, unseqResources);
    IMergeFileSelector mergeFileSelector = new MaxFileMergeFileSelector(resource, 400000);
    List[] result = mergeFileSelector.select();
    List<TsFileResource> seqSelected = result[0];
    List<TsFileResource> unseqSelected = result[1];
    assertEquals(seqResources.subList(0, 3), seqSelected);
    assertEquals(unseqResources.subList(0, 3), unseqSelected);
    resource.clear();
  }
}
