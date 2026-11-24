package qz.auth;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CertificateConcurrencyTests {
    private LinkedHashSet<Certificate> savedRoots;
    private Certificate savedBuiltIn;
    private boolean savedTrustBuiltIn;
    private String savedOverride;

    @BeforeMethod
    public void saveState() {
        savedRoots = Certificate.snapshotRootCAs();
        savedBuiltIn = Certificate.builtIn;
        savedTrustBuiltIn = Certificate.isTrustBuiltIn();
        savedOverride = System.getProperty("authcert.override");
    }

    @AfterMethod(alwaysRun = true)
    public void restoreState() {
        if(savedOverride == null) {
            System.clearProperty("authcert.override");
        } else {
            System.setProperty("authcert.override", savedOverride);
        }

        Certificate.builtIn = savedBuiltIn;
        Certificate.setRootCAs(savedRoots);
        Certificate.setTrustBuiltIn(savedTrustBuiltIn);
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
        CountDownLatch ready = new CountDownLatch(runnables.length);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        ArrayList<Thread> threads = new ArrayList<>();

        Arrays.stream(runnables)
                .map(runnable -> worker(ready, start, runnable, failure))
                .forEach(thread -> {
                    threads.add(thread);
                    thread.start();
                });

        Assert.assertTrue(ready.await(5, TimeUnit.SECONDS), "Certificate operations did not become ready");
        start.countDown();

        for(int i = 0; i < threads.size(); i++) {
            threads.get(i).join(5000); // allow a human + attached debugger some time to investigate
            Assert.assertFalse(threads.get(i).isAlive(), "Certificate operation " + i + " timed out");
        }

        if(failure.get() != null) {
            throw new AssertionError("Concurrent certificate operation failed", failure.get());
        }
    }

    private Thread worker(CountDownLatch ready, CountDownLatch start, Runnable task,
                          AtomicReference<Throwable> failure) {
        Thread worker = new Thread(() -> {
            try {
                ready.countDown();
                if(!start.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Certificate operation was not started");
                }
                task.run();
            }
            catch(Throwable t) {
                failure.compareAndSet(null, t);
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
