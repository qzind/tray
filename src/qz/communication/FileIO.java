package qz.communication;

import qz.utils.FileUpdateHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;

public class FileIO {

  private static final Logger log = LoggerFactory.getLogger(FileIO.class);

  private String filePath;
	private FileWatcher watcher;
	private FileUpdateHandler fileUpdateHandler;
	private Boolean receiveCallbacks;

	public FileIO(String filePath, Boolean receiveCallbacks) throws IOException {
		this.filePath = filePath;
		this.receiveCallbacks = receiveCallbacks;
		if (receiveCallbacks) {
			this.watcher = new FileWatcher(new File(this.filePath));
			this.watcher.start();
		}
		log.debug(String.format("new FileIO for %s", filePath));
	}

	public void setFileUpdateHandler(FileUpdateHandler fuh) {
		this.fileUpdateHandler = fuh;
	}

	/**
	 * Writes the buffered data to the File.
	 */
	public void sendData(String data, Boolean append) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, append))) {
			bw.write(data);
		} catch (IOException e) {
			log.error(String.format("Exception writing data to file %s", filePath), e);
		}
	}

	/**
	 * Closes the file, if open.
	 *
	 * @return Boolean indicating success.
	 * @throws IOException
	 */
	public void close() {
		if (receiveCallbacks) {
			try {
				this.watcher.stopWatching();
			} catch (IOException e) {
				log.error(String.format("Exception while stop watching %s", filePath),e);
			}
		}
	}

	public class FileWatcher extends Thread {
		private final File file;
		private AtomicBoolean run = new AtomicBoolean(true);
		private final WatchService watchService;

		public FileWatcher(File file) throws IOException {
			this.file = file;
			final Path path = this.file.toPath().getParent();
			log.debug(String.format("Watching %s", path.toString()));
			this.watchService = FileSystems.getDefault().newWatchService();
			path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
		}

		public void stopWatching() throws IOException {
			this.run.set(false);
			this.watchService.close();
		}

		@Override
		public void run() {
			while (run.get()) {
				try {
					final WatchKey wk = watchService.take();
					for (WatchEvent<?> event : wk.pollEvents()) {
						final Path changed = (Path) event.context();
						final WatchEvent.Kind<?> kind = event.kind();
						if (kind == StandardWatchEventKinds.ENTRY_MODIFY && changed.toString().equals(file.getName())) {
							log.debug(String.format("file %s %s", file.getName(), kind.toString()));
							if (fileUpdateHandler != null) {
								fileUpdateHandler.handleFileUpdate(file);
							}
						}
					}
					// reset the key
					boolean valid = wk.reset();
					if (!valid) {
						log.debug("Key has been unregistered");
					}
					Thread.yield();
				} catch (InterruptedException ex) {
				} catch (ClosedWatchServiceException ex) {
					if (run.get()) {
						throw ex;
					}
				}
			}
		}
	}
}
