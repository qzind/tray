package qz.printer.action;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.printer.PrintOptions;

public class PrintImageTests {

    @DataProvider(name = "imageRotations")
    public Object[][] imageRotations() {
        return new Object[][] {
                {0, null, false, 0},
                {90, null, false, 90},
                {0, PrintOptions.Orientation.PORTRAIT, false, 0},
                {15, PrintOptions.Orientation.PORTRAIT, false, 15},
                {0, PrintOptions.Orientation.REVERSE_PORTRAIT, false, 180},
                {45, PrintOptions.Orientation.REVERSE_PORTRAIT, false, 225},
                {180, PrintOptions.Orientation.REVERSE_PORTRAIT, false, 360},
                {0, PrintOptions.Orientation.REVERSE_LANDSCAPE, false, 0},
                {0, PrintOptions.Orientation.REVERSE_LANDSCAPE, true, 180},
                {30, PrintOptions.Orientation.REVERSE_LANDSCAPE, true, 210}
        };
    }

    @Test(dataProvider = "imageRotations")
    public void imageRotationTests(double rotation, PrintOptions.Orientation orientation, boolean mac, double expected) {
        Assert.assertEquals(PrintImage.getImageRotation(rotation, orientation, mac), expected);
    }
}
