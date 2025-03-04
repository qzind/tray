package qz.build.provision.params;

import java.util.Locale;

public enum Type {
    SCRIPT,
    SOFTWARE,
    REMOVER, // QZ Tray remover
    CA,
    CERT,
    PROPERTY,
    PREFERENCE;

    public static Type parse(String input) {
        return EnumParser.parse(Type.class, input);
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }
}
