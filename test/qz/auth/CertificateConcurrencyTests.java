package qz.auth;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class CertificateConcurrencyTests {
    private List<Certificate> savedRoots;
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
    public void testStableRoots() throws Exception {
        // root changes must not alter a validation snapshot
        Certificate root = installTestRoot();
        List<Certificate> snapshot = Certificate.snapshotRootCAs();

        Certificate.setTrustBuiltIn(false);

        Assert.assertEquals(snapshot.size(), 1);
        Assert.assertSame(snapshot.getFirst(), root);
        Assert.assertTrue(Certificate.rootCAs.isEmpty());
    }

    @Test
    public void testBuiltInOnce() throws Exception {
        // concurrent enables must publish the built-in root once
        Certificate root = installTestRoot();
        Certificate.setTrustBuiltIn(false);

        runTogether(() -> Certificate.setTrustBuiltIn(true),
                () -> Certificate.setTrustBuiltIn(true));

        Assert.assertEquals(Certificate.rootCAs.size(), 1);
        Assert.assertSame(Certificate.rootCAs.getFirst(), root);
        Assert.assertTrue(Certificate.isTrustBuiltIn());
        Assert.assertFalse(Certificate.hasAdditionalCAs());
    }

    @Test
    public void testOverrideOnce() throws Exception {
        // concurrent rescans must publish one copy of an override
        Certificate.setRootCAs(List.of());
        Certificate.setTrustBuiltIn(false);
        System.setProperty("authcert.override", rootFixture().toString());

        runTogether(Certificate::scanAdditionalCAs, Certificate::scanAdditionalCAs);

        Assert.assertEquals(Certificate.rootCAs.size(), 1);
    }

    private Certificate installTestRoot() throws Exception {
        Certificate root = new Certificate(Files.readString(rootFixture()));
        Certificate.builtIn = root;
        Certificate.setRootCAs(List.of());
        Certificate.setTrustBuiltIn(true);
        return root;
    }

    private void runTogether(Runnable first, Runnable second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread firstThread = worker(start, first, failure);
        Thread secondThread = worker(start, second, failure);

        firstThread.start();
        secondThread.start();
        start.countDown();
        firstThread.join(5000);
        secondThread.join(5000);

        Assert.assertFalse(firstThread.isAlive(), "First certificate operation timed out");
        Assert.assertFalse(secondThread.isAlive(), "Second certificate operation timed out");

        if(failure.get() != null) {
            throw new AssertionError("Concurrent certificate operation failed", failure.get());
        }
    }

    private Thread worker(CountDownLatch start, Runnable task, AtomicReference<Throwable> failure) {
        Thread worker = new Thread(() -> {
            try {
                start.await();
                task.run();
            }
            catch(Throwable t) {
                failure.compareAndSet(null, t);
            }
        });
        worker.setDaemon(true);
        return worker;
    }

    private Path rootFixture() throws Exception {
        URL resourceUrl = getClass().getResource("resources/certificate-chain/ca-root.crt");
        Assert.assertNotNull(resourceUrl, "Missing certificate fixture ca-root.crt");
        return Path.of(resourceUrl.toURI());
    }
}
