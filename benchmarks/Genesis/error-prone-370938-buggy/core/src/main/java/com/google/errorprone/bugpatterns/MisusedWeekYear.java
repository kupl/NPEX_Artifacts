/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodSelect;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;

/**
 * Ban use of YYYY in a SimpleDateFormat pattern, unless it is being used for a week date.
 * Otherwise the user almost certainly meant yyyy instead.  See the summary in the {@link
 * BugPattern} below for more details.
 *
 * <p>This bug caused a Twitter outage in December 2014.
 */
@BugPattern(name = "MisusedWeekYear",
    summary = "Use of \"YYYY\" (week year) in a date pattern without \"ww\" (week in year). "
        + "You probably meant to use \"yyyy\" (year) instead.",
    explanation = "\"YYYY\" in a date pattern means \"week year\".  The week year is defined to "
        + "begin at the beginning of the week that contains the year's first Thursday.  For "
        + "example, the week year 2015 began on Monday, December 29, 2014, since January 1, 2015, "
        + "was on a Thursday.\n\n"
        + "\"Week year\" is intended to be used for week dates, e.g. \"2015-W01-1\", but is often "
        + "mistakenly used for calendar dates, e.g. 2014-12-29, in which case the year may be "
        + "incorrect during the last week of the year.  If you are formatting anything other than "
        + "a week date, you should use the year specifier \"yyyy\" instead.",
    category = JDK, severity = ERROR, maturity = MATURE)
public class MisusedWeekYear extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final Matcher<NewClassTree> simpleDateFormatConstructorMatcher =
      Matchers.<NewClassTree>anyOf(
          constructor("java.text.SimpleDateFormat",
              ImmutableList.of("java.lang.String")),
          constructor("java.text.SimpleDateFormat",
              ImmutableList.of("java.lang.String", "java.text.DateFormatSymbols")),
          constructor("java.text.SimpleDateFormat",
              ImmutableList.of("java.lang.String", "java.util.Locale")));

  private static final Matcher<MethodInvocationTree> applyPatternMatcher =
      methodSelect(Matchers.<ExpressionTree>anyOf(
          instanceMethod(isSameType("java.text.SimpleDateFormat"), "applyPattern"),
          instanceMethod(isSameType("java.text.SimpleDateFormat"), "applyLocalizedPattern")));

  /**
   * Match uses of SimpleDateFormat.applyPattern and SimpleDateFormat.applyLocalizedPattern in
   * which the pattern passed in contains YYYY but not ww, signifying that it was not intended to
   * be a week date.  If the pattern is a string literal, suggest replacing the YYYY with yyyy.
   * If the pattern is a constant, don't give a suggested fix since the fix is nonlocal.
   */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!applyPatternMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return constructDescription(tree, tree.getArguments().get(0));
  }

  /**
   * Match uses of the SimpleDateFormat constructor in which the pattern passed in contains
   * YYYY but not ww, signifying that it was not intended to be a week date.  If the pattern
   * is a string literal, suggest replacing the YYYY with yyyy.  If the pattern is a constant, don't
   * give a suggested fix since the fix is nonlocal.
   */
  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!simpleDateFormatConstructorMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return constructDescription(tree, tree.getArguments().get(0));
  }

  /**
   * Given the {@link ExpressionTree} representing the pattern argument to the various
   * methods in {@link java.text.SimpleDateFormat} that accept a pattern, construct the description
   * for this matcher to return.  May be {@link Description#NO_MATCH} if the pattern does not
   * have a constant value, does not use the week year format specifier, or is in proper week
   * date format.
   */
  private Description constructDescription(Tree tree, ExpressionTree patternArg) {
    String pattern = (String) ASTHelpers.constValue((JCTree) patternArg);
    if (pattern != null && pattern.contains("YYYY") && !pattern.contains("ww")) {
      if (patternArg.getKind() == Kind.STRING_LITERAL) {
        String replacement = patternArg.toString().replace("YYYY", "yyyy");
        return describeMatch(tree, SuggestedFix.replace(patternArg, replacement));
      } else {
        return describeMatch(tree);
      }
    }

    return Description.NO_MATCH;
  }
}
