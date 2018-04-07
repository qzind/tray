package qz;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class Translation {

    private Properties originalTranslation;
    private Properties translation;
    private Path path;

    public Translation(Path path) throws IOException {
        this.path = path;

        this.originalTranslation = new Properties();
        originalTranslation.load(new FileReader(path.toFile()));

        this.translation = (Properties)this.originalTranslation.clone();
    }

    public void setProperty(String key, String value) { translation.setProperty(key, value);}

    public boolean containsKey(String key) {return translation.containsKey(key);}

    public void store() throws IOException {
        if (!translation.equals(originalTranslation)) {
            FileWriter fileWriter = new FileWriter(path.toFile());
            translation.store(fileWriter, "updated Translations");
            fileWriter.close();
        }
    }

    public Path getPath() {
        return path;
    }
}
