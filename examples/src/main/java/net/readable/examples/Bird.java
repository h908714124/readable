package net.readable.examples;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.readable.Readable;

@Readable
final class Bird {

  private static final ThreadLocal<Bird_Builder.PerThreadFactory> FACTORY =
      ThreadLocal.withInitial(Bird_Builder::perThreadFactory);

  final ImmutableList<Date> feathers;
  final ImmutableSet<String> feet;
  final ImmutableMap<String, String> eyes;
  final List<Date> beak;
  final Set<String> wings;
  final Map<Date, String> tail;

  Bird(ImmutableList<Date> feathers,
       ImmutableSet<String> feet,
       ImmutableMap<String, String> eyes,
       List<Date> beak,
       Set<String> wings,
       Map<Date, String> tail) {
    this.feathers = feathers;
    this.feet = feet;
    this.eyes = eyes;
    this.beak = beak;
    this.wings = wings;
    this.tail = tail;
  }

  Bird_Builder toBuilder() {
    return FACTORY.get().builder(this);
  }

  @Readable
  static final class Nest {

    final ImmutableList<? extends Iterable<? extends String>> feathers;
    final ImmutableList<String> sticks;
    final String addToSticks;

    Nest(ImmutableList<? extends Iterable<? extends String>> feathers,
         ImmutableList<String> sticks,
         String addToSticks) {
      this.feathers = feathers;
      this.sticks = sticks;
      this.addToSticks = addToSticks;
    }
  }
}
