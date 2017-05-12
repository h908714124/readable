package net.readable.examples;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class AnimalTest {

  @Test
  public void testBasic() throws Exception {
    Animal spiderPig = Animal_Builder.builder()
        .name("Spider-Pig")
        .good(false)
        .build();
    Animal horse = spiderPig.toBuilder()
        .name("Horse")
        .good(true)
        .build();
    assertThat(spiderPig.name, is("Spider-Pig"));
    assertThat(spiderPig.good, is(false));
    assertThat(horse.name, is("Horse"));
    assertThat(horse.good, is(true));
  }

  @Test
  public void testFactoryNestingWorksCorrectly() throws Exception {
    Animal spiderPig = Animal_Builder.builder().name("").build();
    Animal horse = spiderPig.toBuilder()
        .good(true)
        .name(spiderPig.toBuilder()
            .name("Horse")
            .good(false)
            .build().name)
        .build();
    assertThat(horse.name, is("Horse"));
    assertThat("nested builder calls leads to incorrect results",
        horse.good, is(true));
  }

  @Test
  public void testFactoryBuildersAreReused() throws Exception {
    Animal spiderPig = Animal_Builder.builder().name("").build();
    Animal_Builder builder_1 = spiderPig.toBuilder();
    Animal badger = builder_1.name("Badger").build();
    Animal_Builder builder_2 = spiderPig.toBuilder();
    Animal snake = builder_2.name("Snake").build();
    assertThat(badger.name, is("Badger"));
    assertThat(snake.name, is("Snake"));
    assertThat("builders are not reused",
        builder_1, sameInstance(builder_2));
  }
}