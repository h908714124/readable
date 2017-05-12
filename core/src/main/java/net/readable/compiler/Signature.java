package net.readable.compiler;

import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Character.toUpperCase;

final class Signature {

  final String propertyName;
  final TypeName propertyType;

  private Signature(String propertyName, TypeName propertyType) {
    this.propertyName = propertyName;
    this.propertyType = propertyType;
  }

  static Signature create(VariableElement parameter) {
    String name = parameter.getSimpleName().toString();
    TypeName type = TypeName.get(parameter.asType());
    return new Signature(name, type);
  }

  static Signature create(ExecutableElement method) {
    if (!method.getParameters().isEmpty() ||
        method.getReturnType().getKind() == TypeKind.VOID) {
      throw new AssertionError();
    }
    TypeName type = TypeName.get(method.getReturnType());
    String name = method.getSimpleName().toString();
    return new Signature(name, type);
  }

  Signature getterStyle() {
    String name = "get" +
        toUpperCase(propertyName.charAt(0)) +
        propertyName.substring(1);
    return new Signature(name, propertyType);
  }

  Optional<Signature> isserStyle() {
    if (propertyType != TypeName.BOOLEAN) {
      return Optional.empty();
    }
    String name = "is" +
        toUpperCase(propertyName.charAt(0)) +
        propertyName.substring(1);
    return Optional.of(new Signature(name, propertyType));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Signature signature = (Signature) o;
    return Objects.equals(propertyName, signature.propertyName) &&
        Objects.equals(propertyType, signature.propertyType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(propertyName, propertyType);
  }

  @Override
  public String toString() {
    return propertyName + " : " + propertyType;
  }
}
