package dv.trung.glux.processer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

import dv.trung.glux.annotations.Font;

public class FontsProcessor extends AbstractProcessor {

    private static final ClassName String = ClassName.get("java.lang", "String", new String[0]);
    private static final ClassName Typeface = ClassName.get("android.graphics", "Typeface");
    private static final ClassName Context = ClassName.get("android.content", "Context");
    private static final ClassName ViewPump = ClassName.get("io.github.inflationx.viewpump", "ViewPump");
    private static final ClassName CalligraphyInterceptor = ClassName.get("io.github.inflationx.calligraphy3", "CalligraphyInterceptor");
    private static final ClassName CalligraphyConfig = ClassName.get("io.github.inflationx.calligraphy3", "CalligraphyConfig");
    private static final ClassName ViewPumpContextWrapper = ClassName.get("io.github.inflationx.viewpump", "ViewPumpContextWrapper");

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
            if (fontElement.packageName().isEmpty()) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Please provide you packageName");
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

            TypeSpec.Builder fontClass = TypeSpec
                    .classBuilder("Font")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
            File[] files = Objects.requireNonNull(fonts.listFiles());

            String packageName = getApplicationPackageName(fontElement);
            ClassName R = ClassName.get(packageName, "R", new String[0]);

            for (File file : files) {
                String[] splitName = file.getName().split("\\.");
                if (splitName.length != 2) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Font " + file.getName() + " in the wrong format name. Format name is 'FontFamily-Style.ext'");
                    return false;
                }
                String fontName = splitName[0].replace("-", "_");
                String fontPath = java.lang.String.format("fonts/%s", file.getName());
                FieldSpec intentMethod = FieldSpec
                        .builder(String, fontName.toUpperCase(), Modifier.PUBLIC, Modifier.STATIC)
                        .initializer("$S", fontPath)
                        .build();

                fontClass.addField(intentMethod);
            }
            MethodSpec.Builder getFontType = getMethodFontType();
            fontClass.addMethod(getFontType.build());

            MethodSpec.Builder setupFont = MethodSpec
                    .methodBuilder("setupFont")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(String, "fontDefaultPath")
                    .returns(TypeName.VOID);

            setupFont.addStatement("$T.init($T.builder().addInterceptor(new $T(new $T.Builder().setFontAttrId($T.attr.fontPath).setDefaultFontPath(fontDefaultPath).build())).build())", ViewPump, ViewPump, CalligraphyInterceptor, CalligraphyConfig, R);
            fontClass.addMethod(setupFont.build());

            MethodSpec.Builder fontWrapper = MethodSpec
                    .methodBuilder("fontWrapper")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Context, "context")
                    .returns(Context);

            fontWrapper.addStatement("return $T.wrap(context)", ViewPumpContextWrapper);
            fontClass.addMethod(fontWrapper.build());
            try {
                writeFontUtils(packageName, fontClass);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            break;
        }

        return true;
    }

    @NotNull
    private String getApplicationPackageName(Font element) {
        return element.packageName();
    }

    private File findFontsFolder() throws Exception {
        File dummyFile = new File(filer.getResource(StandardLocation.CLASS_OUTPUT, "", "dv").toUri());
        File projectRoot = dummyFile.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
        return new File(projectRoot.getAbsolutePath() + "/src/main/assets/fonts");
    }

    private void writeFontUtils(java.lang.String packageName, TypeSpec.Builder fontClass) throws IOException {
        JavaFile.builder(packageName + ".font", fontClass.build()).build().writeTo(filer);
    }

    private MethodSpec.Builder getMethodFontType() {
        MethodSpec.Builder getFontType = MethodSpec
                .methodBuilder("getTypeface")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(Typeface)
                .addParameter(Context, "context")
                .addParameter(String, "fontPath");

        getFontType.addStatement("return Typeface.createFromAsset(context.getAssets(), fontPath)");
        return getFontType;
    }
}
