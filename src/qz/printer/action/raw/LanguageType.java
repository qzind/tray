/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.printer.action.raw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Enum for print languages, such as ZPL, EPL, etc.
 *
 * @author tfino
 */
public enum LanguageType {

    ZPL(false, true, 203, "ZPL", "ZPL2", "ZPLII", "ZEBRA"),
    EPL(true, true, 203, "EPL", "EPL2", "EPLII"),
    CPCL(false, true, 203),
    ESCP(false, false, 180, "ESCP", "ESCP2", "ESCPOS", "ESC", "ESC/P", "ESC/P2", "ESCP/P2", "ESC/POS", "ESC\\P", "EPSON"),
    EVOLIS(false, false, 300),
    SBPL(false, true, 203, "SATO"),
    UNKNOWN(false, false, 72);


    private boolean imgOutputInvert = false;
    private boolean imgWidthValidated = false;
    private double defaultDensity = 72;
    private List<String> altNames;

    LanguageType(boolean imgOutputInvert, boolean imgWidthValidated, double defaultDensity, String... altNames) {
        this.imgOutputInvert = imgOutputInvert;
        this.imgWidthValidated = imgWidthValidated;
        this.defaultDensity = defaultDensity;

        this.altNames = new ArrayList<>();
        Collections.addAll(this.altNames, altNames);
    }

    public static LanguageType getType(String type) {
        for(LanguageType lang : LanguageType.values()) {
            if (lang.name().equalsIgnoreCase(type) || lang.altNames.contains(type)) {
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

    public double getDefaultDensity() {
        return defaultDensity;
    }

}
