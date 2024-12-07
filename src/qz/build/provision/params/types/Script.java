package qz.build.provision.params.types;

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
        if(input != null) {
            if(input.contains(".")) {
                String extension = input.substring(input.lastIndexOf(".") + 1);
                return EnumParser.parse(Script.class, extension);
            } else {
                // If no file extension, assume a shell script
                return SH;
            }
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