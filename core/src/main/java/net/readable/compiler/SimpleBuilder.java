package net.readable.compiler;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.readable.compiler.ReadableProcessor.rawType;

final class SimpleBuilder {

  private final Model model;

  private SimpleBuilder(Model model) {
    this.model = model;
  }

  static SimpleBuilder create(Model model) {
    return new SimpleBuilder(model);
  }

  TypeSpec define() {
    return TypeSpec.classBuilder(rawType(model.simpleBuilderClass))
        .superclass(model.generatedClass)
        .addMethod(buildMethod())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private MethodSpec buildMethod() {
    return MethodSpec.methodBuilder("build")
        .addAnnotation(Override.class)
        .addStatement("return super.build()")
        .returns(model.sourceClass())
        .addModifiers(model.maybePublic())
        .build();
  }
}
