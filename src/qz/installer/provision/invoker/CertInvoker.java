package qz.installer.provision.invoker;

import qz.build.provision.Step;
import qz.common.Constants;
import qz.utils.FileUtilities;

import java.io.File;

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
        return FileUtilities.addToCertList(Constants.ALLOW_FILE, cert) == SUCCESS;
    }
}
