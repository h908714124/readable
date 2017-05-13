package net.readable.compiler;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.Generated;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.readable.compiler.ReadableProcessor.rawType;

final class Analyser {

  private final Model model;
  private final MethodSpec initMethod;
  private final MethodSpec staticBuildMethod;
  private final RefTrackingBuilder optionalRefTrackingBuilder;

  private Analyser(Model model) {
    this.model = model;
    this.initMethod = initMethod(model);
    this.staticBuildMethod = staticBuildMethod(model);
    this.optionalRefTrackingBuilder =
        RefTrackingBuilder.create(model, staticBuildMethod);
  }

  static Analyser create(Model model) {
    return new Analyser(model);
  }

  TypeSpec analyse() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(rawType(model.generatedClass));
    builder.addMethod(builderMethod());
    builder.addMethod(builderMethodWithParam());
    builder.addMethod(perThreadFactoryMethod(optionalRefTrackingBuilder));
    builder.addMethod(initMethod);
    builder.addMethod(staticBuildMethod);
    builder.addMethod(abstractBuildMethod());
    builder.addType(SimpleBuilder.create(model, staticBuildMethod)
        .define());
    if (optionalRefTrackingBuilder != null) {
      RefTrackingBuilder refTrackingBuilder = requireNonNull(optionalRefTrackingBuilder);
      builder.addType(refTrackingBuilder.define());
      builder.addType(PerThreadFactory.create(model, initMethod, refTrackingBuilder).define());
    } else {
      builder.addType(PerThreadFactory.createStub(model));
    }
    for (Property property : model.properties) {
      FieldSpec field = property.asInitializedField();
      ParameterSpec p = ParameterSpec.builder(property.type(),
          property.propertyName()).build();
      builder.addField(field);
      builder.addMethod(setterMethod(property, field, p));
      property.optionalInfo()
          .filter(OptionalInfo::isRegular)
          .ifPresent(optionalInfo ->
              builder.addMethod(optionalSetterMethod(property,
                  optionalInfo)));
    }
    return builder.addModifiers(model.maybePublic())
        .addModifiers(ABSTRACT)
        .addTypeVariables(model.typevars())
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE)
            .build())
        .addAnnotation(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", ReadableProcessor.class.getCanonicalName())
            .build())
        .build();
  }

  private MethodSpec setterMethod(Property property, FieldSpec f, ParameterSpec p) {
    return MethodSpec.methodBuilder(
        property.propertyName())
        .addStatement("this.$N = $N", f, p)
        .addStatement("return this")
        .addParameter(p)
        .addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec optionalSetterMethod(
      Property property, OptionalInfo optionalInfo) {
    ParameterSpec p = ParameterSpec.builder(optionalInfo.wrapped,
        property.propertyName()).build();
    FieldSpec f = property.asField().build();
    CodeBlock.Builder block = CodeBlock.builder();
    if (optionalInfo.isOptional()) {
      block.addStatement("this.$N = $T.ofNullable($N)", f, optionalInfo.wrapper, p);
    } else {
      block.addStatement("this.$N = $T.of($N)", f, optionalInfo.wrapper, p);
    }
    return MethodSpec.methodBuilder(
        property.propertyName())
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(p)
        .addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec builderMethod() {
    return MethodSpec.methodBuilder("builder")
        .addModifiers(STATIC)
        .addTypeVariables(model.typevars())
        .addStatement("return new $T()", model.simpleBuilderClass)
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec builderMethodWithParam() {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass, "builder").build();
    ParameterSpec input = ParameterSpec.builder(model.sourceClass, "input").build();
    CodeBlock.Builder block = CodeBlock.builder()
        .addStatement("$T $N = new $T()", builder.type, builder, model.simpleBuilderClass)
        .addStatement("$N($N, $N)", initMethod, builder, input)
        .addStatement("return $N", builder);
    return MethodSpec.methodBuilder("builder")
        .addCode(block.build())
        .addParameter(input)
        .addTypeVariables(model.typevars())
        .addModifiers(STATIC)
        .returns(model.generatedClass)
        .build();
  }

  private static MethodSpec initMethod(Model model) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass,
        "builder").build();
    ParameterSpec input = ParameterSpec.builder(model.sourceClass, "input").build();
    CodeBlock.Builder block = CodeBlock.builder();
    for (Property accessorPair : model.properties) {
      block.addStatement("$N.$N($N.$L)", builder, accessorPair.propertyName(),
          input, accessorPair.access());
    }
    return MethodSpec.methodBuilder("init")
        .addCode(block.build())
        .addParameters(Arrays.asList(builder, input))
        .addModifiers(PRIVATE, STATIC)
        .addTypeVariables(model.typevars())
        .build();
  }

  private static MethodSpec staticBuildMethod(Model model) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass,
        "builder").build();
    CodeBlock.Builder block = CodeBlock.builder();
    for (int i = 0; i < model.properties.size(); i++) {
      Property property = model.properties.get(i);
      FieldSpec f = property.asField().build();
      if (i > 0) {
        block.add(",");
      }
      block.add("\n    $N.$N", builder, f);
    }
    return MethodSpec.methodBuilder("build")
        .addCode("return new $T(", model.sourceClass)
        .addCode(block.build())
        .addCode(");\n")
        .addTypeVariables(model.typevars())
        .returns(model.sourceClass)
        .addParameter(builder)
        .addModifiers(PRIVATE, STATIC)
        .build();
  }

  private MethodSpec abstractBuildMethod() {
    return MethodSpec.methodBuilder("build")
        .returns(model.sourceClass)
        .addModifiers(ABSTRACT)
        .addModifiers(model.maybePublic())
        .build();
  }

  private MethodSpec perThreadFactoryMethod(RefTrackingBuilder optionalRefTrackingBuilder) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("perThreadFactory")
        .returns(RefTrackingBuilder.perThreadFactoryClass(model))
        .addModifiers(STATIC);
    if (optionalRefTrackingBuilder != null) {
      RefTrackingBuilder refTrackingBuilder = requireNonNull(optionalRefTrackingBuilder);
      return builder.addStatement("return new $T()",
          refTrackingBuilder.perThreadFactoryClass)
          .build();
    } else {
      return builder.addStatement("throw new $T(\n$S)",
          UnsupportedOperationException.class, model.cacheWarning())
          .addModifiers(PRIVATE)
          .build();
    }
  }
}
