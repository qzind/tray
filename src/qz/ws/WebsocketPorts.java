package qz.ws;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.build.provision.Step;
import qz.build.provision.params.Type;
import qz.common.Constants;
import qz.installer.provision.invoker.PropertyInvoker;
import qz.utils.ArgValue;
import qz.utils.PrefsSearch;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static qz.common.Constants.PROVISION_FILE;

public class WebsocketPorts {
    private static final Logger log = LogManager.getLogger(WebsocketPorts.class);
    private List<Integer> securePorts;
    private List<Integer> insecurePorts;

    public List<Integer> getUnusedSecurePorts() {
        List<Integer> unused = new ArrayList<>(securePorts);
        unused.remove(securePortIndex.get());
        return unused;
    }

    public List<Integer> getUnusedInsecurePorts() {
        List<Integer> unused = new ArrayList<>(insecurePorts);
        unused.remove(insecurePortIndex.get());
        return unused;
    }

    public void setHttpsOnly(boolean httpsOnly) {
        if(httpsOnly) {
            insecurePortIndex.set(-1);
        }
    }

    public void setHttpOnly(boolean httpOnly) {
        if(httpOnly) {
            securePortIndex.set(-1);
        }
    }

    private static final AtomicInteger securePortIndex = new AtomicInteger(0);
    private static final AtomicInteger insecurePortIndex = new AtomicInteger(0);

    private WebsocketPorts(List<Integer> securePorts, List<Integer> insecurePorts) {
        this.securePorts = securePorts;
        this.insecurePorts = insecurePorts;
    }

    public int getSecurePort() {
        return securePorts.get(securePortIndex.get());
    }

    public int getInsecurePort() {
        return insecurePorts.get(insecurePortIndex.get());
    }

    public int getSecureIndex() {
        return securePortIndex.get();
    }

    public int getInsecureIndex() {
        return insecurePortIndex.get();
    }

    public int nextSecureIndex() {
        return securePortIndex.incrementAndGet();
    }

    public int nextInsecureIndex() {
        return insecurePortIndex.incrementAndGet();
    }

    public void resetIndices() {
        securePortIndex.set(0);
        insecurePortIndex.set(0);
    }

    public boolean secureBoundsCheck() {
        return securePortIndex.get() < securePorts.size();
    }

    public boolean insecureBoundsCheck() {
        return insecurePortIndex.get() < insecurePorts.size();
    }

    /**
     * Parses WebSocket ports from preferences or fallback to defaults is a problem is found
     */
    public static WebsocketPorts parseFromProperties() {
        return fromList(PrefsSearch.getIntegerArray(ArgValue.WEBSOCKET_SECURE_PORTS),
                        PrefsSearch.getIntegerArray(ArgValue.WEBSOCKET_INSECURE_PORTS));
    }

    /**
     * Loops through steps searching for a property which sets a custom websocket ports
     */
    public static WebsocketPorts parseFromSteps(List<Step> steps) {
        List<Integer> secure = new ArrayList<>();
        List<Integer> insecure = new ArrayList<>();
        for(Step step : steps) {
            if(step.getType() == Type.PROPERTY) {
                HashMap<String, String> pairs = PropertyInvoker.parsePropertyPairs(step);
                String foundPorts;
                if((foundPorts = pairs.get(ArgValue.WEBSOCKET_SECURE_PORTS.getMatch())) != null) {
                    secure = PrefsSearch.parseIntegerArray(foundPorts);
                    if(!secure.isEmpty()) {
                        log.info("Picked up custom secure ports from {}: [{}]", PROVISION_FILE, StringUtils.join(secure, ","));
                    }
                }
                if((foundPorts = pairs.get(ArgValue.WEBSOCKET_INSECURE_PORTS.getMatch())) != null) {
                    insecure = PrefsSearch.parseIntegerArray(foundPorts);
                    if(!insecure.isEmpty()) {
                        log.info("Picked up custom insecure ports from {}: [{}]", PROVISION_FILE, StringUtils.join(insecure, ","));
                    }
                }
            }
        }
        return fromList(secure, insecure);
    }

    /**
     * Constructs a new instance of <code>WebsocketPorts</code> with the specified secure and
     * insecure port ranges, falling back to <code>Constants.DEFAULT_WSS_PORTS</code>,
     * <code>Constants.DEFAULT_WS_PORTS</code> if a problem occurred.
     *
     * @param secure Port listing with at least one element.  Size must match <code>insecure</code>
     * @param insecure Port listing with at least one element.  Size must match <code>secure</code>
     */
    public static WebsocketPorts fromList(List<Integer> secure, List<Integer> insecure) {
        boolean fallback = false;
        if(secure.isEmpty() || insecure.isEmpty()) {
            log.warn("One or more WebSocket ports is empty, falling back to defaults");
            fallback = true;
        }
        if(secure.size() != insecure.size()) {
            log.warn("Secure ({}) and insecure ({}) WebSocket port counts mismatch, falling back to defaults", secure, insecure);
            fallback = true;
        }
        if(fallback) {
            log.warn("Falling back to default WebSocket ports: ({}), ({})", secure, insecure);
            secure = Arrays.asList(Constants.DEFAULT_WSS_PORTS);
            insecure = Arrays.asList(Constants.DEFAULT_WS_PORTS);
        }

        return new WebsocketPorts(Collections.unmodifiableList(secure), Collections.unmodifiableList(insecure));
    }

    public String allPortsAsString() {
        return StringUtils.join(securePorts, ",") + "," + StringUtils.join(insecurePorts, ",");
    }
}
