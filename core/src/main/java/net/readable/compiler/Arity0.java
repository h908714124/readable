package net.readable.compiler;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;

import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.type.TypeKind.VOID;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static net.readable.compiler.LessTypes.asTypeElement;
import static net.readable.compiler.LessTypes.getPackage;

final class Arity0 {

  static Map<Signature, ExecutableElement> parameterlessMethods(
      TypeElement type) {
    PackageElement packageElement = getPackage(type);
    Map<Signature, ExecutableElement> methods = new HashMap<>();
    addFromSuperclass(type, methods, packageElement);
    return methods;
  }

  private static void addFromSuperclass(
      TypeElement type, Map<Signature,
      ExecutableElement> methods,
      PackageElement packageElement) {
    addEnclosedMethods(type, methods, packageElement);
    TypeMirror superclass = type.getSuperclass();
    if (superclass.getKind() != TypeKind.DECLARED) {
      return;
    }
    addFromSuperclass(asTypeElement(superclass), methods,
        packageElement);
  }

  private static void addEnclosedMethods(
      TypeElement type,
      Map<Signature, ExecutableElement> methods,
      PackageElement packageElement) {
    methodsIn(type.getEnclosedElements())
        .stream()
        .filter((ExecutableElement method) -> {
          PackageElement methodPackage = getPackage(method);
          return methodPackage.equals(packageElement) ||
              method.getModifiers().contains(PROTECTED) ||
              method.getModifiers().contains(PUBLIC);
        })
        .filter(method -> method.getParameters().isEmpty())
        .filter(method -> method.getReturnType().getKind() != VOID)
        .forEach(method -> {
          if (method.getKind() == METHOD
              && !method.getModifiers().contains(STATIC)
              && !method.getModifiers().contains(ABSTRACT)
              && !method.getModifiers().contains(PRIVATE)) {
            Signature signature = Signature.create(method);
            methods.computeIfAbsent(signature,
                __ -> method);
          }
        });
  }
}
