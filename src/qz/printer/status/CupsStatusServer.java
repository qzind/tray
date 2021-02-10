package qz.printer.status;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Created by kyle on 4/27/17.
 */
public class CupsStatusServer {
    private static final Logger log = LoggerFactory.getLogger(CupsStatusServer.class);

    public static final List<Integer> CUPS_RSS_PORTS = Collections.unmodifiableList(Arrays.asList(Constants.CUPS_RSS_PORTS));

    public static int cupsRSSPort = -1;
    private static Server server;

    public static synchronized void runServer() {
        CupsUtils.initCupsHttp();
        CupsUtils.clearSubscriptions();
        boolean started = false;
        for(int p = 0; p < CUPS_RSS_PORTS.size(); p++) {
            server = new Server(CUPS_RSS_PORTS.get(p));
            server.setHandler(new CupsStatusHandler());

            try {
                server.start();
                cupsRSSPort = CUPS_RSS_PORTS.get(p);
                CupsUtils.startSubscription(cupsRSSPort);
                started = true;
            }
            catch(Exception e) {
                log.warn("Could not start CUPS status server on port {}, using fallback port.", p);
            }
            if (started) {
                break;
            }
        }
        if (!started) {
            log.warn("Could not start CUPS status server. No printer status changes will be reported.");
        }
    }

    public static synchronized boolean isRunning() {
        return server != null && server.isRunning();
    }

    public static synchronized void stopServer() {
        if (server != null) {
            CupsUtils.freeIppObjs();
            server.setStopTimeout(10000);
            new Thread(() -> {
                try {
                    log.warn("Stopping CUPS status server");
                    server.stop();
                }
                catch(Exception ex) {
                    log.warn("Failed to stop CUPS status server.");
                }
            }).start();
        }
    }
}

