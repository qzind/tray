package qz;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractGettext extends Task {

    private static final Pattern pattern = Pattern.compile("gettext\\s*\\(\\s*\"(.+?)\\s*\"\\s*\\)");

    File src;
    File translations;
    boolean fail = false;

    public void setSrc(File src) {
        this.src = src;
    }

    public void setTranslations(File translations) {
        this.translations = translations;
    }

    public void setFail(boolean b) {
        fail = b;
    }


    public void execute() {

        if (src == null) {
            throw new BuildException("No src directory set.");
        }

        if (translations == null) {
            throw new BuildException("No translations directory set.");
        }

        List<Translation> translationsList = loadTranslations(translations.toPath());

        translationsList = analyzeJavaSourceCode(src.toPath(), translationsList);

        writeUpdatedTranslations(translationsList);

        log("done");
    }

    private void writeUpdatedTranslations(List<Translation> translations) {
        translations.forEach((translation) -> {
            try {
                translation.store();
                log("translation resource file updated " + translation.getPath());
            }
            catch(IOException e) {
                handleError("Failed to write translation resource file " + translation.getPath(), e);
            }
        });
    }

    private List<Translation> analyzeJavaSourceCode(Path sourceDirectory, List<Translation> translations) {
        try {
            PathMatcher javaPathMatcher = FileSystems.getDefault().getPathMatcher("glob:**.java");
            Files.find(sourceDirectory, Integer.MAX_VALUE, (path, basicFileAttributes) -> javaPathMatcher.matches(path))
                    .forEach((javaFile) -> {
                        log("analyse " + javaFile);
                        try {
                            Scanner scanner = new Scanner(javaFile, StandardCharsets.UTF_8.toString());
                            while(scanner.hasNextLine()) {
                                Matcher matcher = pattern.matcher(scanner.nextLine());

                                while(matcher.find()) {
                                    String key = matcher.group(1).replaceAll("\\\\\"", "\"");

                                    log("extracted translation key: " + key);

                                    translations.forEach((translation) -> {
                                        if (!translation.containsKey(key)) {
                                            log("translation misses key " + key);
                                            translation.setProperty(key, key);
                                        }
                                    });
                                }
                            }
                        }
                        catch(IOException e) {
                            handleError("Failed to read java source file " + javaFile, e);
                        }
                    });
        }
        catch(IOException e) {
            handleError("Failed to search for java source files in " + sourceDirectory, e);
        }

        return translations;
    }

    private List<Translation> loadTranslations(Path translationsDirectory) {
        List<Translation> translations = new ArrayList<>();
        PathMatcher translationsMatcher = FileSystems.getDefault().getPathMatcher("glob:**.properties");
        try {
            Files.find(translationsDirectory, 1, (path, basicFileAttributes) -> translationsMatcher.matches(path))
                    .forEach((path) -> {
                        try {
                            Translation translation = new Translation(path);
                            translations.add(translation);
                            log("loaded translation resource file " + path);
                        }
                        catch(IOException e) {
                            handleError("Failed to load translation resource file " + path, e);
                        }
                    });
        }
        catch(IOException e) {
            handleError("Failed to search for translation resource files in " + translationsDirectory, e);
        }
        return translations;
    }

    private void handleError(String message, Exception e) {
        if (fail) {
            throw new BuildException(message, e);
        } else {
            log(message, e, 0);
        }
    }
}