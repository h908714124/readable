package net.readable.compiler;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.type.TypeKind.VOID;

final class Property {

  private final ExecutableElement executableElement;
  private final VariableElement field;

  private Property(ExecutableElement executableElement, VariableElement field) {
    if (executableElement != null &&
        !checkExecutableElement(executableElement)) {
      throw new AssertionError();
    }
    if (field != null && !checkField(field)) {
      throw new AssertionError();
    }
    this.executableElement = executableElement;
    this.field = field;
  }

  private static boolean checkExecutableElement(ExecutableElement executableElement) {
    return executableElement.getReturnType().getKind() != VOID &&
        executableElement.getParameters().isEmpty() &&
        !executableElement.getModifiers().contains(PRIVATE) &&
        !executableElement.getModifiers().contains(ABSTRACT);
  }

  private static boolean checkField(VariableElement field) {
    return !field.getModifiers().contains(PRIVATE);
  }

  static Property create(ExecutableElement executableElement) {
    return new Property(executableElement, null);
  }

  static Property create(VariableElement field) {
    return new Property(null, field);
  }

  TypeName type() {
    if (executableElement != null) {
      return TypeName.get(executableElement.getReturnType());
    }
    return TypeName.get(field.asType());
  }

  String access() {
    if (executableElement != null) {
      return executableElement.getSimpleName() + "()";
    }
    return field.getSimpleName().toString();
  }

  FieldSpec.Builder asField() {
    return FieldSpec.builder(type(),
        propertyName())
        .addModifiers(PRIVATE);
  }

  String propertyName() {
    throw new UnsupportedOperationException("not yet");
  }
}
