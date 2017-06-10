package net.readable.examples;

import java.util.List;
import java.util.Optional;
import net.readable.Readable;

@Readable
final class Snake extends Animal {

  private static final ThreadLocal<Snake_Builder.PerThreadFactory> FACTORY =
      ThreadLocal.withInitial(Snake_Builder::perThreadFactory);

  private final long length;
  private final List<String> friends;

  Snake(String name,
        boolean good,
        Optional<Optional<String>> maybeMaybe,
        Optional<String> maybe,
        long length,
        List<String> friends) {
    super(name, good, maybeMaybe, maybe);
    this.length = length;
    this.friends = friends;
  }

  boolean isGood() {
    return false;
  }

  long getLength() {
    return length;
  }

  List<String> getFriends() {
    return friends;
  }

  Snake_Builder builderize() {
    return FACTORY.get().builder(this);
  }
}
