package net.readable.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.readable.compiler.Collectionish.CollectionType.LIST;
import static net.readable.compiler.Collectionish.CollectionType.MAP;
import static net.readable.compiler.GuavaCollection.ofGuava;
import static net.readable.compiler.Util.AS_DECLARED;
import static net.readable.compiler.Util.AS_TYPE_ELEMENT;
import static net.readable.compiler.Util.className;
import static net.readable.compiler.Util.downcase;
import static net.readable.compiler.Util.equalsType;
import static net.readable.compiler.Util.upcase;
import static net.readable.compiler.UtilCollection.ofUtil;

final class Collectionish extends ParaParameter {

  enum CollectionType {
    LIST(1, "addTo"), MAP(2, "putIn");
    final int typeParams;
    final String accumulatorPrefix;

    CollectionType(int typeParams, String accumulatorPrefix) {
      this.typeParams = typeParams;
      this.accumulatorPrefix = accumulatorPrefix;
    }
  }

  static abstract class Base {

    private final String collectionClassName;
    private final String overloadArgumentType;

    final CollectionType collectionType;

    abstract CodeBlock accumulatorInitBlock(FieldSpec builderField);
    abstract CodeBlock emptyBlock();
    abstract ParameterizedTypeName accumulatorType(Property property);
    abstract ParameterizedTypeName accumulatorOverloadArgumentType(Property property);
    abstract CodeBlock setterAssignment(Property property);
    abstract CodeBlock buildBlock(ParameterSpec builder, FieldSpec field);
    abstract ParameterSpec setterParameter(Property property);

    Base(String collectionClassName,
         String overloadArgumentType,
         CollectionType collectionType) {
      this.collectionClassName = collectionClassName;
      this.overloadArgumentType = overloadArgumentType;
      this.collectionType = collectionType;
    }

    ClassName overloadArgumentType() {
      return className(overloadArgumentType);
    }

    ClassName collectionClassName() {
      return className(collectionClassName);
    }
  }

  private static final class LookupResult {
    final Base base;
    final DeclaredType declaredType;
    LookupResult(Base base, DeclaredType declaredType) {
      this.base = base;
      this.declaredType = declaredType;
    }
  }

  private static final Map<String, Base> LOOKUP = createLookup(
      ofUtil("List", "emptyList", ArrayList.class, LIST),
      ofUtil("Set", "emptySet", HashSet.class, LIST),
      ofUtil("Map", "emptyMap", HashMap.class, MAP),
      ofGuava("ImmutableList", Iterable.class, LIST),
      ofGuava("ImmutableSet", Iterable.class, LIST),
      ofGuava("ImmutableMap", Map.class, MAP));

  private final Base base;

  final Property property;

  private Collectionish(Base base, Property property) {
    this.base = base;
    this.property = property;
  }

  static Optional<ParaParameter> create(Property property) {
    return lookup(property).map(lookupResult ->
        new Collectionish(lookupResult.base, property));
  }

  static Optional<CodeBlock> emptyBlock(Property property) {
    ParameterSpec builder = property.model.builderParameter();
    return lookup(property).map(lookupResult -> {
      FieldSpec field = property.asField();
      return CodeBlock.builder()
          .add("$N.$N != null ? $N.$N : ",
              builder, field, builder, field)
          .add(lookupResult.base.emptyBlock())
          .build();
    });
  }

  private static Optional<LookupResult> lookup(Property property) {
    TypeMirror type = property.asType();
    DeclaredType declaredType = type.accept(AS_DECLARED, null);
    if (declaredType == null) {
      return Optional.empty();
    }
    TypeElement typeElement = declaredType.asElement().accept(AS_TYPE_ELEMENT, null);
    if (typeElement == null) {
      return Optional.empty();
    }
    Base base = LOOKUP.get(typeElement.getQualifiedName().toString());
    if (base == null) {
      return Optional.empty();
    }
    if (base.collectionType.typeParams !=
        declaredType.getTypeArguments().size()) {
      return Optional.empty();
    }
    if (base.collectionType.typeParams == 1 &&
        equalsType(declaredType.getTypeArguments().get(0),
            base.overloadArgumentType)) {
      return Optional.empty();
    }
    return Optional.of(new LookupResult(base, declaredType));
  }

  private static Map<String, Base> createLookup(Base... bases) {
    Map<String, Base> map = new HashMap<>(bases.length);
    for (Base base : bases) {
      map.put(base.collectionClassName, base);
    }
    return map;
  }

  MethodSpec accumulatorMethod() {
    return base.collectionType == MAP ?
        putInMethod() :
        addToMethod();
  }

  MethodSpec accumulatorMethodOverload() {
    ParameterizedTypeName addAllType = base.accumulatorOverloadArgumentType(property);
    return base.collectionType == MAP ?
        putAllInMethod(addAllType) :
        addAllToMethod(addAllType);
  }

  CodeBlock getFieldValue() {
    FieldSpec field = property.asField();
    FieldSpec builderField = asBuilderField();
    ParameterSpec builder = property.model.builderParameter();
    return CodeBlock.builder()
        .add("$N.$N != null ? ", builder, builderField)
        .add(base.buildBlock(builder, builderField))
        .add(" :\n        ")
        .add("$N.$N != null ? $N.$N : ",
            builder, field, builder, field)
        .add(base.emptyBlock())
        .build();
  }

  Collectionish withParameter(Property property) {
    return new Collectionish(base, property);
  }

  CodeBlock setterAssignment() {
    return base.setterAssignment(property);
  }

  String builderFieldName() {
    return downcase(property.propertyName()) + "Builder";
  }

  FieldSpec asBuilderField() {
    return FieldSpec.builder(base.accumulatorType(property),
        builderFieldName()).addModifiers(PRIVATE).build();
  }

  String accumulatorName() {
    return base.collectionType.accumulatorPrefix + upcase(property.propertyName());
  }

  ParameterSpec asSetterParameter() {
    return base.setterParameter(property);
  }

  private CodeBlock normalAddAll(CodeBlock what) {
    FieldSpec builderField = asBuilderField();
    return CodeBlock.builder()
        .addStatement("this.$N.addAll($L)",
            builderField, what)
        .build();
  }

  private CodeBlock normalPutAll(CodeBlock what) {
    FieldSpec builderField = asBuilderField();
    return CodeBlock.builder()
        .addStatement("this.$N.putAll($L)",
            builderField, what)
        .build();
  }

  private String addMethod() {
    return base.collectionType == LIST ? "add" : "put";
  }

  private CodeBlock addAllBlock(CodeBlock what) {
    return base.collectionType == LIST ?
        normalAddAll(what) :
        normalPutAll(what);
  }

  private MethodSpec addAllToMethod(
      ParameterizedTypeName addAllType) {
    FieldSpec field = property.asField();
    FieldSpec builderField = asBuilderField();
    ParameterSpec values =
        ParameterSpec.builder(addAllType, "values").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if ($N == null)", values)
        .addStatement("return this")
        .endControlFlow();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .add(base.accumulatorInitBlock(builderField))
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.add(addAllBlock(CodeBlock.of("$N", values)));
    return MethodSpec.methodBuilder(accumulatorName())
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(values)
        .addModifiers(FINAL)
        .addModifiers(property.model.maybePublic())
        .returns(property.model.generatedClass)
        .build();
  }

  private MethodSpec putAllInMethod(
      ParameterizedTypeName addAllType) {
    FieldSpec field = property.asField();
    FieldSpec builderField = asBuilderField();
    ParameterSpec map =
        ParameterSpec.builder(addAllType, "map").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if ($N == null)", map)
        .addStatement("return this")
        .endControlFlow();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .add(base.accumulatorInitBlock(builderField))
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.add(addAllBlock(CodeBlock.of("$N", map)));
    return MethodSpec.methodBuilder(
        accumulatorName())
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(map)
        .addModifiers(FINAL)
        .addModifiers(property.model.maybePublic())
        .returns(property.model.generatedClass)
        .build();
  }

  private MethodSpec addToMethod() {
    FieldSpec field = property.asField();
    FieldSpec builderField = asBuilderField();
    ParameterizedTypeName accumulatorType = base.accumulatorType(property);
    ParameterSpec key =
        ParameterSpec.builder(accumulatorType.typeArguments.get(0), "value").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .add(base.accumulatorInitBlock(builderField))
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.addStatement("this.$N.$L($N)",
        builderField, addMethod(), key);
    return MethodSpec.methodBuilder(
        accumulatorName())
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(key)
        .addModifiers(FINAL)
        .addModifiers(property.model.maybePublic())
        .returns(property.model.generatedClass)
        .build();
  }

  private MethodSpec putInMethod() {
    FieldSpec field = property.asField();
    FieldSpec builderField = asBuilderField();
    ParameterizedTypeName accumulatorType = base.accumulatorType(property);
    ParameterSpec key =
        ParameterSpec.builder(accumulatorType.typeArguments.get(0), "key").build();
    ParameterSpec value =
        ParameterSpec.builder(accumulatorType.typeArguments.get(1), "value").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .add(base.accumulatorInitBlock(builderField))
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.addStatement("this.$N.$L($N, $N)",
        builderField, addMethod(), key, value);
    return MethodSpec.methodBuilder(
        accumulatorName())
        .addCode(block.build())
        .addStatement("return this")
        .addParameters(asList(key, value))
        .addModifiers(FINAL)
        .addModifiers(property.model.maybePublic())
        .returns(property.model.generatedClass)
        .build();
  }

  @Override
  <R, P> R accept(Cases<R, P> cases, P p) {
    return cases.collectionish(this, p);
  }
}
