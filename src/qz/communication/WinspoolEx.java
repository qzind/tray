package qz.communication;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Winspool;
import com.sun.jna.win32.W32APIOptions;

@SuppressWarnings("unused")
public interface WinspoolEx extends Winspool {
    WinspoolEx INSTANCE = Native.load("Winspool.drv", WinspoolEx.class, W32APIOptions.DEFAULT_OPTIONS);

    int JOB_CONTROL_NONE = 0x00000000; // Perform no additional action.
    int JOB_CONTROL_PAUSE = 0x00000001; // Pause the print job.
    int JOB_CONTROL_RESUME = 0x00000002; // Resume a paused print job.
    int JOB_CONTROL_CANCEL = 0x00000003; // Delete a print job.
    int JOB_CONTROL_RESTART = 0x00000004; // Restart a print job.
    int JOB_CONTROL_DELETE = 0x00000005; // Delete a print job.
    int JOB_CONTROL_SENT_TO_PRINTER = 0x00000006; // Used by port monitors to signal that a print job has been sent to the printer. This value SHOULD NOT be used remotely.
    int JOB_CONTROL_LAST_PAGE_EJECTED = 0x00000007; // Used by language monitors to signal that the last page of a print job has been ejected from the printer. This value SHOULD NOT be used remotely.
    int JOB_CONTROL_RETAIN = 0x00000008; // Keep the print job in the print queue after it prints.
    int JOB_CONTROL_RELEASE = 0x00000009; // Release the print job, undoing the effect of a JOB_CONTROL_RETAIN action.

    boolean SetJob(WinNT.HANDLE hPrinter, int JobId, int Level, Pointer pJob, int Command);
}