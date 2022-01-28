/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class VisitorState {

  private final DescriptionListener descriptionListener;
  public final Context context;
  private final TreePath path;
  private final Map<String, SeverityLevel> severityMap;

  // The default no-op implementation of DescriptionListener. We use this instead of null so callers
  // of getDescriptionListener() don't have to do null-checking.
  private static final DescriptionListener NULL_LISTENER = new DescriptionListener() {
    @Override public void onDescribed(Description description) {}
  };

  public VisitorState(Context context) {
    this(context, null, NULL_LISTENER, ImmutableMap.<String, SeverityLevel>of());
  }

  public VisitorState(Context context, DescriptionListener listener) {
    this(context, null, listener, Collections.<String, SeverityLevel>emptyMap());
  }

  public VisitorState(Context context, DescriptionListener listener,
      Map<String, SeverityLevel> severityMap) {
    this(context, null, listener, severityMap);
  }

  private VisitorState(Context context, TreePath path,
      DescriptionListener descriptionListener, Map<String, SeverityLevel> severityMap) {
    this.context = context;
    this.path = path;
    this.descriptionListener = descriptionListener;
    this.severityMap = severityMap;
  }

  public VisitorState withPath(TreePath path) {
    return new VisitorState(context, path, descriptionListener, severityMap);
  }

  public TreePath getPath() {
    return path;
  }

  public TreeMaker getTreeMaker() {
    return TreeMaker.instance(context);
  }

  public Types getTypes() {
    return Types.instance(context);
  }

  public Symtab getSymtab() {
    return Symtab.instance(context);
  }

  public void reportMatch(Description description) {
    // TODO(user): creating Descriptions with the default severity and updating them here isn't
    // ideal (we could forget to do the update), so consider removing severity from Description.
    // Instead, there could be another method on the listener that took a description and a
    // (separate) SeverityLevel. Adding the method to the interface would require updating the
    // existing implementations, though. Wait for default methods?
    SeverityLevel override = severityMap.get(description.checkName);
    if (override != null) {
      description = description.applySeverityOverride(override);
    }
    descriptionListener.onDescribed(description);
  }

  public Name getName(String nameStr) {
    return Names.instance(context).fromString(nameStr);
  }

  /**
   * Given the string representation of a simple (non-array, non-generic) type, return the
   * matching Type.
   *
   * <p>If this method returns null, the compiler doesn't have access to this type, which means
   * that if you are comparing other types to this for equality or the subtype relation, your
   * result would always be false even if it could create the type.  Thus it might be best to bail
   * out early in your matcher if this method returns null on your type of interest.
   *
   * @param typeStr The canonical string representation of a simple type (e.g., "java.lang.Object")
   * @return The Type that corresponds to the string, or null if it cannot be found
   */
  public Type getTypeFromString(String typeStr) {
    validateTypeStr(typeStr);
    if (isPrimitiveType(typeStr)) {
      return getPrimitiveType(typeStr);
    }
    Name typeName = getName(typeStr);
    try {
      ClassSymbol typeSymbol = getSymtab().classes.get(typeName);
      if (typeSymbol == null) {
        JavaCompiler compiler = JavaCompiler.instance(context);
        Symbol sym = compiler.resolveIdent(typeStr);
        if (!(sym instanceof ClassSymbol)) {
          return null;
        }
        typeSymbol = (ClassSymbol) sym;
      }
      Type type = typeSymbol.asType();
      // Throws CompletionFailure if the source/class file for this type is not available.
      // This is hacky but the best way I can think of to handle this case.
      type.complete();
      if (type.isErroneous()) {
        return null;
      }
      return type;
    } catch (CompletionFailure failure) {
      return null;
    }
  }

  /**
   * @param symStr the string representation of a symbol
   * @return the Symbol object, or null if it cannot be found
   */
  public Symbol getSymbolFromString(String symStr) {
    try {
      Name symName = getName(symStr);
      Symbol result = getSymtab().classes.get(symName);
      if (result != null) {
        // Force a completion failure if the type is not available.
        result.complete();
      }
      return result;
    } catch (CompletionFailure failure) {
      return null;
    }
  }

  /**
   * Build an instance of a Type.
   */
  public Type getType(Type baseType, boolean isArray, List<Type> typeParams) {
    boolean isGeneric = typeParams != null && !typeParams.equals(List.nil());
    if (!isArray && !isGeneric) {
      // Simple type.
      return baseType;
    } else if (isArray && !isGeneric) {
      // Array type, not generic.
      ClassSymbol arraySymbol = getSymtab().arrayClass;
      return new ArrayType(baseType, arraySymbol);
    } else if (!isArray && isGeneric) {
      return new ClassType(Type.noType, typeParams, baseType.tsym);
    } else {
      throw new IllegalArgumentException("Unsupported arguments to getType");
    }
  }

  /**
   * Build an instance of a Type.
   */
  public Type getType(Type baseType, boolean isArray, java.util.List<Type> typeParams) {
    boolean isGeneric = typeParams != null && !typeParams.equals(List.nil());
    if (!isArray && !isGeneric) {
      // Simple type.
      return baseType;
    } else if (isArray && !isGeneric) {
      // Array type, not generic.
      ClassSymbol arraySymbol = getSymtab().arrayClass;
      return new ArrayType(baseType, arraySymbol);
    } else if (!isArray && isGeneric) {
      // Generic type, not array.
      List<Type> typeParamsCopy = List.from(typeParams.toArray(new Type[typeParams.size()]));
      return new ClassType(Type.noType, typeParamsCopy, baseType.tsym);
    } else {
      throw new IllegalArgumentException("Unsupported arguments to getType");
    }
  }

  /**
   * Returns the {@link TreePath} to the nearest tree node of one of the given types. To instead
   * retrieve the element directly, use {@link #findEnclosing(Class...)}.
   *
   * @return the path, or {@code null} if there is no match
   */
  @SafeVarargs
  public final TreePath findPathToEnclosing(Class<? extends Tree>... classes) {
    TreePath enclosingPath = getPath();
    while (enclosingPath != null) {
      for (Class<? extends Tree> clazz : classes) {
        if (clazz.isInstance(enclosingPath.getLeaf())) {
          return enclosingPath;
        }
      }
      enclosingPath = enclosingPath.getParentPath();
    }
    return null;
  }

  /**
   * Find the first enclosing tree node of one of the given types.
   *
   * @return the node, or {@code null} if there is no match
   */
  @SuppressWarnings("unchecked") // findPathToEnclosing guarantees that the type is from |classes|
  @SafeVarargs
  public final <T extends Tree> T findEnclosing(Class<? extends T>... classes) {
    TreePath pathToEnclosing = findPathToEnclosing(classes);
    return (pathToEnclosing == null) ? null : (T) pathToEnclosing.getLeaf();
  }

  /**
   * Gets the current source file.
   *
   * @return the source file as a sequence of characters, or null if it is not available
   */
  public CharSequence getSourceCode() {
    try {
      return getPath().getCompilationUnit().getSourceFile().getCharContent(false);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Gets the original source code that represents the given node.
   *
   * <p>Note that this may be different from what is returned by calling .toString() on the node.
   * This returns exactly what is in the source code, whereas .toString() pretty-prints the node
   * from its AST representation.
   *
   * @return the source code that represents the node.
   */
  public CharSequence getSourceForNode(JCTree node) {
    int start = node.getStartPosition();
    int end = getEndPosition(node);
    if (end < 0) {
      return null;
    }
    return getSourceCode().subSequence(start, end);
  }

  /**
   * Gets the end position of the given node.
   *
   * @return the end position of the node, or -1 if it is not available
   */
  public int getEndPosition(JCTree node) {
    JCCompilationUnit compilationUnit = (JCCompilationUnit) getPath().getCompilationUnit();
    if (compilationUnit.endPositions == null) {
      return -1;
    }
    return node.getEndPosition(compilationUnit.endPositions);
  }

  /**
   * Validates a type string, ensuring it is not generic and not an array type.
   */
  private static void validateTypeStr(String typeStr) {
    if (typeStr.contains("[") || typeStr.contains("]")) {
      throw new IllegalArgumentException("Cannot convert array types, please build them using "
          + "getType()");
    }
    if (typeStr.contains("<") || typeStr.contains(">")) {
      throw new IllegalArgumentException("Cannot covnert generic types, please build them using "
          + "getType()");
    }
  }

  /**
   * Given a string that represents a primitive type (e.g., "int"), return the corresponding Type.
   */
  private Type getPrimitiveType(String typeStr) {
    if (typeStr.equals("byte")) {
      return getSymtab().byteType;
    } else if (typeStr.equals("short")) {
      return getSymtab().shortType;
    } else if (typeStr.equals("int")) {
      return getSymtab().intType;
    } else if (typeStr.equals("long")) {
      return getSymtab().longType;
    } else if (typeStr.equals("float")) {
      return getSymtab().floatType;
    } else if (typeStr.equals("double")) {
      return getSymtab().doubleType;
    } else if (typeStr.equals("boolean")) {
      return getSymtab().booleanType;
    } else if (typeStr.equals("char")) {
      return getSymtab().charType;
    } else {
      throw new IllegalStateException("Type string " + typeStr + " expected to be primitive");
    }
  }

  private static boolean isPrimitiveType(String typeStr) {
    return typeStr.equals("byte") || typeStr.equals("short") || typeStr.equals("int") ||
        typeStr.equals("long") || typeStr.equals("float") || typeStr.equals("double") ||
        typeStr.equals("boolean") || typeStr.equals("char");
  }
}
