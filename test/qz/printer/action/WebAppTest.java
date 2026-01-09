package qz.printer.action;

import javafx.print.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.printer.action.html.WebApp;
import qz.printer.action.html.WebAppModel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class WebAppTest {

    private static final Logger log = LogManager.getLogger(WebAppTest.class);
    private static final int SPOOLER_WAIT = 2000; // millis
    private static final Path RASTER_OUTPUT_DIR = Paths.get("./out"); // see ant ${out.dir}
    private static final String RASTER_OUTPUT_FORMAT = "png";

    public static void main(String[] args) {
        try {
            WebApp.initialize();
            cleanup();

            // RASTER//

            int rasterKnownHeightTests = 1000;
            if (args.length > 1) { rasterKnownHeightTests = Integer.parseInt(args[1]); }
            int rasterFittedHeightTests = 1000;
            if (args.length > 2) { rasterFittedHeightTests = Integer.parseInt(args[2]); }

            if (!testRasterKnownSize(rasterKnownHeightTests)) {
                log.error("Testing well defined sizes failed");
            } else if (!testRasterFittedSize(rasterFittedHeightTests)) {
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
        }
        catch(Throwable t) {
            log.error("Tests failed due to an exception", t);
        }

        System.exit(0); //explicit exit since jfx is running in background
    }


    public static boolean testRasterKnownSize(int trials) throws Throwable {
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
            boolean passed = true;

            if (!Arrays.asList(expectedWidth, expectedWidth + 1, expectedWidth - 1).contains(sample.getWidth())) {
                log.error("Expected width to be {} but got {}", expectedWidth, sample.getWidth());
                passed = false;
            }
            if (!Arrays.asList(expectedHeight, expectedHeight + 1, expectedHeight - 1).contains(sample.getHeight())) {
                log.error("Expected height to be {} but got {}", expectedHeight, sample.getHeight());
                passed = false;
            }

            saveAudit(passed? id:"invalid", sample);

            if (!passed) {
                return false;
            }
        }

        return true;
    }

    public static boolean testRasterFittedSize(int trials) throws Throwable {
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
            boolean passed = true;

            if (!Arrays.asList(expectedWidth, expectedWidth + 1, expectedWidth - 1).contains(sample.getWidth())) {
                log.error("Expected width to be {} but got {}", expectedWidth, sample.getWidth());
                passed = false;
            }

            saveAudit(passed? id:"invalid", sample);

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
            log.info("Waiting {} seconds for the spooler to catch up.", SPOOLER_WAIT / 1000);
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
            log.info("Waiting {} seconds for the spooler to catch up.", SPOOLER_WAIT / 1000);
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
                                                    "           <td>Physical size:</td>" +
                                                    "           <td>" + (width / 72d) + "x" + (height / 72d) + "</td>" +
                                                    "       </tr>" +
                                                    "       <tr>" +
                                                    "           <td>Zoomed to</td>" +
                                                    "           <td>x " + zoom + "</td>" +
                                                    "       </tr>" +
                                                    "   </table>" +
                                                    "</body>" +
                                                    "</html>",
                                            true, width, height, scale, zoom);

        log.trace("Generating #{} = [({},{}), x{}]", index, model.getWidth(), model.getHeight(), model.getZoom());

        return model;
    }

    private static PrinterJob buildVectorJob(String name) throws Throwable {
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
        settings.setJobName(name);

        return job;
    }

    private static void cleanup() {
        File[] files;
        if ((files = RASTER_OUTPUT_DIR.toFile().listFiles()).length > 0) {
            for(File file : files) {
                if (file.getName().endsWith("." + RASTER_OUTPUT_FORMAT)
                        && file.getName().startsWith(String.format("%s-", Constants.DATA_DIR))) {
                    if (!file.delete()) {
                        log.warn("Could not delete {}", file);
                    }
                }
            }
        }
    }

    private static void saveAudit(String id, BufferedImage capture) throws IOException {
        Path image = RASTER_OUTPUT_DIR.resolve(String.format("%s-%s.%s", Constants.DATA_DIR, id, RASTER_OUTPUT_FORMAT));
        ImageIO.write(capture, RASTER_OUTPUT_FORMAT, image.toFile());
        log.info("Wrote {}: {}", id, image);
    }

}
