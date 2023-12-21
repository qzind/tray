package qz.installer.provision.invoker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface Invokable {
    Logger log = LogManager.getLogger(Invokable.class);

    boolean invoke() throws Exception;
}
