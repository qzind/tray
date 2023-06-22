/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package qz.printer.rendering;

import java.awt.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

/**
 * FontManager class pulled from PDFBOX 1.8
 * with the help of Alexander Scherbatiy
 */

public class FontManager {
    // HashMap with all known fonts
    private static HashMap<String,java.awt.Font> envFonts = new HashMap<>();
    private static Properties fontMapping = new Properties();

    static {
        loadFonts();
        loadBasefontMapping();
        loadFontMapping();
    }

    private FontManager() {}

    /**
     * Get the font for the given fontname.
     *
     * @param font The name of the font.
     * @return The font we are looking for or a similar font or null if nothing is found.
     */
    public static java.awt.Font getAwtFont(String font) {
        String fontname = normalizeFontname(font);
        if (envFonts.containsKey(fontname)) {
            return envFonts.get(fontname);
        }

        return null;
    }

    /**
     * Load all available fonts from the environment.
     */
    private static void loadFonts() {
        for(Font font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
            String family = normalizeFontname(font.getFamily());
            String psname = normalizeFontname(font.getPSName());

            if (isBoldItalic(font)) {
                envFonts.put(family + "bolditalic", font);
            } else if (isBold(font)) {
                envFonts.put(family + "bold", font);
            } else if (isItalic(font)) {
                envFonts.put(family + "italic", font);
            } else {
                envFonts.put(family, font);
            }

            if (!family.equals(psname)) {
                envFonts.put(normalizeFontname(font.getPSName()), font);
            }
        }
    }

    /**
     * Normalize the fontname.
     *
     * @param fontname The name of the font.
     * @return The normalized name of the font.
     */
    private static String normalizeFontname(String fontname) {
        // Terminate all whitespaces, commas and hyphens
        String normalizedFontname = fontname.toLowerCase().replaceAll(" ", "").replaceAll(",", "").replaceAll("-", "");
        // Terminate trailing characters up to the "+".
        // As far as I know, these characters are used in names of embedded fonts
        // If the embedded font can't be read, we'll try to find it here
        if (normalizedFontname.contains("+")) {
            normalizedFontname = normalizedFontname.substring(normalizedFontname.indexOf("+") + 1);
        }
        // normalize all kinds of fonttypes. There are several possible version which have to be normalized
        // e.g. Arial,Bold Arial-BoldMT Helevtica-oblique ...
        boolean isBold = normalizedFontname.contains("bold");
        boolean isItalic = normalizedFontname.contains("italic") || normalizedFontname.contains("oblique");
        normalizedFontname = normalizedFontname.toLowerCase().replaceAll("bold", "")
                .replaceAll("italic", "").replaceAll("oblique", "");
        if (isBold) {
            normalizedFontname += "bold";
        }
        if (isItalic) {
            normalizedFontname += "italic";
        }

        return normalizedFontname;
    }


    /**
     * Add a font-mapping.
     *
     * @param font       The name of the font.
     * @param mappedName The name of the mapped font.
     */
    private static boolean addFontMapping(String font, String mappedName) {
        String fontname = normalizeFontname(font);
        // is there already a font mapping ?
        if (envFonts.containsKey(fontname)) {
            return false;
        }
        String mappedFontname = normalizeFontname(mappedName);
        // is the mapped font available ?
        if (!envFonts.containsKey(mappedFontname)) {
            return false;
        }
        envFonts.put(fontname, envFonts.get(mappedFontname));
        return true;
    }

    /**
     * Load the mapping for the well knwon font-substitutions.
     */
    private static void loadFontMapping() {
        boolean addedMapping = true;
        // There could be some recursive mappings in the fontmapping, so that we have to
        // read the list until no more additional mapping is added to it
        while(addedMapping) {
            int counter = 0;
            Enumeration<Object> keys = fontMapping.keys();
            while(keys.hasMoreElements()) {
                String key = (String)keys.nextElement();
                if (addFontMapping(key, (String)fontMapping.get(key))) {
                    counter++;
                }
            }
            if (counter == 0) {
                addedMapping = false;
            }
        }
    }

    /**
     * Mapping for the basefonts.
     */
    private static void loadBasefontMapping() {
        // use well known substitutions if the environments doesn't provide native fonts for the 14 standard fonts
        // Times-Roman -> Serif
        if (!addFontMapping("Times-Roman", "TimesNewRoman")) {
            addFontMapping("Times-Roman", "Serif");
        }
        if (!addFontMapping("Times-Bold", "TimesNewRoman,Bold")) {
            addFontMapping("Times-Bold", "Serif.bold");
        }
        if (!addFontMapping("Times-Italic", "TimesNewRoman,Italic")) {
            addFontMapping("Times-Italic", "Serif.italic");
        }
        if (!addFontMapping("Times-BoldItalic", "TimesNewRoman,Bold,Italic")) {
            addFontMapping("Times-BoldItalic", "Serif.bolditalic");
        }
        // Helvetica -> SansSerif
        if (!addFontMapping("Helvetica", "Helvetica")) {
            addFontMapping("Helvetica", "SansSerif");
        }
        if (!addFontMapping("Helvetica-Bold", "Helvetica,Bold")) {
            addFontMapping("Helvetica-Bold", "SansSerif.bold");
        }
        if (!addFontMapping("Helvetica-Oblique", "Helvetica,Italic")) {
            addFontMapping("Helvetica-Oblique", "SansSerif.italic");
        }
        if (!addFontMapping("Helvetica-BoldOblique", "Helvetica,Bold,Italic")) {
            addFontMapping("Helvetica-BoldOblique", "SansSerif.bolditalic");
        }
        // Courier -> Monospaced
        if (!addFontMapping("Courier", "Courier")) {
            addFontMapping("Courier", "Monospaced");
        }
        if (!addFontMapping("Courier-Bold", "Courier,Bold")) {
            addFontMapping("Courier-Bold", "Monospaced.bold");
        }
        if (!addFontMapping("Courier-Oblique", "Courier,Italic")) {
            addFontMapping("Courier-Oblique", "Monospaced.italic");
        }
        if (!addFontMapping("Courier-BoldOblique", "Courier,Bold,Italic")) {
            addFontMapping("Courier-BoldOblique", "Monospaced.bolditalic");
        }
        // some well known (??) substitutions found on fedora linux
        addFontMapping("Symbol", "StandardSymbolsL");
        addFontMapping("ZapfDingbats", "Dingbats");
    }

    /**
     * Try to determine if the font has both a BOLD and an ITALIC-type.
     *
     * @param font The font.
     * @return font has BOLD and ITALIC-type or not
     */
    private static boolean isBoldItalic(java.awt.Font font) {
        return isBold(font) && isItalic(font);
    }

    /**
     * Try to determine if the font has a BOLD-type.
     *
     * @param font The font.
     * @return font has BOLD-type or not
     */
    private static boolean isBold(java.awt.Font font) {
        String name = font.getName().toLowerCase();
        if (name.contains("bold")) {
            return true;
        }

        String psname = font.getPSName().toLowerCase();
        return psname.contains("bold");
    }

    /**
     * Try to determine if the font has an ITALIC-type.
     *
     * @param font The font.
     * @return font has ITALIC-type or not
     */
    private static boolean isItalic(java.awt.Font font) {
        String name = font.getName().toLowerCase();
        // oblique is the same as italic
        if (name.contains("italic") || name.contains("oblique")) {
            return true;
        }

        String psname = font.getPSName().toLowerCase();
        return psname.contains("italic") || psname.contains("oblique");
    }

}
