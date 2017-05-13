# readable

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/readable/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/readable)

Generate builders for immutable objects. The generated builder is roughly equal to what
[auto-builder](https://github.com/h908714124/auto-builder) and
[bean-standard](https://github.com/h908714124/bean-standard) would generate.

The common goal of auto-builder, bean-standard and [readable](https://github.com/h908714124/readable)
is to provide a consistent "standalone" builder functionality for all java projects,
no matter what your data classes look like.

### Quick start

1. Put the `@Readable` annotation on a <em>readable class</em>, see definition below.
1. Done! A class `*_Builder.java` will be generated in the same package.

#### Example: A readable class that uses direct field access

````java
@Readable
final class Animal {

  final String name;

  Animal(String name) {
    this.name = name;
  }
}
````

A class `Animal_Builder.java` will be generated in the same package,
with the usual three factory methods:

* `Animal_Builder.builder()`
* `Animal_Builder.builder(Animal input)`
* `Animal_Builder.perThreadFactory()`

If you're not familiar with these methods, you can read more about them
[in the auto-builder docs](https://github.com/h908714124/auto-builder#quick-start),
or check the examples below.

### What's a readable class?

Disregarding scenarios like abstract classes and non-static inner classes,
there are only two rules to worry about.

#### Rule 1: Non-private constructor

* The class must have a non-private constructor.
* If there's more than one such constructor,
  then exacly one of them must be tagged with the `@Readable.Constructor` annotation.

For brevity, we will now refer to this as <em>the</em> constructor,
because all others are simply ignored.

#### Rule 2: Matching accessors

For each constructor parameter `T myParam`, where `T` is a type, there must be either

* a non-private field `T myParam`
* OR a non-private method `T myParam()`
* OR a non-private method `T getMyParam()`
* OR, if `T` is `boolean`, a non-private method `boolean isMyParam()`.

The first match in this order wins, and will be used to initialize the builder 
in `Animal_Builder.builder(Animal input)`.

The following examples are all valid.

#### Example: Accessor method

````java
@Readable
final class Animal {

  private final String name;

  Animal(String name) {
    this.name = name;
  }

  String name() {
    return name();
  }
}
````

#### Example: Tagged constructor

````java
@Readable
final class Animal {

  final String name;

  Animal() {
    this("Charlie");
  }

  @Readable.Constructor
  Animal(String name) {
    this.name = name;
  }
}
````

#### Example: Adding a toBuilder method

````java
@Readable
final class Animal {

  final String name;

  Animal(String name) {
    this.name = name;
  }

  Animal_Builder toBuilder() {
    return Animal_Builder.builder(this);
  }
}
````

#### Example: Caching the builder instance

````java
@Readable
final class Animal {

  private static final ThreadLocal<Animal_Builder.PerThreadFactory> FACTORY =
      ThreadLocal.withInitial(Animal_Builder::perThreadFactory);

  final String name;

  Animal(String name) {
    this.name = name;
  }

  Animal_Builder toBuilder() {
    return FACTORY.get().builder(this);
  }
}
````

Regarding the last example, see the
[note about caching](https://github.com/h908714124/auto-builder#caching) in the auto-builder docs.

You can find some runnable examples in the `src/test` folder of
[the examples](https://github.com/h908714124/readable/tree/master/examples).
Have fun!

### It's maven time

````xml
<dependency>
  <groupId>com.github.h908714124</groupId>
  <artifactId>readable</artifactId>
  <version>1.1</version>
  <scope>provided</scope>
</dependency>
````
