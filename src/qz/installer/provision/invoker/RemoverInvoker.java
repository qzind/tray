package qz.installer.provision.invoker;

import qz.build.provision.Step;
import qz.build.provision.params.Os;
import qz.build.provision.params.types.Remover;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class RemoverInvoker extends InvokableResource {
    private Step step;
    private String aboutTitle;  // e.g. "QZ Tray"
    private String propsFile;   // e.g. "qz-tray"
    private String dataDir;     // e.g. "qz"


    public RemoverInvoker(Step step) {
        this.step = step;
        Remover remover = Remover.parse(step.getData());
        if(remover == Remover.CUSTOM) {
            // Fields are comma delimited in the data field
            parseCustomFromData(step.getData());
        } else {
            aboutTitle = remover.getAboutTitle();
            propsFile = remover.getPropsFile();
            dataDir = remover.getDataDir();
        }
    }

    @Override
    public boolean invoke() throws Exception {
        ArrayList<String> command = getRemoveCommand();
        if(command.size() == 0) {
            log.info("An existing installation of '{}' was not found.  Skipping.", aboutTitle);
            return true;
        }
        boolean success = ShellUtilities.execute(command.toArray(new String[command.size()]));
        if(!success) {
            log.error("An error occurred invoking [{}]", step.getData());
        }
        return success;
    }

    public void parseCustomFromData(String data) {
        String[] parts = data.split(",");
        aboutTitle = parts[0].trim();
        propsFile = parts[1].trim();
        dataDir = parts[2].trim();
    }

    /**
     * Returns the installer command (including the installer itself and if needed, arguments) to
     * invoke the installer file
     */
    public ArrayList<String> getRemoveCommand() {
        ArrayList<String> removeCmd = new ArrayList<>();
        Os os = SystemUtilities.getOs();
        switch(os) {
            case WINDOWS:
                Path win = Paths.get(System.getenv("PROGRAMFILES"))
                        .resolve(aboutTitle)
                        .resolve("uninstall.exe");

                if(win.toFile().exists()) {
                    removeCmd.add(win.toString());
                    removeCmd.add("/S");
                    break;
                }
            case MAC:
                Path legacy = Paths.get("/Applications")
                        .resolve(aboutTitle + ".app")
                        .resolve("Contents")
                        .resolve("uninstall");

                Path mac = Paths.get("/Applications")
                        .resolve(aboutTitle + ".app")
                        .resolve("Contents")
                        .resolve("Resources")
                        .resolve("uninstall");

                if(legacy.toFile().exists()) {
                    removeCmd.add(legacy.toString());
                } else if(mac.toFile().exists()) {
                    removeCmd.add(mac.toString());
                }
                break;
            default:
                Path linux = Paths.get("/opt")
                        .resolve(propsFile)
                        .resolve("uninstall");
                if(linux.toFile().exists()) {
                    removeCmd.add(linux.toString());
                }
        }
        return removeCmd;
    }
}
