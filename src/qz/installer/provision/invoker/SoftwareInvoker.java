package qz.installer.provision.invoker;

import qz.build.provision.Step;
import qz.build.provision.params.Os;
import qz.build.provision.params.types.Software;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SoftwareInvoker extends InvokableResource {
    private Step step;

    public SoftwareInvoker(Step step) {
        this.step = step;
    }

    @Override
    public boolean invoke() throws Exception {
        File payload = dataToFile(step);
        if(payload == null) {
            return false;
        }
        Software installer = Software.parse(step.getData());
        ArrayList<String> command = getInstallCommand(installer, step.getArgs(), payload);
        boolean success = ShellUtilities.execute(command.toArray(new String[command.size()]), payload.getParentFile());
        if(!success) {
            log.error("An error occurred invoking [{}]", step.getData());
        }
        return success;
    }

    /**
     * Returns the installer command (including the installer itself and if needed, arguments) to
     * invoke the installer file
     */
    public ArrayList<String> getInstallCommand(Software installer, List<String> args, File payload) {
        ArrayList<String> interpreter = new ArrayList<>();
        Os os = SystemUtilities.getOs();
        switch(installer) {
            case EXE:
                if(!SystemUtilities.isWindows()) {
                    interpreter.add("wine");
                }
                // Executable on its own
                interpreter.add(payload.toString());
                interpreter.addAll(args); // Assume exe args come after payload
                break;
            case MSI:
                interpreter.add(os == Os.WINDOWS ? "msiexec.exe" : "msiexec");
                interpreter.add("/i"); // Assume standard install
                interpreter.add(payload.toString());
                interpreter.addAll(args); // Assume msiexec args come after payload
                break;
            case PKG:
                if(os == Os.MAC) {
                    interpreter.add("installer");
                    interpreter.addAll(args); // Assume installer args come before payload
                    interpreter.add("-package");
                    interpreter.add(payload.toString());
                    interpreter.add("-target");
                    interpreter.add("/"); // Assume we don't want this on a removable volume
                } else {
                    throw new UnsupportedOperationException("PKG is not yet supported on this platform");
                }
                break;
            case DMG:
                // DMG requires "hdiutil attach", but the mount point is unknown
                throw new UnsupportedOperationException("DMG is not yet supported");
            case RUN:
                if(SystemUtilities.isWindows()) {
                    interpreter.add("bash");
                    interpreter.add("-c");
                }
                interpreter.add(payload.toString());
                interpreter.addAll(args); // Assume run args come after payload
                // Executable on its own
                break;
            default:
                // We'll try to parse it from the shebang just before invocation time
        }
        return interpreter;
    }

}
