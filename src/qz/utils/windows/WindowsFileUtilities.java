package qz.utils.windows;

import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static com.sun.jna.platform.win32.WinNT.*;

public class WindowsFileUtilities {
    /**
     * Recursively walks and deletes all files
     * <ul>
     *     <li>If a regular file, use Java's <code>Files.delete(...)</code></li>
     *     <li>If a directory, use <code>WindowsFileUtilities.delete(...)</code>
     *     which uses a file lock to avoid carefully timed file swaps</li>
     * </ul>
     */
    public static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Delete individual file (no traversal risk)
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Locks and deletes a file to mitigate time-of-check/time-of-use risks with symlinks/junctions
     */
    static void delete(Path file) throws IOException {
        Kernel32Ex kernel32 = Kernel32Ex.INSTANCE;
        HANDLE hDir = null;

        try {
            // Open a handle with reparse protection and backup semantics
            hDir = kernel32.CreateFile(
                    file.toString(),
                    DELETE | SYNCHRONIZE,
                    FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
                    null,
                    OPEN_EXISTING,
                    FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OPEN_REPARSE_POINT,
                    null
            );

            if (hDir == null || INVALID_HANDLE_VALUE.equals(hDir)) {
                throw new Win32Exception(kernel32.GetLastError());
            }

            // Mark the handle for deletion at the OS kernel level
            WinBase.FILE_DISPOSITION_INFO info =
                    new WinBase.FILE_DISPOSITION_INFO();

            info.DeleteFile = true;

            boolean success = kernel32.SetFileInformationByHandle(hDir, WinBase.FileDispositionInfo, info, info.size());

            if (!success) {
                throw new Win32Exception(kernel32.GetLastError());
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            // Close handle; Execute the pending deletion
            if (hDir != null && !INVALID_HANDLE_VALUE.equals(hDir)) {
                kernel32.CloseHandle(hDir);
            }
        }
    }
}
