package net.readable.examples;

import net.readable.Readable;

@Readable
final class PackagePiranha {

  private final boolean foo;

  PackagePiranha(boolean foo) {
    this.foo = foo;
  }
  
  boolean isFoo() {
    return foo;
  }
}
