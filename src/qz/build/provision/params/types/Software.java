package qz.build.provision.params.types;

import org.apache.commons.io.FilenameUtils;
import qz.build.provision.params.EnumParser;
import qz.build.provision.params.Os;

import java.nio.file.Path;
import java.util.*;

public enum Software {
    EXE,
    MSI,
    PKG,
    DMG,
    RUN,
    UNKNOWN;

    public static Software parse(String input) {
        return EnumParser.parse(Software.class, FilenameUtils.getExtension(input), UNKNOWN);
    }

    public static Software parse(Path path) {
        return parse(path.toString());
    }

    public static List<String> parseArgs(String input) {
        List<String> args = new LinkedList<>();
        if(input != null) {
            String[] parts = input.split(" ");
            for(String part : parts) {
                if(!part.trim().isEmpty()) {
                    args.add(part.trim());
                }
            }
        }
        return args;
    }

    public HashSet<Os> defaultOs() {
        HashSet<Os> list = new HashSet<>();
        switch(this) {
            case EXE:
            case MSI:
                list.add(Os.WINDOWS);
                break;
            case PKG:
            case DMG:
                list.add(Os.MAC);
                break;
            case RUN:
                list.add(Os.LINUX);
                break;
            default:
                list.add(Os.ALL);
        }
        return list;
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }
}
