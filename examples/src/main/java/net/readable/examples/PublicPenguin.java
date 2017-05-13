package net.readable.examples;

import net.readable.Readable;

import java.util.Optional;
import java.util.OptionalInt;

@Readable
public final class PublicPenguin {

  private final String foo;
  final Optional<String> friend;
  final OptionalInt bar;

  public PublicPenguin(String foo, Optional<String> friend, OptionalInt bar) {
    this.foo = foo;
    this.friend = friend;
    this.bar = bar;
  }

  public String foo() {
    return foo;
  }

  public PublicPenguin_Builder toBuilder() {
    return PublicPenguin_Builder.builder(this);
  }
}
