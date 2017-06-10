package net.readable.examples;

import java.util.Optional;
import java.util.OptionalInt;
import net.readable.Readable;

@Readable
final class GradleMan<S extends String> {

  private final Optional<S> name;
  private final S snake;
  private final boolean good;
  private final boolean nice;

  final OptionalInt legs;

  @Readable.Constructor
  GradleMan(Optional<S> name, S snake, boolean good,
            boolean nice, OptionalInt legs) {
    this.name = name;
    this.snake = snake;
    this.good = good;
    this.nice = nice;
    this.legs = legs;
  }

  GradleMan() {
    this(null, null, true, true, null);
  }

  Optional<S> getName() {
    return name;
  }

  S getSnake() {
    return snake;
  }

  boolean good() {
    return good;
  }

  boolean isNice() {
    return nice;
  }
}
