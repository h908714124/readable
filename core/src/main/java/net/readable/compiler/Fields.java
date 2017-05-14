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
      TypeElement sourceTypeElement) {
    PackageElement ourPackage = getPackage(sourceTypeElement);
    Map<Signature, VariableElement> methods = new HashMap<>();
    addFromSuperclass(sourceTypeElement, methods, ourPackage);
    return methods;
  }

  private static void addFromSuperclass(
      TypeElement type,
      Map<Signature, VariableElement> methods,
      PackageElement ourPackage) {
    addEnclosedFields(type, methods, ourPackage);
    TypeMirror superclass = type.getSuperclass();
    if (superclass.getKind() != TypeKind.DECLARED) {
      return;
    }
    addFromSuperclass(asTypeElement(superclass), methods, ourPackage);
  }

  private static void addEnclosedFields(
      TypeElement type,
      Map<Signature, VariableElement> fields,
      PackageElement ourPackage) {
    fieldsIn(type.getEnclosedElements())
        .stream()
        .filter(field -> !field.getModifiers().contains(STATIC))
        .filter(field -> !field.getModifiers().contains(PRIVATE))
        .filter(field -> field.getModifiers().contains(PUBLIC) ||
            getPackage(field).equals(ourPackage))
        .forEach(field ->
            fields.putIfAbsent(Signature.create(field), field));
  }
}
