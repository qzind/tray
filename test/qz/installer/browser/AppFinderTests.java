package qz.installer.browser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.installer.certificate.firefox.locator.AppAlias;
import qz.installer.certificate.firefox.locator.AppLocator;

import java.util.ArrayList;
import java.util.Date;

public class AppFinderTests {
    private static final Logger log = LoggerFactory.getLogger(AppFinderTests.class);

    public static void main(String ... args) throws Exception {
        runTest(AppAlias.FIREFOX);
    }

    private static void runTest(AppAlias app) {
        Date begin = new Date();
        ArrayList<AppLocator> appList = AppLocator.locate(app);

        StringBuilder output = new StringBuilder("Found apps:\n");
        for (AppLocator info : appList) {
            output.append(String.format("      name: '%s', path: '%s', version: '%s'\n",
                                        info.getName(),
                                        info.getPath(),
                                        info.getVersion()
            ));
        }

        Date end = new Date();
        log.debug(output.toString());
        log.debug("Time to find find {}: {}s", app.name(), (end.getTime() - begin.getTime())/1000.0f);
    }
}