package qz.ws;

import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.eclipse.jetty.ee8.websocket.api.Session;
import org.eclipse.jetty.ee8.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.ee8.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.ee8.websocket.api.exceptions.UpgradeException;
import org.eclipse.jetty.ee8.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.ee8.websocket.client.WebSocketClient;
import org.eclipse.jetty.ee8.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.ee8.websocket.servlet.WebSocketUpgradeFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ConstantThrowable;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import qz.common.Constants;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SocketCompatibilityTests {
    private static final String ALLOWED_ORIGIN = "https://allowed.example";
    private static final String BLOCKED_ORIGIN = "https://blocked.example";

    private Server server;
    private WebSocketClient client;
    private URI httpUri;
    private URI websocketUri;

    @BeforeClass
    public void startServer() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.addFilter(HttpAboutServlet.originFilter(ALLOWED_ORIGIN), "/*", null);
        context.addFilter(WebSocketUpgradeFilter.class, "/", EnumSet.of(DispatcherType.REQUEST));
        JettyWebSocketServletContainerInitializer.configure(context, (ctx, container) ->
                container.addMapping("/*", (request, response) -> {
                    // Keep QZ's origin decision while using a minimal test endpoint
                    PrintSocketClient accepted = PrintSocketClient.originFilterUpgrade(
                            request, response, server, List.of(ALLOWED_ORIGIN));
                    return accepted == null ? null : new ServerEndpoint();
                }));
        context.addServlet(new ServletHolder(new StatusServlet()), "/json");
        server.setHandler(context);
        server.start();

        int port = connector.getLocalPort();
        httpUri = URI.create("http://127.0.0.1:" + port + "/json");
        websocketUri = URI.create("ws://127.0.0.1:" + port);

        client = new WebSocketClient();
        client.start();
    }

    @AfterClass(alwaysRun = true)
    public void stopServer() throws Exception {
        if (client != null) {
            client.stop();
        }
        if (server != null) {
            server.stop();
        }
    }

    // Keeps the HTTP and private-network headers wired
    @Test
    public void testHttpResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(httpUri)
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Private-Network", "true")
                .timeout(Duration.ofSeconds(5))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        Assert.assertEquals(response.statusCode(), HttpServletResponse.SC_OK);
        Assert.assertEquals(response.headers().firstValue("Access-Control-Allow-Origin").orElse(null), ALLOWED_ORIGIN);
        Assert.assertEquals(response.headers().firstValue("Access-Control-Allow-Private-Network").orElse(null), "true");
    }

    // keeps the single-instance check working over websocket
    @Test
    public void testSingleInstanceCheck() throws Exception {
        ClientEndpoint endpoint = new ClientEndpoint();
        Session session = connect(endpoint, ALLOWED_ORIGIN);
        try {
            session.getRemote().sendString(Constants.PROBE_REQUEST);
            Assert.assertEquals(endpoint.message.get(5, TimeUnit.SECONDS), Constants.PROBE_RESPONSE);
        } finally {
            session.close();
        }
    }

    // Keeps blocked origins from upgrading
    @Test
    public void testBlockedOrigin() {
        ExecutionException failure = Assert.expectThrows(ExecutionException.class,
                () -> connect(new ClientEndpoint(), BLOCKED_ORIGIN));

        Assert.assertTrue(failure.getCause() instanceof UpgradeException);
        Assert.assertEquals(((UpgradeException)failure.getCause()).getResponseStatusCode(),
                HttpServletResponse.SC_FORBIDDEN);
    }

    // keeps a closed signal treated as a closed channel
    @Test
    public void testClosedSocket() throws Exception {
        Assert.expectThrows(ClosedChannelException.class,
                () -> ClosedSocketException.filter(new IOException(new ConstantThrowable("Closed"))));

        ClosedSocketException.filter(new IOException(new ConstantThrowable("Other")));
    }

    private Session connect(ClientEndpoint endpoint, String origin) throws Exception {
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.setHeader("Origin", origin);
        return client.connect(endpoint, websocketUri, request).get(5, TimeUnit.SECONDS);
    }

    private static class StatusServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) {
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    @WebSocket
    public static class ServerEndpoint {
        @OnWebSocketMessage
        public void onMessage(Session session, String message) throws IOException {
            if (Constants.PROBE_REQUEST.equals(message)) {
                session.getRemote().sendString(Constants.PROBE_RESPONSE);
            }
        }
    }

    @WebSocket
    public static class ClientEndpoint {
        private final CompletableFuture<String> message = new CompletableFuture<>();

        @OnWebSocketMessage
        public void onMessage(String value) {
            message.complete(value);
        }
    }
}
