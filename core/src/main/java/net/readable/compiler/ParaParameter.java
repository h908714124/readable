package net.readable.compiler;

import com.squareup.javapoet.FieldSpec;

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

  static final Function<ParaParameter, FieldSpec> AS_INITIALIZED_FIELD =
      asFunction(new Cases<FieldSpec, Void>() {
        @Override
        FieldSpec property(Property property, Void _null) {
          return property.asField().build();
        }
        @Override
        FieldSpec optionalish(Optionalish optionalish, Void _null) {
          return optionalish.property.asField()
              .initializer("$T.empty()", optionalish.wrapper)
              .build();
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