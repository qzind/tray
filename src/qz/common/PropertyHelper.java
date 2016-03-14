package qz.common;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Tres on 12/16/2015.
 */
public class PropertyHelper extends Properties {
    private static Logger log = Logger.getLogger(PropertyHelper.class.getName());
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

    public boolean getBoolean(String key, boolean defaultVal) {
        try {
            return Boolean.parseBoolean(getProperty(key));
        } catch (Throwable t) {
            return defaultVal;
        }
    }

    public void setProperty(String key, boolean value) {
        setProperty(key, "" + value);
    }

    public void load(String file) {
        FileInputStream f = null;
        try {
            f = new FileInputStream(file);
            load(f);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error loading " + file, e);
        } finally {
            if (f != null) {
                try { f.close(); } catch(Throwable ignore) {};
            }
        }
    }

    public void save() {
        FileOutputStream f = null;
        try {
            f = new FileOutputStream(file);
            this.store(f, null);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error saving " + file, e);
        } finally {
            if (f != null) {
                try { f.close(); } catch(Throwable ignore) {};
            }
        }
    }
}
