package qz.printer.action.html;

import javafx.print.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Tests WebApp's ability to print a consistent height
 */
public class HtmlVectorHeightTests {
    private static final Logger log = LogManager.getLogger(HtmlVectorHeightTests.class);
    private static final int SPOOLER_WAIT = 2000; // millis

    @DataProvider
    public Object[][] getHtmlSamples() {
        int attempts = 20;
        Object[][] data = new Object[attempts][2];
        for (int i = 0; i < attempts; i++) {
            data[i] = new Object[] { "resources/pbj-receipt.html", 525 };
        }
        return data;
    }

    @Test(dataProvider = "getHtmlSamples")
    public void testVectorHeight(String resource, double expectedSize) throws Throwable {
        WebApp.initialize();
        PrinterJob job = buildVectorJob("test-vector-height");
        WebAppModel model = new WebAppModel(getClass().getResource(resource).toString(), false, job.getPrinter().getDefaultPageLayout().getPrintableWidth(), 0, true, 1);
        WebApp.print(job, model);
        job.endJob();

        Assert.assertEquals(WebApp.pageHeight, expectedSize);
    }

    @AfterClass
    public void cleanUp() {
        try {
            log.info("Waiting {} seconds for the spooler to catch up.", SPOOLER_WAIT / 1000);
            Thread.sleep(SPOOLER_WAIT);
        }
        catch(InterruptedException ignore) {}
    }

    private static PrinterJob buildVectorJob(String jobName) throws Throwable {
        // Get "PDF" printer
        Printer defaultPrinter = Printer.getAllPrinters().stream().filter(printer -> printer.getName().contains("PDF")).findFirst().get();
        PrinterJob job = PrinterJob.createPrinterJob(defaultPrinter);

        // All this to remove margins
        Constructor<PageLayout> plCon = PageLayout.class.getDeclaredConstructor(Paper.class, PageOrientation.class, double.class, double.class, double.class, double.class);
        plCon.setAccessible(true);

        Paper paper = defaultPrinter.getDefaultPageLayout().getPaper();
        PageLayout layout = plCon.newInstance(paper, PageOrientation.PORTRAIT, 0, 0, 0, 0);

        Field field = defaultPrinter.getClass().getDeclaredField("defPageLayout");
        field.setAccessible(true);
        field.set(defaultPrinter, layout);

        JobSettings settings = job.getJobSettings();
        settings.setPageLayout(layout);
        settings.setJobName(jobName);

        return job;
    }

}
