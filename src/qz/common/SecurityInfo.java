package qz.common;

import com.sun.jna.Native;
import org.eclipse.jetty.util.Jetty;
import purejavahidapi.PureJavaHidApi;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by Kyle on 10/27/2017.
 */
public class SecurityInfo {
    private static HashMap<String, String> libVersionMap;
    private static SortedMap<String, String> versionList;

    public static SortedMap<String, String> getAllLibVersions(){
        versionList = new TreeMap<String,String>();
        //This is the preferred way to get versions, but not every lib saves its version internally
        versionList.put("jna.Native",           Native.VERSION_NATIVE);
        versionList.put("jna",                  Native.VERSION);
        versionList.put("jssc",                 jssc.SerialNativeInterface.getLibraryBaseVersion());
        versionList.put("jetty",                Jetty.VERSION);
        versionList.put("pdfbox",               org.apache.pdfbox.util.Version.getVersion());
        versionList.put("purejavahidapi",       PureJavaHidApi.getVersion());
        versionList.put("usb-api",              javax.usb.Version.getApiVersion());
        versionList.put("not-yet-commons-ssl",  org.apache.commons.ssl.Version.VERSION);
        versionList.put("mslinks",              null);
        versionList.put("simplersa",            null);
        versionList.put("bouncycastle",         null);
        versionList.put("hid4java",             null);
        //For all other libs, we get the version from metadata or the classloader
        putLibVersion("jetty-servlet");
        putLibVersion("apache-log4j-extras");
        putLibVersion("jetty-io");
        putLibVersion("websocket-common");
        putLibVersion("slf4j-log4j12");
        putLibVersion("usb4java-javax");
        putLibVersion("java-semver");
        putLibVersion("commons-pool2");
        putLibVersion("websocket-server");
        putLibVersion("jettison");
        putLibVersion("commons-codec");
        putLibVersion("log4j");
        putLibVersion("slf4j-api");
        putLibVersion("websocket-servlet");
        putLibVersion("jetty-http");
        putLibVersion("commons-lang3");
        putLibVersion("javax-websocket-server-impl");
        putLibVersion("javax.servlet-api");
        putLibVersion("usb4java");
        putLibVersion("websocket-api");
        putLibVersion("jetty-util");
        putLibVersion("websocket-client");
        putLibVersion("javax.websocket-api");
        putLibVersion("commons-io");
        return versionList;
    }

    public static void putLibVersion (String name) {
        versionList.put(name, getLibVersion(name));
    }

    public static String getLibVersion (String name) {
        if (libVersionMap == null){
            libVersionMap = getVersionMap();
        }
        try {
            return libVersionMap.get(name);
        } catch(Exception e) {
            return null;
        }
    }

    private static HashMap<String,String> getVersionMap () {
        //Results by <lib name, version>
        final HashMap<String,String> resultMap = new HashMap<>();
        try {
            //Hack to get a ref to our jar
            URI jarLocation = new URI("jar:" + SecurityInfo.class.getProtectionDomain().getCodeSource().getLocation().toString());
            //This jdk1.7x nio util lets us look into the jar, without it we would need ZipStream
            FileSystem fs = FileSystems.newFileSystem(jarLocation, new HashMap<String,String>());

            Files.walkFileTree(fs.getPath("/META-INF/maven"), new HashSet<FileVisitOption>(), 3, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".properties")) {
                        try {
                            List<String> data = Files.readAllLines(file, Charset.defaultCharset());
                            String id = data.get(4);
                            id = id.substring(id.lastIndexOf('=') + 1);
                            String version = data.get(2);
                            version = version.substring(version.lastIndexOf('=') + 1);
                            resultMap.put(id, version);
                        }
                        catch(Exception ignore) {}
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch(Exception ignore) {
            return new HashMap<>();
        }
        return resultMap;
    }

}
