package qz.printer.action;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.action.pdf.BookBundle;
import qz.utils.PrintingUtilities;

import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PrintPDFTests {
    private static final double DELTA = 0.001;
    private static final Path RESOURCES = Paths.get("test/qz/printer/action/resources");

    @Test
    public void testPortrait() throws Exception {
        try(PDDocument pdf = loadPdf("pdf-4x6.pdf")) {
            PageFormat format = applySize(pdf.getPage(0));

            assertSize(format, 288, 432);
        }
    }

    @Test
    public void testLandscape() {
        PageFormat format = applySize(new PDPage(new PDRectangle(432, 288)));

        assertSize(format, 432, 288);
    }

    @Test
    public void testCropBox() {
        PDPage page = new PDPage(new PDRectangle(288, 432));
        page.setCropBox(new PDRectangle(144, 216));

        PageFormat format = applySize(page);

        assertSize(format, 288, 432);
    }

    @Test
    public void testUserUnit() {
        PDPage page = new PDPage(new PDRectangle(144, 216));
        page.setUserUnit(2);

        PageFormat format = applySize(page);

        assertSize(format, 288, 432);
    }

    @Test
    public void testRotate90() {
        PDPage page = new PDPage(new PDRectangle(288, 432));
        page.setRotation(90);

        PageFormat format = applySize(page);

        assertSize(format, 288, 432);
    }

    @Test
    public void testRotate270() {
        PDPage page = new PDPage(new PDRectangle(288, 432));
        page.setRotation(270);

        PageFormat format = applySize(page);

        assertSize(format, 288, 432);
    }

    @Test
    public void testMargins() throws Exception {
        JSONObject options = new JSONObject().put("margins", 0.25);
        PrintOptions printOptions = new PrintOptions(options, new PrintOutput(null), PrintingUtilities.Format.PDF);
        PageFormat format = applySize(new PDPage(new PDRectangle(288, 432)));

        new PrintPDF().applyDefaultSettings(printOptions.getPixelOptions(), format, null);

        Paper paper = format.getPaper();
        assertSize(format, 288, 432);
        Assert.assertEquals(paper.getImageableX(), 18, DELTA);
        Assert.assertEquals(paper.getImageableY(), 18, DELTA);
        Assert.assertEquals(paper.getImageableWidth(), 252, DELTA);
        Assert.assertEquals(paper.getImageableHeight(), 396, DELTA);
    }

    @Test
    public void testZeroSize() {
        assertRejected(new PDPage(new PDRectangle(0, 432)));
    }

    @Test
    public void testInvertedBox() {
        PDRectangle box = new PDRectangle();
        box.setLowerLeftX(288);
        box.setUpperRightX(0);
        box.setUpperRightY(432);

        assertSize(applySize(new PDPage(box)), 288, 432);
    }

    @Test
    public void testNonFiniteSize() {
        PDRectangle box = new PDRectangle(288, 432);
        box.setUpperRightX(Float.NaN);

        assertRejected(new PDPage(box));
    }

    @Test
    public void testInheritedBox() throws Exception {
        try(PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage();
            pdf.addPage(page);
            COSDictionary parent = page.getCOSObject().getCOSDictionary(COSName.PARENT);
            parent.setItem(COSName.MEDIA_BOX, new PDRectangle(288, 432));
            page.getCOSObject().removeItem(COSName.MEDIA_BOX);

            assertSize(applySize(page), 288, 432);
        }
    }

    @Test
    public void testMissingBox() {
        PDPage page = new PDPage();
        page.getCOSObject().removeItem(COSName.MEDIA_BOX);

        assertSize(applySize(page), 612, 792);
    }

    @Test
    public void testOffsetBox() {
        PDRectangle box = new PDRectangle();
        box.setLowerLeftX(72);
        box.setLowerLeftY(144);
        box.setUpperRightX(360);
        box.setUpperRightY(576);

        assertSize(applySize(new PDPage(box)), 288, 432);
    }

    @Test
    public void testInvalidUserUnit() {
        PDPage page = new PDPage(new PDRectangle(288, 432));
        page.getCOSObject().setFloat(COSName.USER_UNIT, 0);

        assertSize(applySize(page), 288, 432);
    }

    @Test
    public void testMixedSizes() throws Exception {
        try(PDDocument pdf = loadPdf("pdf-mixed-sizes.pdf")) {
            Book book = bundlePages(pdf, 0, 1).wrapAndPresent();

            Assert.assertEquals(book.getNumberOfPages(), 2);
            assertSize(book.getPageFormat(0), 288, 432);
            assertSize(book.getPageFormat(1), 612, 792);
        }
    }

    @Test
    public void testFirstPage() throws Exception {
        try(PDDocument pdf = loadPdf("pdf-mixed-sizes.pdf")) {
            Book book = bundlePages(pdf, 0).wrapAndPresent();

            Assert.assertEquals(book.getNumberOfPages(), 1);
            assertSize(book.getPageFormat(0), 288, 432);
        }
    }

    @Test
    public void testLaterPage() throws Exception {
        try(PDDocument pdf = loadPdf("pdf-mixed-sizes.pdf")) {
            Book book = bundlePages(pdf, 1).wrapAndPresent();

            Assert.assertEquals(book.getNumberOfPages(), 1);
            assertSize(book.getPageFormat(0), 612, 792);
        }
    }

    @Test
    public void testDisjointPages() throws Exception {
        try(PDDocument pdf = loadPdf("pdf-mixed-sizes.pdf")) {
            Book book = bundlePages(pdf, 0, 2).wrapAndPresent();

            Assert.assertEquals(book.getNumberOfPages(), 2);
            assertSize(book.getPageFormat(0), 288, 432);
            assertSize(book.getPageFormat(1), 360, 504);
        }
    }

    @Test
    public void testBundleSubset() throws Exception {
        try(PDDocument pdf = loadPdf("pdf-mixed-sizes.pdf")) {
            Book book = bundlePages(pdf, 0, 1, 2).wrapAndPresent(1, 2);

            Assert.assertEquals(book.getNumberOfPages(), 2);
            assertSize(book.getPageFormat(0), 612, 792);
            assertSize(book.getPageFormat(1), 360, 504);
        }
    }

    private PageFormat applySize(PDPage page) {
        PageFormat format = new PageFormat();
        PrintPDF.applyInheritedSize(format, page);
        return format;
    }

    private BookBundle bundlePages(PDDocument pdf, int... pages) {
        BookBundle bundle = new BookBundle();
        for(int page : pages) {
            bundle.append((graphics, format, index) -> Printable.PAGE_EXISTS, applySize(pdf.getPage(page)));
        }
        return bundle;
    }

    private PDDocument loadPdf(String name) throws IOException {
        return Loader.loadPDF(RESOURCES.resolve(name).toFile());
    }

    private void assertSize(PageFormat format, double width, double height) {
        Assert.assertEquals(format.getPaper().getWidth(), width, DELTA);
        Assert.assertEquals(format.getPaper().getHeight(), height, DELTA);
    }

    private void assertRejected(PDPage page) {
        PageFormat format = new PageFormat();
        double width = format.getPaper().getWidth();
        double height = format.getPaper().getHeight();

        Assert.assertFalse(PrintPDF.applyInheritedSize(format, page));
        assertSize(format, width, height);
    }
}
