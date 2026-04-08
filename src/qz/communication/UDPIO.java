package qz.communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.utils.DeviceUtilities;
import qz.utils.NetworkUtilities;
import qz.ws.SocketConnection;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;

public class UDPIO implements NetworkIO {

    private static final Logger log = LogManager.getLogger(UDPIO.class);

    private String host;
    private int port;
    private int localPort;
    private Charset encoding;

    private DatagramSocket socket;
    private InetAddress address;

    public UDPIO(String host, int port, Charset encoding, SocketConnection websocket) {
        this(host, port, 0, encoding, websocket);
    }

    public UDPIO(String host, int port, int localPort, Charset encoding, SocketConnection websocket) {
        this.host = host;
        this.port = port;
        this.localPort = localPort;
        this.encoding = encoding;
    }


    @Override
    public boolean open() throws IOException {
        address = InetAddress.getByName(host);
        if (localPort > 0) {
            socket = new DatagramSocket(localPort);
        } else {
            socket = new DatagramSocket();
        }
        socket.setSoTimeout(NetworkUtilities.SOCKET_TIMEOUT);
        log.info("Opened UDP socket on local port {} to {}:{}", socket.getLocalPort(), host, port);
        return true;
    }

    @Override
    public boolean isOpen() {
        return socket != null && !socket.isClosed();
    }

    @Override
    public void sendData(JSONObject params) throws JSONException, IOException {
        byte[] data = DeviceUtilities.getDataBytes(params, encoding);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        log.debug("Sending UDP data ({} bytes) to {}:{}", data.length, host, port);
        socket.send(packet);
    }

    @Override
    public byte[] processResponse() throws IOException {
        byte[] buffer = new byte[8192];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(packet);
            if (packet.getLength() > 0) {
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                log.debug("Received UDP packet ({} bytes) from {}:{}", data.length, packet.getAddress(), packet.getPort());
                return data;
            }
        } catch (SocketTimeoutException e) {
            // Normal for UDP listening, just return null so the loop continues
            return null;
        }
        return null;
    }

    @Override
    public void close() {
        if (socket != null) {
            socket.close();
            log.info("Closed UDP socket on local port {}", socket.getLocalPort());
        }
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }
}
