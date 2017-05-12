package net.readable.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.readable.compiler.ReadableProcessor.rawType;

final class Model {

  private static final String SUFFIX = "_Builder";
  private static final Modifier[] PUBLIC_MODIFIER = {PUBLIC};
  private static final Modifier[] NO_MODIFIERS = new Modifier[0];

  final TypeName generatedClass;
  final TypeElement sourceClassElement;
  final List<Property> accessorPairs;
  final ClassName simpleBuilderClass;
  final ClassName refTrackingBuilderClass;

  private Model(TypeName generatedClass,
                TypeElement sourceClassElement,
                ClassName simpleBuilderClass,
                ClassName refTrackingBuilderClass) {
    this.generatedClass = generatedClass;
    this.sourceClassElement = sourceClassElement;
    this.accessorPairs = MethodScanner.scan(sourceClassElement);
    this.simpleBuilderClass = simpleBuilderClass;
    this.refTrackingBuilderClass = refTrackingBuilderClass;
  }

  static Model create(TypeElement sourceClassElement) { List<ExecutableElement> constructors = ElementFilter.constructorsIn(
        sourceClassElement.getEnclosedElements());
    if (constructors.stream()
        .noneMatch(c -> !c.getModifiers().contains(PRIVATE) &&
            c.getParameters().isEmpty())) {
      throw new ValidationException(
          "Default constructor not found", sourceClassElement);
    }
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

  ClassName sourceClass() {
    return rawType(TypeName.get(sourceClassElement.asType()));
  }
}
