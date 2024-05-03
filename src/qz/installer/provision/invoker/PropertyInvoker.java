package qz.installer.provision.invoker;

import qz.build.provision.Step;
import qz.common.Constants;
import qz.common.PropertyHelper;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class PropertyInvoker implements Invokable {
    private Step step;
    PropertyHelper properties;

    public PropertyInvoker(Step step, PropertyHelper properties) {
        this.step = step;
        this.properties = properties;
    }

    public boolean invoke() {
        HashMap<String, String> pairs = parsePropertyPairs(step);
        if (!pairs.isEmpty()) {
            for(Map.Entry<String, String> pair : pairs.entrySet()) {
                properties.setProperty(pair);
            }
            if (properties.save()) {
                log.info("Successfully provisioned '{}' '{}'", pairs.size(), step.getType());
                return true;
            }
            log.error("An error occurred saving properties '{}' to file", step.getData());
        }
        return false;
    }

    public static PropertyHelper getProperties(Step step) {
        File propertiesFile;
        if(step.getRelativePath() != null) {
            // Assume qz-tray.properties is one directory up from provision folder
            // required to prevent installing to payload
            propertiesFile = step.getRelativePath().getParent().resolve(Constants.PROPS_FILE + ".properties").toFile();
        } else {
            // If relative path isn't set, fallback to the jar's parent path
            propertiesFile = SystemUtilities.getJarParentPath(".").resolve(Constants.PROPS_FILE + ".properties").toFile();
        }
        log.info("Provisioning '{}' to properties file: '{}'", step.getData(), propertiesFile);
        return new PropertyHelper(propertiesFile);
    }

    public static PropertyHelper getPreferences(Step step) {
        return new PropertyHelper(FileUtilities.USER_DIR + File.separator + Constants.PREFS_FILE + ".properties");
    }

    public static HashMap<String, String> parsePropertyPairs(Step step) {
        HashMap<String, String> pairs = new HashMap<>();
        if(step.getData() != null && !step.getData().trim().isEmpty()) {
            String[] props = step.getData().split("\\|");
            for(String prop : props) {
                AbstractMap.SimpleEntry<String,String> pair = parsePropertyPair(step, prop);
                if (pair != null) {
                    if(pairs.get(pair.getKey()) != null) {
                        log.warn("Property {} already exists, replacing [before: {}, after: {}] ",
                                 pair.getKey(), pairs.get(pair.getKey()), pair.getValue());
                    }
                    pairs.put(pair.getKey(), pair.getValue());
                }
            }
        } else {
            log.error("Skipping Step '{}', Data is null or empty", step.getType());
        }
        return pairs;
    }


    private static AbstractMap.SimpleEntry<String, String> parsePropertyPair(Step step, String prop) {
        if(prop.contains("=")) {
            String[] pair = prop.split("=", 2);
            if (!pair[0].trim().isEmpty()) {
                if (!pair[1].trim().isEmpty()) {
                    return new AbstractMap.SimpleEntry<>(pair[0], pair[1]);
                } else {
                    log.warn("Skipping '{}' '{}', property value is malformed", step.getType(), prop);
                }
            } else {
                log.warn("Skipping '{}' '{}', property name is malformed", step.getType(), prop);
            }
        } else {
            log.warn("Skipping '{}' '{}', property is malformed", step.getType(), prop);
        }

        return null;
    }
}
