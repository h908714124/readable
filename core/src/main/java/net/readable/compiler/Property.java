package net.readable.compiler;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.type.TypeKind.VOID;

final class Property extends ParaParameter {

  private final ExecutableElement executableElement;
  private final VariableElement field;

  private final Signature signature;
  final Model model;

  private Property(
      ExecutableElement executableElement,
      VariableElement field,
      Signature signature,
      Model model) {
    this.signature = signature;
    this.executableElement = executableElement;
    this.field = field;
    this.model = model;
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

  static ParaParameter create(
      Signature signature,
      ExecutableElement method,
      Model model) {
    if (!checkExecutableElement(method)) {
      throw new AssertionError();
    }
    return new Property(method, null, signature, model).enrich();
  }

  static ParaParameter create(
      Signature signature,
      VariableElement field,
      Model model) {
    if (!checkField(field)) {
      throw new AssertionError();
    }
    return new Property(null, field, signature, model).enrich();
  }

  private ParaParameter enrich() {
    return Optionalish.create(this).orElse(this);
  }

  TypeName type() {
    return signature.propertyType;
  }

  String access() {
    if (executableElement != null) {
      return executableElement.getSimpleName() + "()";
    }
    return field.getSimpleName().toString();
  }

  String propertyName() {
    return signature.propertyName;
  }

  FieldSpec.Builder asField() {
    return FieldSpec.builder(type(),
        propertyName())
        .addModifiers(PRIVATE);
  }

  TypeMirror asType() {
    return asType(executableElement, field);
  }

  private static TypeMirror asType(
      ExecutableElement executableElement,
      VariableElement field) {
    return field != null ?
        field.asType() :
        executableElement.getReturnType();
  }

  @Override
  <R, P> R accept(Cases<R, P> cases, P p) {
    return cases.property(this, p);
  }
}
