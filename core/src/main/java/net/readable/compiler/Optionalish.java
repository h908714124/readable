package net.readable.compiler;

import static net.readable.compiler.ReadableProcessor.rawType;
import static net.readable.compiler.Util.AS_DECLARED;
import static net.readable.compiler.Util.AS_TYPE_ELEMENT;
import static net.readable.compiler.Util.equalsType;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

final class Optionalish extends ParaParameter {

  private static final String JAVA_UTIL_OPTIONAL = "java.util.Optional";
  private static final ClassName OPTIONAL_CLASS =
      ClassName.get(Optional.class);

  private static final Map<String, TypeName> OPTIONAL_PRIMITIVES;

  static {
    OPTIONAL_PRIMITIVES = new HashMap<>(3);
    OPTIONAL_PRIMITIVES.put("java.util.OptionalInt", TypeName.INT);
    OPTIONAL_PRIMITIVES.put("java.util.OptionalDouble", TypeName.DOUBLE);
    OPTIONAL_PRIMITIVES.put("java.util.OptionalLong", TypeName.LONG);
  }

  final ClassName wrapper;
  final TypeName wrapped;
  final Property property;

  private Optionalish(
      ClassName wrapper,
      TypeName wrapped,
      Property property) {
    this.wrapper = wrapper;
    this.wrapped = wrapped;
    this.property = property;
  }

  private static final class CheckoutResult {
    final DeclaredType declaredType;
    final Optionalish optionalish;
    CheckoutResult(DeclaredType declaredType, Optionalish optionalish) {
      this.declaredType = declaredType;
      this.optionalish = optionalish;
    }
  }

  static Optional<ParaParameter> create(Property property) {
    return checkout(property)
        .filter(checkoutResult -> checkoutResult.optionalish.wrapped.isPrimitive() ||
            !equalsType(checkoutResult.declaredType.getTypeArguments().get(0),
                JAVA_UTIL_OPTIONAL))
        .map(checkoutResult -> checkoutResult.optionalish);
  }

  static Optional<CodeBlock> emptyBlock(Property property, ParameterSpec builder) {
    FieldSpec field = property.asField();
    return checkout(property)
        .map(checkoutResult -> checkoutResult.optionalish)
        .map(optionalish -> CodeBlock.of("$N.$N != null ? $N.$N : $T.empty()",
            builder, field, builder, field, optionalish.wrapper));
  }

  private static Optional<CheckoutResult> checkout(
      Property property) {
    DeclaredType declaredType = property.asType().accept(AS_DECLARED, null);
    if (declaredType == null) {
      return Optional.empty();
    }
    TypeElement typeElement =
        declaredType.asElement().accept(AS_TYPE_ELEMENT, null);
    if (typeElement == null) {
      return Optional.empty();
    }
    if (declaredType.getTypeArguments().size() > 1) {
      return Optional.empty();
    }
    if (declaredType.getTypeArguments().isEmpty()) {
      TypeName primitive = OPTIONAL_PRIMITIVES.get(
          typeElement.getQualifiedName().toString());
      return primitive != null ?
          Optional.of(new CheckoutResult(declaredType,
              new Optionalish(ClassName.get(typeElement), primitive, property))) :
          Optional.empty();
    }
    return equalsType(declaredType, JAVA_UTIL_OPTIONAL) ?
        Optional.of(new CheckoutResult(declaredType,
            new Optionalish(OPTIONAL_CLASS,
                TypeName.get(declaredType.getTypeArguments().get(0)), property))) :
        Optional.empty();
  }


  boolean isOptional() {
    return wrapper.equals(OPTIONAL_CLASS);
  }

  private boolean isIrregular() {
    return wrapped instanceof TypeVariableName ||
        isOptional() &&
            rawType(wrapped).equals(OPTIONAL_CLASS);
  }

  boolean isRegular() {
    return !isIrregular();
  }

  CodeBlock getFieldValue(ParameterSpec builder) {
    FieldSpec field = property.asField();
    return CodeBlock.of("$N.$N != null ? $N.$N : $T.empty()",
        builder, field, builder, field, wrapper);
  }

  @Override
  <R, P> R accept(Cases<R, P> cases, P p) {
    return cases.optionalish(this, p);
  }
}
