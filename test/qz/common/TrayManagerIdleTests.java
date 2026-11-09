package qz.common;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TrayManagerIdleTests {
    /**
     * Timer whose cancel() pauses after cancelling so a concurrent performIfIdle(...) can run after
     * voidIdleActions()'s cancel() but before it clears idleTimer.
     */
    private static class SlowCancelTimer extends Timer {
        final CountDownLatch cancelling = new CountDownLatch(1);

        SlowCancelTimer() {
            super(true);
        }

        @Override
        public void cancel() {
            super.cancel();
            cancelling.countDown();
            try {
                Thread.sleep(300);
            } catch(InterruptedException ignore) {}
        }
    }

    @Test
    public void testIdleActionsConcurrentVoidAndSchedule() throws Exception {
        // Avoid the Swing-heavy constructor, only the idle timer is under test
        java.lang.reflect.Constructor<?> objCtor = Object.class.getDeclaredConstructor();
        sun.reflect.ReflectionFactory rf = sun.reflect.ReflectionFactory.getReflectionFactory();
        @SuppressWarnings("unchecked")
        java.lang.reflect.Constructor<TrayManager> ctor = (java.lang.reflect.Constructor<TrayManager>)rf.newConstructorForSerialization(TrayManager.class, objCtor);
        TrayManager tray = ctor.newInstance();

        Field timerField = TrayManager.class.getDeclaredField("idleTimer");
        timerField.setAccessible(true);
        SlowCancelTimer timer = new SlowCancelTimer();
        timerField.set(tray, timer);

        Method perform = TrayManager.class.getDeclaredMethod("performIfIdle", int.class, java.awt.event.ActionListener.class);
        perform.setAccessible(true);

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread voider = new Thread(tray::voidIdleActions);
        voider.start();
        Assert.assertTrue(timer.cancelling.await(5, TimeUnit.SECONDS));

        Thread scheduler = new Thread(() -> {
            try {
                perform.invoke(tray, 60000, (java.awt.event.ActionListener)evt -> {});
            } catch(Throwable t) {
                failure.set(t.getCause() != null ? t.getCause() : t);
            }
        });
        scheduler.start();
        voider.join(5000);
        scheduler.join(5000);

        Assert.assertNull(failure.get(), "performIfIdle failed during concurrent voidIdleActions: " + failure.get());
    }
}
