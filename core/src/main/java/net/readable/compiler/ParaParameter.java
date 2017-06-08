package net.readable.compiler;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

abstract class ParaParameter {

  static abstract class Cases<R, P> {

    abstract R property(Property property, P p);

    abstract R optionalish(Optionalish optionalish, P p);
  }

  private static <R> Function<ParaParameter, R> asFunction(Cases<R, Void> cases) {
    return parameter -> parameter.accept(cases, null);
  }

  private static <P> BiConsumer<ParaParameter, P> asConsumer(Cases<Void, P> cases) {
    return (parameter, p) -> parameter.accept(cases, p);
  }

  private static <R, P> BiFunction<ParaParameter, P, R> biFunction(Cases<R, P> cases) {
    return (parameter, p) -> parameter.accept(cases, p);
  }

  abstract <R, P> R accept(Cases<R, P> cases, P p);

  static final Function<ParaParameter, Property> GET_PROPERTY =
      asFunction(new Cases<Property, Void>() {
        @Override
        Property property(Property property, Void _null) {
          return property;
        }

        @Override
        Property optionalish(Optionalish optionalish, Void _null) {
          return optionalish.property;
        }
      });

  static final BiFunction<ParaParameter, ParameterSpec, CodeBlock> GET_FIELD_VALUE =
      biFunction(new ParaParameter.Cases<CodeBlock, ParameterSpec>() {
        @Override
        CodeBlock property(Property property, ParameterSpec builder) {
          return Optionalish.emptyBlock(property, builder)
              .orElse(CodeBlock.of("$N.$N", builder, property.asField()));
        }

        @Override
        CodeBlock optionalish(Optionalish optionalish, ParameterSpec builder) {
          return optionalish.getFieldValue(builder);
        }
      });

  static final BiFunction<ParaParameter, ParameterSpec, Optional<CodeBlock>> CLEANUP_CODE =
      biFunction(new ParaParameter.Cases<Optional<CodeBlock>, ParameterSpec>() {
        @Override
        Optional<CodeBlock> property(Property parameter, ParameterSpec builder) {
          if (parameter.asType().getKind().isPrimitive()) {
            return Optional.empty();
          }
          return Optional.of(CodeBlock.builder().addStatement("$N.$L(null)",
              builder, parameter.propertyName()).build());
        }

        @Override
        Optional<CodeBlock> optionalish(Optionalish optionalish, ParameterSpec builder) {
          return Optional.of(CodeBlock.builder().addStatement("$N.$L(($T) null)",
              builder, optionalish.property.propertyName(),
              optionalish.property.type()).build());
        }
      });

  static final Function<ParaParameter, Optional<Optionalish>> OPTIONAL_INFO =
      asFunction(new Cases<Optional<Optionalish>, Void>() {
        @Override
        Optional<Optionalish> property(Property property, Void _null) {
          return Optional.empty();
        }

        @Override
        Optional<Optionalish> optionalish(Optionalish optionalish, Void _null) {
          return Optional.of(optionalish);
        }
      });
}