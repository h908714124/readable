package net.readable.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.readable.Readable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;

public final class ReadableProcessor extends AbstractProcessor {

  private final Set<String> done = new HashSet<>();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> strings = new HashSet<>();
    strings.add(Readable.class.getCanonicalName());
    return strings;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    Set<TypeElement> typeElements =
        typesIn(env.getElementsAnnotatedWith(Readable.class));
    Util util = new Util(processingEnv);
    for (TypeElement sourceClassElement : typeElements) {
      String key = sourceClassElement.getQualifiedName().toString();
      if (done.contains(key)) {
        continue;
      }
      try {
        Model model = Model.create(sourceClassElement, util);
        TypeSpec typeSpec = Analyser.create(model).analyse();
        write(rawType(model.generatedClass), typeSpec);
        done.add(key);
      } catch (ValidationException e) {
        processingEnv.getMessager().printMessage(ERROR, e.getMessage(), e.about);
      } catch (Exception e) {
        String trace = getStackTraceAsString(e);
        String message = "Unexpected error: " + trace;
        processingEnv.getMessager().printMessage(ERROR, message);
      }
    }
    return false;
  }

  private void write(ClassName generatedType, TypeSpec typeSpec) throws IOException {
    JavaFile javaFile = JavaFile.builder(generatedType.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
    JavaFileObject sourceFile = processingEnv.getFiler()
        .createSourceFile(generatedType.toString(),
            javaFile.typeSpec.originatingElements.toArray(new Element[0]));
    try (Writer writer = sourceFile.openWriter()) {
      writer.write(javaFile.toString());
    }
  }

  static ClassName rawType(TypeName typeName) {
    if (typeName instanceof TypeVariableName) {
      return TypeName.OBJECT;
    }
    if (typeName.getClass().equals(TypeName.class)) {
      return TypeName.OBJECT;
    }
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).rawType;
    }
    return ((ClassName) typeName);
  }

  private static String getStackTraceAsString(Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }
}
