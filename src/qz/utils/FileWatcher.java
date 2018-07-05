package qz.utils;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.communication.FileIO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class FileWatcher {

    private static final Logger log = LoggerFactory.getLogger(FileWatcher.class);

    private static Thread watchThread;
    private static WatchService watchService;

    private static HashSet<FileIO> fileIOs = new HashSet<>();


    public synchronized static void startWatchThread() throws IOException {
        if (watchThread != null && watchThread.isAlive()) {
            log.debug("File WatchService is already started, reusing");
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
                }
                catch(InterruptedException | ClosedWatchServiceException closed) {
                    log.error("File WatchService ending");
                    alive = false;
                }
            }
        });
        watchThread.start();
    }

    public static void registerWatch(FileIO fileIO) throws IOException {
        fileIO.setWk(fileIO.getAbsolutePath().register(watchService,
                                                       StandardWatchEventKinds.ENTRY_MODIFY,
                                                       StandardWatchEventKinds.ENTRY_CREATE,
                                                       StandardWatchEventKinds.ENTRY_DELETE));

        fileIOs.add(fileIO);
    }

    public static void deregisterWatch(FileIO fileIO) {
        fileIOs.remove(fileIO);
    }


    private synchronized static void fileChanged(Path path, String fileName, String type) {
        Path filePath = path.resolve(fileName);
        for(FileIO fio : fileIOs) {
            String fileData = null;
            if (fio.getAbsolutePath().equals(path.normalize().toAbsolutePath())) {
                if (!type.equals("ENTRY_DELETE") && fio.returnsContents() && !Files.isDirectory(filePath)) {
                    try {
                        switch(fio.getReadType()) {
                            case BYTES:
                                fileData = getBytes(filePath, fio);
                                break;
                            case LINES:
                            default:
                                fileData = getLines(filePath, fio);
                        }
                    }
                    catch(IOException e) {
                        log.error("Failed to read file due to {}", e.toString());
                        fio.sendError("Failed to read file data due to " + e.getClass().getName());
                    }
                }

                fio.fileChanged(fileName, type, fileData);
            }
        }

    }

    private synchronized static String getBytes(Path path, FileIO listener) throws IOException {
        try(RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            byte[] bytes = listener.getBytes() == -1? new byte[(int)raf.length()]:new byte[(int)Math.min(raf.length(), listener.getBytes())];
            if (listener.isReversed()) { raf.seek(raf.length() - bytes.length); }
            raf.readFully(bytes);

            return new String(bytes, Charset.forName("UTF-8"));
        }
    }

    private synchronized static String getLines(Path path, FileIO listener) throws IOException {
        ArrayList<String> linesRead = new ArrayList<>();

        String buffer;
        if (listener.isReversed()) {
            try(ReversedLinesFileReader reader = new ReversedLinesFileReader(path.toFile())) {
                int count = 0;
                while((buffer = reader.readLine()) != null && count++ != listener.getLines()) {
                    // Warning, this will strip "\r" from the data
                    linesRead.add(buffer);
                }

                Collections.reverse(linesRead); //ensure last X lines are returned in natural order
            }
        } else {
            try(BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
                int count = 0;
                while((buffer = reader.readLine()) != null && count++ != listener.getLines()) {
                    // Warning, this will strip "\r" from the data
                    linesRead.add(buffer);
                }
            }
        }

        return StringUtils.join(linesRead, "\n");
    }

}
