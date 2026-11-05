package qz.utils;

import org.codehaus.jettison.json.JSONArray;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Test(singleThreaded = true)
public class NetworkUtilitiesTests {
    @Test
    public void testConcurrentInitialization() throws Exception {
        List<NetworkInterface> interfaces = networkInterfaces();
        CountDownLatch enumerationStarted = new CountDownLatch(1);
        CountDownLatch continueEnumeration = new CountDownLatch(1);
        try(ServerSocket endpoint = endpoint(); ExecutorService workers = Executors.newFixedThreadPool(2)) {
            PausedNetworkUtilities utilities = new PausedNetworkUtilities(endpoint.getLocalPort(), interfaces,
                    enumerationStarted, continueEnumeration);
            CountDownLatch secondCallStarted = new CountDownLatch(1);
            Future<JSONArray> firstCall = workers.submit(() -> utilities.getDevicesJSON());

            try {
                await(enumerationStarted);
                Future<JSONArray> secondCall = workers.submit(() -> {
                    secondCallStarted.countDown();
                    return utilities.getDevicesJSON();
                });
                await(secondCallStarted);
                assertWaiting(secondCall);

                continueEnumeration.countDown();
                JSONArray firstResult = firstCall.get(5, TimeUnit.SECONDS);
                JSONArray secondResult = secondCall.get(5, TimeUnit.SECONDS);

                Assert.assertEquals(secondResult.toString(), firstResult.toString());
                Assert.assertEquals(secondResult.length(), interfaces.size());
                Assert.assertEquals(utilities.enumerationCount.get(), 1);
            } finally {
                continueEnumeration.countDown();
            }
        }
    }

    @Test
    public void testEndpointCache() throws Exception {
        try(ServerSocket firstEndpoint = endpoint(); ServerSocket secondEndpoint = endpoint()) {
            NetworkUtilities first = NetworkUtilities.getInstance("127.0.0.1", firstEndpoint.getLocalPort());
            NetworkUtilities repeated = NetworkUtilities.getInstance("127.0.0.1", firstEndpoint.getLocalPort());
            NetworkUtilities changed = NetworkUtilities.getInstance("127.0.0.1", secondEndpoint.getLocalPort());

            Assert.assertSame(repeated, first);
            Assert.assertNotSame(changed, first);
        }
    }

    private static List<NetworkInterface> networkInterfaces() throws Exception {
        return Collections.list(NetworkInterface.getNetworkInterfaces());
    }

    private static ServerSocket endpoint() throws Exception {
        return new ServerSocket(0, 2, InetAddress.getLoopbackAddress());
    }

    private static void await(CountDownLatch latch) {
        try {
            Assert.assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for test coordination");
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void assertWaiting(Future<JSONArray> call) throws Exception {
        try {
            JSONArray partial = call.get(1, TimeUnit.SECONDS);
            Assert.fail("Published partial device list with " + partial.length() + " entries");
        } catch(TimeoutException expected) {
            // The second call should wait for initialization
        }
    }

    private static class PausedNetworkUtilities extends NetworkUtilities {
        private final List<NetworkInterface> interfaces;
        private final CountDownLatch enumerationStarted;
        private final CountDownLatch continueEnumeration;
        private final AtomicInteger enumerationCount = new AtomicInteger();

        PausedNetworkUtilities(int port, List<NetworkInterface> interfaces,
                               CountDownLatch enumerationStarted, CountDownLatch continueEnumeration) {
            super("127.0.0.1", port);
            this.interfaces = interfaces;
            this.enumerationStarted = enumerationStarted;
            this.continueEnumeration = continueEnumeration;
        }

        @Override
        List<NetworkInterface> getNetworkInterfaces() {
            enumerationCount.incrementAndGet();
            enumerationStarted.countDown();
            await(continueEnumeration);
            return interfaces;
        }
    }
}
