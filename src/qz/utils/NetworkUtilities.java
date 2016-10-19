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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * @author Tres
 */
public class NetworkUtilities {

    private static final Logger log = LoggerFactory.getLogger(NetworkUtilities.class);

    private static NetworkUtilities instance;

    private ArrayList<Adapter> adapters;


    private NetworkUtilities(String hostname, int port) throws SocketException {
        gatherNetworkInfo(hostname, port);
    }

    public static NetworkUtilities getInstance() {
        return getInstance("google.com", 443);
    }

    private static NetworkUtilities getInstance(String hostname, int port) {
        if (instance == null) {
            try { instance = new NetworkUtilities(hostname, port); }
            catch(Exception e) { e.printStackTrace(); }
        }

        return instance;
    }


    public static JSONArray getAdaptersJSON() throws JSONException {
        JSONArray network = new JSONArray();
        NetworkUtilities netUtil = getInstance();

        if (netUtil != null) {
            ArrayList<Adapter> adapters = getInstance().adapters;
            for(Adapter adapter : adapters) {
                network.put(new JSONObject()
                                    .put("ipAddress", adapter.inetAddress)
                                    .put("macAddress", adapter.hardwareAddress)
                                    .put("primary", adapter.primary)
                                    .put("up", adapter.up)
                                    .put("id", adapter.id)
                );
            }
        } else {
            network.put(new JSONObject().put("error", "Unable to initialize network utilities"));
        }

        return network;
    }

    public static JSONObject getAdapterJSON() throws JSONException {
        JSONArray adapters = getAdaptersJSON();

        for(int i = 0; i < adapters.length(); i++) {
            JSONObject adapter = adapters.getJSONObject(i);
            if (adapter.getBoolean("primary")) {
                return adapter;
            }
        }

        // returned only if primary cannot be found
        return new JSONObject().put("error", "Unable to determine primary adapter");
    }


    private void gatherNetworkInfo(String hostname, int port) throws SocketException {
        adapters = new ArrayList<>();

        InetAddress primary = getPrimaryInetAddress(hostname, port);

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while(interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();

            String hardwareAddress = getMac(iface);
            String inetAddress = null;
            boolean isPrimary = false;
            boolean isUp = false;

            if (hardwareAddress != null) {
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements() && !isPrimary) {
                    InetAddress address = addresses.nextElement();

                    inetAddress = address.getHostAddress();
                    isPrimary = address.equals(primary);
                    isUp = iface.isUp();
                }
            }

            if (inetAddress != null) {
                adapters.add(new Adapter(hardwareAddress, inetAddress, isUp, isPrimary));
            }
        }
    }

    private static String getMac(NetworkInterface iface) {
        try {
            return ByteUtilities.bytesToHex(iface.getHardwareAddress());
        }
        catch(Exception ignore) {}

        return null;
    }

    private static InetAddress getPrimaryInetAddress(String hostname, int port) {
        log.info("Initiating a temporary connection to \"{}:{}\" to determine main Network Interface", hostname, port);

        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(hostname, port));

            return socket.getLocalAddress();
        }
        catch(IOException e) {
            log.warn("Could not fetch primary adapter", e);
        }
        finally {
            if (socket != null) {
                try { socket.close(); }
                catch(Exception ignore) {}
            }
        }

        return null;
    }


    private static class Adapter {
        private String hardwareAddress, inetAddress, id;
        private boolean up, primary;

        Adapter(String hardwareAddress, String inetAddress, boolean up, boolean primary) {
            this.hardwareAddress = hardwareAddress;
            this.primary = primary;
            this.up = up;

            if (inetAddress != null && inetAddress.contains("%")) {
                String[] split = inetAddress.split("%");
                this.inetAddress = split[0];
                id = split[split.length - 1];
            } else {
                this.inetAddress = inetAddress;
            }
        }
    }

}
