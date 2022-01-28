/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.common.base.StandardSystemProperty.JAVA_VERSION;
import static org.junit.Assume.assumeTrue;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test cases for {@link LongLiteralLowerCaseSuffix}.
 *
 * @author Simon Nickerson (sjnickerson@google.com)
 */
@RunWith(JUnit4.class)
public class LongLiteralLowerCaseSuffixTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {

    compilationHelper = CompilationTestHelper.newInstance(new LongLiteralLowerCaseSuffix());
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(compilationHelper.fileManager()
        .sources(getClass(), "LongLiteralLowerCaseSuffixPositiveCase1.java"));
  }

  /**
   * Test for Java 7 integer literals that include underscores.
   */
  @Test
  public void testJava7PositiveCase() throws Exception {
    String[] javaVersion = JAVA_VERSION.value().split("\\.");
    assumeTrue(Integer.parseInt(javaVersion[1]) >= 7);
    compilationHelper.assertCompileFailsWithMessages(compilationHelper.fileManager()
        .sources(getClass(), "LongLiteralLowerCaseSuffixPositiveCase2.java"));
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.assertCompileSucceeds(compilationHelper.fileManager()
        .sources(getClass(), "LongLiteralLowerCaseSuffixNegativeCases.java"));
  }

  @Test
  public void testDisableable() throws Exception {
    compilationHelper.assertCompileSucceeds(compilationHelper.fileManager()
        .sources(getClass(), "LongLiteralLowerCaseSuffixPositiveCase1.java"),
        ImmutableList.of("-Xep:LongLiteralLowerCaseSuffix:OFF"));
  }
}
