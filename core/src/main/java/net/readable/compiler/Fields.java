package net.readable.compiler;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static net.readable.compiler.LessTypes.asTypeElement;
import static net.readable.compiler.LessTypes.getPackage;

final class Fields {

  static Map<Signature, VariableElement> fields(
      TypeElement type) {
    PackageElement packageElement = getPackage(type);
    Map<Signature, VariableElement> methods = new HashMap<>();
    addFromSuperclass(type, methods, packageElement);
    return methods;
  }

  private static void addFromSuperclass(
      TypeElement type,
      Map<Signature, VariableElement> methods,
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
      Map<Signature, VariableElement> fields,
      PackageElement packageElement) {
    fieldsIn(type.getEnclosedElements())
        .stream()
        .filter(field -> {
          PackageElement fieldPackage = getPackage(field);
          return fieldPackage.equals(packageElement) ||
              field.getModifiers().contains(PUBLIC);
        })
        .forEach(field -> {
          if (!field.getModifiers().contains(STATIC)
              && !field.getModifiers().contains(PRIVATE)) {
            Signature signature = Signature.create(field);
            fields.computeIfAbsent(signature,
                __ -> field);
          }
        });
  }
}
