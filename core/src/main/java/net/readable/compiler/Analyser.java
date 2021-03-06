package net.readable.compiler;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Generated;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.readable.compiler.ParaParameter.ADD_ACCUMULATOR_FIELD;
import static net.readable.compiler.ParaParameter.ADD_ACCUMULATOR_METHOD;
import static net.readable.compiler.ParaParameter.ADD_ACCUMULATOR_OVERLOAD;
import static net.readable.compiler.ParaParameter.ADD_OPTIONALISH_OVERLOAD;
import static net.readable.compiler.ParaParameter.AS_SETTER_PARAMETER;
import static net.readable.compiler.ParaParameter.CLEANUP_CODE;
import static net.readable.compiler.ParaParameter.CLEAR_ACCUMULATOR;
import static net.readable.compiler.ParaParameter.GET_FIELD_VALUE;
import static net.readable.compiler.ParaParameter.GET_PROPERTY;
import static net.readable.compiler.ParaParameter.SETTER_ASSIGNMENT;
import static net.readable.compiler.ReadableProcessor.rawType;
import static net.readable.compiler.Util.joinCodeBlocks;

final class Analyser {

  private final Model model;
  private final List<ParaParameter> properties;
  private final MethodSpec initMethod;
  private final MethodSpec staticBuildMethod;
  private final Optional<RefTrackingBuilder> optionalRefTrackingBuilder;

  private Analyser(Model model) {
    this.properties = TypeScanner.scan(model);
    this.initMethod = initMethod(model, properties);
    this.staticBuildMethod = staticBuildMethod(model, properties);
    this.model = model;
    this.optionalRefTrackingBuilder =
        RefTrackingBuilder.create(model, properties, staticBuildMethod);
  }

  static Analyser create(Model model) {
    return new Analyser(model);
  }

  TypeSpec analyse() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(rawType(model.generatedClass));
    builder.addTypeVariables(model.typevars());
    builder.addMethod(builderMethod());
    builder.addMethod(builderMethodWithParam());
    builder.addMethod(perThreadFactoryMethod());
    builder.addMethod(initMethod);
    builder.addMethod(staticBuildMethod);
    builder.addMethod(abstractBuildMethod());
    builder.addType(SimpleBuilder.create(model, staticBuildMethod)
        .define());
    OptionalConsumer.of(optionalRefTrackingBuilder)
        .ifPresent(refTrackingBuilder -> {
          builder.addType(refTrackingBuilder.define());
          builder.addType(PerThreadFactory.create(model, initMethod, refTrackingBuilder)
              .define());
        })
        .otherwise(() -> {
          builder.addType(PerThreadFactory.createStub(model));
        });
    for (ParaParameter property : properties) {
      builder.addField(GET_PROPERTY.apply(property).asField());
      builder.addMethod(setterMethod(property));
      ADD_OPTIONALISH_OVERLOAD.accept(property, builder);
      ADD_ACCUMULATOR_FIELD.accept(property, builder);
      ADD_ACCUMULATOR_METHOD.accept(property, builder);
      ADD_ACCUMULATOR_OVERLOAD.accept(property, builder);
    }
    builder.addModifiers(model.maybePublic());
    return builder.addModifiers(ABSTRACT)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE)
            .build())
        .addAnnotation(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", ReadableProcessor.class.getCanonicalName())
            .build())
        .build();
  }

  private MethodSpec setterMethod(ParaParameter parameter) {
    ParameterSpec p = AS_SETTER_PARAMETER.apply(parameter);
    CodeBlock.Builder block = CodeBlock.builder();
    block.add(SETTER_ASSIGNMENT.apply(parameter));
    CLEAR_ACCUMULATOR.accept(parameter, block);
    block.addStatement("return this");
    return MethodSpec.methodBuilder(
        GET_PROPERTY.apply(parameter).propertyName())
        .addCode(block.build())
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

  private static MethodSpec initMethod(
      Model model, List<ParaParameter> properties) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass,
        "builder").build();
    ParameterSpec input = ParameterSpec.builder(model.sourceClass, "input").build();
    CodeBlock.Builder block = CodeBlock.builder();
    for (ParaParameter property : properties) {
      block.addStatement("$N.$N = $N.$L", builder,
          GET_PROPERTY.apply(property).asField(),
          input,
          GET_PROPERTY.apply(property).access());
    }
    return MethodSpec.methodBuilder("init")
        .addCode(block.build())
        .addParameters(Arrays.asList(builder, input))
        .addModifiers(PRIVATE, STATIC)
        .addTypeVariables(model.typevars())
        .build();
  }

  private static MethodSpec staticBuildMethod(
      Model model, List<ParaParameter> properties) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass,
        "builder").build();
    ParameterSpec result = ParameterSpec.builder(model.sourceClass, "result")
        .build();
    List<CodeBlock> invocation = properties.stream()
        .map(GET_FIELD_VALUE)
        .collect(Collectors.toList());
    CodeBlock.Builder cleanup = CodeBlock.builder();
    properties.forEach(property -> CLEANUP_CODE.accept(property, cleanup));
    return MethodSpec.methodBuilder("build")
        .addCode("$T $N = new $T(\n    ",
            model.sourceClass, result, rawType(model.sourceClass))
        .addCode(invocation.stream().collect(joinCodeBlocks(",\n    ")))
        .addCode(");\n")
        .addCode(cleanup.build())
        .addStatement("return $N", result)
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

  private MethodSpec perThreadFactoryMethod() {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("perThreadFactory")
        .returns(RefTrackingBuilder.perThreadFactoryClass(model))
        .addModifiers(STATIC);
    OptionalConsumer.of(optionalRefTrackingBuilder)
        .ifPresent(refTrackingBuilder -> builder.addStatement("return new $T()",
            refTrackingBuilder.perThreadFactoryClass))
        .otherwise(() -> builder.addStatement("throw new $T(\n$S)",
            UnsupportedOperationException.class, model.cacheWarning())
            .addModifiers(PRIVATE));
    return builder.build();
  }
}
