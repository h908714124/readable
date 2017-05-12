package net.readable.examples;

import net.readable.Readable;

@Readable
final class PackagePinranha {

  private final boolean foo;

  PackagePinranha(boolean foo) {
    this.foo = foo;
  }
  
  boolean isFoo() {
    return foo;
  }
}
