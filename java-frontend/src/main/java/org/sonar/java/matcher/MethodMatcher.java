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
package org.sonar.java.matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

public class MethodMatcher {

  private NewMethodMatchers internalMatcher = NewMethodMatchers.create();
  private List<Predicate<Type>> parameterTypes = new ArrayList<>();
  private boolean shouldSetParameters = false;
  private boolean typeSet = false;
  private boolean fieldSet = false;

  public static MethodMatcher create() {
    return new MethodMatcher();
  }

  public MethodMatcher copy() {
    MethodMatcher copy = new MethodMatcher();
    copy.internalMatcher = internalMatcher.copy();
    return copy;
  }

  public MethodMatcher name(String methodName) {
    internalMatcher.name(methodName);
    return this;
  }

  public MethodMatcher name(NameCriteria methodName) {
    internalMatcher.name(methodName);
    return this;
  }

  public MethodMatcher typeDefinition(TypeCriteria typeDefinition) {
    internalMatcher.ofType(typeDefinition);
    typeSet = true;
    return this;
  }

  public MethodMatcher typeDefinition(String fullyQualifiedTypeName) {
    internalMatcher.ofType(fullyQualifiedTypeName);
    typeSet = true;
    return this;
  }

  public MethodMatcher callSite(TypeCriteria callSite) {
    internalMatcher.callSite(callSite);
    typeSet = true;
    return this;
  }

  public MethodMatcher addParameter(String fullyQualifiedTypeParameterName) {
    return addParameter(TypeCriteria.is(fullyQualifiedTypeParameterName));
  }

  public MethodMatcher addParameter(TypeCriteria parameterTypeCriteria) {
    parameterTypes.add(parameterTypeCriteria);
    shouldSetParameters = true;
    return this;
  }

  public MethodMatcher parameters(String... parameterTypes) {
    if (parameterTypes.length == 0) {
      return withoutParameter();
    }
    for (String type : parameterTypes) {
      addParameter(type);
    }
    return this;
  }

  public MethodMatcher parameters(TypeCriteria... parameterTypes) {
    if (parameterTypes.length == 0) {
      return withoutParameter();
    }
    for (TypeCriteria type : parameterTypes) {
      addParameter(type);
    }
    return this;
  }

  public MethodMatcher withAnyParameters() {
    internalMatcher.startWithParameters();
    return this;
  }

  public MethodMatcher withoutParameter() {
    internalMatcher.withParameters();
    return this;
  }

  public boolean matches(NewClassTree newClassTree) {
    setInternalMatcherFields();
    return internalMatcher.matches(newClassTree);
  }

  public boolean matches(MethodInvocationTree mit) {
    setInternalMatcherFields();
    return internalMatcher.matches(mit);
  }

  public boolean matches(MethodTree methodTree) {
    setInternalMatcherFields();
    return internalMatcher.matches(methodTree);
  }

  public boolean matches(MethodReferenceTree methodReferenceTree) {
    setInternalMatcherFields();
    return internalMatcher.matches(methodReferenceTree);
  }

  public boolean matches(Symbol symbol) {
    setInternalMatcherFields();
    return internalMatcher.matches(symbol);
  }

  private void setInternalMatcherFields() {
    if (!fieldSet) {
      if (!typeSet) {
        internalMatcher.ofAnyType();
      }
      if (shouldSetParameters) {
        internalMatcher.withParameters(parameterTypes);
      }
      fieldSet = true;
    }
  }

}
