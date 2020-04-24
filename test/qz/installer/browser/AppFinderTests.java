package qz.installer.browser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.installer.Installer;
import qz.installer.certificate.firefox.FirefoxCertificateInstaller;
import qz.installer.certificate.firefox.locator.AppAlias;
import qz.installer.certificate.firefox.locator.AppInfo;
import qz.installer.certificate.firefox.locator.AppLocator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;

public class AppFinderTests {
    private static final Logger log = LoggerFactory.getLogger(AppFinderTests.class);

    public static void main(String ... args) throws Exception {
        runTest(AppAlias.FIREFOX);
    }

    private static void runTest(AppAlias app) {
        Date begin = new Date();
        ArrayList<AppInfo> appList = AppLocator.getInstance().locate(app);
        ArrayList<Path> processPaths = AppLocator.getRunningPaths(appList);

        StringBuilder output = new StringBuilder("Found apps:\n");
        for (AppInfo appInfo : appList) {
            output.append(String.format("      name: '%s', path: '%s', exePath: '%s', version: '%s'\n",
                                        appInfo.getName(),
                                        appInfo.getPath(),
                                        appInfo.getExePath(),
                                        appInfo.getVersion()
            ));

            if(processPaths.contains(appInfo.getExePath())) {
                if (appInfo.getVersion().greaterThanOrEqualTo(FirefoxCertificateInstaller.FIREFOX_RESTART_VERSION)) {
                    try {
                        Installer.getInstance().spawn(appInfo.getExePath().toString(), "-private", "about:restartrequired");
                        continue;
                    } catch(Exception ignore) {}
                }
                log.warn("{} must be restarted for changes to take effect", appInfo.getName());
            }
        }

        Date end = new Date();
        log.debug(output.toString());
        log.debug("Time to find and execute {}: {}s", app.name(), (end.getTime() - begin.getTime())/1000.0f);
    }
}