package qz.ws.substitutions;

public enum RestrictedKey {
    COPIES("copies", RootKey.OPTIONS),
    DATA("data", RootKey.DATA);
    private String subkey;
    private RootKey parent;
    RestrictedKey(String subkey, RootKey parent) {
        this.subkey = subkey;
        this.parent = parent;
    }

    public RootKey getParent() {
        return parent;
    }

    public String getSubkey() {
        return subkey;
    }
}