/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.installer.shortcut;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.SystemUtilities;

/**
 * Utility class for creating, querying and removing startup shortcuts and
 * desktop shortcuts.
 *
 * @author Tres Finocchiaro
 */
public abstract class ShortcutCreator {
    private static ShortcutCreator instance;
    protected static final Logger log = LoggerFactory.getLogger(ShortcutCreator.class);
    public abstract boolean canAutoStart();
    public abstract void createDesktopShortcut();

    public static ShortcutCreator getInstance() {
        if (instance == null) {
            if (SystemUtilities.isWindows()) {
                instance = new WindowsShortcutCreator();
            } else if (SystemUtilities.isMac()) {
                instance = new MacShortcutCreator();
            } else {
                instance = new LinuxShortcutCreator();
            }
        }
        return instance;
    }
}
