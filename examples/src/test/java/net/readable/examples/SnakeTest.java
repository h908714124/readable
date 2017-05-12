package net.readable.examples;

import org.junit.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SnakeTest {

  @Test
  public void testInheritance() {
    Snake snake = Snake_Builder.builder()
        .name("Kaa")
        .good(true)
        .friends(singletonList("Python"))
        .build();
    assertThat(snake.name, is("Kaa"));
    assertThat(snake.getFriends(), is(singletonList("Python")));
  }

  @Test
  public void testFactoryNestingWorksCorrectly() throws Exception {
    Snake spiderPig = Snake_Builder.builder()
        .name("")
        .friends(emptyList())
        .build();
    Snake horse = spiderPig.builderize()
        .name(spiderPig.builderize()
            .name("Horse")
            .friends(singletonList("Charlie"))
            .build().name)
        .build();
    assertThat(horse.name, is("Horse"));
    assertThat(horse.getFriends(), is(emptyList()));
  }
}