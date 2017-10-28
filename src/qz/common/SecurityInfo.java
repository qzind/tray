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
        versionList.put("hid4java", null);
        versionList.put("jna.Native", Native.VERSION_NATIVE);
        versionList.put("jna", Native.VERSION);
        versionList.put("jssc", jssc.SerialNativeInterface.getLibraryBaseVersion());
        versionList.put("jetty", Jetty.VERSION);
        versionList.put("pdfbox", org.apache.pdfbox.util.Version.getVersion());
        versionList.put("purejavahidapi", PureJavaHidApi.getVersion());
        versionList.put("usb-api", javax.usb.Version.getApiVersion());
        versionList.put("not-yet-commons-ssl", org.apache.commons.ssl.Version.VERSION);
        versionList.put("mslinks", null);
        versionList.put("simplersa", null);
        //For all other libs, we get the version from metadata or the classloader
        putLibVersion("jetty-servlet", "org.eclipse.jetty.servlet");
        putLibVersion("apache-log4j-extras", "org.apache.log4j");
        putLibVersion("jetty-io", "org.eclipse.jetty.io");
        putLibVersion("websocket-common", "org.eclipse.jetty.websocket.common");
        putLibVersion("slf4j-log4j12", "org.slf4j.impl");
        putLibVersion("usb4java-javax", "org.usb4java.javax");
        putLibVersion("java-semver", "com.github.zafarkhaja.semver");
        putLibVersion("commons-pool2", "org.apache.commons.pool2");
        putLibVersion("websocket-server", "org.eclipse.jetty.websocket.server");
        putLibVersion("jettison", "org.codehaus.jettison");
        putLibVersion("commons-codec", "org.apache.commons.codec");
        putLibVersion("log4j", "org.apache.log4j");
        putLibVersion("slf4j-api", "org.slf4j");
        putLibVersion("websocket-servlet", "org.eclipse.jetty.websocket.servlet");
        putLibVersion("jetty-http", "org.eclipse.jetty.http");
        putLibVersion("commons-lang3", "org.apache.commons.lang3");
        putLibVersion("javax-websocket-server-impl", "org.eclipse.jetty.websocket.jsr356.server");
        putLibVersion("javax.servlet-api", "javax.servlet");
        putLibVersion("usb4java", "org.usb4java");
        putLibVersion("websocket-api", "org.eclipse.jetty.websocket.api");
        putLibVersion("jetty-util", "org.eclipse.jetty.util");
        putLibVersion("websocket-client", "org.eclipse.jetty.websocket.client");
        putLibVersion("javax.websocket-api", "javax.websocket");
        putLibVersion("commons-io", "org.apache.commons.io");

        return versionList;
    }

    public static void putLibVersion (String name, String packageName) {
        versionList.put(name, getLibVersion(name,packageName));
    }

    public static String getLibVersion (String name, String packageName) {
        //todo try getting name from classloader
        //if not, look at the jar metadata
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
