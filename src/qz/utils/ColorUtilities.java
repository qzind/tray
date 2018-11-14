/*
 * Copyright 2005 Joe Walker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package qz.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Utilities for working with colors.
 *
 * @author Joe Walker [joe at getahead dot ltd dot uk]
 */
public class ColorUtilities {

    private static final Logger log = LoggerFactory.getLogger(ColorUtilities.class);

    public static final Color DEFAULT_COLOR = Color.WHITE;

    /**
     * Decode an HTML color string like '#F567BA;' into a {@link Color}
     *
     * @param colorString The string to decode
     * @return The decoded color, or White if it cannot be parsed
     */
    public static Color decodeHtmlColorString(String colorString) {
        colorString = colorString.replaceAll("[#;\\s]", "");

        int red, green, blue;
        switch(colorString.length()) {
            case 6:
                red = Integer.parseInt(colorString.substring(0, 2), 16);
                green = Integer.parseInt(colorString.substring(2, 4), 16);
                blue = Integer.parseInt(colorString.substring(4, 6), 16);
                break;
            case 3:
                red = Integer.parseInt(colorString.substring(0, 1), 16);
                green = Integer.parseInt(colorString.substring(1, 2), 16);
                blue = Integer.parseInt(colorString.substring(2, 3), 16);
                break;
            case 1:
                red = green = blue = Integer.parseInt(colorString.substring(0, 1), 16);
                break;
            default:
                log.warn("Unable to read {} as a proper color string, returning default", colorString);
                return DEFAULT_COLOR;
        }

        return new Color(red, green, blue);
    }

    /**
     * Determines if the provided buffered image is black
     * @param bi BufferedImage to check for all black pixels
     * @return true if all black, false otherwise
     */
    public static boolean isBlack(BufferedImage bi) {
        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                int pixel = bi.getRGB(x, y);
                if (pixel>>24 != 0x00 && (pixel & 0x00FFFFFF) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Inverts the color of all pixels in an image
     * @param bi Source BufferedImage
     * @return New inverted BufferedImage
     */
    public static BufferedImage invert(BufferedImage bi) {
        BufferedImage inverted = new BufferedImage(bi.getWidth(), bi.getWidth(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                int pixel = bi.getRGB(x, y);
                int a = (pixel>>24)&0xFF;
                int r = 0xFF ^ ((pixel>>16)&0xFF);
                int g = 0xFF ^ ((pixel>>8)&0xFF);
                int b = 0xFF ^ ((pixel>>0)&0xFF);
                inverted.setRGB(x, y,  a << 24 | r  << 16 | g << 8 | b << 0);
            }
        }
        return inverted;
    }
}
