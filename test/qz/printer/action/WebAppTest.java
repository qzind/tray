package qz.printer.action;

import javafx.print.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

public class WebAppTest {

    private static final Logger log = LoggerFactory.getLogger(WebAppTest.class);
    private static final int SPOOLER_WAIT = 2000; // millis

    public static void main(String[] args) throws Throwable {
        WebApp.initialize();

        // RASTER//

        boolean audit = false;
        if (args.length > 0) { audit = Boolean.parseBoolean(args[0]) || "1".equals(args[0]); }
        int knownHeightTests = 1000;
        if (args.length > 1) { knownHeightTests = Integer.parseInt(args[1]); }
        int fitToHeightTests = 1000;
        if (args.length > 2) { fitToHeightTests = Integer.parseInt(args[2]); }

        if (!testKnownSize(knownHeightTests, audit)) {
            log.error("Testing well defined sizes failed");
        } else if (!testFittedSize(fitToHeightTests, audit)) {
            log.error("Testing fit to height sizing failed");
        } else {
            log.info("All raster tests passed");
        }


        // VECTOR //

        int vectorKnownHeightPrints = 100;
        if (args.length > 3) { vectorKnownHeightPrints = Integer.parseInt(args[3]); }
        int vectorFittedHeightPrints = 100;
        if (args.length > 4) { vectorFittedHeightPrints = Integer.parseInt(args[4]); }

        if (!testVectorKnownPrints(vectorKnownHeightPrints)) {
            log.error("Failed vector prints with defined heights");
        } else if (!testVectorFittedPrints(vectorFittedHeightPrints)) {
            log.error("Failed vector prints with fit to height sizing");
        } else {
            log.info("All vector prints completed");
        }


        System.exit(0); //explicit exit since jfx is running in background
    }


    public static boolean testKnownSize(int trials, boolean enableAuditing) throws Throwable {
        for(int i = 0; i < trials; i++) {
            //new size every run
            double printW = Math.max(2, (int)(Math.random() * 110) / 10d) * 72d;
            double printH = Math.max(3, (int)(Math.random() * 110) / 10d) * 72d;
            double zoom = Math.max(0.5d, (int)(Math.random() * 30) / 10d);

            String id = "known-" + i;
            WebAppModel model = buildModel(id, printW, printH, zoom, true, (int)(Math.random() * 360));
            BufferedImage sample = WebApp.raster(model);

            if (sample == null) {
                log.error("Failed to create capture");
                return false;
            }

            //TODO - check bottom right matches expected color
            //check capture for dimensional accuracy within 1 pixel of expected (due to int rounding)
            int expectedWidth = (int)Math.round(printW * (96d / 72d) * zoom);
            int expectedHeight = (int)Math.round(printH * (96d / 72d) * zoom);
            boolean audit = enableAuditing && Math.random() < 0.1;
            boolean passed = true;

            if (!Arrays.asList(expectedWidth, expectedWidth + 1, expectedWidth - 1).contains(sample.getWidth())) {
                log.error("Expected width to be {} but got {}", expectedWidth, sample.getWidth());
                audit = true;
                passed = false;
            }
            if (!Arrays.asList(expectedHeight, expectedHeight + 1, expectedHeight - 1).contains(sample.getHeight())) {
                log.error("Expected height to be {} but got {}", expectedHeight, sample.getHeight());
                audit = true;
                passed = false;
            }

            if (audit) {
                saveAudit(passed? id:"invalid", sample);
            }
            if (!passed) {
                return false;
            }
        }

        return true;
    }

    public static boolean testFittedSize(int trials, boolean enableAuditing) throws Throwable {
        for(int i = 0; i < trials; i++) {
            //new size every run (height always starts at 0)
            double printW = Math.max(2, (int)(Math.random() * 110) / 10d) * 72d;
            double zoom = Math.max(0.5d, (int)(Math.random() * 30) / 10d);

            String id = "fitted-" + i;
            WebAppModel model = buildModel(id, printW, 0, zoom, true, (int)(Math.random() * 360));
            BufferedImage sample = WebApp.raster(model);

            if (sample == null) {
                log.error("Failed to create capture");
                return false;
            }

            //TODO - check bottom right matches expected color
            //check capture for dimensional accuracy within 1 pixel of expected (due to int rounding)
            //expected height is not known for these tests
            int expectedWidth = (int)Math.round(printW * (96d / 72d) * zoom);
            boolean audit = enableAuditing && Math.random() < 0.1;
            boolean passed = true;

            if (!Arrays.asList(expectedWidth, expectedWidth + 1, expectedWidth - 1).contains(sample.getWidth())) {
                log.error("Expected width to be {} but got {}", expectedWidth, sample.getWidth());
                audit = true;
                passed = false;
            }

            if (audit) {
                saveAudit(passed? id:"invalid", sample);
            }
            if (!passed) {
                return false;
            }
        }

        return true;
    }

    public static boolean testVectorKnownPrints(int trials) throws Throwable {
        PrinterJob job = buildVectorJob("vector-test-known");
        for(int i = 0; i < trials; i++) {
            //new size every run
            double printW = Math.max(2, (int)(Math.random() * 85) / 10d) * 72d;
            double printH = Math.max(3, (int)(Math.random() * 110) / 10d) * 72d;

            String id = "known-" + i;
            WebAppModel model = buildModel(id, printW, printH, 1, false, (int)(Math.random() * 360));

            WebApp.print(job, model);
        }
        job.endJob();

        try {
            log.info("Waiting {} seconds for the spooler to catch up.", SPOOLER_WAIT/1000);
            Thread.sleep(SPOOLER_WAIT);
        }
        catch(InterruptedException ignore) {}

        return job.getJobStatus() != PrinterJob.JobStatus.ERROR;
    }

    public static boolean testVectorFittedPrints(int trials) throws Throwable {
        PrinterJob job = buildVectorJob("vector-test-fitted");
        for(int i = 0; i < trials; i++) {
            //new size every run
            double printW = Math.max(2, (int)(Math.random() * 85) / 10d) * 72d;

            String id = "fitted-" + i;
            WebAppModel model = buildModel(id, printW, 0, 1, false, (int)(Math.random() * 360));

            WebApp.print(job, model);
        }
        job.endJob();

        try {
            log.info("Waiting {} seconds for the spooler to catch up.", SPOOLER_WAIT/1000);
            Thread.sleep(SPOOLER_WAIT);
        }
        catch(InterruptedException ignore) {}

        return job.getJobStatus() != PrinterJob.JobStatus.ERROR;
    }

    private static WebAppModel buildModel(String index, double width, double height, double zoom, boolean scale, int hue) {
        int level = (int)(Math.random() * 50) + 25;
        WebAppModel model = new WebAppModel("<html>" +
                                                    "<body style='background-color: hsl(" + hue + "," + level + "%," + level + "%);'>" +
                                                    "   <table style='font-family: monospace; border: 1px;'>" +
                                                    "       <tr style='height: 6cm;'>" +
                                                    "           <td valign='top'>Generated content:</td>" +
                                                    "           <td valign='top'><b>" + index + "</b></td>" +
                                                    "       </tr>" +
                                                    "       <tr>" +
                                                    "           <td>Content size:</td>" +
                                                    "           <td>" + width + "x" + height + "</td>" +
                                                    "       </tr>" +
                                                    "       <tr>" +
                                                    "           <td>Zoomed to</td>" +
                                                    "           <td>x " + zoom + "</td>" +
                                                    "       </tr>" +
                                                    "   </table>" +
                                                    "</body>" +
                                                    "</html>",
                                            true, width, height, scale, zoom);

        log.trace("Generating #{} = [({},{}), x{}]", index, model.getWebWidth(), model.getWebHeight(), model.getZoom());

        return model;
    }

    private static PrinterJob buildVectorJob(String name) throws Throwable {
        Printer defaultPrinter = Printer.getDefaultPrinter();
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
        settings.setJobName(name);

        return job;
    }

    private static void saveAudit(String id, BufferedImage capture) throws IOException {
        File temp = File.createTempFile("qz-" + id, ".png");
        ImageIO.write(capture, "png", temp);

        log.info("Sampled {}: {}", id, temp.getName());
    }

}
