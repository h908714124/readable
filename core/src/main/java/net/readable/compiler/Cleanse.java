package net.readable.compiler;

import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static net.readable.compiler.ParaParameter.asFunction;
import static net.readable.compiler.Util.isDistinct;

final class Cleanse {

  static List<ParaParameter> detox(List<ParaParameter> parameters) {
    if (!parameters.stream()
        .map(FIELD_NAMES)
        .map(List::stream)
        .flatMap(Function.identity())
        .collect(isDistinct()) ||
        !parameters.stream()
            .map(METHOD_NAMES)
            .map(List::stream)
            .flatMap(Function.identity())
            .collect(isDistinct())) {
      parameters = parameters.stream()
          .map(NO_ACCUMULATOR)
          .collect(toList());
    }
    return parameters;
  }

  private static final Function<ParaParameter, List<String>> METHOD_NAMES =
      asFunction(new ParaParameter.Cases<List<String>, Void>() {
        @Override
        List<String> property(Property property, Void _null) {
          return singletonList(property.propertyName());
        }

        @Override
        List<String> collectionish(Collectionish collectionish, Void _null) {
          return asList(collectionish.property.propertyName(), collectionish.accumulatorName());
        }

        @Override
        List<String> optionalish(Optionalish optionalish, Void _null) {
          return singletonList(optionalish.property.propertyName());
        }
      });

  private static final Function<ParaParameter, List<String>> FIELD_NAMES =
      asFunction(new ParaParameter.Cases<List<String>, Void>() {
        @Override
        List<String> property(Property property, Void _null) {
          return singletonList(property.propertyName());
        }

        @Override
        List<String> collectionish(Collectionish collectionish, Void _null) {
          return asList(collectionish.property.propertyName(),
              collectionish.builderFieldName());
        }

        @Override
        List<String> optionalish(Optionalish optionalish, Void _null) {
          return singletonList(optionalish.property.propertyName());
        }
      });

  private static final Function<ParaParameter, ParaParameter> NO_ACCUMULATOR =
      asFunction(new ParaParameter.Cases<ParaParameter, Void>() {
        @Override
        ParaParameter property(Property property, Void _null) {
          return property;
        }

        @Override
        ParaParameter collectionish(Collectionish collectionish, Void _null) {
          return collectionish.property;
        }

        @Override
        ParaParameter optionalish(Optionalish optionalish, Void _null) {
          return optionalish;
        }
      });
}
