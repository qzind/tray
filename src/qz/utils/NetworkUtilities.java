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

    private static class Adapter {
        private String hardwareAddress;
        private String inetAddress;
        private String id;
        private boolean up;
        private boolean primary;
        Adapter(String hardwareAddress, String inetAddress, boolean up, boolean primary) {
            this.hardwareAddress = hardwareAddress;
            if (inetAddress != null && inetAddress.contains("%")) {
                String[] split = inetAddress.split("%");
                this.inetAddress = split[0];
                this.id = split[split.length - 1];
            } else {
                this.inetAddress = inetAddress;
            }
            this.up = up;
            this.primary = primary;
        }
        String getHardwareAddress() { return hardwareAddress; }
        String getInetAddress() { return inetAddress; }
        boolean isUp() { return up; }
        boolean isPrimary() { return primary; }
        String getId() { return id; }
    }

    private ArrayList<Adapter> adapters;

    public static NetworkUtilities getInstance() {
        return getInstance("google.com", 443);
    }

    private static NetworkUtilities getInstance(String hostname, int port) {
        if (instance == null) {
            try {
                instance = new NetworkUtilities(hostname, port);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }

        return instance;
    }

    private NetworkUtilities(String hostname, int port) throws SocketException {
        gatherNetworkInfo(hostname, port);
    }

    private ArrayList<Adapter> getAdapters() {
        return adapters;
    }

    private void gatherNetworkInfo(String hostname, int port) throws SocketException {
        adapters = new ArrayList<>();
        InetAddress primary = getPrimaryInetAddress(hostname, port);
        Enumeration<NetworkInterface> all = NetworkInterface.getNetworkInterfaces();
        while (all.hasMoreElements()){
            NetworkInterface i = all.nextElement();
            Enumeration<InetAddress> addresses = i.getInetAddresses();
            String inetAddress = null;
            boolean isUp = false;
            boolean isPrimary = false;

            String hardwareAddress = getMac(i);
            while (addresses.hasMoreElements() && hardwareAddress != null){
                InetAddress a = addresses.nextElement();
                inetAddress = a.getHostAddress();
                isUp = i.isUp();
                if (a.equals(primary))
                    isPrimary = true;
            }

            if (inetAddress != null) {
                adapters.add(new Adapter(hardwareAddress, inetAddress, isUp, isPrimary));
            }
        }
    }

    private static String getMac(InetAddress inet) {
        try {
            return ByteUtilities.bytesToHex(NetworkInterface.getByInetAddress(inet).getHardwareAddress());
        }
        catch(Exception ignore) {}
        return null;
    }

    private static String getMac(NetworkInterface iface) {
        try {
            return ByteUtilities.bytesToHex(iface.getHardwareAddress());
        }
        catch (Exception ignore) {}
        return null;
    }

    private static InetAddress getPrimaryInetAddress(String hostname, int port) {
        log.info("Initiating a temporary connection to \"{}:{}\" to determine main Network Interface", hostname, port);

        Socket socket = null;
        try {
            SocketAddress endpoint = new InetSocketAddress(hostname, port);
            socket = new Socket();
            socket.connect(endpoint);
            return socket.getLocalAddress();
        } catch(IOException e) {
            log.warn("Could not fetch primary adapter", e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ignore) {}
            }
        }

        return null;
    }

    public static JSONArray getAdaptersJSON() throws JSONException {
        JSONArray network = new JSONArray();
        NetworkUtilities netUtil = getInstance();
        if (netUtil != null) {
            ArrayList<Adapter> adapters = getInstance().getAdapters();
            for (Adapter a : adapters) {
                JSONObject adapter = new JSONObject();
                adapter.put("ipAddress", a.getInetAddress())
                        .put("macAddress", a.getHardwareAddress())
                        .put("up", a.isUp())
                        .put("primary", a.isPrimary())
                        .put("id", a.getId());
                network.put(adapter);
            }
        }

        return network;
    }

    public static JSONObject getAdapterJSON() throws JSONException {
        JSONObject network = new JSONObject();
        NetworkUtilities netUtil = getInstance();
        if (netUtil != null) {
            ArrayList<Adapter> adapters = getInstance().getAdapters();
            for (Adapter a : adapters) {
                if (a.isPrimary()) {
                    network.put("ipAddress", a.getInetAddress())
                            .put("macAddress", a.getHardwareAddress())
                            .put("up", a.isUp())
                            .put("primary", true)
                            .put("id", a.id);
                    break;
                }
            }
            if (!network.has("ipAddress")) {
                network.put("error", "Unable to determine primary adapter");
            }
        } else {
            network.put("error", "Unable to initialize network utilities");
        }

        return network;
    }
}
