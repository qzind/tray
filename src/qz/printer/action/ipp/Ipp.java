package qz.printer.action.ipp;

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

import java.awt.print.PrinterException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Ipp {
    private static final Logger log = LogManager.getLogger(Ipp.class);
    private static final HashMap<Session, HashMap<UUID,ServerEntry>> servers = new HashMap<>();
    private static final String[] SUPPORTED_SCHEMES = { "ipp", "ipps", "http", "https" };

    public enum Type {
        IPP_SERVER("ipp-server"),
        IPP_PRINTER("ipp-printer"),
        UNKNOWN("ipp-unknown");

        private String id;

        Type(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static Type parse(String id) {
            for(Type type : values()) {
                if(type.name().equalsIgnoreCase(id)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    public static JSONObject addServer(Session session, JSONObject params) throws JSONException, URISyntaxException {
        String serverUriString = params.optString("uri", "");
        URI uri;
        try {
            // TODO: Add URI allow list support
            uri = sanitizeUri(schemeFilter(URI.create(serverUriString)));
        } catch(IllegalArgumentException e) {
            if(e.getCause() instanceof URISyntaxException) {
                throw (URISyntaxException)e.getCause();
            } else {
                throw e;
            }
        }

        ServerEntry server = new ServerEntry(uri, params.optString("username", ""), params.optString("password", ""));
        if (!servers.containsKey(session)) {
            servers.put(session, new HashMap<>());
        }

        // If we already have this server, just do a reverse lookup and give them the old UUID
        if (servers.get(session).containsValue(server)) {
            for (Map.Entry<UUID,ServerEntry> entry : servers.get(session).entrySet()) {
                if (entry.getValue().equals(server)) {
                    return makeJson(Type.IPP_SERVER, entry.getKey().toString(), uri);
                }
            }
        }

        // Otherwise, slap a new UUID on the server and send the UUID to the user
        UUID serverId = UUID.randomUUID();
        servers.get(session).put(serverId, server);
        return makeJson(Type.IPP_SERVER, serverId.toString(), uri);
    }

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

    public static Object find(Session session, JSONObject params) throws PrinterException, JSONException {
        JSONObject server = params.getJSONObject("server");
        Type type = Type.parse(server.getString("type"));
        switch(type) {
            case IPP_SERVER:
                return findRemote(session,
                                  server.getString("uuid"),
                                  params.optString("query", "")
                );
            default:
                throw new UnsupportedOperationException("Type " + type + " is not yet supported.");
        }
    }

    public static Object findRemote(Session session, String uuidString, String query) throws PrinterException, JSONException {
        HashMap<UUID,ServerEntry> serverEntrySet = servers.get(session);
        if(serverEntrySet == null) {
           throw new PrinterException("No EntrySet found"); // FIXME: Improve wording
        }

        UUID uuid = UUID.fromString(uuidString);
        ServerEntry serverEntry = serverEntrySet.get(uuid);
        if (serverEntry == null) {
            throw new PrinterException("No ServerEntry found"); // FIXME: Improve wording
        }

        IppClient ippClient = new IppClient();
        CupsClient cupsClient = new CupsClient(serverEntry.serverUri, ippClient);
        //cupsClient.setUserName(server.uname); // Requesting username, may not be needed

        if (!serverEntry.uname.isEmpty() && !serverEntry.pass.isEmpty()) {
            cupsClient.basicAuth(serverEntry.uname, serverEntry.pass);
        }

        // If there is no query, list all printers
        if (query.isEmpty()) {
            JSONArray names = new JSONArray();
            for(IppPrinter p: cupsClient.getPrinters()) {
                names.put(makeJson(p, uuid));
            }

            return names;
        } else {
            return makeJson(cupsClient.getPrinter(query), uuid);
        }
    }

    public static JSONObject makeJson(IppPrinter printer, UUID uuid) throws JSONException {
        return new JSONObject()
            .put("name", printer.getName())
            .put("uri", printer.getPrinterUri())
            .put("info", printer.getInfo())
            .put("uuid", uuid.toString()
        );
    }

    public static JSONObject makeJson(Type type, String uuid, URI uri) throws JSONException {
        return new JSONObject()
            .put("type", type.getId())
            .put("uuid", uuid)
            .put("uri", uri.toString()
        );
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

    /**
     * The IPP protocol prohibits query parameters and anchor links, strip them out
     */
    public static URI sanitizeUri(URI uri) throws URISyntaxException {
        return new URI(
            uri.getScheme(),
            uri.getAuthority(),
            uri.getPath()
        );
    }

    public static URI schemeFilter(URI uri) throws URISyntaxException {
        String scheme = uri.getScheme();
        if(scheme != null) {
            for(String supportedScheme : SUPPORTED_SCHEMES) {
                if (supportedScheme.equalsIgnoreCase(scheme)) {
                    return uri;
                }
            }
        }
        throw new URISyntaxException(uri.toString(), "URI contains an invalid or blank scheme");
    }
}