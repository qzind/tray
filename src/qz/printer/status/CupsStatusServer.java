package qz.printer.status;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Created by kyle on 4/27/17.
 */
public class CupsStatusServer {
    private static final Logger log = LoggerFactory.getLogger(CupsStatusServer.class);


    public static final List<Integer> CUPS_RSS_PORTS = Collections.unmodifiableList(Arrays.asList(8586, 8687, 8788, 8889));
    public static int cupsRSSPort = -1;
    private static Server server;

    public static void runServer() {
        CupsUtils.initCupsStuff();
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
                log.warn("Could not start CUPS event server on port {}, using fallback port.", p);
            }
            if (started) {
                break;
            }
        }
        if (!started) {
            log.warn("Could not start CUPS event server. No printer status changes will be reported.");
        }
    }
}

