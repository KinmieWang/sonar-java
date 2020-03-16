package org.sonar.java.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

public class NewMethodMatcherImpl implements NewMethodMatchers {

  private Predicate<Type> typePredicate;
  private Predicate<String> methodName;
  private List<Predicate<List<Type>>> parameters = new ArrayList<>();

  private List<NewMethodMatchers> otherMethodMatchers = new ArrayList<>();

  @Override
  public NewMethodMatchers create() {
    return new NewMethodMatcherImpl();
  }

  @Override
  public NewMethodMatchers name(String methodName) {
    return name(methodName::equals);
  }

  @Override
  public NewMethodMatchers names(String... names) {
    NewMethodMatchers currentMatcher = this;
    for (String name : names) {
      currentMatcher = currentMatcher.name(name);
    }
    return currentMatcher;
  }

  @Override
  public NewMethodMatchers anyName() {
    return name(n -> true);
  }

  @Override
  public NewMethodMatchers startWithName(String name) {
    return name(n -> n.startsWith(name));
  }

  @Override
  public NewMethodMatchers constructor() {
    return name("<inti>");
  }

  @Override
  public NewMethodMatchers name(Predicate<String> methodName) {
    checkState();
    if (this.methodName == null) {
      this.methodName = methodName;
    } else {
      this.methodName = this.methodName.or(methodName);
    }
    return this;
  }

  @Override
  public NewMethodMatchers ofSubType(String fullyQualifiedTypeName) {
    return ofType(t -> t.isSubtypeOf(fullyQualifiedTypeName));
  }

  @Override
  public NewMethodMatchers ofSubTypes(String... fullyQualifiedTypeNames) {
    NewMethodMatchers currentMatcher = this;
    for (String name : fullyQualifiedTypeNames) {
      currentMatcher = currentMatcher.ofSubType(name);
    }
    return currentMatcher;
  }

  @Override
  public NewMethodMatchers ofAnyType() {
    return ofType(t -> true);
  }

  @Override
  public NewMethodMatchers ofType(String fullyQualifiedTypeName) {
    return ofType(t -> t.is(fullyQualifiedTypeName));
  }

  @Override
  public NewMethodMatchers ofType(Predicate<Type> typePredicate) {
    checkState();
    if (this.typePredicate == null) {
      this.typePredicate = typePredicate;
    } else {
      this.typePredicate = this.typePredicate.or(typePredicate);
    }
    return this;
  }

  @Override
  public NewMethodMatchers withParameters(String... parametersType) {
    checkState();
    List<Predicate<Type>> newParameterList = new ArrayList<>();
    for (String parameterType : parametersType) {
      newParameterList.add(t -> t.isSubtypeOf(parameterType));
    }
    parameters.add(actualTypes -> exactMatchesParameters(newParameterList, actualTypes));
    return this;
  }

  @Override
  public NewMethodMatchers withParameters(Predicate<Type>... parametersType) {
    checkState();
    parameters.add(actualTypes -> exactMatchesParameters(Arrays.asList(parametersType), actualTypes));
    return this;
  }

  @Override
  public NewMethodMatchers startWithParameters(String... parametersType) {
    checkState();
    List<Predicate<Type>> newParameterList = new ArrayList<>();
    for (String parameterType : parametersType) {
      newParameterList.add(t -> t.isSubtypeOf(parameterType));
    }
    parameters.add(actualTypes -> startWithParameters(newParameterList, actualTypes));
    return this;
  }

  @Override
  public NewMethodMatchers startWithParameters(Predicate<Type>... parametersType) {
    checkState();
    parameters.add(actualTypes -> startWithParameters(Arrays.asList(parametersType), actualTypes));
    return this;
  }

  @Override
  public NewMethodMatchers or(NewMethodMatchers... methodMatchers) {
    otherMethodMatchers.addAll(Arrays.asList(methodMatchers));
    return this;
  }

  @Override
  public boolean matches(NewClassTree newClassTree) {
    return matches(newClassTree.constructorSymbol());
  }

  @Override
  public boolean matches(MethodInvocationTree mit) {
    IdentifierTree id = getIdentifier(mit);
    return matches(id.symbol());
  }

  @Override
  public boolean matches(MethodTree methodTree) {
    return matches(methodTree.symbol());
  }

  @Override
  public boolean matches(MethodReferenceTree methodReferenceTree) {
    return matches(methodReferenceTree.method().symbol());
  }

  @Override
  public boolean matches(Symbol symbol) {
    if (typePredicate == null || methodName == null || parameters.isEmpty()) {
      throw new IllegalStateException("A method matcher should set at least one type, name, and parameter list.");
    }

    if (symbol.isMethodSymbol()) {
      MethodSymbol methodSymbol = (MethodSymbol) symbol;
      if (methodName.test(methodSymbol.name())
        && typePredicate.test(methodSymbol.owner().type())
        && parameters.stream().anyMatch(p -> p.test(methodSymbol.parameterTypes()))) {
        return true;
      }
    }
    return otherMethodMatchers.stream().anyMatch(m -> m.matches(symbol));
  }

  private void checkState() {
    if (!otherMethodMatchers.isEmpty()) {
      throw new IllegalStateException("Can not configure a method matcher containing multiple matcher.");
    }
  }

  private static boolean exactMatchesParameters(List<Predicate<Type>> expectedTypes, List<Type> actualTypes) {
    if (actualTypes.size() != expectedTypes.size()) {
      return false;
    }
    return matchesParameters(expectedTypes, actualTypes);
  }

  private static boolean startWithParameters(List<Predicate<Type>> expectedTypes, List<Type> actualTypes) {
    if (actualTypes.size() >= expectedTypes.size()) {
      return false;
    }
    return matchesParameters(expectedTypes, actualTypes);
  }

  private static boolean matchesParameters(List<Predicate<Type>> expectedTypes, List<Type> actualTypes) {
    for (int i = 0; i < actualTypes.size(); i++) {
      if (!expectedTypes.get(i).test(actualTypes.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static IdentifierTree getIdentifier(MethodInvocationTree mit) {
    // methodSelect can only be Tree.Kind.IDENTIFIER or Tree.Kind.MEMBER_SELECT
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) mit.methodSelect();
    }
    return ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
  }
}
