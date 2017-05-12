package net.readable.compiler;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.readable.compiler.ReadableProcessor.rawType;

final class SimpleBuilder {

  private final Model model;
  private final MethodSpec staticBuildMethod;

  private SimpleBuilder(Model model, MethodSpec staticBuildMethod) {
    this.model = model;
    this.staticBuildMethod = staticBuildMethod;
  }

  static SimpleBuilder create(Model model,
                              MethodSpec staticBuildMethod) {
    return new SimpleBuilder(model, staticBuildMethod);
  }

  TypeSpec define() {
    return TypeSpec.classBuilder(rawType(model.simpleBuilderClass))
        .superclass(model.generatedClass)
        .addTypeVariables(model.typevars())
        .addMethod(buildMethod())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private MethodSpec buildMethod() {
    return MethodSpec.methodBuilder("build")
        .addAnnotation(Override.class)
        .addStatement("return $T.$N(this)",
            rawType(model.generatedClass), staticBuildMethod)
        .returns(model.sourceClass)
        .addModifiers(model.maybePublic())
        .build();
  }
}
