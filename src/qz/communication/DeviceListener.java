package qz.communication;

public interface DeviceListener {
    /**
     * Cleanup task for when a socket closes while a device is still streaming
     */
    void close();

}
