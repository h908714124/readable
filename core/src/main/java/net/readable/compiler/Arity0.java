package net.readable.compiler;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.type.TypeKind.VOID;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.readable.compiler.LessTypes.asTypeElement;

final class Arity0 {

  static List<ExecutableElement> parameterlessMethods(
      TypeElement type) {
    Map<String, ExecutableElement> methods = new LinkedHashMap<>();
    addFromSuperclass(type, methods);
    return new ArrayList<>(methods.values());
  }

  private static void addFromSuperclass(
      TypeElement type, Map<String, ExecutableElement> methods) {
    addEnclosedMethods(type, methods);
    TypeMirror superclass = type.getSuperclass();
    if (superclass.getKind() != TypeKind.DECLARED) {
      return;
    }
    addFromSuperclass(asTypeElement(superclass), methods);
  }

  private static void addEnclosedMethods(TypeElement type, Map<String, ExecutableElement> methods) {
    methodsIn(type.getEnclosedElements())
        .stream()
        .filter(method -> method.getParameters().isEmpty())
        .filter(method -> method.getReturnType().getKind() != VOID)
        .forEach(method -> {
          if (method.getKind() == METHOD
              && !method.getModifiers().contains(STATIC)
              && !method.getModifiers().contains(ABSTRACT)
              && !method.getModifiers().contains(PRIVATE)) {
            methods.computeIfAbsent(method.getSimpleName().toString(),
                __ -> method);
          }
        });
  }
}
