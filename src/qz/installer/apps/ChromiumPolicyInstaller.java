package qz.installer.apps;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.Installer;
import qz.installer.apps.chromium.LinuxChromiumPolicyInstaller;
import qz.installer.apps.chromium.MacChromiumPolicyInstaller;
import qz.installer.apps.chromium.WindowsChromiumPolicyInstaller;
import qz.installer.apps.locator.AppAlias;
import qz.utils.SystemUtilities;

import static qz.installer.Installer.*;

public abstract class ChromiumPolicyInstaller {
    private static final Logger log = LogManager.getLogger(ChromiumPolicyInstaller.class);

    static ChromiumPolicyInstaller INSTANCE = getInstance();

    public abstract String calculateLocation(PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray);

    public abstract boolean install(PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray, Object ... values);

    public abstract boolean uninstall(PrivilegeLevel scope, AppAlias.Alias alias, String policyName, boolean isArray, Object ... values);


    public static void main(String ... args) {
        AppAlias.Alias chrome = AppAlias.CHROMIUM.getAliases()[0];
        AppAlias.Alias edge = AppAlias.CHROMIUM.getAliases()[1];
        /*
        getInstance().install(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        getInstance().install(PrivilegeLevel.USER, "URLAllowlist", "pp://");
        getInstance().install(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "pp://");
        */

        getInstance().install(PrivilegeLevel.USER, chrome, "URLAllowlist", true,"qz://");
        getInstance().install(PrivilegeLevel.USER, chrome,"URLAllowlist", true,"pp://");
        getInstance().install(PrivilegeLevel.USER, chrome,"URLAllowlist", true,"qz://");

        getInstance().install(PrivilegeLevel.USER, edge, "URLAllowlist", true,"qz://");
        getInstance().install(PrivilegeLevel.USER, edge,"URLAllowlist", true,"pp://");
        getInstance().install(PrivilegeLevel.USER, edge,"URLAllowlist", true,"qz://");

        getInstance().install(Installer.PrivilegeLevel.USER, chrome, "SafeBrowsingEnabled", false, true);
        getInstance().install(Installer.PrivilegeLevel.USER, edge,"SafeBrowsingEnabled", false, false);

        //getInstance().uninstall(Installer.PrivilegeLevel.USER, chrome, "SafeBrowsingEnabled", false);
        //getInstance().uninstall(Installer.PrivilegeLevel.USER, edge,"SafeBrowsingEnabled", false);


        //getInstance().uninstall(Installer.PrivilegeLevel.SYSTEM, chrome, "SafeBrowsingEnabled", false);
        //getInstance().uninstall(Installer.PrivilegeLevel.SYSTEM, edge,"SafeBrowsingEnabled", false);


        //getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        //getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        //getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "pp://");
    }

    public static ChromiumPolicyInstaller getInstance() {
       if(INSTANCE == null) {
           switch(SystemUtilities.getOs()) {
               case WINDOWS:
                   INSTANCE = new WindowsChromiumPolicyInstaller();
                   break;
               case MAC:
                   INSTANCE = new MacChromiumPolicyInstaller();
                   break;
               case LINUX:
               default:
                   INSTANCE = new LinuxChromiumPolicyInstaller();
           }
       }
       return INSTANCE;
    }

}
