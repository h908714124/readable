package net.readable.compiler;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.Generated;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.readable.compiler.ParaParameter.AS_INITIALIZED_FIELD;
import static net.readable.compiler.ParaParameter.GET_PROPERTY;
import static net.readable.compiler.ParaParameter.OPTIONAL_INFO;
import static net.readable.compiler.ReadableProcessor.rawType;

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
      FieldSpec field = AS_INITIALIZED_FIELD.apply(property);
      builder.addField(field);
      builder.addMethod(setterMethod(property));
      OPTIONAL_INFO.apply(property)
          .filter(Optionalish::isRegular)
          .ifPresent(optionalish ->
              builder.addMethod(
                  optionalSetterMethod(property,
                      optionalish)));
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

  private MethodSpec setterMethod(ParaParameter property) {
    ParameterSpec p = ParameterSpec.builder(GET_PROPERTY.apply(property).type(),
        GET_PROPERTY.apply(property).propertyName()).build();
    FieldSpec f = GET_PROPERTY.apply(property).asField().build();
    return MethodSpec.methodBuilder(
        GET_PROPERTY.apply(property).propertyName())
        .addStatement("this.$N = $N", f, p)
        .addStatement("return this")
        .addParameter(p)
        .addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec optionalSetterMethod(
      ParaParameter property, Optionalish optionalish) {
    ParameterSpec p = ParameterSpec.builder(optionalish.wrapped,
        GET_PROPERTY.apply(property).propertyName()).build();
    FieldSpec f = GET_PROPERTY.apply(property).asField().build();
    CodeBlock.Builder block = CodeBlock.builder();
    if (optionalish.isOptional()) {
      block.addStatement("this.$N = $T.ofNullable($N)", f, optionalish.wrapper, p);
    } else {
      block.addStatement("this.$N = $T.of($N)", f, optionalish.wrapper, p);
    }
    return MethodSpec.methodBuilder(
        GET_PROPERTY.apply(property).propertyName())
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

  private static MethodSpec initMethod(
      Model model, List<ParaParameter> properties) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass,
        "builder").build();
    ParameterSpec input = ParameterSpec.builder(model.sourceClass, "input").build();
    CodeBlock.Builder block = CodeBlock.builder();
    for (ParaParameter property : properties) {
      block.addStatement("$N.$N($N.$L)", builder, GET_PROPERTY.apply(property).propertyName(),
          input, GET_PROPERTY.apply(property).access());
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
    CodeBlock.Builder block = CodeBlock.builder();
    for (int i = 0; i < properties.size(); i++) {
      ParaParameter property = properties.get(i);
      FieldSpec f = GET_PROPERTY.apply(property).asField().build();
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
