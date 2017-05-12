package net.readable.compiler;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.readable.compiler.ReadableProcessor.rawType;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Arrays;
import javax.annotation.Generated;

final class Analyser {

  private final Model model;
  private final MethodSpec initMethod;
  private final RefTrackingBuilder refTrackingBuilder;

  private Analyser(Model model) {
    this.model = model;
    this.initMethod = initMethod(model);
    this.refTrackingBuilder = RefTrackingBuilder.create(model);
  }

  static Analyser create(Model model) {
    return new Analyser(model);
  }

  TypeSpec analyse() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(rawType(model.generatedClass));
    builder.addMethod(builderMethod());
    builder.addMethod(builderMethodWithParam());
    builder.addMethod(perThreadFactoryMethod(refTrackingBuilder));
    builder.addMethod(initMethod);
    builder.addMethod(abstractBuildMethod());
    builder.addType(SimpleBuilder.create(model).define());
    builder.addType(refTrackingBuilder.define());
    builder.addType(PerThreadFactory.create(model, initMethod, refTrackingBuilder)
        .define());
    for (Property parameter : model.accessorPairs) {
      FieldSpec field = FieldSpec.builder(parameter.type(), parameter.propertyName()).build();
      ParameterSpec p = ParameterSpec.builder(parameter.type(), parameter.propertyName()).build();
      builder.addField(field);
      builder.addMethod(setterMethod(parameter, field, p));
    }
    return builder.addModifiers(model.maybePublic())
        .addModifiers(ABSTRACT)
        .addMethod(MethodSpec.constructorBuilder()
            .build())
        .addAnnotation(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", ReadableProcessor.class.getCanonicalName())
            .build())
        .build();
  }

  private MethodSpec setterMethod(Property parameter, FieldSpec f, ParameterSpec p) {
    return MethodSpec.methodBuilder(
        parameter.propertyName())
        .addStatement("this.$N = $N", f, p)
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
        .addStatement("return new $T()", model.simpleBuilderClass)
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec builderMethodWithParam() {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass, "builder").build();
    ParameterSpec input = ParameterSpec.builder(model.sourceClass(), "input").build();
    CodeBlock.Builder block = CodeBlock.builder()
        .addStatement("$T $N = new $T()", builder.type, builder, model.simpleBuilderClass)
        .addStatement("$N($N, $N)", initMethod, builder, input)
        .addStatement("return $N", builder);
    return MethodSpec.methodBuilder("builder")
        .addCode(block.build())
        .addParameter(input)
        .addModifiers(STATIC)
        .returns(model.generatedClass)
        .build();
  }

  private static MethodSpec initMethod(Model model) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass, "builder").build();
    ParameterSpec input = ParameterSpec.builder(model.sourceClass(), "input").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if ($N == null)", input)
        .addStatement("throw new $T($S)",
            NullPointerException.class, "Null " + input.name)
        .endControlFlow();
    for (Property accessorPair : model.accessorPairs) {
      block.addStatement("$N.$N($N.$L)", builder, accessorPair.propertyName(),
          input, accessorPair.access());
    }
    return MethodSpec.methodBuilder("init")
        .addCode(block.build())
        .addParameters(Arrays.asList(builder, input))
        .addModifiers(PRIVATE, STATIC)
        .build();
  }

  private MethodSpec abstractBuildMethod() {
    return MethodSpec.methodBuilder("build")
        .returns(model.sourceClass())
        .addModifiers(ABSTRACT)
        .addModifiers(model.maybePublic())
        .build();
  }

  private MethodSpec perThreadFactoryMethod(RefTrackingBuilder refTrackingBuilder) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("perThreadFactory")
        .returns(refTrackingBuilder.perThreadFactoryClass)
        .addModifiers(STATIC);
    return builder.addStatement("return new $T()",
        refTrackingBuilder.perThreadFactoryClass)
        .build();
  }
}
