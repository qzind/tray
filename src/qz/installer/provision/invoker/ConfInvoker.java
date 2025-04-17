package qz.installer.provision.invoker;

import qz.build.provision.Step;
import qz.common.PropertyHelper;
import qz.utils.SystemUtilities;

import java.util.AbstractMap;

public class ConfInvoker extends PropertyInvoker {
    public ConfInvoker(Step step) {
        super(step, new PropertyHelper(calculateConfPath(step)));
    }

    public static String calculateConfPath(Step step) {
        String relativePath = step.getArgs().get(0);
        if(SystemUtilities.isMac()) {
            return SystemUtilities.getJarParentPath().
                    resolve("../PlugIns/Java.runtime/Contents/Home/conf").
                    resolve(relativePath).
                    normalize()
                    .toString();
        } else {
            return SystemUtilities.getJarParentPath()
                    .resolve("runtime/conf")
                    .resolve(relativePath)
                    .normalize()
                    .toString();
        }
    }

    @Override
    public boolean invoke() {
        Step step = getStep();
        // Java uses the same "|" delimiter as we do, only parse one property at a time
        AbstractMap.SimpleEntry<String, String> pair = parsePropertyPair(step, step.getData());
        if (!pair.getValue().isEmpty()) {
            properties.setProperty(pair);
            if (properties.save()) {
                log.info("Successfully provisioned '1' '{}'", step.getType());
                return true;
            }
            log.error("An error occurred saving properties '{}' to file", step.getData());
        }
        return false;
    }
}
