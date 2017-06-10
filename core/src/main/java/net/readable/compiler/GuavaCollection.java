package net.readable.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Map;

import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static net.readable.compiler.ParaParameter.AS_SETTER_PARAMETER;
import static net.readable.compiler.Util.typeArgumentSubtypes;

final class GuavaCollection extends Collectionish.Base {

  private static final ClassName MAP_ENTRY = ClassName.get(Map.Entry.class);
  private static final String GCC = "com.google.common.collect.";

  private final ClassName setterParameterClassName;

  private GuavaCollection(
      String className,
      Collectionish.CollectionType type,
      ClassName setterParameterClassName) {
    super(className, "java.lang.Iterable", type);
    this.setterParameterClassName = setterParameterClassName;
  }

  static Collectionish.Base ofGuava(
      String simpleName,
      Class<?> setterParameterClass,
      Collectionish.CollectionType type) {
    return new GuavaCollection(GCC + simpleName, type,
        ClassName.get(setterParameterClass));
  }

  @Override
  CodeBlock accumulatorInitBlock(FieldSpec builderField) {
    return CodeBlock.builder().addStatement("this.$N = $T.builder()",
        builderField, collectionClassName()).build();
  }

  @Override
  CodeBlock emptyBlock() {
    return CodeBlock.of("$T.of()", collectionClassName());
  }

  @Override
  ParameterizedTypeName accumulatorType(Property property) {
    ParameterizedTypeName typeName =
        (ParameterizedTypeName) TypeName.get(property.asType());
    return ParameterizedTypeName.get(collectionClassName().nestedClass("Builder"),
        typeName.typeArguments.toArray(new TypeName[typeName.typeArguments.size()]));
  }

  @Override
  ParameterizedTypeName accumulatorOverloadArgumentType(Property property) {
    TypeName[] typeArguments = typeArgumentSubtypes(
        property.asType());
    return collectionType == Collectionish.CollectionType.LIST ?
        ParameterizedTypeName.get(overloadArgumentType(), typeArguments) :
        ParameterizedTypeName.get(overloadArgumentType(),
            subtypeOf(ParameterizedTypeName.get(MAP_ENTRY, typeArguments)));
  }

  @Override
  CodeBlock setterAssignment(Property property) {
    FieldSpec field = property.asField();
    ParameterSpec p = AS_SETTER_PARAMETER.apply(property);
    return CodeBlock.builder()
        .addStatement("this.$N = $N != null ? $T.copyOf($N) : null",
            field, p, collectionClassName(), p)
        .build();
  }

  @Override
  CodeBlock buildBlock(ParameterSpec builder, FieldSpec field) {
    return CodeBlock.of("$N.$N.build()", builder, field);
  }

  @Override
  ParameterSpec setterParameter(Property property) {
    TypeName type =
        ParameterizedTypeName.get(setterParameterClassName,
            typeArgumentSubtypes(
                property.asType()));
    return ParameterSpec.builder(type, property.propertyName()).build();
  }
}
