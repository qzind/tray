package qz.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
/**
 * Created by Tres on 12/16/2015.
 */
public class PropertyHelper extends Properties {
    private static final Logger log = LogManager.getLogger(TrayManager.class);
    private String file;

    /**
     * Default constructor
     */
    public PropertyHelper() {
        super();
    }

    /**
     * Default constructor
     * @param p Initial Properties
     */
    public PropertyHelper(Properties p) {
        super(p);
    }

    /**
     * Custom constructor, attempts to load from file
     * @param file File to load properties from
     */
    public PropertyHelper(String file) {
        super();
        this.file = file;
        load(file);
    }

    public PropertyHelper(File file) {
        this(file == null ? null : file.getAbsolutePath());
    }

    public boolean getBoolean(String key, boolean defaultVal) {
        String prop = getProperty(key);
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        } else {
            return defaultVal;
        }
    }

    public void setProperty(String key, boolean value) {
        setProperty(key, "" + value);
    }

    public void load(File file) {
        load(file == null ? null : file.getAbsolutePath());
    }

    public void load(String file) {
        FileInputStream f = null;
        try {
            f = new FileInputStream(file);
            load(f);
        } catch (IOException e) {
            log.warn("Could not load file: {}, reason: {}", file, e.getLocalizedMessage());
        } finally {
            if (f != null) {
                try { f.close(); } catch(Throwable ignore) {};
            }
        }
    }

    public boolean save() {
        boolean success = false;
        FileOutputStream f = null;
        try {
            f = new FileOutputStream(file);
            this.store(f, null);
            success = true;
        } catch (IOException e) {
            log.error("Error saving file: {}", file, e);
        } finally {
            if (f != null) {
                try { f.close(); } catch(Throwable ignore) {};
            }
        }
        return success;
    }
}
