package qz.installer.provision.invoker;

import qz.build.provision.Step;
import qz.common.Constants;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
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
            log.error("Unable to read lines from file '{}'", file, e);
        }
        if(!lines.isEmpty()) {
            for(String line : lines) {
                // TODO: Make more efficient; Opens and close the file on each write
                FileUtilities.printLineToFile(Constants.ALLOW_FILE, line, local);
            }
        }
    }
}
