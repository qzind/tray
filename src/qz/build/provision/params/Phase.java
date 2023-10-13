package qz.build.provision.params;

import java.util.Locale;

public enum Phase {
    INSTALL,
    CERTGEN,
    STARTUP,
    UNINSTALL;

    public static Phase parse(String input) {
        return EnumParser.parse(Phase.class, input);
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }
}
