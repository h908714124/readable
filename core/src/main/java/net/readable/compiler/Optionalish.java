package net.readable.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import static javax.lang.model.element.Modifier.FINAL;
import static net.readable.compiler.Util.AS_DECLARED;
import static net.readable.compiler.Util.AS_TYPE_ELEMENT;
import static net.readable.compiler.Util.equalsType;

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

  private static final String OF = "of";
  private static final String OF_NULLABLE = "ofNullable";

  final ClassName wrapper;
  final TypeName wrapped;
  final Property property;

  private final String of;

  private Optionalish(
      ClassName wrapper,
      TypeName wrapped,
      Property property,
      String of) {
    this.wrapper = wrapper;
    this.wrapped = wrapped;
    this.property = property;
    this.of = of;
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

  static Optional<CodeBlock> emptyBlock(Property property) {
    FieldSpec field = property.asField();
    ParameterSpec builder = property.model.builderParameter();
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
              new Optionalish(ClassName.get(typeElement), primitive, property, OF))) :
          Optional.empty();
    }
    return equalsType(declaredType, JAVA_UTIL_OPTIONAL) ?
        Optional.of(new CheckoutResult(declaredType,
            new Optionalish(OPTIONAL_CLASS,
                TypeName.get(declaredType.getTypeArguments().get(0)),
                property, OF_NULLABLE))) :
        Optional.empty();
  }


  boolean isOptional() {
    return wrapper.equals(OPTIONAL_CLASS);
  }

  CodeBlock getFieldValue() {
    FieldSpec field = property.asField();
    ParameterSpec builder = property.model.builderParameter();
    return CodeBlock.of("$N.$N != null ? $N.$N : $T.empty()",
        builder, field, builder, field, wrapper);
  }

  MethodSpec convenienceOverloadMethod() {
    FieldSpec f = property.asField();
    ParameterSpec p = ParameterSpec.builder(wrapped,
        property.propertyName()).build();
    CodeBlock.Builder block = CodeBlock.builder();
    if (wrapper.equals(OPTIONAL_CLASS)) {
      block.addStatement("this.$N = $T.$L($N)", f, wrapper, of, p);
    } else {
      block.addStatement("this.$N = $T.of($N)", f, wrapper, p);
    }
    return MethodSpec.methodBuilder(
        property.propertyName())
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(p)
        .addModifiers(FINAL)
        .addModifiers(property.model.maybePublic())
        .returns(property.model.generatedClass)
        .build();
  }


  @Override
  <R, P> R accept(Cases<R, P> cases, P p) {
    return cases.optionalish(this, p);
  }
}
