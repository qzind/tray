package qz.utils.windows;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class WindowsFileUtilities {
    // Win32 Constants
    private static final int FILE_FLAG_BACKUP_SEMANTICS = 0x02000000;
    private static final int FILE_FLAG_OPEN_REPARSE_POINT = 0x00200000;
    private static final int FILE_SHARE_READ = 0x00000001;
    private static final int FILE_SHARE_WRITE = 0x00000002;
    private static final int FILE_SHARE_DELETE = 0x00000004;
    private static final int OPEN_EXISTING = 3;
    private static final int DELETE_ACCESS = 0x00010001; // DELETE | SYNCHRONIZE

    // File Information Classes
    private static final int FileDispositionInformation = 13;

    public static class FILE_DISPOSITION_INFO extends Structure {
        public boolean DeleteFile;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("DeleteFile");
        }
    }

    /**
     * Recursively walks and deletes all files
     * <ul>
     *     <li>If a regular file, use Java's <code>Files.delete(...)</code></li>
     *     <li>If a regular file, use <code>WindowsFileUtilities.delete(...)</code>
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
        WinNT.HANDLE hDir = null;

        try {
            // Open a handle with reparse protection and backup semantics
            hDir = kernel32.CreateFile(
                    file.toString(),
                    DELETE_ACCESS,
                    FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
                    null,
                    OPEN_EXISTING,
                    FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OPEN_REPARSE_POINT,
                    null
            );

            if (hDir == null || WinNT.INVALID_HANDLE_VALUE.equals(hDir)) {
                throw new Win32Exception(kernel32.GetLastError());
            }

            // Mark the handle for deletion at the OS kernel level
            FILE_DISPOSITION_INFO info = new FILE_DISPOSITION_INFO();
            info.DeleteFile = true;

            boolean success = kernel32.SetFileInformationByHandle(hDir, FileDispositionInformation, info, info.size());

            if (!success) {
                throw new Win32Exception(kernel32.GetLastError());
            }
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            // Close handle; Execute the pending deletion
            if (hDir != null && !WinNT.INVALID_HANDLE_VALUE.equals(hDir)) {
                kernel32.CloseHandle(hDir);
            }
        }
    }
}
