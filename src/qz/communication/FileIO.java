package qz.communication;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.LoggerFactory;
import qz.ws.SocketConnection;

import javax.management.ListenerNotFoundException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

/**
 * Created by Kyle on 12/5/2017.
 */
public class FileIO {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(FileIO.class);

    private static Thread watchThread;
    private static WatchService watchService;
    private static ArrayList<FileClientPair> connectionPairs = new ArrayList<>();

    private synchronized static void startWatchThread() throws IOException {
        if (watchThread != null && watchThread.isAlive()) {
            log.debug("File WatchService is already started, we'll attach to it");
            return;
        }

        log.debug("Starting File WatchService");
        watchService = FileSystems.getDefault().newWatchService();
        watchThread = new Thread(() -> {
            boolean alive = true;

            while(alive) {
                try {
                    WatchKey wk = watchService.take();
                    // pollEvents() blocks, it must be interrupted
                    for(WatchEvent<?> event : wk.pollEvents()) {
                        fileChanged((Path)wk.watchable(), event.context().toString(), event.kind().toString());
                    }
                    wk.reset();
                } catch(InterruptedException | ClosedWatchServiceException closed) {
                    alive = false;
                    log.warn("File WatchService ending");
                    // Notify each subscribed connection once and only once
                    ArrayList<SocketConnection> connections = new ArrayList<>();
                    connectionPairs.stream().filter(f -> !connections.contains(f.getConnection())).forEach(f -> {
                        connections.add(f.getConnection());
                        f.getConnection().getFileListener().sendError("File WatchService ended");
                        f.getConnection().stopListening();
                    });
                    connectionPairs.clear();
                }
            }
        });
        watchThread.start();
    }

    public synchronized static void startListening(FileClientPair pair, FileListener listener) throws IOException {
        if (!pair.getConnection().isFileListening()) {
            pair.getConnection().startFileListening(listener);
        }

        startWatchThread();
        if (!connectionPairs.contains(pair)) {
            pair.registerWatch(watchService);
            connectionPairs.add(pair);
        } else {
            throw new FileAlreadyExistsException(pair.getOriginalPath().toString());
        }
    }

    public synchronized static void closeListener(FileClientPair pair) throws ListenerNotFoundException, IOException {
        if (!connectionPairs.contains(pair)) {
            throw new ListenerNotFoundException();
        }
        connectionPairs.remove(connectionPairs.indexOf(pair)).cancelWatch();
        if (connectionPairs.isEmpty()) {
            watchService.close();
        }
    }

    public synchronized static boolean isListening(SocketConnection client) {
        for (FileClientPair pair : connectionPairs) {
            if (pair.getConnection() == client) return true;
        }
        return false;
    }

    public synchronized static void closeListeners(SocketConnection client) throws IOException {
        for (Iterator<FileClientPair> it = connectionPairs.iterator(); it.hasNext();) {
            if (it.next().getConnection() == client) {
                it.remove();
            }
        }

        if (connectionPairs.isEmpty()) {
            watchService.close();
        }
    }

    private synchronized static void fileChanged(Path path, String fileName, String type) {
        Path filePath = path.resolve(fileName);
        for (FileClientPair f : connectionPairs) {
            String fileData = null;
            FileListener listener = f.getConnection().getFileListener();
            if (f.getAbsolutePath().equals(path.normalize().toAbsolutePath())) {
                if (!type.equals("ENTRY_DELETE") && listener.returnsContents() && !Files.isDirectory(filePath)){
                    try {
                        switch(listener.getReadType()) {
                            case BYTES:
                                fileData = getBytes(filePath, listener);
                                break;
                            case LINES:
                            default:
                                fileData = getLines(filePath, listener);
                        }
                    } catch(IOException e) {
                        log.error("Failed to read file due to {}", e.toString());
                        listener.sendError("Failed to read file data due to " + e.getClass().getName());
                    }
                }
                f.getConnection().getFileListener().fileChanged(f.getOriginalPath().resolve(fileName), type, fileData);
            }
        }

    }

    private synchronized static String getBytes(Path path, FileListener listener) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            byte[] bytes = listener.getBytes() == -1 ? new byte[(int)raf.length()] : new byte[(int)Math.min(raf.length(), listener.getBytes())];
            if (listener.isReversed()) raf.seek(raf.length() - bytes.length);
            raf.readFully(bytes);
            return new String(bytes, Charset.forName("UTF-8"));
        }
    }

    private synchronized static String getLines(Path path, FileListener listener) throws IOException {
        StringBuffer sb = new StringBuffer();
        String buffer;

        if (listener.isReversed()) {
            try (ReversedLinesFileReader reader = new ReversedLinesFileReader(path.toFile())) {
                int count = 0;
                while((buffer = reader.readLine()) != null && count++ != listener.getLines()) {
                    // Warning, this will strip "\r" from the data
                    sb.append(buffer).append("\n");
                }
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
                int count = 0;
                while((buffer = reader.readLine()) != null && count++ != listener.getLines()) {
                    // Warning, this will strip "\r" from the data
                    sb.append(buffer).append("\n");
                }
            }
        }

        return sb.toString();
    }
}
