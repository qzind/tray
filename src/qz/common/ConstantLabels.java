package qz.common;

import static qz.common.I18NLoader.gettext;

public class ConstantLabels {

    public static final String TRUSTED_PUBLISHER = String.format(gettext("Verified by %s"), Constants.ABOUT_COMPANY);
    public static final String UNTRUSTED_PUBLISHER = gettext("Untrusted website");

    public static final String WHITE_LIST = gettext("Permanently allowed \"%s\" to access local resources");
    public static final String BLACK_LIST = gettext("Permanently blocked \"%s\" from accessing local resources");
    public static final String BLACK_LIST_PROMPT = gettext("Permanently block \"%s\" from accessing local resources?");

    public static final String WHITE_SITES = gettext("Sites permanently allowed access");
    public static final String BLACK_SITES = gettext("Sites permanently blocked from access");

    public static final String HTML_PRINT = String.format(gettext("%s HTML Print"), Constants.ABOUT_TITLE);
    public static final String PDF_PRINT = String.format(gettext("%s PDF Print"), Constants.ABOUT_TITLE);
    public static final String IMAGE_PRINT = String.format(gettext("%s Pixel Print"), Constants.ABOUT_TITLE);
    public static final String RAW_PRINT = String.format(gettext("%s Raw Print"), Constants.ABOUT_TITLE);

    public static final String ALLOWED = gettext("Allowed");
    public static final String BLOCKED = gettext("Blocked");
}
