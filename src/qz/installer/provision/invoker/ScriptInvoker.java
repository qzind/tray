package qz.installer.provision.invoker;

import qz.build.provision.Step;
import qz.build.provision.params.Os;
import qz.build.provision.params.types.Script;
import qz.utils.FileUtilities;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScriptInvoker extends InvokableResource {
    private final Step step;

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
        ArrayList<String> command = getInterpreter(engine, script);
        if(command.isEmpty() && SystemUtilities.isWindows()) {
            log.warn("No interpreter found for {}, skipping", step.getData());
            return false;
        }
        command.add(script.toString());
        boolean success = ShellUtilities.execute(command.toArray(new String[0]));
        if(!success) {
            log.error("An error occurred invoking [{}]", step.getData());
        }
        return success;
    }


    /**
     * Returns the interpreter command (and if needed, arguments) to invoke the script file
     * <p>
     * An empty array will fall back to Unix "shebang" notation, e.g. #!/usr/bin/python3
     * which will allow the OS to select the correct interpreter for the given file
     * </p>
     * <b>Note: </b> Special attention for "shebang" is given on macOS, see #1396
     */
    private static ArrayList<String> getInterpreter(Script engine, File script) {
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
                if(SystemUtilities.isMac()) {
                    interpreter.add("sh"); // see #1396
                }
            default:
                // macOS no longer supports direct invocation see #1396
                if(SystemUtilities.isMac()) {
                    ArrayList<String> shebang = parseShebang(script);
                    if(!shebang.isEmpty()) {
                        // prefer shebang to file extension
                        interpreter = shebang;
                    }
                }
                // Allow the environment to parse it from the shebang at invocation time
        }
        return interpreter;
    }

    private static ArrayList<String> parseShebang(File script) {
        ArrayList<String> interpreter = new ArrayList<>();
        String shebang = getShebangLine(script);
        if(shebang != null) {
            Collections.addAll(interpreter, shebang.substring(2).split("\\s+"));
        }
        return interpreter;
    }

    private static String getShebangLine(File script) {
        if(script.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(script))) {
                String line;
                if((line = br.readLine()) != null) {
                    // only check the first line
                    if(line.startsWith("#!")) {
                        return line;
                    }
                }
            } catch(IOException e) {
                log.error("Step failed", e);
            }
        }
        return null;
    }
}
