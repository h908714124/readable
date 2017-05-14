package net.readable.compiler;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.type.TypeKind.VOID;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.readable.compiler.LessTypes.asTypeElement;
import static net.readable.compiler.LessTypes.getPackage;

final class Arity0 {

  static Map<Signature, ExecutableElement> parameterlessMethods(
      TypeElement sourceTypeElement) {
    PackageElement ourPackage = getPackage(sourceTypeElement);
    Map<Signature, ExecutableElement> methods = new HashMap<>();
    addFromSuperclass(sourceTypeElement, methods, ourPackage);
    return methods;
  }

  private static void addFromSuperclass(
      TypeElement type, Map<Signature,
      ExecutableElement> methods,
      PackageElement ourPackage) {
    addEnclosedMethods(type, methods, ourPackage);
    TypeMirror superclass = type.getSuperclass();
    if (superclass.getKind() != TypeKind.DECLARED) {
      return;
    }
    addFromSuperclass(asTypeElement(superclass), methods, ourPackage);
  }

  private static void addEnclosedMethods(
      TypeElement type,
      Map<Signature, ExecutableElement> methods,
      PackageElement ourPackage) {
    methodsIn(type.getEnclosedElements())
        .stream()
        .filter(method -> !method.getModifiers().contains(ABSTRACT))
        .filter(method -> !method.getModifiers().contains(STATIC))
        .filter(method -> !method.getModifiers().contains(PRIVATE))
        .filter(method -> method.getParameters().isEmpty())
        .filter(method -> method.getReturnType().getKind() != VOID)
        .filter(method -> method.getModifiers().contains(PUBLIC) ||
            getPackage(method).equals(ourPackage))
        .forEach(method ->
            methods.putIfAbsent(Signature.create(method), method));
  }
}
