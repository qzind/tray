package qz.installer.provision;

import org.codehaus.jettison.json.JSONException;
import qz.common.Constants;

import java.io.IOException;
import java.io.InputStream;

public class ProvisionerInstallerTests {

    public static void main(String ... args) throws JSONException, IOException {
        InputStream in = ProvisionerInstallerTests.class.getResourceAsStream("resources/" + Constants.PROVISION_FILE);

        // Parse the JSON
        ProvisionInstaller provisionInstaller = new ProvisionInstaller(ProvisionerInstallerTests.class, in);

        // Invoke all parsed steps
        provisionInstaller.invoke();
    }
}
