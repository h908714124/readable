package net.readable.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.readable.compiler.ReadableProcessor.rawType;

final class Model {

  private static final String SUFFIX = "_Builder";
  private static final Modifier[] PUBLIC_MODIFIER = {PUBLIC};
  private static final Modifier[] NO_MODIFIERS = new Modifier[0];

  final TypeName generatedClass;
  final TypeElement sourceClassElement;
  final List<Property> properties;
  final ClassName simpleBuilderClass;
  final ClassName refTrackingBuilderClass;

  private Model(TypeName generatedClass,
                TypeElement sourceClassElement,
                ClassName simpleBuilderClass,
                ClassName refTrackingBuilderClass) {
    this.generatedClass = generatedClass;
    this.sourceClassElement = sourceClassElement;
    this.properties = MethodScanner.scan(sourceClassElement);
    this.simpleBuilderClass = simpleBuilderClass;
    this.refTrackingBuilderClass = refTrackingBuilderClass;
  }

  static Model create(TypeElement sourceClassElement) {
    ClassName generatedClass = peer(TypeName.get(sourceClassElement.asType()));
    ClassName simpleBuilderClass = generatedClass.nestedClass("SimpleBuilder");
    ClassName refTrackingBuilderClass =
        generatedClass.nestedClass("RefTrackingBuilder");

    return new Model(generatedClass,
        sourceClassElement, simpleBuilderClass, refTrackingBuilderClass);
  }

  private static ClassName peer(TypeName type) {
    String name = String.join("_", rawType(type).simpleNames()) + SUFFIX;
    return rawType(type).topLevelClassName().peerClass(name);
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

  ClassName sourceClass() {
    return rawType(TypeName.get(sourceClassElement.asType()));
  }
}
