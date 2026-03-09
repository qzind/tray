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
        chromeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "qz://").log();
        chromeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "pp://").log();
        chromeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "qz://").log();
        chromeInstaller.install(PolicyState.Type.VALUE, "SafeBrowsingEnabled", true).log();

        // UNINSTALL
        chromeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "qz://").log();
        chromeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "pp://").log();
        chromeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "qz://").log();
        chromeInstaller.uninstall(PolicyState.Type.VALUE, "SafeBrowsingEnabled").log();

        //
        // MICROSOFT EDGE
        //
        AppAlias.Alias edge = AppAlias.CHROMIUM.getAliases()[1];
        PolicyInstaller edgeInstaller = new PolicyInstaller(PrivilegeLevel.USER, edge);

        // INSTALL
        edgeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "qz://").log();
        edgeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "pp://").log();
        edgeInstaller.install(PolicyState.Type.ARRAY, "URLAllowlist", "qz://").log();
        edgeInstaller.install(PolicyState.Type.VALUE, "SafeBrowsingEnabled", true).log();

        // UNINSTALL
        edgeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "qz://").log();
        edgeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "pp://").log();
        edgeInstaller.uninstall(PolicyState.Type.ARRAY, "URLAllowlist", "qz://").log();
        edgeInstaller.uninstall(PolicyState.Type.VALUE, "SafeBrowsingEnabled").log();

        // TODO: Add firefox
    }
}
