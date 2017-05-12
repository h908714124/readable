package net.readable.examples;

import net.readable.Readable;

import java.util.Optional;

@Readable
final class GradleMan<S extends String> {

  private final S name;
  private final S snake;
  private final boolean good;
  private final boolean nice;

  GradleMan(Optional<S> name, S snake, boolean good, boolean nice) {
    this.name = name.orElse(null);
    this.snake = snake;
    this.good = good;
    this.nice = nice;
  }

  Optional<S> getName() {
    return Optional.ofNullable(name);
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
