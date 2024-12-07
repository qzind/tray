package qz.installer.provision.invoker;

import qz.build.provision.Step;
import qz.build.provision.params.Os;
import qz.build.provision.params.types.Script;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.util.ArrayList;

public class ScriptInvoker extends InvokableResource {
    private Step step;

    public ScriptInvoker(Step step) {
        this.step = step;
    }

    @Override
    public boolean invoke() throws Exception {
        File script = dataToFile(step);
        if(script == null) {
            return false;
        }
        Script engine = Script.parse(step.getData());
        ArrayList<String> command = getInterpreter(engine);
        if(command.isEmpty() && SystemUtilities.isWindows()) {
            log.warn("No interpreter found for {}, skipping", step.getData());
            return false;
        }
        command.add(script.toString());
        boolean success = ShellUtilities.execute(command.toArray(new String[command.size()]));
        if(!success) {
            log.error("An error occurred invoking [{}]", step.getData());
        }
        return success;
    }


    /**
     * Returns the interpreter command (and if needed, arguments) to invoke the script file
     *
     * An empty array will fall back to Unix "shebang" notation, e.g. #!/usr/bin/python3
     * which will allow the OS to select the correct interpreter for the given file
     *
     * No special attention is given to "shebang", behavior may differ between OSs
     */
    private static ArrayList<String> getInterpreter(Script engine) {
        ArrayList<String> interpreter = new ArrayList<>();
        Os osType = SystemUtilities.getOs();
        switch(engine) {
            case PS1:
                if(osType == Os.WINDOWS) {
                    interpreter.add("powershell.exe");
                } else if(osType == Os.MAC) {
                    interpreter.add("/usr/local/bin/pwsh");
                } else {
                    interpreter.add("pwsh");
                }
                interpreter.add("-File");
                break;
            case PY:
                interpreter.add(osType == Os.WINDOWS ? "python3.exe" : "python3");
                break;
            case BAT:
                interpreter.add(osType == Os.WINDOWS ? "cmd.exe" : "wineconsole");
                break;
            case RB:
                interpreter.add(osType == Os.WINDOWS ? "ruby.exe" : "ruby");
                break;
            case SH:
            default:
                // Allow the environment to parse it from the shebang at invocation time
        }
        return interpreter;
    }
}
