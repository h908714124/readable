package net.readable.compiler;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.Optional;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.type.TypeKind.VOID;

final class Property {

  private final ExecutableElement executableElement;
  private final VariableElement field;

  private final Signature signature;
  private final OptionalInfo optionalInfo;

  private Property(ExecutableElement executableElement,
                   VariableElement field,
                   Signature signature) {
    this.signature = signature;
    this.executableElement = executableElement;
    this.field = field;
    this.optionalInfo = OptionalInfo.create(signature.propertyType);
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

  static Property create(
      Signature signature,
      ExecutableElement executableElement) {
    if (!checkExecutableElement(executableElement)) {
      throw new AssertionError();
    }
    return new Property(executableElement, null, signature);
  }

  static Property create(
      Signature signature,
      VariableElement field) {
    if (!checkField(field)) {
      throw new AssertionError();
    }
    return new Property(null, field, signature);
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

  FieldSpec asInitializedField() {
    FieldSpec.Builder fieldBuilder = asField();
    if (optionalInfo != null) {
      fieldBuilder.initializer("$T.empty()", optionalInfo.wrapper);
    }
    return fieldBuilder.build();
  }

  Optional<OptionalInfo> optionalInfo() {
    return Optional.ofNullable(optionalInfo);
  }
}
