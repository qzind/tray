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

import org.codehaus.jettison.json.JSONObject;
import qz.common.Sluggable;
import qz.printer.action.raw.converter.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Enum for print languages, such as ZPL, EPL, etc.
 */
public enum LanguageType implements Sluggable {
    CPCL(Cpcl::new, false, true, 203, "COMTEC"),
    CPL(null /* TODO */, 203, "COGNITIVE"),
    DPL(null /* TODO */, 300, "DATAMAX"),
    EPL(Epl::new, true, true, 203, "ELTRON", "EPL", "EPL2", "EPLII"),
    ESCP(null /* TODO */, 180, "ESC/P2", "ESC/P", "ESCP2"),
    ESCPOS(EscPos::new, false, true, 180, "ESC/POS"),
    EVOLIS(Evolis::new, 300),
    FGL(null /* TODO */, 203, "BOCA"),
    JSCRIPT(null /* TODO */, 300, "CAB"),
    LP(null /* TODO */, 203, "LABELPOINT", "LPII"),
    PGL(Pgl::new, 203, "IGP/PGL", "PRINTRONIX"),
    SBPL(Sbpl::new, false, true, 203, "SATO"),
    STAR(null /* TODO */, 203),
    ZPL(Zpl::new, false, true, 203, "ZEBRA", "ZPL", "ZPL2", "ZPLII"),
    UNKNOWN(null, 72);

    private final Supplier<ImageConverter> supplier;
    private final boolean imgOutputInvert;
    private final boolean imgWidthValidated;
    private final double defaultDensity;
    private final List<String> altNames;

    LanguageType(Supplier<ImageConverter> supplier, double defaultDensity, String ... altNames) {
        this(supplier, false, false, defaultDensity, altNames);
    }

    LanguageType(Supplier<ImageConverter> supplier, boolean imgOutputInvert, boolean imgWidthValidated, double defaultDensity, String... altNames) {
        this.supplier = supplier;
        this.imgOutputInvert = imgOutputInvert;
        this.imgWidthValidated = imgWidthValidated;
        this.defaultDensity = defaultDensity;
        this.altNames = new ArrayList<>();
        Collections.addAll(this.altNames, altNames);
    }

    public ImageConverter newImageConverter(BufferedImage img, JSONObject opt) {
        if(supplier != null) {
            ImageConverter converter = supplier.get();
            converter.setLanguageType(this);
            converter.setParams(opt);
            converter.setBufferedImage(img);
            return converter;
        }
        throw new MissingImageConverterException("ImageConverter missing for LanguageType: " + this);
    }

    public static LanguageType parse(String type) {
        for(LanguageType lang : LanguageType.values()) {
            if (lang.name().equalsIgnoreCase(type) || lang.altNames.contains(type)) {
                return lang;
            }
        }

        return UNKNOWN;
    }

    /**
     * Returns whether this {@code LanguageType}
     * inverts the black and white pixels before sending to the printer.
     *
     * @return {@code true} if language type flips black and white pixels
     */
    public boolean requiresImageOutputInverted() {
        return imgOutputInvert;
    }

    /**
     * Returns whether the specified {@code LanguageType} requires
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

    @Override
    public String slug() {
        return Sluggable.slugOf(this);
    }
}
