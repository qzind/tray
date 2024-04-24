package qz.ws.substitutions;

public enum Type {
    OPTIONS("options", "config"),
    PRINTER("printer"),
    DATA("data");

    private String key;
    private boolean readOnly;
    private String[] alts;

    Type(String key, String ... alts) {
        this(key, false, alts);
    }
    Type(String key, boolean readOnly, String... alts) {
        this.key = key;
        this.readOnly = readOnly;
        this.alts = alts;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public static Type parse(Object o) {
        for(Type root : values()) {
            if (root.key.equals(o)) {
                return root;
            }
        }
        return null;
    }

    public String getKey() {
        return key;
    }

    public String[] getAlts() {
        return alts;
    }
}