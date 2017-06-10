package net.readable.compiler;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.function.BiConsumer;
import java.util.function.Function;

abstract class ParaParameter {

  static abstract class Cases<R, P> {

    abstract R property(Property property, P p);

    abstract R collectionish(Collectionish collectionish, P p);

    abstract R optionalish(Optionalish optionalish, P p);
  }

  static <R> Function<ParaParameter, R> asFunction(Cases<R, Void> cases) {
    return parameter -> parameter.accept(cases, null);
  }

  private static <P> BiConsumer<ParaParameter, P> asConsumer(Cases<Void, P> cases) {
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
        Property collectionish(Collectionish collectionish, Void _null) {
          return collectionish.property;
        }

        @Override
        Property optionalish(Optionalish optionalish, Void _null) {
          return optionalish.property;
        }
      });

  static final Function<ParaParameter, ParameterSpec> AS_SETTER_PARAMETER =
      asFunction(new Cases<ParameterSpec, Void>() {
        @Override
        ParameterSpec property(Property property, Void _null) {
          return ParameterSpec.builder(property.type(), property.propertyName()).build();
        }

        @Override
        ParameterSpec collectionish(Collectionish collectionish, Void _null) {
          return collectionish.asSetterParameter();
        }

        @Override
        ParameterSpec optionalish(Optionalish optionalish, Void _null) {
          return ParameterSpec.builder(optionalish.property.type(),
              optionalish.property.propertyName()).build();
        }
      });

  static final Function<ParaParameter, CodeBlock> SETTER_ASSIGNMENT =
      asFunction(new ParaParameter.Cases<CodeBlock, Void>() {
        @Override
        CodeBlock property(Property property, Void _null) {
          FieldSpec field = property.asField();
          ParameterSpec p = AS_SETTER_PARAMETER.apply(property);
          return CodeBlock.builder()
              .addStatement("this.$N = $N", field, p).build();
        }

        @Override
        CodeBlock collectionish(Collectionish collectionish, Void _null) {
          return collectionish.setterAssignment();
        }

        @Override
        CodeBlock optionalish(Optionalish optionalish, Void _null) {
          FieldSpec field = optionalish.property.asField();
          ParameterSpec p = AS_SETTER_PARAMETER.apply(optionalish);
          return CodeBlock.builder()
              .addStatement("this.$N = $N", field, p).build();
        }
      });

  static final Function<ParaParameter, CodeBlock> GET_FIELD_VALUE =
      asFunction(new Cases<CodeBlock, Void>() {
        @Override
        CodeBlock property(Property property, Void _null) {
          return Collectionish.emptyBlock(property)
              .orElse(Optionalish.emptyBlock(property)
                  .orElse(CodeBlock.of("$N.$N",
                      property.model.builderParameter(),
                      property.asField())));
        }

        @Override
        CodeBlock collectionish(Collectionish collectionish, Void _null) {
          return collectionish.getFieldValue();
        }

        @Override
        CodeBlock optionalish(Optionalish optionalish, Void _null) {
          return optionalish.getFieldValue();
        }
      });

  static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_ACCUMULATOR_FIELD =
      asConsumer(new ParaParameter.Cases<Void, TypeSpec.Builder>() {
        @Override
        Void property(Property property, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder builder) {
          builder.addField(collectionish.asBuilderField());
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          return null;
        }
      });

  static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_ACCUMULATOR_METHOD =
      asConsumer(new ParaParameter.Cases<Void, TypeSpec.Builder>() {
        @Override
        Void property(Property property, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder builder) {
          builder.addMethod(collectionish.accumulatorMethod());
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          return null;
        }
      });

  static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_ACCUMULATOR_OVERLOAD =
      asConsumer(new Cases<Void, TypeSpec.Builder>() {
        @Override
        Void property(Property property, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder builder) {
          builder.addMethod(collectionish.accumulatorMethodOverload());
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          return null;
        }
      });

  static final BiConsumer<ParaParameter, CodeBlock.Builder> CLEANUP_CODE =
      asConsumer(new Cases<Void, CodeBlock.Builder>() {
        @Override
        Void property(Property property, CodeBlock.Builder builder) {
          if (!property.asType().getKind().isPrimitive()) {
            builder.addStatement("$N.$L(null)",
                property.model.builderParameter(),
                property.propertyName());
          }
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, CodeBlock.Builder builder) {
          builder.addStatement("$N.$L(null)",
              collectionish.property.model.builderParameter(),
              collectionish.property.propertyName());
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, CodeBlock.Builder builder) {
          builder.addStatement("$N.$L(($T) null)",
              optionalish.property.model.builderParameter(),
              optionalish.property.propertyName(),
              optionalish.property.type());
          return null;
        }
      });

  static final BiConsumer<ParaParameter, CodeBlock.Builder> CLEAR_ACCUMULATOR =
      asConsumer(new ParaParameter.Cases<Void, CodeBlock.Builder>() {
        @Override
        Void property(Property property, CodeBlock.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, CodeBlock.Builder builder) {
          builder.addStatement("this.$N = null",
              collectionish.asBuilderField());
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, CodeBlock.Builder builder) {
          return null;
        }
      });


  static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_OPTIONALISH_OVERLOAD =
      asConsumer(new Cases<Void, TypeSpec.Builder>() {
        @Override
        Void property(Property parameter, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder block) {
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          builder.addMethod(optionalish.convenienceOverloadMethod());
          return null;
        }
      });
}