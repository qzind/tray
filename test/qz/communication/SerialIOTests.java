package qz.communication;

import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SerialIOTests {
    private static final String PORT_NAME = "TEST_PORT";
    private static final int EXPECTED_READ_TIMEOUT = 1200;

    private static SerialIO openedSerialWith(FakeSerialPortAdapter fake) throws SerialPortException {
        SerialIO serial = serialWith(fake);
        Assert.assertTrue(serial.open(new SerialOptions()));
        return serial;
    }

    private static SerialIO serialWith(FakeSerialPortAdapter fake) {
        return new SerialIO(PORT_NAME, null, portName -> fake);
    }

    private static SerialIO serialWith(FakeSerialPortAdapter first, FakeSerialPortAdapter second) {
        AtomicInteger creates = new AtomicInteger();
        // SerialIO creates the native adapter during open
        // so return one fake per open
        return new SerialIO(PORT_NAME, null, portName -> creates.getAndIncrement() == 0 ? first : second);
    }

    private static SerialPortEvent rxEvent(int value) {
        return new SerialPortEvent(PORT_NAME, SerialPortEvent.RXCHAR, value);
    }

    private static void expectSerialPortException(SerialAction action) throws Exception {
        try {
            action.run();
            Assert.fail("Expected SerialPortException");
        } catch (SerialPortException expected) {
        }
    }

    @Test
    public void closeShouldIgnoreAlreadyClosedSerial() {
        FakeSerialPortAdapter fake = new FakeSerialPortAdapter();
        SerialIO serial = serialWith(fake);

        serial.close();

        Assert.assertEquals(fake.closeCalls, 0);
        Assert.assertFalse(serial.isOpen());
    }

    @Test
    public void closeShouldOnlyCloseNativePortOnce() throws Exception {
        FakeSerialPortAdapter fake = new FakeSerialPortAdapter();
        SerialIO serial = openedSerialWith(fake);

        serial.close();
        serial.close();

        Assert.assertEquals(fake.closeCalls, 1);
        Assert.assertFalse(serial.isOpen());
    }

    @Test
    public void sendDataAfterCloseShouldNotWriteToNativePort() throws Exception {
        FakeSerialPortAdapter fake = new FakeSerialPortAdapter();
        SerialIO serial = openedSerialWith(fake);

        serial.close();

        expectSerialPortException(() -> serial.sendData(new JSONObject().put("data", "hello"), null));
        Assert.assertEquals(fake.writeCalls, 0);
    }

    @Test
    public void processSerialEventAfterCloseShouldNotReadNativePort() throws Exception {
        FakeSerialPortAdapter fake = new FakeSerialPortAdapter();
        SerialIO serial = openedSerialWith(fake);

        serial.close();
        String output = serial.processSerialEvent(rxEvent(5));

        Assert.assertNull(output);
        Assert.assertEquals(fake.readCalls, 0);
    }

    @Test
    public void processSerialEventShouldUseConfiguredReadTimeout() throws Exception {
        FakeSerialPortAdapter fake = new FakeSerialPortAdapter();
        fake.timeoutOnRead = true;
        SerialIO serial = openedSerialWith(fake);

        String output = serial.processSerialEvent(rxEvent(5));

        Assert.assertNull(output);
        Assert.assertEquals(fake.readCalls, 1);
        Assert.assertEquals(fake.lastReadTimeout, EXPECTED_READ_TIMEOUT);
        Assert.assertTrue(serial.isOpen());
    }

    @Test
    public void closePortFalseShouldCloseQzStatePredictably() throws Exception {
        FakeSerialPortAdapter fake = new FakeSerialPortAdapter();
        fake.closeResult = false;
        SerialIO serial = openedSerialWith(fake);

        serial.close();

        Assert.assertEquals(fake.closeCalls, 1);
        Assert.assertFalse(serial.isOpen());
        expectSerialPortException(() -> serial.sendData(new JSONObject().put("data", "hello"), null));
        Assert.assertEquals(fake.writeCalls, 0);
    }

    @Test
    public void closePortExceptionShouldCloseQzStatePredictably() throws Exception {
        FakeSerialPortAdapter fake = new FakeSerialPortAdapter();
        fake.throwOnClose = true;
        SerialIO serial = openedSerialWith(fake);

        serial.close();

        Assert.assertEquals(fake.closeCalls, 1);
        Assert.assertFalse(serial.isOpen());
        expectSerialPortException(() -> serial.sendData(new JSONObject().put("data", "hello"), null));
        Assert.assertEquals(fake.writeCalls, 0);
    }

    @Test
    public void pendingReadResultShouldBeIgnoredAfterCloseRequest() throws Exception {
        FakeSerialPortAdapter fake = new FakeSerialPortAdapter();
        fake.readBytes = "late data".getBytes(StandardCharsets.UTF_8);
        fake.blockRead = true;
        SerialIO serial = openedSerialWith(fake);

        final String[] output = new String[1];
        Thread reader = new Thread(() -> output[0] = serial.processSerialEvent(rxEvent(fake.readBytes.length)));
        reader.start();

        // Hold readBytes open so close can
        // move QZ state to CLOSED first
        Assert.assertTrue(fake.readStarted.await(2, TimeUnit.SECONDS), "Timed out waiting for fake read");
        serial.close();
        fake.releaseRead.countDown();
        reader.join(2000);

        Assert.assertFalse(reader.isAlive(), "Reader thread did not finish");
        Assert.assertNull(output[0]);
        Assert.assertEquals(fake.readCalls, 1);
        Assert.assertFalse(serial.isOpen());
    }

    @Test
    public void reopenAfterSuccessfulCloseShouldUseNewNativePort() throws Exception {
        FakeSerialPortAdapter first = new FakeSerialPortAdapter();
        FakeSerialPortAdapter second = new FakeSerialPortAdapter();
        SerialIO serial = serialWith(first, second);

        Assert.assertTrue(serial.open(new SerialOptions()));
        serial.close();
        Assert.assertFalse(serial.isOpen());

        Assert.assertTrue(serial.open(new SerialOptions()));
        Assert.assertTrue(serial.isOpen());
        Assert.assertEquals(first.closeCalls, 1);
        Assert.assertEquals(second.openCalls, 1);
    }

    @Test
    public void failedCloseShouldAllowClearReopenAttempt() throws Exception {
        FakeSerialPortAdapter first = new FakeSerialPortAdapter();
        FakeSerialPortAdapter second = new FakeSerialPortAdapter();
        first.closeResult = false;
        SerialIO serial = serialWith(first, second);

        Assert.assertTrue(serial.open(new SerialOptions()));
        serial.close();
        Assert.assertFalse(serial.isOpen());

        // QZ releases its stale adapter so a later
        // open gets a clean native attempt
        Assert.assertTrue(serial.open(new SerialOptions()));
        Assert.assertTrue(serial.isOpen());
        Assert.assertEquals(first.closeCalls, 1);
        Assert.assertEquals(second.openCalls, 1);
    }

    // Lets expectSerialPortException accept
    // lambdas with checked exceptions
    private interface SerialAction {
        void run() throws Exception;
    }

    // Small fake instead of a mock
    // this avoids adding a test dependency for one narrow seam
    // Model only the JSSC behavior SerialIO owns
    // and that keeps the testing hardware-free
    private static class FakeSerialPortAdapter implements SerialPortAdapter {
        boolean opened;
        boolean closeResult = true;
        boolean throwOnClose;
        boolean throwOnRead;
        boolean timeoutOnRead;
        boolean blockRead;
        byte[] readBytes = new byte[0];
        byte[] writtenBytes;
        int openCalls;
        int closeCalls;
        int readCalls;
        int writeCalls;
        int lastReadTimeout;
        SerialPortEventListener listener;
        CountDownLatch readStarted = new CountDownLatch(1);
        CountDownLatch releaseRead = new CountDownLatch(1);

        @Override
        public boolean openPort() {
            openCalls++;
            opened = true;
            return true;
        }

        @Override
        public boolean closePort() throws SerialPortException {
            closeCalls++;
            if (throwOnClose) {
                throw new SerialPortException(PORT_NAME, "closePort", "test close failure");
            }
            opened = false;
            return closeResult;
        }

        @Override
        public boolean isOpened() {
            return opened;
        }

        @Override
        public void addEventListener(SerialPortEventListener listener) {
            // Listener registration only proves
            // SerialIO reached an open state
            this.listener = listener;
        }

        @Override
        public void setParams(int baudRate, int dataBits, int stopBits, int parity) {
            // Option application is not under test here
            // allow setup to continue
        }

        @Override
        public void setFlowControlMode(int mask) {
            // Flow control is outside the
            // lifecycle behavior these tests exercise
        }

        @Override
        public void writeBytes(byte[] data) {
            writeCalls++;
            writtenBytes = Arrays.copyOf(data, data.length);
        }

        @Override
        public byte[] readBytes(int byteCount, int timeout) throws SerialPortException, SerialPortTimeoutException {
            readCalls++;
            lastReadTimeout = timeout;
            if (blockRead) {
                readStarted.countDown();
                try {
                    releaseRead.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (throwOnRead) {
                throw new SerialPortException(PORT_NAME, "readBytes", "test read failure");
            }
            if (timeoutOnRead) {
                throw new SerialPortTimeoutException(PORT_NAME, "readBytes", timeout);
            }
            return readBytes;
        }
    }
}