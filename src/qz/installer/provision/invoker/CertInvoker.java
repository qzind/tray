package qz.installer.provision.invoker;

import qz.build.provision.Step;
import qz.common.Constants;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import static qz.utils.ArgParser.ExitStatus.*;

public class CertInvoker extends InvokableResource {
    private Step step;

    public CertInvoker(Step step) {
        this.step = step;
    }

    @Override
    public boolean invoke() throws Exception {
        File cert = dataToFile(step);
        if(cert == null) {
            return false;
        }
        dedupe();
        return FileUtilities.addToCertList(Constants.ALLOW_FILE, cert) == SUCCESS;
    }

    /**
     * Provisioned certs might have been duped, try to clean them up
     * per <a href="https://github.com/qzind/tray/issues/1489">#1489</a>
     */
    private void dedupe() {
        HashSet<String> lines = new HashSet<>();
        boolean local = !SystemUtilities.isAdmin();
        File file = FileUtilities.getFile(Constants.ALLOW_FILE, local);

        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch(IOException e) {
            log.error("Unable to read file '{}'", file, e);
        }

        // Clear file before writing new values
        try(FileOutputStream fos = new FileOutputStream(file, false)) {
            for(String line : lines) {
                // "\r\n" is a holdover from FileUtilities.printLineToFile()
                fos.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
        } catch(IOException e) {
            log.error("Unable to write file '{}'", file, e);
        }
    }
}
