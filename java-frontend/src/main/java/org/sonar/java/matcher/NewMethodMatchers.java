package org.sonar.java.matcher;

import java.util.List;
import java.util.function.Predicate;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

/**
 * Helper interface to help to identify method with given Type, Name and Parameter lists.
 *
 * The starting point to define a MethodMatcher is {@link #create()}.
 * It is required to provide at least the following:
 *
 * - a type definition
 * - a method name
 * - a list of parameters
 *
 * If any of the three is missing, throw an Exception.
 * For any of the three elements above, you can specify that any name/type is accepted by using the predicate name -> true.
 * It is also possible to define a name/type multiple times, to match one method OR another.
 *
 * Examples:
 *
 * - match method "a" and "b" from any type, and without parameters
 * MethodMatcher.create().ofAnyType().names("a", "b").withParameters();
 * alternatively
 * MethodMatcher.create().ofAnyType().name("a").name("b").withParameters();
 *
 * - match method "a" and "b" from (subtype) of A, and "b" and "c" from B, with any parameters:
 * MethodMatcher.create().ofSubType("A").names("a", "b").startWithParameters().or(
 * MethodMatcher.create().ofSubType("B").names("b", "c").startWithParameters());
 *
 * - match method "f" with any type and with:
 *   MethodMatcher.create().ofAnyType().name("f")
 *  - one parameter of type either int or long
 *    .withParameters("int").withParameters("long");
 *  - one parameter of type int or one parameter of type long with any other number of parameters
 *    .withParameters("int").startWithParameters("long");
 *
 * - match any method with any type, with parameter int, any, int
 *   MethodMatcher.create().anyName().withParameters(t-> t.is("int"), t -> true, t -> t.is("int"));
 *
 * TODO: add more example
 *
 */
public interface NewMethodMatchers {

  //TODO: add popular constant?
  // ex:  private static final String JAVA_LANG_STRING = "java.lang.String";
  //      private static final String JAVA_LANG_OBJECT = "java.lang.Object";

  static NewMethodMatchers create() {
    return new NewMethodMatcherImpl();
  }

  NewMethodMatchers copy();

  // Methods related to name

  /**
   * Set a method name to match.
   * Can be called multiple times to match any of the name.
   */
  NewMethodMatchers name(String methodName);

  /**
   * Match a method with any name is the list.
   */
  NewMethodMatchers names(String... names);

  /**
   * Match a method with any name.
   * Equivalent to .name(n -> true).
   */
  NewMethodMatchers anyName();

  /**
   * Match a name starting with a prefix.
   * Equivalent to .name(n -> n.startWith("something"))
   */
  NewMethodMatchers startWithName(String name);

  /**
   * Match a constructor.
   * Equivalent to .name(n -> "<init>".equals(n))
   */
  NewMethodMatchers constructor();

  /**
   * Match the name matching the predicate.
   * Can be called multiple times to match a method satisfying a predicate or another.
   */
  NewMethodMatchers name(Predicate<String> methodName);

  // Methods related to types

  /**
   * Match the type and sub-type of the fully qualified name.
   */
  NewMethodMatchers ofSubType(String fullyQualifiedTypeName);

  /**
   * Match any of the type and sub-type of the fully qualified name.
   */
  NewMethodMatchers ofSubTypes(String... fullyQualifiedTypeNames);

  /**
   * Match any type.
   */
  NewMethodMatchers ofAnyType();

  /**
   * Match the fully qualified name type, but not the subtype.
   *
   * TODO: Could be removed, if not used a lot, can be build with predicate
   */
  NewMethodMatchers ofType(String fullyQualifiedTypeName);

  /**
   * Match a type matching a predicate.
   */
  NewMethodMatchers ofType(Predicate<Type> typePredicate);


  // Methods related to call site

  /**
   * Type of the call site
   */
  NewMethodMatchers callSite(Predicate<Type> callSitePredicate);

  // Methods related to parameters
  //TODO: parameters match type of subtype??
  /**
   * Exact list of parameters.
   * Pass empty list for method without parameters.
   * Can be called multiple time to match any of the parameters lists.
   */
  NewMethodMatchers withParameters(String... parametersType);
  NewMethodMatchers withParameters(List<Predicate<Type>> parametersType);

  /**
   * Start of list of parameters, with any other (0 or more) parameter of any type.
   * Can be called multiple time to match any of the parameters lists.
   */
  NewMethodMatchers startWithParameters(String... parametersType);
  NewMethodMatchers startWithParameters(List<Predicate<Type>> parametersType);

  // Methods related to combination

  /**
   * Combine multiple method matcher. The matcher will match any of the given matcher.
   *
   * TODO: should be static?
   *
   */
  NewMethodMatchers or(NewMethodMatchers... methodMatchers);


  // Methods related to usage
  boolean matches(NewClassTree newClassTree);
  boolean matches(MethodInvocationTree mit);
  boolean matches(MethodTree methodTree);
  boolean matches(MethodReferenceTree methodReferenceTree);
  boolean matches(Symbol symbol);

}
