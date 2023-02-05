package qz.utils;

import org.apache.commons.lang3.StringUtils;

public class StringUtilities {

    public static final String[] HTML_ENTITIES = {"&", "<", ">", "\"", "'", "/"};
    public static final String[] HTML_REPLACED =  {"&amp;", "&lt;", "&gt;", "&quot;", "&apos;", "&sol;"};

    public static String escapeHtmlEntities(String text) {
        return StringUtils.replaceEach(text, HTML_ENTITIES, HTML_REPLACED);
    }
}
