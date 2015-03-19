/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.java.api.tree;

import com.google.common.annotations.Beta;
import org.sonar.plugins.java.api.semantic.Symbol;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Method invocation expression.
 *
 * JLS 15.12
 *
 * <pre>
 *   {@link #methodSelect()} ( {@link #arguments()} )
 *   this . {@link #typeArguments} {@link #methodSelect()} ( {@link #arguments} )
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface MethodInvocationTree extends ExpressionTree {

  /**
   * @since Java 1.5
   */
  @Nullable
  TypeArguments typeArguments();

  ExpressionTree methodSelect();

  SyntaxToken openParenToken();

  List<ExpressionTree> arguments();

  SyntaxToken closeParenToken();

  Symbol symbol();

}
