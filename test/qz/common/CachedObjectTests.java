package qz.common;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CachedObjectTests {
    @Test
    public void testConcurrentColdStartNeverReturnsNull() throws Exception {
        final CountDownLatch supplierEntered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger(0);
        CachedObject<String> cache = new CachedObject<>(() -> {
            calls.incrementAndGet();
            supplierEntered.countDown();
            try {
                release.await();
            } catch(InterruptedException ignore) {}
            return "value";
        }, 60000);

        final AtomicReference<String> first = new AtomicReference<>();
        Thread t = new Thread(() -> first.set(cache.get()));
        t.start();
        supplierEntered.await();

        // Second caller arrives while the first is still inside the supplier
        final AtomicReference<String> second = new AtomicReference<>();
        Thread t2 = new Thread(() -> second.set(cache.get()));
        t2.start();
        t2.join(300);
        release.countDown();
        t.join();
        t2.join();

        Assert.assertEquals(first.get(), "value");
        Assert.assertEquals(second.get(), "value");
        Assert.assertEquals(calls.get(), 1);
    }
}
