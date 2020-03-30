package dv.trung.glux.processer;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

import dv.trung.glux.annotations.Font;

public class FontsProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Font.class.getCanonicalName());
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> annotated = roundEnvironment.getElementsAnnotatedWith(Font.class);
        if (annotated.size() > 1) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Can be used @Font only one time in Application");
            return false;
        }
        for (Element element : annotated) {
            if (element == null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Can not find element for Font annotation");
                return false;
            }
            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.");
                return false;
            }
            Font fontElement = element.getAnnotation(Font.class);
            if (fontElement == null) {
                return false;
            }
            File fonts = null;
            try {
                fonts = findFontsFolder();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (fonts == null) {
                return false;
            }
            File[] files = Objects.requireNonNull(fonts.listFiles());
            for (int i = 0; i < files.length; i++) {
                messager.printMessage(Diagnostic.Kind.NOTE, "" + files[i].getName());
            }
            break;
        }

        return true;
    }

    private File findFontsFolder() throws Exception {
        File dummyFile = new File(filer.getResource(StandardLocation.CLASS_OUTPUT, "", "dv").toUri());
        File projectRoot = dummyFile.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
        return new File(projectRoot.getAbsolutePath() + "/src/main/assets/fonts");
    }
}
