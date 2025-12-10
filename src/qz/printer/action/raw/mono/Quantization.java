package qz.printer.action.raw.mono;

public enum Quantization {
    BLACK, // color value must be the exact value of black
    ALPHA, // alpha is more than a set threshold is considered black (discarding color info)
    LUMA, // luma (or alpha) must be less than a set threshold to be considered black
    DITHER; // image is processed via a separate black & white dithering algorithm

    public static Quantization parse(String input) {
        for(Quantization quantization : Quantization.values()) {
            if (quantization.name().equalsIgnoreCase(input)) {
                return quantization;
            }
        }
        return BLACK;
    }
}