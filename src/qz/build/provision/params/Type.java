package qz.build.provision.params;

import java.util.Locale;

public enum Type {
    SCRIPT,
    SOFTWARE,
    RESOURCE,
    REMOVER, // QZ Tray remover
    CA,
    CERT,
    CONF,
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
