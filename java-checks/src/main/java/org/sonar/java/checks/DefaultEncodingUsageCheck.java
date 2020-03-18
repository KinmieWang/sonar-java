/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1943")
public class DefaultEncodingUsageCheck extends AbstractMethodDetection {

  private static final String INT = "int";
  private static final String BOOLEAN = "boolean";
  private static final String BYTE_ARRAY = "byte[]";
  private static final String JAVA_IO_FILE = "java.io.File";
  private static final String JAVA_IO_READER = "java.io.Reader";
  private static final String JAVA_IO_WRITER = "java.io.Writer";
  private static final String JAVA_IO_FILEWRITER = "java.io.FileWriter";
  private static final String JAVA_IO_FILEREADER = "java.io.FileReader";
  private static final String JAVA_IO_PRINTWRITER = "java.io.PrintWriter";
  private static final String JAVA_IO_PRINTSTREAM = "java.io.PrintStream";
  private static final String JAVA_IO_INPUTSTREAM = "java.io.InputStream";
  private static final String JAVA_IO_OUTPUTSTREAM = "java.io.OutputStream";
  private static final String JAVA_IO_BYTEARRAYOUTPUTSTREAM = "java.io.ByteArrayOutputStream";
  private static final String JAVA_IO_OUTPUTSTREAMWRITER = "java.io.OutputStreamWriter";
  private static final String JAVA_IO_INPUTSTREAMREADER = "java.io.InputStreamReader";
  private static final String JAVA_NIO_FILE_PATH = "java.nio.file.Path";
  private static final String JAVA_NIO_CHARSET = "java.nio.charset.Charset";
  private static final String JAVA_LANG_CHARSEQUENCE = "java.lang.CharSequence";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_UTIL_SCANNER = "java.util.Scanner";
  private static final String JAVA_UTIL_FORMATTER = "java.util.Formatter";

  private static final String[] FORBIDDEN_TYPES = {JAVA_IO_FILEREADER, JAVA_IO_FILEWRITER};
  private static final String COMMONS_IOUTILS = "org.apache.commons.io.IOUtils";
  private static final String COMMONS_FILEUTILS = "org.apache.commons.io.FileUtils";

  private static final List<MethodMatcher> COMMONS_IO = Arrays.asList(
    method(COMMONS_IOUTILS, "copy").withParameters(JAVA_IO_INPUTSTREAM, JAVA_IO_WRITER),
    method(COMMONS_IOUTILS, "copy").withParameters(JAVA_IO_READER, JAVA_IO_OUTPUTSTREAM),
    method(COMMONS_IOUTILS, "readLines").withParameters(JAVA_IO_INPUTSTREAM),
    method(COMMONS_IOUTILS, "toByteArray").withParameters(JAVA_IO_READER),
    method(COMMONS_IOUTILS, "toCharArray").withParameters(JAVA_IO_INPUTSTREAM),
    method(COMMONS_IOUTILS, "toInputStream").withParameters(JAVA_LANG_CHARSEQUENCE),
    method(COMMONS_IOUTILS, "toInputStream").withParameters(JAVA_LANG_STRING),
    method(COMMONS_IOUTILS, "toString").withParameters(BYTE_ARRAY),
    method(COMMONS_IOUTILS, "toString").withParameters("java.net.URI"),
    method(COMMONS_IOUTILS, "toString").withParameters("java.net.URL"),
    method(COMMONS_IOUTILS, "write").withParameters("char[]", JAVA_IO_OUTPUTSTREAM),
    // TypeCriteria.subtypeOf is used to cover also signatures with String and StringBuffer
    method(COMMONS_IOUTILS, "write").withParameters(TypeCriteria.subtypeOf(JAVA_LANG_CHARSEQUENCE), TypeCriteria.is(JAVA_IO_OUTPUTSTREAM)),
    method(COMMONS_IOUTILS, "writeLines").withParameters("java.util.Collection", JAVA_LANG_STRING, JAVA_IO_OUTPUTSTREAM),

    method(COMMONS_FILEUTILS, "readFileToString").withParameters(JAVA_IO_FILE),
    method(COMMONS_FILEUTILS, "readLines").withParameters(JAVA_IO_FILE),
    method(COMMONS_FILEUTILS, "write").withParameters(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE),
    method(COMMONS_FILEUTILS, "write").withParameters(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, BOOLEAN),
    method(COMMONS_FILEUTILS, "writeStringToFile").withParameters(JAVA_IO_FILE, JAVA_LANG_STRING)
  );

  private static final List<MethodMatcher> COMMONS_IO_WITH_CHARSET = COMMONS_IO.stream()
    .flatMap(m -> Stream.of(m.copy().addParameter(JAVA_LANG_STRING), m.copy().addParameter(JAVA_NIO_CHARSET)))
    .collect(Collectors.toList());

  private static final MethodMatchers COMMONS_IO_CHARSET_MATCHERS = MethodMatchers.or(COMMONS_IO_WITH_CHARSET);

  private static final List<MethodMatcher> FILEUTILS_WRITE_WITH_CHARSET = Arrays.asList(
    method(COMMONS_FILEUTILS, "write").withParameters(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, JAVA_LANG_STRING, BOOLEAN),
    method(COMMONS_FILEUTILS, "write").withParameters(JAVA_IO_FILE, JAVA_LANG_CHARSEQUENCE, JAVA_NIO_CHARSET, BOOLEAN)
  );

  private static final MethodMatchers FILEUTILS_WRITE_WITH_CHARSET_MATCHERS =
    MethodMatchers.or(FILEUTILS_WRITE_WITH_CHARSET);

  private Set<Tree> excluded = new HashSet<>();

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    excluded.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!excluded.contains(tree)) {
      super.visitNode(tree);
      if (tree.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) tree;
        boolean foundIssue = checkForbiddenTypes(variableTree.simpleName(), variableTree.type().symbolType());
        if (foundIssue) {
          excluded.add(variableTree.initializer());
        }
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        checkForbiddenTypes(ExpressionUtils.methodName(mit), mit.symbolType());
      }
    }
  }

  private boolean checkForbiddenTypes(Tree reportTree, Type symbolType) {
    for (String forbiddenType : FORBIDDEN_TYPES) {
      if (symbolType.is(forbiddenType)) {
        reportIssue(reportTree, "Remove this use of \"" + forbiddenType + "\"");
        return true;
      }
    }
    return false;
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    ArrayList<MethodMatcher> matchers = new ArrayList<>(Arrays.asList(
      method(JAVA_LANG_STRING, "getBytes").withoutParameters(),
      method(JAVA_LANG_STRING, "getBytes").withParameters(INT, INT, BYTE_ARRAY, INT),
      constructor(JAVA_LANG_STRING).withParameters(BYTE_ARRAY),
      constructor(JAVA_LANG_STRING).withParameters(BYTE_ARRAY, INT, INT),
      method(JAVA_IO_BYTEARRAYOUTPUTSTREAM, "toString").withoutParameters(),
      constructor(JAVA_IO_FILEREADER).withParameters("java.io.FileDescriptor"),
      constructor(JAVA_IO_FILEREADER).withParameters(JAVA_IO_FILE),
      constructor(JAVA_IO_FILEREADER).withParameters(JAVA_LANG_STRING),
      constructor(JAVA_IO_FILEWRITER).withParameters("java.io.FileDescriptor"),
      constructor(JAVA_IO_FILEWRITER).withParameters(JAVA_IO_FILE),
      constructor(JAVA_IO_FILEWRITER).withParameters(JAVA_IO_FILE, BOOLEAN),
      constructor(JAVA_IO_FILEWRITER).withParameters(JAVA_LANG_STRING),
      constructor(JAVA_IO_FILEWRITER).withParameters(JAVA_LANG_STRING, BOOLEAN),
      constructor(JAVA_IO_INPUTSTREAMREADER).withParameters(JAVA_IO_INPUTSTREAM),
      constructor(JAVA_IO_OUTPUTSTREAMWRITER).withParameters(JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_IO_PRINTSTREAM).withParameters(JAVA_IO_FILE),
      constructor(JAVA_IO_PRINTSTREAM).withParameters(JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_IO_PRINTSTREAM).withParameters(JAVA_IO_OUTPUTSTREAM, BOOLEAN),
      constructor(JAVA_IO_PRINTSTREAM).withParameters(JAVA_LANG_STRING),
      constructor(JAVA_IO_PRINTWRITER).withParameters(JAVA_IO_FILE),
      constructor(JAVA_IO_PRINTWRITER).withParameters(JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_IO_PRINTWRITER).withParameters(JAVA_IO_OUTPUTSTREAM, BOOLEAN),
      constructor(JAVA_IO_PRINTWRITER).withParameters(JAVA_LANG_STRING),
      constructor(JAVA_UTIL_FORMATTER).withParameters(JAVA_LANG_STRING),
      constructor(JAVA_UTIL_FORMATTER).withParameters(JAVA_IO_FILE),
      constructor(JAVA_UTIL_FORMATTER).withParameters(JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_UTIL_SCANNER).withParameters(JAVA_IO_FILE),
      constructor(JAVA_UTIL_SCANNER).withParameters(JAVA_NIO_FILE_PATH),
      constructor(JAVA_UTIL_SCANNER).withParameters(JAVA_IO_INPUTSTREAM)
    ));
    matchers.addAll(COMMONS_IO);
    matchers.addAll(COMMONS_IO_WITH_CHARSET);
    matchers.addAll(FILEUTILS_WRITE_WITH_CHARSET);
    return MethodMatchers.or(matchers);
  }

  private static MethodMatcher method(String type, String methodName) {
    return MethodMatcher.create().ofType(type).name(methodName);
  }

  private static MethodMatcher constructor(String type) {
    return method(type, "<init>");
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (COMMONS_IO_CHARSET_MATCHERS.matches(mit)) {
      Arguments arguments = mit.arguments();
      ExpressionTree lastArgument = arguments.get(arguments.size() - 1);
      testNullLiteralPassedForEncoding(lastArgument);
    } else if (FILEUTILS_WRITE_WITH_CHARSET_MATCHERS.matches(mit)) {
      testNullLiteralPassedForEncoding(mit.arguments().get(2));
    } else {
      reportIssue(ExpressionUtils.methodName(mit), "Remove this use of \"" + mit.symbol().name() + "\"");
    }
  }

  private void testNullLiteralPassedForEncoding(ExpressionTree argument) {
    if (isNullLiteral(argument)) {
      reportIssue(argument, "Replace this \"null\" with actual charset.");
    }
  }

  private static boolean isNullLiteral(ExpressionTree lastArgument) {
    ExpressionTree arg = ExpressionUtils.skipParentheses(lastArgument);
    return arg.is(Tree.Kind.NULL_LITERAL)
      || (arg.is(Tree.Kind.TYPE_CAST) && isNullLiteral(((TypeCastTree) arg).expression()));
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    Symbol symbol = newClassTree.constructorSymbol();
    if (symbol.isMethodSymbol()) {
      Symbol.MethodSymbol constructor = (Symbol.MethodSymbol) symbol;
      String signature = constructor.owner().name() + "(" + constructor.parameterTypes().stream().map(Type::toString).collect(Collectors.joining(",")) + ")";
      reportIssue(newClassTree.identifier(), "Remove this use of constructor \"" + signature + "\"");
    }
  }

}
