package net.readable.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Collections;
import net.readable.compiler.Collectionish.CollectionType;

import static net.readable.compiler.Collectionish.CollectionType.LIST;
import static net.readable.compiler.Model.withTypevars;
import static net.readable.compiler.ParaParameter.AS_SETTER_PARAMETER;
import static net.readable.compiler.Util.typeArgumentSubtypes;
import static net.readable.compiler.Util.typeArguments;

final class UtilCollection extends Collectionish.Base {

  private final String emptyMethod;
  private final ClassName accumulatorClass;

  private UtilCollection(
      ClassName accumulatorClass,
      String emptyMethod,
      String className,
      CollectionType type,
      String accumulatorAddAllType) {
    super(className, accumulatorAddAllType, type);
    this.accumulatorClass = accumulatorClass;
    this.emptyMethod = emptyMethod;
  }

  static Collectionish.Base ofUtil(
      String simpleName,
      String emptyMethod,
      Class<?> builderClass,
      CollectionType collectionType) {
    String accumulatorAddAllType = collectionType == LIST ?
        "java.util.Collection" :
        "java.util.Map";
    return new UtilCollection(
        ClassName.get(builderClass),
        emptyMethod,
        "java.util." + simpleName,
        collectionType,
        accumulatorAddAllType);
  }

  @Override
  CodeBlock accumulatorInitBlock(FieldSpec builderField) {
    return CodeBlock.builder().addStatement("this.$N = new $T<>()",
        builderField, accumulatorClass).build();
  }

  @Override
  CodeBlock emptyBlock() {
    return CodeBlock.of("$T.$L()", Collections.class, emptyMethod);
  }

  @Override
  ParameterizedTypeName accumulatorType(Property property) {
    return ParameterizedTypeName.get(accumulatorClass,
        typeArguments(property.asType()));
  }

  @Override
  ParameterizedTypeName accumulatorOverloadArgumentType(Property property) {
    return ParameterizedTypeName.get(overloadArgumentType(),
        typeArgumentSubtypes(property.asType()));
  }

  @Override
  CodeBlock setterAssignment(Property property) {
    FieldSpec field = property.asField();
    ParameterSpec p = AS_SETTER_PARAMETER.apply(property);
    return CodeBlock.builder()
        .addStatement("this.$N = $N", field, p)
        .build();
  }

  @Override
  CodeBlock buildBlock(ParameterSpec builder, FieldSpec field) {
    return CodeBlock.of("$N.$N", builder, field);
  }

  @Override
  ParameterSpec setterParameter(Property parameter) {
    TypeName type = withTypevars(
        collectionClassName(),
        typeArguments(parameter.asType()));
    return ParameterSpec.builder(type, parameter.propertyName()).build();
  }
}
