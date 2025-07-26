package qz.communication;

import de.gmuth.ipp.attributes.TemplateAttributes;
import de.gmuth.ipp.client.CupsClient;
import de.gmuth.ipp.client.IppClient;
import de.gmuth.ipp.client.IppJob;
import de.gmuth.ipp.client.IppPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Ipp {
    private static final Logger log = LogManager.getLogger(Ipp.class);
    private static final HashMap<Session, HashMap<UUID,ServerEntry>> servers = new HashMap<>();

//"printer":{"name":"PDFwriter","uri":"ipps:\/\/192.168.1.16:631\/printers\/PDFWriter","info":"PDFwriter", "serverUuid":"44184659-809b-4411-a05e-7a7d18bc00c9"},
//"options":{"bounds":null,"colorType":"color","copies":1,"density":0,"duplex":false,"fallbackDensity":null,"interpolation":"bicubic","jobName":null,"legacy":false,"margins":0,"orientation":null,"paperThickness":null,"printerTray":null,"rasterize":false,"rotation":0,"scaleContent":true,"size":{"width":4,"height":6},"units":"in","forceRaw":false,"encoding":null,"spool":null,"ipp":{"CUPS_FLIP_LONG_EDGE_BLAH":true}},
//"data":[{"flavor":"plain","data":"test test test","options":{"ipp":{"CUPS_FLIP_LONG_EDGE_BLAH":"true"}}}]}

    public static void print(Session session, String UID, JSONObject params) throws JSONException, IOException {
        log.warn(params);
        JSONArray printData = params.getJSONArray("data");
        JSONObject printer = params.optJSONObject("printer");
        JSONObject options = params.optJSONObject("options");

        UUID uuid = UUID.fromString(printer.getString("serverUuid"));
        URI requestedUri = URI.create(printer.optString("uri"));

        IppClient ippClient = new IppClient();
        ServerEntry serverEntry = servers.get(session).get(uuid);
        CupsClient cupsClient = new CupsClient(serverEntry.serverUri, ippClient);

        // requestedUri is user provided, we must make sure it belongs to the claimed server
        if(!serverEntry.serverUri.getScheme().equals(requestedUri.getScheme()) ||
            !serverEntry.serverUri.getAuthority().equals(requestedUri.getAuthority())) {
            throw new UnknownHostException(serverEntry.serverUri + " Is not a printer of the server " + requestedUri);
        }

        //todo: this would also be a good time to raise a prompt

        IppPrinter ippPrinter = new IppPrinter(requestedUri.toString());

        // todo: match this to PrintServiceMatcher.getPrintersJSON syntax
        if (!serverEntry.uname.isEmpty() && !serverEntry.pass.isEmpty()) {
            cupsClient.basicAuth(serverEntry.uname, serverEntry.pass);
        }

        // todo: for testing, assume all data is just plaintext. There are a lot of things to discuss about filetype and format.

        IppJob job = ippPrinter.createJob(TemplateAttributes.jobName("test"));
        String dataString = printData.getJSONObject(0).getString("data");

        byte[] dataBytes = dataString.getBytes(StandardCharsets.UTF_8);
        try (InputStream in = new ByteArrayInputStream(dataBytes)) {
            job.sendDocument(in);
        }
        // todo: I assume we wait?
        job.waitForTermination();
    }

    public static Object find(Session session, JSONObject params) throws JSONException {
        JSONObject server = params.getJSONObject("server");
        String query = params.optString("query", "");
        String serverUuid = server.getString("uuid");
        UUID uuid = UUID.fromString(serverUuid);

        ServerEntry serverEntry = servers.get(session).get(uuid);
        if (serverEntry == null) throw new JSONException("Unknown Server");

        IppClient ippClient = new IppClient();
        CupsClient cupsClient = new CupsClient(serverEntry.serverUri, ippClient);
        //cupsClient.setUserName(server.uname);

        // todo: match this to PrintServiceMatcher.getPrintersJSON syntax
        if (!serverEntry.uname.isEmpty() && !serverEntry.pass.isEmpty()) {
            cupsClient.basicAuth(serverEntry.uname, serverEntry.pass);
        }

        // If there is no query, list all printers
        if (query.isEmpty()) {
            JSONArray names = new JSONArray();
            for(IppPrinter p: cupsClient.getPrinters()) {
                names.put(p.getName());
            }

            return names;
        } else {
            JSONObject ret = new JSONObject();
            IppPrinter printer = cupsClient.getPrinter(query);

            log.warn(printer.toString());
            ret.put("name", printer.getName());
            ret.put("uri", printer.getPrinterUri());
            ret.put("info", printer.getInfo());
            ret.put("serverUuid", uuid.toString());
            return ret;
        }
    }

    public static String addServer(Session session, JSONObject params) throws URISyntaxException {
        String serverUriString = params.optString("url", "");
        URI uri = URI.create(serverUriString);
        // Todo: we could ditch query and ref fields from the uri? idk if they are ever helpful here, or just a danger
        // more URL specific logic would be smart here, we should make sure things like filepaths get regected. The URL class didnt work, it wouldn't
        // accept ipp:// as a scheme, nor was there any sane way to add it. Maybe there is something in apache commons.

        // Todo: whitelist blacklist check?

        ServerEntry server = new ServerEntry(uri, params.optString("username", ""), params.optString("password", ""));
        if (!servers.containsKey(session)) {
            servers.put(session, new HashMap<>());
        }

        // If we already have this server, just do a reverse lookup and give them the old UUID
        if (servers.get(session).containsValue(server)) {
            for (Map.Entry<UUID,ServerEntry> entry : servers.get(session).entrySet()) {
                if (entry.getValue().equals(server)) {
                    return entry.getKey().toString();
                }
            }
        }

        // Otherwise, slap a new UUID on the server and send the UUID to the user
        UUID serverId = UUID.randomUUID();
        servers.get(session).put(serverId, server);
        return serverId.toString();
    }

    public static ServerEntry getServerEntry(Session session, UUID uuid) {
        return servers.get(session).get(uuid);
    }

    public static class ServerEntry {
        public final URI serverUri;
        public final String uname;
        public final String pass;

        ServerEntry(URI serverUri, String uname, String pass) {
            this.serverUri = serverUri;
            this.uname = uname;
            this.pass = pass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ServerEntry)) return false;
            ServerEntry that = (ServerEntry)o;
            return serverUri.equals(that.serverUri) &&
                    uname.equals(that.uname) &&
                    pass.equals(that.pass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serverUri, uname, pass);
        }
    }
}