package qz.printer.action;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.printer.PrintOptions;

public class PrintPixelTests {

    @DataProvider(name = "adjustedRotations")
    public Object[][] adjustedRotations() {
        return new Object[][] {
                {0, null, 0},
                {90, null, 90},
                {0, PrintOptions.Orientation.PORTRAIT, 0},
                {15, PrintOptions.Orientation.PORTRAIT, 15},
                {0, PrintOptions.Orientation.REVERSE_PORTRAIT, 180},
                {45, PrintOptions.Orientation.REVERSE_PORTRAIT, 225},
                {180, PrintOptions.Orientation.REVERSE_PORTRAIT, 360},
                {0, PrintOptions.Orientation.LANDSCAPE, 0}
        };
    }

    @Test(dataProvider = "adjustedRotations")
    public void adjustedRotationTests(double rotation, PrintOptions.Orientation orientation, double expected) {
        Assert.assertEquals(PrintPixel.getAdjustedRotation(rotation, orientation), expected);
    }

    @DataProvider(name = "landscapeOrientations")
    public Object[][] landscapeOrientations() {
        return new Object[][] {
                {null, false},
                {PrintOptions.Orientation.PORTRAIT, false},
                {PrintOptions.Orientation.REVERSE_PORTRAIT, false},
                {PrintOptions.Orientation.LANDSCAPE, true},
                {PrintOptions.Orientation.REVERSE_LANDSCAPE, true}
        };
    }

    @Test(dataProvider = "landscapeOrientations")
    public void landscapeOrientationTests(PrintOptions.Orientation orientation, boolean expected) {
        Assert.assertEquals(PrintPixel.isLandscapeOrientation(orientation), expected);
    }
}
