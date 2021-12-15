package qz.installer.browser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.certificate.firefox.FirefoxCertificateInstaller;
import qz.installer.certificate.firefox.locator.AppAlias;
import qz.installer.certificate.firefox.locator.AppInfo;
import qz.installer.certificate.firefox.locator.AppLocator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;

public class AppFinderTests {
    private static final Logger log = LogManager.getLogger(AppFinderTests.class);

    public static void main(String ... args) throws Exception {
        runTest(AppAlias.FIREFOX);
    }

    private static void runTest(AppAlias app) {
        Date begin = new Date();
        ArrayList<AppInfo> appList = AppLocator.getInstance().locate(app);
        ArrayList<Path> runningPaths = AppLocator.getRunningPaths(appList);

        StringBuilder output = new StringBuilder("Found apps:\n");
        for (AppInfo appInfo : appList) {
            output.append(String.format("      name: '%s', path: '%s', exePath: '%s', version: '%s'\n",
                                        appInfo.getAlias().getName(),
                                        appInfo.getPath(),
                                        appInfo.getExePath(),
                                        appInfo.getVersion()
            ));

            if(runningPaths.contains(appInfo.getExePath())) {
                FirefoxCertificateInstaller.issueRestartWarning(runningPaths, appInfo);
            }
        }

        Date end = new Date();
        log.debug(output.toString());
        log.debug("Time to find and execute {}: {}s", app.name(), (end.getTime() - begin.getTime())/1000.0f);
    }
}
