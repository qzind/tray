package qz.build.provision.params.types;

import org.apache.commons.lang3.StringUtils;
import qz.build.provision.params.EnumParser;
import qz.common.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public enum Remover {
    QZ("QZ Tray", "qz-tray", "qz"),
    CUSTOM(null, null, null); // reserved

    private String aboutTitle;
    private String propsFile;
    private String dataDir;

    Remover(String aboutTitle, String propsFile, String dataDir) {
        this.aboutTitle = aboutTitle;
        this.propsFile = propsFile;
        this.dataDir = dataDir;
    }

    public String getAboutTitle() {
        return aboutTitle;
    }

    public String getPropsFile() {
        return propsFile;
    }

    public String getDataDir() {
        return dataDir;
    }

    public static String valuesDelimited(String delimiter) {
        ArrayList<Remover> listing = new ArrayList<>(Arrays.asList(values()));
        listing.remove(CUSTOM);
        return StringUtils.join(listing, delimiter).toLowerCase(Locale.ENGLISH);
    }

    /**
     * Defaults to custom if not found
     */
    public static Remover parse(String input) {
        Remover remover = EnumParser.parse(Remover.class, input);
        if(remover == CUSTOM) {
            throw new UnsupportedOperationException("Remover 'custom' is reserved for internal purposes");
        }
        if(remover == null) {
            remover = CUSTOM;
        }
        return remover;
    }

    public boolean matchesCurrentSystem() {
        return Constants.ABOUT_TITLE.equals(aboutTitle) ||
                Constants.PROPS_FILE.equals(propsFile) ||
                Constants.DATA_DIR.equals(dataDir);
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }
}
