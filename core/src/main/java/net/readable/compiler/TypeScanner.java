package net.readable.compiler;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.readable.compiler.Arity0.parameterlessMethods;
import static net.readable.compiler.Util.asTypeElement;

final class TypeScanner {

  static List<ParaParameter> scan(
      Model model) {
    if (model.sourceClassElement.getModifiers().contains(Modifier.PRIVATE)) {
      throw new ValidationException("The class may not be private",
          model.sourceClassElement);
    }
    if (model.sourceClassElement.getModifiers().contains(Modifier.ABSTRACT)) {
      throw new ValidationException("The class may not be abstract",
          model.sourceClassElement);
    }
    if (model.sourceClassElement.getEnclosingElement() != null &&
        model.sourceClassElement.getEnclosingElement().getKind() == ElementKind.CLASS &&
        !model.sourceClassElement.getModifiers().contains(Modifier.STATIC)) {
      throw new ValidationException("The inner class must be static",
          model.sourceClassElement);
    }
    ExecutableElement constructor = constructor(model.sourceClassElement);
    return getters(model.sourceClassElement, constructor, model, model.util);
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

  private static List<ParaParameter> getters(
      TypeElement sourceTypeElement,
      ExecutableElement constructor,
      Model model,
      Util util) {
    List<Signature> signatures = constructor.getParameters().stream()
        .map(Signature::create)
        .collect(Collectors.toList());
    List<ParaParameter> result = new ArrayList<>(signatures.size());
    Map<Signature, ExecutableElement> methods =
        parameterlessMethods(sourceTypeElement);
    Map<Signature, VariableElement> fields =
        Fields.fields(sourceTypeElement);
    for (Signature signature : signatures) {
      VariableElement field = fields.get(signature);
      if (field != null) {
        result.add(Property.create(
            signature, field, model));
        continue;
      }
      ExecutableElement method = getMethod(methods, signature);
      if (method == null) {
        throw new ValidationException("missing readable property " +
            signature, constructor);
      }
      result.add(Property.create(
          signature, method, model));
    }
    return result;
  }

  private static ExecutableElement getMethod(
      Map<Signature, ExecutableElement> methods,
      Signature signature) {
    ExecutableElement method = methods.get(signature);
    if (method != null) {
      return method;
    }
    method = methods.get(signature.getterStyle());
    if (method != null) {
      return method;
    }
    return signature.isserStyle()
        .map(methods::get)
        .orElse(null);
  }

}
