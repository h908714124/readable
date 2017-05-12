package net.readable.compiler;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.groupingBy;
import static net.readable.compiler.Arity0.parameterlessMethods;

final class MethodScanner {

  static final Pattern GETTER_PATTERN =
      Pattern.compile("^get[A-Z].*$");
  static final Pattern IS_PATTERN =
      Pattern.compile("^is[A-Z].*$");

  static List<Property> scan(TypeElement sourceClassElement) {
    if (!sourceClassElement.getTypeParameters().isEmpty()) {
      throw new ValidationException("Type parameters not allowed here", sourceClassElement);
    }
    if (sourceClassElement.getModifiers().contains(Modifier.PRIVATE)) {
      throw new ValidationException("The class may not be private", sourceClassElement);
    }
    if (sourceClassElement.getModifiers().contains(Modifier.ABSTRACT)) {
      throw new ValidationException("The class may not be abstract", sourceClassElement);
    }
    if (sourceClassElement.getEnclosingElement() != null &&
        sourceClassElement.getEnclosingElement().getKind() == ElementKind.CLASS &&
        !sourceClassElement.getModifiers().contains(Modifier.STATIC)) {
      throw new ValidationException("The inner class must be static " +
          sourceClassElement.getEnclosingElement(), sourceClassElement);
    }
    return getters(sourceClassElement);
  }

  private static String truncatedGetterName(ExecutableElement getter) {
    String getterName = getter.getSimpleName().toString();
    String truncatedGetterName;
    if (GETTER_PATTERN.matcher(getterName).matches()) {
      truncatedGetterName = getterName.substring(3);
    } else if (getter.getReturnType().getKind() == TypeKind.BOOLEAN &&
        IS_PATTERN.matcher(getterName).matches()) {
      truncatedGetterName = getterName.substring(2);
    } else {
      throw new AssertionError();
    }
    return truncatedGetterName;
  }

  private static List<Property> getters(TypeElement sourceTypeElement) {
    List<Property> result = new ArrayList<>();
    parameterlessMethods(sourceTypeElement).stream()
        .filter(m -> m.getParameters().isEmpty())
        .filter(m -> m.getReturnType().getKind() != TypeKind.VOID)
        .filter(m -> GETTER_PATTERN.matcher(m.getSimpleName().toString()).matches() ||
            TypeKind.BOOLEAN == m.getReturnType().getKind() &&
                IS_PATTERN.matcher(m.getSimpleName().toString()).matches())
        .collect(groupingBy(m -> m.getSimpleName().toString()))
        .forEach((name, executableElements) -> {
          ExecutableElement getter = executableElements.get(0);
        });
    return result;
  }
}
