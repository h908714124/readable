package net.readable.examples;

import net.readable.Readable;

import java.util.List;

@Readable
final class Snake extends Animal {

  private static final ThreadLocal<Snake_Builder.PerThreadFactory> FACTORY =
      ThreadLocal.withInitial(Snake_Builder::perThreadFactory);

  private final long length;
  private final List<String> friends;

  Snake(String name, boolean good, long length, List<String> friends) {
    super(name, good);
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
