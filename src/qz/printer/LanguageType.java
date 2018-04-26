/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.printer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Enum for print languages, such as ZPL, EPL, etc.
 *
 * @author tfino
 */
public enum LanguageType {

    ZPLII(false, true, "ZEBRA", "ZPL2"),
    ZPL(false, true),
    EPL2(true, true, "EPLII"),
    EPL(true, true),
    CPCL(false, true),
    ESCP(false, false, "ESC", "ESC/P"),
    ESCP2(false, false, "ESC/P2"),
    ESCPOS(false, false, "ESC/POS", "EPSON"),
    EVOLIS(false, false),
    UNKNOWN(false, false);


    private boolean imgOutputInvert = false;
    private boolean imgWidthValidated = false;
    private List<String> altNames;

    LanguageType(boolean imgOutputInvert, boolean imgWidthValidated, String... altNames) {
        this.imgOutputInvert = imgOutputInvert;
        this.imgWidthValidated = imgWidthValidated;

        this.altNames = new ArrayList<>();
        Collections.addAll(this.altNames, altNames);
    }

    public static LanguageType getType(String type) {
        for(LanguageType lang : LanguageType.values()) {
            if (type.equalsIgnoreCase(lang.name()) || lang.altNames.contains(type)) {
                return lang;
            }
        }

        return UNKNOWN;
    }


    /**
     * Returns whether or not this {@code LanguageType}
     * inverts the black and white pixels before sending to the printer.
     *
     * @return {@code true} if language type flips black and white pixels
     */
    public boolean requiresImageOutputInverted() {
        return imgOutputInvert;
    }

    /**
     * Returns whether or not the specified {@code LanguageType} requires
     * the image width to be validated prior to processing output.  This
     * is required for image formats that normally require the image width to
     * be a multiple of 8
     *
     * @return {@code true} if the printer requires image width validation
     */
    public boolean requiresImageWidthValidated() {
        return imgWidthValidated;
    }
}
