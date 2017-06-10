package net.readable.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.readable.compiler.ReadableProcessor.rawType;
import static net.readable.compiler.Util.typeArguments;

final class Model {

  private static final String SUFFIX = "_Builder";
  private static final Modifier[] PUBLIC_MODIFIER = {PUBLIC};
  private static final Modifier[] NO_MODIFIERS = new Modifier[0];

  private final ParameterSpec builderParameter;

  final Optional<ClassName> optionalRefTrackingBuilderClass;
  final TypeName generatedClass;
  final TypeElement sourceClassElement;
  final TypeName sourceClass;
  final TypeName simpleBuilderClass;
  final Util util;

  private Model(
      TypeName generatedClass,
      TypeElement sourceClassElement,
      TypeName simpleBuilderClass,
      Optional<ClassName> optionalRefTrackingBuilderClass,
      Util util) {
    this.generatedClass = generatedClass;
    this.sourceClassElement = sourceClassElement;
    this.simpleBuilderClass = simpleBuilderClass;
    this.optionalRefTrackingBuilderClass = optionalRefTrackingBuilderClass;
    this.sourceClass = TypeName.get(sourceClassElement.asType());
    this.util = util;
    this.builderParameter = ParameterSpec.builder(generatedClass, "builder").build();
  }

  static Model create(
      TypeElement sourceClassElement,
      Util util) {
    TypeName generatedClass = generatedClassName(sourceClassElement);
    TypeName simpleBuilderClass = simpleBuilderClass(sourceClassElement, generatedClass);
    Optional<ClassName> optionalRefTrackingBuilderClass =
        sourceClassElement.getTypeParameters().isEmpty() ?
            Optional.of(rawType(generatedClass).nestedClass("RefTrackingBuilder")) :
            Optional.empty();

    return new Model(generatedClass,
        sourceClassElement, simpleBuilderClass,
        optionalRefTrackingBuilderClass, util);
  }

  private static TypeName generatedClassName(TypeElement sourceClassElement) {
    TypeName type = TypeName.get(sourceClassElement.asType());
    String name = String.join("_", rawType(type).simpleNames()) + SUFFIX;
    ClassName className = rawType(type).topLevelClassName().peerClass(name);
    return withTypevars(className, typeArguments(sourceClassElement));
  }

  static TypeName withTypevars(ClassName className, TypeName[] typevars) {
    if (typevars.length == 0) {
      return className;
    }
    return ParameterizedTypeName.get(className, typevars);
  }

  private static TypeName simpleBuilderClass(
      TypeElement sourceClassElement, TypeName generatedClass) {
    return withTypevars(rawType(generatedClass).nestedClass("SimpleBuilder"),
        typeArguments(sourceClassElement));
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

  String cacheWarning() {
    return String.format(
        "Caching not implemented: %s has type parameters: <%s>",
        sourceClassElement.getSimpleName(),
        sourceClassElement.getTypeParameters().stream()
            .map(Element::getSimpleName)
            .collect(joining(", ")));
  }

  ParameterSpec builderParameter() {
    return builderParameter;
  }
}
