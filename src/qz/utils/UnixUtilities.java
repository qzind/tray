/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2021 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper functions for both Linux and MacOS
 */
public class UnixUtilities {
    private static final Logger log = LoggerFactory.getLogger(UnixUtilities.class);
    private static Integer pid;

    static String getHostName() {
        String hostName = null;
        try {
            byte[] bytes = new byte[255];
            if (LibC.INSTANCE.gethostname(bytes, bytes.length) == 0) {
                hostName = Native.toString(bytes);
            }
        } catch(Throwable ignore) {}
        return hostName;
    }

    static int getProcessId() {
        if(pid == null) {
            try {
                pid = UnixUtilities.CLibrary.INSTANCE.getpid();
            }
            catch(UnsatisfiedLinkError | NoClassDefFoundError e) {
                log.warn("Could not obtain process ID.  This usually means JNA isn't working.  Returning -1.");
                pid = -1;
            }
        }
        return pid;
    }

    private interface CLibrary extends Library {
        CLibrary INSTANCE = Native.load("c", CLibrary.class);
        int getpid();
    }
}
