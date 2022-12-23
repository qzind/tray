package qz.build.jlink;

/**
 * Handling of architectures
 */
public enum Arch implements Parsable {
    AMD64("amd64", "x86_64", "x64"),
    AARCH64("aarch64", "arm64");

    public final String[] matches;
    Arch(String ... matches) { this.matches = matches; }

    public static Arch parse(String value, Arch fallback) {
        return Parsable.parse(Arch.class, value, fallback);
    }

    public static Arch parse(String value) {
        return Parsable.parse(Arch.class, value);
    }

    public static Arch getCurrentArch() {
        return Parsable.parse(Arch.class, System.getProperty("os.arch"));
    }
}
