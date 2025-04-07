package qz.build.provision.params.types;

import org.apache.commons.io.FilenameUtils;
import qz.build.provision.params.EnumParser;

import java.nio.file.Path;
import java.util.Locale;

public enum Script {
    PS1,
    BAT,
    SH,
    PY,
    RB;

    public static Script parse(String input) {
        if(input != null && !input.isEmpty()) {
            return EnumParser.parse(Script.class, FilenameUtils.getExtension(input), SH);
        }
        return null;
    }

    public static Script parse(Path path) {
        return parse(path.toAbsolutePath().toString());
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }
}