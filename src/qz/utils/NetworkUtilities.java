/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.utils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

/**
 * @author Tres
 */
public class NetworkUtilities {

    private static final Logger log = LogManager.getLogger(NetworkUtilities.class);

    private static NetworkUtilities instance;
    private static String systemName = ShellUtilities.getHostName();
    private static String userName = System.getProperty("user.name");

    // overridable in preferences, see "networking.hostname", "networking.port"
    private static String defaultHostname = "google.com";
    private static int defaultPort = 443;

    private ArrayList<Device> devices;
    private Device primaryDevice;


    private NetworkUtilities(String hostname, int port) {
        try { primaryDevice = new Device(getPrimaryInetAddress(hostname, port), true); }
        catch(SocketException ignore) {}
    }

    public static NetworkUtilities getInstance(String hostname, int port) {
        if (instance == null) {
            try { instance = new NetworkUtilities(hostname, port); }
            catch(Exception e) { e.printStackTrace(); }
        }

        return instance;
    }

    public static JSONArray getDevicesJSON(JSONObject params) throws JSONException {
        JSONArray network = new JSONArray();

        try {
            for(Device device : getInstance(params.optString("hostname", defaultHostname),
                                            params.optInt("port", defaultPort)).gatherDevices()) {
                network.put(device.toJSON());
            }
        }
        catch(SocketException ignore) {
            network.put(new JSONObject().put("error", "Unable to initialize network utilities"));
        }
        return network;
    }

    public static JSONObject getDeviceJSON(JSONObject params) throws JSONException {
        Device primary = getInstance(params.optString("hostname", defaultHostname),
                                     params.optInt("port", defaultPort)).primaryDevice;

        if (primary != null) {
            return primary.toJSON();
        } else {
            return new JSONObject().put("error", "Unable to initialize network utilities");
        }
    }

    private ArrayList<Device> gatherDevices() throws SocketException {
        if (devices == null) {
            devices = new ArrayList<>();

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                Device next = new Device(iface);
                next.primary = next.equals(primaryDevice);

                devices.add(next);
            }
        }

        return devices;
    }

    private static InetAddress getPrimaryInetAddress(String hostname, int port) {
        log.info("Initiating a temporary connection to \"{}:{}\" to determine main Network Interface", hostname, port);

        try(Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostname, port));

            return socket.getLocalAddress();
        }
        catch(IOException e) {
            log.warn("Could not fetch primary adapter", e);
        }

        return null;
    }

    /**
     * Sets the <code>defaultHostname</code> and <code>defaultPort</code> by parsing the properties file
     */
    public static void setPreferences(Properties props) {
        String hostName = props.getProperty("networking.hostname");
        if(hostName != null && !hostName.trim().isEmpty()) {
            defaultHostname = hostName;
        }

        String port = props.getProperty("networking.port");
        if(port != null && !port.trim().isEmpty()) {
            try {
                defaultPort = Integer.parseInt(port);
            } catch(Exception parseError) {
                log.warn("Unable to parse \"networking.port\"", parseError);
            }
        }
    }

    private static class Device {

        String mac, ip, id, name;
        ArrayList<String> ip4, ip6;
        boolean up, primary;

        Device(InetAddress inet, boolean primary) throws SocketException {
            this(NetworkInterface.getByInetAddress(inet));
            ip = inet.getHostAddress(); //use primary
            this.primary = primary;
        }

        Device(NetworkInterface iface) {
            try { mac = ByteUtilities.bytesToHex(iface.getHardwareAddress()); } catch(Exception ignore) {}
            try { up = iface.isUp(); } catch(SocketException ignore) {}

            ip4 = new ArrayList<>();
            ip6 = new ArrayList<>();

            name = iface.getDisplayName();

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while(addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address) {
                    ip4.add(address.getHostAddress());
                } else if (address instanceof Inet6Address) {
                    String ip6 = address.getHostAddress();
                    if (ip6.contains("%")) {
                        String[] split = ip6.split("%");
                        this.ip6.add(split[0]);
                        id = split[split.length - 1];
                    } else {
                        this.ip6.add(ip6);
                    }
                } else {
                    log.warn("InetAddress type {} unsupported", address.getClass().getName());
                }

                if (ip6.size() > 0) {
                    ip = ip6.get(0);
                } else if (ip4.size() > 0) {
                    ip = ip4.get(0);
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Device) {
                Device device = (Device)obj;
                boolean ip4match = ip4 != null && ip4.containsAll(device.ip4);
                boolean ip6match = ip6 != null && ip6.containsAll(device.ip6);
                return mac != null && mac.equals(device.mac) && ip4match && ip6match;
            }

            return false;
        }

        static JSONArray toJSONArray(ArrayList<String> list) {
            if (list != null && list.size() > 0) {
                JSONArray array = new JSONArray();
                list.forEach(array::put);
                return array;
            }

            return null;
        }

        JSONObject toJSON() throws JSONException {
            return new JSONObject()
                    .put("name", name)
                    .put("mac", mac)
                    .put("ip", ip)
                    .put("ip4", toJSONArray(ip4))
                    .put("ip6", toJSONArray(ip6))
                    .put("primary", primary)
                    .put("up", up)
                    .put("hostname", systemName)
                    .put("username", userName)
                    .put("id", id);
        }
    }

}
