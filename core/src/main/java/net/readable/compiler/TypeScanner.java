package net.readable.compiler;

import static net.readable.compiler.Arity0.parameterlessMethods;
import static net.readable.compiler.LessTypes.asTypeElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

final class TypeScanner {

  static List<Property> scan(TypeElement sourceClassElement) {
    if (sourceClassElement.getModifiers().contains(Modifier.PRIVATE)) {
      throw new ValidationException("The class may not be private", sourceClassElement);
    }
    if (sourceClassElement.getModifiers().contains(Modifier.ABSTRACT)) {
      throw new ValidationException("The class may not be abstract", sourceClassElement);
    }
    if (sourceClassElement.getEnclosingElement() != null &&
        sourceClassElement.getEnclosingElement().getKind() == ElementKind.CLASS &&
        !sourceClassElement.getModifiers().contains(Modifier.STATIC)) {
      throw new ValidationException("The inner class must be static", sourceClassElement);
    }
    ExecutableElement constructor = constructor(sourceClassElement);
    return getters(sourceClassElement, constructor);
  }

  private static ExecutableElement constructor(TypeElement sourceClassElement) {
    List<ExecutableElement> constructors =
        ElementFilter.constructorsIn(sourceClassElement.getEnclosedElements())
            .stream()
            .filter(constructor -> !constructor.getModifiers().contains(Modifier.PRIVATE))
            .collect(Collectors.toList());
    if (constructors.size() == 1) {
      return constructors.get(0);
    }
    if (constructors.isEmpty()) {
      throw new ValidationException("No non-private constructor found", sourceClassElement);
    }
    constructors = constructors.stream()
        .filter(constructor ->
            constructor.getAnnotationMirrors().stream().anyMatch(mirror ->
                asTypeElement(mirror.getAnnotationType())
                    .getQualifiedName().toString()
                    .endsWith(".Constructor")))
        .collect(Collectors.toList());
    if (constructors.isEmpty()) {
      throw new ValidationException("Use @Readable.Constructor " +
          "to tag a constructor", sourceClassElement);
    }
    if (constructors.size() > 1) {
      throw new ValidationException("Only one @Constructor " +
          "annotation is allowed per class", sourceClassElement);
    }
    return constructors.get(0);
  }

  private static List<Property> getters(TypeElement sourceTypeElement,
                                        ExecutableElement constructor) {
    List<Signature> signatures = constructor.getParameters().stream()
        .map(Signature::create)
        .collect(Collectors.toList());
    List<Property> result = new ArrayList<>(signatures.size());
    Map<Signature, ExecutableElement> methods =
        parameterlessMethods(sourceTypeElement);
    Map<Signature, VariableElement> fields =
        Fields.fields(sourceTypeElement);
    for (Signature signature : signatures) {
      VariableElement field = fields.get(signature);
      if (field != null) {
        result.add(Property.create(signature, field));
        continue;
      }
      ExecutableElement method = methods.get(signature);
      if (method != null) {
        result.add(Property.create(signature, method));
        continue;
      }
      method = methods.get(signature.getterStyle());
      if (method != null) {
        result.add(Property.create(signature, method));
        continue;
      }
      method = signature.isserStyle()
          .map(methods::get)
          .orElseThrow(() ->
              new ValidationException("missing readable property " +
                  signature, constructor));
      result.add(Property.create(signature, method));
    }
    return result;
  }
}
