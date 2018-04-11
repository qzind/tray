package qz.communication;

import qz.auth.Certificate;
import qz.utils.FileUtilities;
import qz.ws.SocketConnection;

import java.io.IOException;
import java.nio.file.*;

/**
 * Created by Kyle on 12/8/2017.
 */
public class FileClientPair {
    private Path originalPath;
    private Path absolutePath;

    private SocketConnection connection;
    private WatchKey wk;

    public FileClientPair(Path originalPath, Path absolutePath, SocketConnection connection) throws IOException {
        this.connection = connection;
        this.originalPath = originalPath;
        this.absolutePath = absolutePath;
    }

    SocketConnection getConnection() {
        return connection;
    }

    Path getAbsolutePath() {
        return absolutePath;
    }

    Path getOriginalPath() {
        return originalPath;
    }

    public void registerWatch(WatchService watchService) throws IOException {
        wk = getAbsolutePath().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
    }

    public void cancelWatch() {
        wk.cancel();
    }

    @Override
    public boolean equals (Object o) {
        if (o instanceof FileClientPair){
            FileClientPair o1 = (FileClientPair)o;
            return ((connection == o1.connection) && (o1.getAbsolutePath().compareTo(absolutePath) == 0));
        }
        return false;
    }
}
