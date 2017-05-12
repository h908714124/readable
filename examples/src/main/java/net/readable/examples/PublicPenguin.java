package net.readable.examples;

import net.readable.Readable;

@Readable
public final class PublicPenguin {

  private final String foo;

  public PublicPenguin(String foo) {
    this.foo = foo;
  }

  public String foo() {
    return foo;
  }
}
