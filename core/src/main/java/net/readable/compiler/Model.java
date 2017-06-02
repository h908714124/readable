package net.readable.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.readable.compiler.ReadableProcessor.rawType;

final class Model {

  private static final String SUFFIX = "_Builder";
  private static final Modifier[] PUBLIC_MODIFIER = {PUBLIC};
  private static final Modifier[] NO_MODIFIERS = new Modifier[0];

  final TypeName generatedClass;
  final TypeElement sourceClassElement;
  final TypeName sourceClass;
  final TypeName simpleBuilderClass;
  private final ClassName refTrackingBuilderClass;
  final Util util;

  private Model(
      TypeName generatedClass,
      TypeElement sourceClassElement,
      TypeName simpleBuilderClass,
      ClassName refTrackingBuilderClass,
      Util util) {
    this.generatedClass = generatedClass;
    this.sourceClassElement = sourceClassElement;
    this.simpleBuilderClass = simpleBuilderClass;
    this.refTrackingBuilderClass = refTrackingBuilderClass;
    this.sourceClass = TypeName.get(sourceClassElement.asType());
    this.util = util;
  }

  static Model create(
      TypeElement sourceClassElement,
      Util util) {
    TypeName sourceClass = TypeName.get(sourceClassElement.asType());
    TypeName generatedClass = peer(sourceClass);
    TypeName simpleBuilderClass = nestedClass(generatedClass, "SimpleBuilder");
    ClassName optionalRefTrackingBuilderClass =
        typeArguments(generatedClass).isEmpty() ?
            rawType(generatedClass).nestedClass("RefTrackingBuilder") :
            null;

    return new Model(generatedClass,
        sourceClassElement, simpleBuilderClass,
        optionalRefTrackingBuilderClass, util);
  }

  private static TypeName peer(TypeName type) {
    String name = String.join("_", rawType(type).simpleNames()) + SUFFIX;
    ClassName className = rawType(type).topLevelClassName().peerClass(name);
    return withTypevars(className, typeArguments(type));
  }

  private static TypeName withTypevars(ClassName className, List<TypeName> typevars) {
    if (typevars.isEmpty()) {
      return className;
    }
    return ParameterizedTypeName.get(className, typevars.toArray(
        new TypeName[typevars.size()]));
  }

  private static TypeName nestedClass(TypeName generatedClass, String name) {
    return withTypevars(rawType(generatedClass).nestedClass(name),
        typeArguments(generatedClass));
  }

  private boolean isPublic() {
    return sourceClassElement.getModifiers().contains(PUBLIC);
  }

  Modifier[] maybePublic() {
    if (isPublic()) {
      return PUBLIC_MODIFIER;
    }
    return NO_MODIFIERS;
  }

  List<TypeVariableName> typevars() {
    return sourceClassElement.getTypeParameters().stream()
        .map(TypeVariableName::get)
        .collect(toList());
  }

  private static List<TypeName> typeArguments(TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).typeArguments;
    }
    return Collections.emptyList();
  }

  Optional<ClassName> optionalRefTrackingBuilderClass() {
    return Optional.ofNullable(refTrackingBuilderClass);
  }

  String cacheWarning() {
    return "Caching not implemented: " +
        rawType(sourceClass).simpleName() +
        "<" +
        typevars().stream()
            .map(TypeVariableName::toString)
            .collect(joining(", ")) +
        "> has type parameters";
  }
}
