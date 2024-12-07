package qz.build.provision.params;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

/**
 * Basic architecture parser
 *
 * Note: All aliases must be lowercase
 */
public enum Arch {
    X86("x32", "i386", "i486", "i586", "i686"),
    X86_64("amd64"),
    ARM32("arm", "armv1", "armv2", "armv3", "armv4", "armv5", "armv6", "armv7"),
    AARCH64("arm64", "armv8", "armv9"),
    RISCV32("rv32"),
    RISCV64("rv64"),
    PPC64("powerpc", "powerpc64"),
    ALL(), // special handling
    UNKNOWN();

    private HashSet<String> aliases = new HashSet<>();
    Arch(String ... aliases) {
        this.aliases.add(name().toLowerCase(Locale.ENGLISH));
        this.aliases.addAll(Arrays.asList(aliases));
    }

    public static Arch parseStrict(String input) throws UnsupportedOperationException {
        return EnumParser.parseStrict(Arch.class, input, ALL, UNKNOWN);
    }

    public static HashSet<Arch> parse(String input) {
        return EnumParser.parseSet(Arch.class, Arch.ALL, input);
    }

    public static Arch parse(String input, Arch fallback) {
        Arch found = bestMatch(input);
        return found == UNKNOWN ? fallback : found;
    }

    public static Arch bestMatch(String input) {
        if(input != null) {
            for(Arch arch : values()) {
                if (arch.aliases.contains(input.toLowerCase())) {
                    return arch;
                }
            }
        }
        return Arch.UNKNOWN;
    }

    public boolean matches(HashSet<Arch> archList) {
        return this == ALL || archList.contains(ALL) || (this != UNKNOWN && archList.contains(this));
    }

    public static String serialize(HashSet<Arch> archList) {
        if(archList.contains(ALL)) {
            return "*";
        }
        return StringUtils.join(archList, "|");
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }
}
