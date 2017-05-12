package net.readable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for the
 * <a href="https://github.com/h908714124/readable">readable</a>
 * annotation processor.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Readable {

  /**
   * Marker annotation for the
   * <a href="https://github.com/h908714124/readable">readable</a>
   * annotation processor.
   * <ul>
   * <li>Readable.Constructor is only necessary if there is more than one non-private constructor.</li>
   * <li>If the enclosing class doesn't have the Readable annotation,
   * the Readable.Constructor annotation will be ignored.</li>
   * </ul>
   */
  @Target(ElementType.CONSTRUCTOR)
  @Retention(RetentionPolicy.SOURCE)
  @interface Constructor {
  }
}
