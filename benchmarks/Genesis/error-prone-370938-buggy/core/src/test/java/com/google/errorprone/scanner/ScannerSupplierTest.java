/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.scanner;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneJavaCompilerTest;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.bugpatterns.ArrayEquals;
import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.ChainingConstructorIgnoresParameter;
import com.google.errorprone.bugpatterns.DepAnn;
import com.google.errorprone.bugpatterns.LongLiteralLowerCaseSuffix;
import com.google.errorprone.bugpatterns.PreconditionsCheckNotNull;
import com.google.errorprone.bugpatterns.StaticAccessedFromInstance;
import com.google.errorprone.bugpatterns.StringEquality;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link ScannerSupplier}.
 */
@RunWith(JUnit4.class)
public class ScannerSupplierTest {

  @Test
  public void fromBugCheckerClassesWorks() {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckerClasses(
        ArrayEquals.class,
        StaticAccessedFromInstance.class);

    Set<BugChecker> expected = ImmutableSet.of(
        new ArrayEquals(),
        new StaticAccessedFromInstance());

    assertThat(ss.getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  public void fromBugCheckersWorks() {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new ArrayEquals(),
        new StaticAccessedFromInstance());

   Set<BugChecker> expected = ImmutableSet.of(
        new ArrayEquals(),
        new StaticAccessedFromInstance());

    assertThat(ss.getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  public void plusWorks() {
    ScannerSupplier ss1 = ScannerSupplier.fromBugCheckers(
        new ArrayEquals(),
        new StaticAccessedFromInstance());
    ScannerSupplier ss2 = ScannerSupplier.fromBugCheckers(
        new BadShiftAmount(),
        new PreconditionsCheckNotNull());

    Set<BugChecker> expected = ImmutableSet.of(
        new ArrayEquals(),
        new StaticAccessedFromInstance(),
        new BadShiftAmount(),
        new PreconditionsCheckNotNull());

    assertThat(ss1.plus(ss2).getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  // Calling ScannerSupplier.plus() just to make sure it throws the right exception
  @SuppressWarnings("CheckReturnValue")
  public void plusDoesntAllowDuplicateChecks() {
    ScannerSupplier ss1 = ScannerSupplier.fromBugCheckers(
        new ArrayEquals(), new StaticAccessedFromInstance());
    ScannerSupplier ss2 = ScannerSupplier.fromBugCheckers(
        new ArrayEquals());

    try {
      ss1.plus(ss2);
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage()).contains("ArrayEquals");
    }
  }

  @Test
  public void filterWorks() {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new ArrayEquals(),
        new BadShiftAmount(),
        new StaticAccessedFromInstance());
    Predicate<BugChecker> isBadShiftAmount = new Predicate<BugChecker>() {
      @Override
      public boolean apply(BugChecker input) {
        return input.canonicalName().equals("BadShiftAmount");
      }
    };

    Set<BugChecker> expected = ImmutableSet.<BugChecker>of(new BadShiftAmount());

    assertThat(ss.filter(isBadShiftAmount).getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  public void applyOverridesWorksOnEmptySeverityMap() throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new ChainingConstructorIgnoresParameter(),
        new DepAnn(),
        new LongLiteralLowerCaseSuffix());
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(Collections.<String>emptyList());

    Set<BugChecker> expected = ss.getEnabledChecks();
    assertThat(ss.applyOverrides(epOptions).getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  public void applyOverridesEnablesCheck() throws Exception {
    ScannerSupplier ss = ScannerSupplier
        .fromBugCheckers(
            new ArrayEquals(),
            new BadShiftAmount(),
            new StaticAccessedFromInstance())
        .filter(Predicates.alwaysFalse());    // disables all checks

    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of("-Xep:ArrayEquals", "-Xep:BadShiftAmount"));

    Set<BugChecker> expected = ImmutableSet.of(
        new ArrayEquals(),
        new BadShiftAmount());

    assertThat(ss.applyOverrides(epOptions).getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  public void applyOverridesDisablesChecks() throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new ChainingConstructorIgnoresParameter(),
        new DepAnn(),
        new LongLiteralLowerCaseSuffix());
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of(
            "-Xep:LongLiteralLowerCaseSuffix:OFF",
            "-Xep:ChainingConstructorIgnoresParameter:OFF"));

    Set<BugChecker> expected = ImmutableSet.<BugChecker>of(new DepAnn());

    assertThat(ss.applyOverrides(epOptions).getEnabledChecks()).isEqualTo(expected);
  }

  @Test
  // Calling ScannerSupplier.applyOverrides() just to make sure it throws the right exception
  @SuppressWarnings("CheckReturnValue")
  public void applyOverridesThrowsExceptionWhenDisablingNonDisablableCheck() throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new ErrorProneJavaCompilerTest.UnsuppressibleArrayEquals());
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of("-Xep:ArrayEquals:OFF"));

    try {
      ss.applyOverrides(epOptions);
      fail();
    } catch (InvalidCommandLineOptionException expected) {
      assertThat(expected.getMessage()).contains("may not be disabled");
    }
  }

  @Test
  // Calling ScannerSupplier.applyOverrides() just to make sure it throws the right exception
  @SuppressWarnings("CheckReturnValue")
  public void applyOverridesThrowsExceptionWhenDemotingNonDisablableCheck() throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new ErrorProneJavaCompilerTest.UnsuppressibleArrayEquals());
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of("-Xep:ArrayEquals:WARN"));

    try {
      ss.applyOverrides(epOptions);
      fail();
    } catch (InvalidCommandLineOptionException expected) {
      assertThat(expected.getMessage()).contains("may not be demoted to a warning");
    }
  }

  @Test
  // Calling ScannerSupplier.applyOverrides() just to make sure it does not throw an exception
  @SuppressWarnings("CheckReturnValue")
  public void applyOverridesSucceedsWhenDisablingUnknownCheckAndIgnoreUnknownCheckNamesIsSet()
      throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(new ArrayEquals());
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of("-XepIgnoreUnknownCheckNames", "-Xep:foo:OFF"));

    ss.applyOverrides(epOptions);
  }

  @Test
  public void applyOverridesSetsSeverity() throws Exception {
    ScannerSupplier ss = ScannerSupplier.fromBugCheckers(
        new BadShiftAmount(),
        new ChainingConstructorIgnoresParameter(),
        new StringEquality());
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(
        ImmutableList.of(
            "-Xep:ChainingConstructorIgnoresParameter:WARN",
            "-Xep:StringEquality:ERROR"));
    ScannerSupplier overriddenScannerSupplier = ss.applyOverrides(epOptions);
    
    Map<String, SeverityLevel> expected = ImmutableMap.of(
        "BadShiftAmount", SeverityLevel.ERROR,
        "ChainingConstructorIgnoresParameter", SeverityLevel.WARNING,
        "StringEquality", SeverityLevel.ERROR);

    assertThat(overriddenScannerSupplier.severities()).isEqualTo(expected);
  }
}
