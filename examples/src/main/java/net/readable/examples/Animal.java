package net.readable.examples;

import java.util.Optional;
import net.readable.Readable;

@Readable
class Animal {

  private static final ThreadLocal<Animal_Builder.PerThreadFactory> FACTORY =
      ThreadLocal.withInitial(Animal_Builder::perThreadFactory);

  final String name;
  final boolean good;

  final Optional<Optional<String>> maybeMaybe;
  final Optional<String> maybe;

  Animal(String name,
         boolean good,
         Optional<Optional<String>> maybeMaybe,
         Optional<String> maybe) {
    this.name = name;
    this.good = good;
    this.maybeMaybe = maybeMaybe;
    this.maybe = maybe;
  }

  Animal_Builder toBuilder() {
    return FACTORY.get().builder(this);
  }
}
