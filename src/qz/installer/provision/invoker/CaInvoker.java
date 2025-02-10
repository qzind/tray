package qz.installer.provision.invoker;

import qz.build.provision.Step;
import qz.common.PropertyHelper;
import qz.utils.ArgValue;
import qz.utils.FileUtilities;

import java.io.File;
import java.io.IOException;

/**
 * Combines ResourceInvoker and PropertyInvoker to deploy a file and set a property to its deployed path
 */
public class CaInvoker extends InvokableResource {
    Step step;
    PropertyHelper properties;

    public CaInvoker(Step step, PropertyHelper properties) {
        this.step = step;
        this.properties = properties;
    }

    @Override
    public boolean invoke() throws IOException {
        // First, write our cert file
        File caCert = dataToFile(step);
        if(caCert == null) {
            return false;
        }

        // Next, handle our property step
        Step propsStep = step.clone();

        // If the property already exists, snag it
        String key = ArgValue.AUTHCERT_OVERRIDE.getMatch();
        String value = caCert.getPath();
        if (properties.containsKey(key)) {
            value = properties.getProperty(key) + FileUtilities.FILE_SEPARATOR + value;
        }

        propsStep.setData(String.format("%s=%s", key, value));

        if (new PropertyInvoker(propsStep, properties).invoke()) {
            return true;
        }

        return false;
    }
}
