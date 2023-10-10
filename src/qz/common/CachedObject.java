package qz.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * A generic class that encapsulates an object for caching. The cached object
 * will be refreshed automatically when accessed after its lifespan has expired.
 *
 * @param <T> The type of object to be cached.
 */
public class CachedObject<T> {
    public static final long DEFAULT_LIFESPAN = 5000; // in milliseconds
    T lastObject;
    Supplier<T> supplier;
    private long timestamp;
    private long lifespan;

    /**
     * Creates a new CachedObject with a default lifespan of 5000 milliseconds
     *
     * @param supplier The function to pull new values from
     */
    public CachedObject(Supplier<T> supplier) {
        this(supplier, DEFAULT_LIFESPAN);
    }

    /**
     * Creates a new CachedObject
     *
     * @param supplier The function to pull new values from
     * @param lifespan The lifespan of the cached object in milliseconds
     */
    public CachedObject(Supplier<T> supplier, long lifespan) {
        this.supplier = supplier;
        setLifespan(lifespan);
        timestamp = Long.MIN_VALUE;  // System.nanoTime() can be negative, MIN_VALUE guarantees a first-run.
    }

    /**
     * Registers a new supplier for the CachedObject
     *
     * @param supplier The function to pull new values from
     */
    @SuppressWarnings("unused")
    public void registerSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /**
     * Sets the lifespan of the cached object
     *
     * @param milliseconds The lifespan of the cached object in milliseconds
     */
    public void setLifespan(long milliseconds) {
        lifespan = Math.max(0, milliseconds); // prevent overflow
    }

    /**
     * Retrieves the cached object.
     * If the cached object's lifespan has expired, it gets refreshed before being returned.
     *
     * @return The cached object
     */
    public T get() {
        return get(false);
    }

    /**
     * Retrieves the cached object.
     * If the cached object's lifespan is expired or forceRefresh is true, it gets refreshed before being returned.
     *
     * @param forceRefresh If true, the cached object will be refreshed before being returned regardless of its lifespan
     * @return The cached object
     */
    public T get(boolean forceRefresh) {
        long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        // check lifespan
        if (forceRefresh || (timestamp + lifespan <= now)) {
            timestamp = now;
            lastObject = supplier.get();
        }
        return lastObject;
    }

    // Test
    public static void main(String ... args) throws InterruptedException {
        final AtomicInteger testInt = new AtomicInteger(0);

        CachedObject<Integer> cachedString = new CachedObject<>(testInt::incrementAndGet);
        for(int i = 0; i < 100; i++) {
            Thread.sleep(1500);
            System.out.println(cachedString.get());
        }
    }
}
