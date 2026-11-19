package qz.auth;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class CertificateConcurrencyTests {
    private final Certificate qzRoot = Certificate.builtIn;

    @AfterMethod(alwaysRun = true)
    public void restoreState() {
        Certificate.setRootCAs(); // clear
        System.clearProperty("authcert.override");
        Certificate.builtIn = qzRoot;
        Certificate.setTrustBuiltIn(true);
    }

    @Test
    public void testBuiltInOverride() throws Exception {
        singleBuiltIn(new Certificate(customRootCa()));
        Assert.assertEquals(Certificate.rootCAs.size(), 1);
    }
    
    @Test
    public void testBuiltInRemoval() throws Exception {
        singleBuiltIn(new Certificate(customRootCa()));
        Certificate.setTrustBuiltIn(false);
        Assert.assertTrue(Certificate.rootCAs.isEmpty());
    }

    @Test
    public void testBuiltInRace() throws Exception {
        Certificate root = singleBuiltIn(new Certificate(customRootCa()));
        Certificate.setTrustBuiltIn(false);

        // ensure we start from zero
        Assert.assertEquals(Certificate.rootCAs.size(), 0);

        race(
                () -> Certificate.setTrustBuiltIn(true),
                () -> Certificate.setTrustBuiltIn(true),
                () -> Certificate.setTrustBuiltIn(true),
                () -> Certificate.setTrustBuiltIn(true)
        );

        // dupes should only be added once
        Assert.assertEquals(Certificate.rootCAs.size(), 1);
        Assert.assertSame(Certificate.rootCAs.getFirst(), root);
        Assert.assertTrue(Certificate.isTrustBuiltIn());
        Assert.assertFalse(Certificate.hasAdditionalCAs());
    }

    @Test
    public void testOverrideRace() throws Exception {
        Certificate.setRootCAs(); // clear
        Certificate.setTrustBuiltIn(false);

        // ensure we start from zero
        Assert.assertEquals(Certificate.rootCAs.size(), 0);
        
        System.setProperty("authcert.override", customRootCaPath().toString());

        race(
                Certificate::scanAdditionalCAs,
                Certificate::scanAdditionalCAs,
                Certificate::scanAdditionalCAs,
                Certificate::scanAdditionalCAs
        );

        // dupes should only be added once
        Assert.assertEquals(Certificate.rootCAs.size(), 1);
    }

    private Certificate singleBuiltIn(Certificate builtIn) throws Exception {
        Certificate.setRootCAs(); // clear
        Certificate.builtIn = builtIn;
        Certificate.setTrustBuiltIn(true);
        return Certificate.builtIn;
    }

    private void race(Runnable ... runnables) throws Exception {
        CountDownLatch latch = new CountDownLatch(runnables.length);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        ArrayList<Thread> threads = new ArrayList<>();

        Arrays.stream(runnables)
                .map(runnable -> worker(latch, runnable, failure))
                .forEach(thread -> {
                    threads.add(thread);
                    thread.start();
                });
        
        for(int i = 0; i < threads.size(); i++) {
            threads.get(i).join(5000); // allow a human + attached debugger some time to investigate
            Assert.assertFalse(threads.get(i).isAlive(), "Certificate operation " + i + " timed out");
        }
        
        // Wait for all threads to finish
        latch.await();

        if(failure.get() != null) {
            throw new AssertionError("Concurrent certificate operation failed", failure.get());
        }
    }

    private Thread worker(CountDownLatch latch, Runnable task, AtomicReference<Throwable> failure) {
        Thread worker = new Thread(() -> {
            try {
                task.run();
            }
            catch(Throwable t) {
                failure.compareAndSet(null, t);
            }
            finally {
                latch.countDown();
            }
        });
        worker.setDaemon(true);
        return worker;
    }
    
    private Path customRootCaPath() throws Exception {
        URL resourceUrl = getClass().getResource("resources/certificate-chain/ca-root.crt");
        Assert.assertNotNull(resourceUrl, "Missing certificate ca-root.crt");
        return Path.of(resourceUrl.toURI());
    }

    private String customRootCa() throws Exception {
        return Files.readString(customRootCaPath());
    }
}
