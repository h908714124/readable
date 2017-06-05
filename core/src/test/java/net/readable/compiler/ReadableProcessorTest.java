package net.readable.compiler;

import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class ReadableProcessorTest {

  @Test
  public void inheritedFieldIsPackagePrivate() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "",
        "public class Animal {",
        "  final String name;",
        "  protected Animal(String name) {",
        "    this.name = name;",
        "  }",
        "}");
    List<String> horse = asList(
        "package horse;",
        "import animal.Animal;",
        "import net.readable.Readable;",
        "",
        "@Readable",
        "class Horse extends Animal {",
        "  Horse(String name) {",
        "    super(name);",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    JavaFileObject horseFile = forSourceLines("horse.Horse", horse);
    assertAbout(javaSources()).that(asList(animalFile, horseFile))
        .processedWith(new ReadableProcessor())
        .failsToCompile()
        .withErrorContaining("missing readable property");
  }

  @Test
  public void inheritedFieldIsProtected() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "",
        "public class Animal {",
        "  protected final String name;",
        "  protected Animal(String name) {",
        "    this.name = name;",
        "  }",
        "}");
    List<String> horse = asList(
        "package horse;",
        "import animal.Animal;",
        "import net.readable.Readable;",
        "",
        "@Readable",
        "class Horse extends Animal {",
        "  Horse(String name) {",
        "    super(name);",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    JavaFileObject horseFile = forSourceLines("horse.Horse", horse);
    assertAbout(javaSources()).that(asList(animalFile, horseFile))
        .processedWith(new ReadableProcessor())
        .failsToCompile()
        .withErrorContaining("missing readable property");
  }

  @Test
  public void inheritedFieldIsPublic() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "",
        "public class Animal {",
        "  public final String name;",
        "  protected Animal(String name) {",
        "    this.name = name;",
        "  }",
        "}");
    List<String> horse = asList(
        "package horse;",
        "import animal.Animal;",
        "import net.readable.Readable;",
        "",
        "@Readable",
        "class Horse extends Animal {",
        "  Horse(String name) {",
        "    super(name);",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    JavaFileObject horseFile = forSourceLines("horse.Horse", horse);
    assertAbout(javaSources()).that(asList(animalFile, horseFile))
        .processedWith(new ReadableProcessor())
        .compilesWithoutError();
  }

  @Test
  public void inheritedMethodIsPackagePrivate() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "",
        "public class Animal {",
        "  private final String name;",
        "  protected Animal(String name) {",
        "    this.name = name;",
        "  }",
        "  String getName() {",
        "    return name;",
        "  }",
        "}");
    List<String> horse = asList(
        "package horse;",
        "import animal.Animal;",
        "import net.readable.Readable;",
        "",
        "@Readable",
        "class Horse extends Animal {",
        "  Horse(String name) {",
        "    super(name);",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    JavaFileObject horseFile = forSourceLines("horse.Horse", horse);
    assertAbout(javaSources()).that(asList(animalFile, horseFile))
        .processedWith(new ReadableProcessor())
        .failsToCompile()
        .withErrorContaining("missing readable property");
  }

  @Test
  public void inheritedMethodIsProtected() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "",
        "public class Animal {",
        "  private final String name;",
        "  protected Animal(String name) {",
        "    this.name = name;",
        "  }",
        "  String getName() {",
        "    return name;",
        "  }",
        "}");
    List<String> horse = asList(
        "package horse;",
        "import animal.Animal;",
        "import net.readable.Readable;",
        "",
        "@Readable",
        "class Horse extends Animal {",
        "  Horse(String name) {",
        "    super(name);",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    JavaFileObject horseFile = forSourceLines("horse.Horse", horse);
    assertAbout(javaSources()).that(asList(animalFile, horseFile))
        .processedWith(new ReadableProcessor())
        .failsToCompile()
        .withErrorContaining("missing readable property");
  }

  @Test
  public void inheritedMethodIsPublic() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "",
        "public class Animal {",
        "  private final String name;",
        "  protected Animal(String name) {",
        "    this.name = name;",
        "  }",
        "  public String getName() {",
        "    return name;",
        "  }",
        "}");
    List<String> horse = asList(
        "package horse;",
        "import animal.Animal;",
        "import net.readable.Readable;",
        "",
        "@Readable",
        "class Horse extends Animal {",
        "  Horse(String name) {",
        "    super(name);",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    JavaFileObject horseFile = forSourceLines("horse.Horse", horse);
    assertAbout(javaSources()).that(asList(animalFile, horseFile))
        .processedWith(new ReadableProcessor())
        .compilesWithoutError();
  }

  @Test
  public void twoConstructors() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "import net.readable.Readable;",
        "",
        "@Readable",
        "class Animal {",
        "  final String name;",
        "",
        "  Animal(String name) {",
        "    this.name = name;",
        "  }",
        "",
        "  Animal() {",
        "    this(null);",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    assertAbout(javaSources()).that(singletonList(animalFile))
        .processedWith(new ReadableProcessor())
        .failsToCompile()
        .withErrorContaining("Use @Readable.Constructor to tag a constructor");
  }

  @Test
  public void twoAnnotatedConstructors() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "import net.readable.Readable;",
        "",
        "@Readable",
        "class Animal {",
        "  final String name;",
        "",
        "  @Readable.Constructor",
        "  Animal(String name) {",
        "    this.name = name;",
        "  }",
        "",
        "  @Readable.Constructor",
        "  Animal() {",
        "    this(null);",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    assertAbout(javaSources()).that(singletonList(animalFile))
        .processedWith(new ReadableProcessor())
        .failsToCompile()
        .withErrorContaining("Only one @Constructor annotation " +
            "is allowed per class");
  }

  @Test
  public void privateConstructor() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "import net.readable.Readable;",
        "",
        "@Readable",
        "class Animal {",
        "  final String name;",
        "  private Animal(String name) {",
        "    this.name = name;",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    assertAbout(javaSources()).that(singletonList(animalFile))
        .processedWith(new ReadableProcessor())
        .failsToCompile()
        .withErrorContaining("No non-private constructor found");
  }

  @Test
  public void abstractClass() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "import net.readable.Readable;",
        "",
        "@Readable",
        "abstract class Animal {",
        "  final String name;",
        "  Animal(String name) {",
        "    this.name = name;",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    assertAbout(javaSources()).that(singletonList(animalFile))
        .processedWith(new ReadableProcessor())
        .failsToCompile()
        .withErrorContaining("The class may not be abstract");
  }

  @Test
  public void invalidNesting() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "import net.readable.Readable;",
        "",
        "class Animal {",
        "",
        "  @Readable",
        "  class Inner {",
        "    final String name;",
        "    Inner(String name) {",
        "      this.name = name;",
        "    }",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    assertAbout(javaSources()).that(singletonList(animalFile))
        .processedWith(new ReadableProcessor())
        .failsToCompile()
        .withErrorContaining("The inner class must be static");
  }

  @Test
  public void privateClass() throws Exception {
    List<String> animal = asList(
        "package animal;",
        "import net.readable.Readable;",
        "",
        "class Animal {",
        "",
        "  @Readable",
        "  private static class Inner {",
        "    final String name;",
        "    Inner(String name) {",
        "      this.name = name;",
        "    }",
        "  }",
        "}");
    JavaFileObject animalFile = forSourceLines("animal.Animal", animal);
    assertAbout(javaSources()).that(singletonList(animalFile))
        .processedWith(new ReadableProcessor())
        .failsToCompile()
        .withErrorContaining("The class may not be private");
  }
}