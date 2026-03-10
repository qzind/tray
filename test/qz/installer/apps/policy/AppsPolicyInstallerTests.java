package qz.installer.apps.policy;

import qz.installer.apps.locator.AppAlias;

import static qz.installer.Installer.*;

public abstract class AppsPolicyInstallerTests {
    public static void main(String ... args) {
        //
        // GOOGLE CHROME
        //
        AppAlias.Alias chrome = AppAlias.CHROMIUM.getAliases()[0];
        PolicyInstaller chromeInstaller = new PolicyInstaller(PrivilegeLevel.USER, chrome);

        // INSTALL
        chromeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "qz://");
        chromeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "pp://");
        chromeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "qz://");
        chromeInstaller.install(PolicyState.Type.VALUE, "SafeBrowsingEnabled", true);

        // UNINSTALL
        chromeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "qz://");
        chromeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "pp://");
        chromeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "qz://");
        chromeInstaller.uninstall(PolicyState.Type.VALUE, "SafeBrowsingEnabled");

        //
        // MICROSOFT EDGE
        //
        AppAlias.Alias edge = AppAlias.CHROMIUM.getAliases()[1];
        PolicyInstaller edgeInstaller = new PolicyInstaller(PrivilegeLevel.USER, edge);

        // INSTALL
        edgeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "qz://");
        edgeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "pp://");
        edgeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "qz://");
        edgeInstaller.install(PolicyState.Type.VALUE, "SafeBrowsingEnabled", true);

        // UNINSTALL
        edgeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "qz://");
        edgeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "pp://");
        edgeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "qz://");
        edgeInstaller.uninstall(PolicyState.Type.VALUE, "SafeBrowsingEnabled");

        // TODO: Add firefox
    }
}
