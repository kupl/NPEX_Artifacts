/*
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.dataflow.sdk.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.dataflow.sdk.testing.ResetDateTimeProvider;
import com.google.cloud.dataflow.sdk.testing.RestoreSystemProperties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link DataflowPipelineOptions}. */
@RunWith(JUnit4.class)
public class DataflowPipelineOptionsTest {
  @Rule public TestRule restoreSystemProperties = new RestoreSystemProperties();
  @Rule public ResetDateTimeProvider resetDateTimeProviderRule = new ResetDateTimeProvider();

  @Test
  public void testJobNameIsSet() {
    DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
    options.setJobName("TestJobName");
    assertEquals("TestJobName", options.getJobName());
  }

  @Test
  public void testUserNameIsNotSet() {
    resetDateTimeProviderRule.setDateTimeFixed("2014-12-08T19:07:06.698Z");
    System.getProperties().remove("user.name");
    DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
    options.setAppName("TestApplication");
    assertEquals("testapplication--1208190706", options.getJobName());
    assertTrue(options.getJobName().length() <= 40);
  }

  @Test
  public void testAppNameAndUserNameIsTooLong() {
    resetDateTimeProviderRule.setDateTimeFixed("2014-12-08T19:07:06.698Z");
    System.getProperties().put("user.name", "abcdeabcdeabcdeabcdeabcdeabcde");
    DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
    options.setAppName("1234567890123456789012345678901234567890");
    assertEquals("a234567890123456789-abcdeabcd-1208190706", options.getJobName());
    assertTrue(options.getJobName().length() <= 40);
  }

  @Test
  public void testAppNameIsTooLong() {
    resetDateTimeProviderRule.setDateTimeFixed("2014-12-08T19:07:06.698Z");
    System.getProperties().put("user.name", "abcde");
    DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
    options.setAppName("1234567890123456789012345678901234567890");
    assertEquals("a2345678901234567890123-abcde-1208190706", options.getJobName());
    assertTrue(options.getJobName().length() <= 40);
  }

  @Test
  public void testUserNameIsTooLong() {
    resetDateTimeProviderRule.setDateTimeFixed("2014-12-08T19:07:06.698Z");
    System.getProperties().put("user.name", "abcdeabcdeabcdeabcdeabcdeabcde");
    DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
    options.setAppName("1234567890");
    assertEquals("a234567890-abcdeabcdeabcdeabc-1208190706", options.getJobName());
    assertTrue(options.getJobName().length() <= 40);
  }


  @Test
  public void testUtf8UserNameAndApplicationNameIsNormalized() {
    resetDateTimeProviderRule.setDateTimeFixed("2014-12-08T19:07:06.698Z");
    System.getProperties().put("user.name", "??i ??nt????n??????n??l ");
    DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
    options.setAppName("f????n??t??k ??so??si??e????n");
    assertEquals("f00n0t0k00so0si0e00-0i00nt00n-1208190706", options.getJobName());
    assertTrue(options.getJobName().length() <= 40);
  }
}
