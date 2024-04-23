package qz.ws.substitutions;

import java.util.ArrayList;

public enum RootKey {
    OPTIONS(true, "options", "config"),
    PRINTER(true, "printer"),
    DATA(true, "data");

    private String key;
    private String[] alts;
    private boolean replaceAllowed;
    private ArrayList<RestrictedKey> restrictedKeys;

    RootKey(boolean replaceAllowed, String key, String... alts) {
        this.replaceAllowed = replaceAllowed;
        this.key = key;
        this.alts = alts;
        this.restrictedKeys = getRestrictedSubkeys();
    }

    public boolean isReplaceAllowed() {
        return replaceAllowed;
    }

    public boolean isSubkeyRestricted(String subkey) {
        for(RestrictedKey key : restrictedKeys) {
            if (key.getSubkey().equals(subkey)) {
                return true;
            }
        }
        return false;
    }

    public static RootKey parse(Object o) {
        for(RootKey root : values()) {
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

    public ArrayList<RestrictedKey> getRestrictedSubkeys() {
        if (restrictedKeys == null) {
            restrictedKeys = new ArrayList();
            for(RestrictedKey restricted : RestrictedKey.values()) {
                if (restricted.getParent() == this) {
                    restrictedKeys.add(restricted);
                }
            }
        }
        return restrictedKeys;
    }
}