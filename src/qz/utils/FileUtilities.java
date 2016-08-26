/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.utils;

import org.apache.commons.io.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import qz.common.ByteArrayBuilder;
import qz.common.Constants;
import qz.exception.NullCommandException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.util.HashMap;


/**
 * Common static file i/o utilities
 *
 * @author Tres Finocchiaro
 */
public class FileUtilities {

    private static final Logger log = LoggerFactory.getLogger(FileUtilities.class);

    private static final String[] badExtensions = new String[] {
            "exe", "pif", "paf", "application", "msi", "com", "cmd", "bat", "lnk", // Windows Executable program or script
            "gadget", // Windows desktop gadget
            "msp", "mst", // Microsoft installer patch/transform file
            "cpl", "scr", "ins", // Control Panel/Screen Saver/Internet Settings
            "hta", // HTML application, run as trusted application without sandboxing
            "msc", // Microsoft Management Console file
            "jar", "jnlp", // Java Executable
            "vb", "vbs", "vbe", "js", "jse", "ws", "wsf", "wsc", "wsh",// Windows Script
            "ps1", "ps1xml", "ps2", "ps2xml", "ps1", "ps1xml", "ps2", "ps2xml", "psc1", "psc2", // Windows PowerShell script
            "msh", "msh1", "msh2", "mshxml", "msh1xml", "msh2xml", // Monad/PowerShell script
            "scf", "inf", // Windows Explorer/AutoRun command file
            "reg", // Windows Registry file
            "doc", "docx", "dot", "dotx", "dotm", // Microsoft Word
            "xls", "xlt", "xlm", "xlsx", "xlsm", "xltx", "xltm", "xlsb", "xla", "xlam", "xll", "xlw", // Microsoft Excel
            "ppt", "pps", "pptx", "pptm", "potx", "potm", "ppam", "ppsx", "ppsm", "sldx", "sldm", // Microsoft PowerPoint
            "ade", "adp", "adn", "accdb", "accdr", "accdt", "mdb", "mda", "mdn", "mdt", // Microsoft Access
            "mdw", "mdf", "mde", "accde", "mam", "maq", "mar", "mat", "maf", "ldb", "laccdb", // Microsoft Access
            "app", "action", "bin", "command", "workflow", // Mac OS Application/Executable
            "sh", "ksh", "csh", "pl", "py", "bash", "run",  // Unix Script
            "ipa, apk", // iOS/Android App
            "widget", // Yahoo Widget
            "url" // Internet Shortcut
    };

    /* resource files */
    private static HashMap<String,File> localFileMap = new HashMap<>();
    private static HashMap<String,File> sharedFileMap = new HashMap<>();


    public static boolean isBadExtension(String fileName) {
        String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
        if (tokens.length == 2) {
            String extension = tokens[1];
            for(String bad : FileUtilities.badExtensions) {
                if (bad.equalsIgnoreCase(extension)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns whether or not the supplied path is restricted, such as the qz-tray data directory
     * Warning:  This does not follow symlinks
     *
     * @param path File or Directory path to test
     * @return {@code true} if restricted, {@code false} otherwise
     */
    public static boolean isBadPath(String path) {
        if (SystemUtilities.isWindows()) {
            // Case insensitive
            return path.toLowerCase().contains(SystemUtilities.getDataDirectory().toLowerCase());
        }

        return path.contains(SystemUtilities.getDataDirectory());
    }


    public static String readLocalFile(String file) throws IOException {
        return new String(readFile(new DataInputStream(new FileInputStream(file))), Charsets.UTF_8);
    }

    public static byte[] readRawFile(String url) throws IOException {
        return readFile(new DataInputStream(new URL(url).openStream()));
    }

    private static byte[] readFile(DataInputStream in) throws IOException {
        ByteArrayBuilder cmds = new ByteArrayBuilder();
        byte[] buffer = new byte[Constants.BYTE_BUFFER_SIZE];

        int len;
        while((len = in.read(buffer)) > -1) {
            byte[] temp = new byte[len];
            System.arraycopy(buffer, 0, temp, 0, len);
            cmds.append(temp);
        }
        in.close();

        return cmds.getByteArray();
    }


    /**
     * Reads an XML file from URL, searches for the tag specified by
     * {@code dataTag} tag name and returns the {@code String} value
     * of that tag.
     *
     * @param url     location of the xml file to be read
     * @param dataTag tag in the file to be searched
     * @return value of the tag if found
     */
    public static String readXMLFile(String url, String dataTag) throws DOMException, IOException, NullCommandException,
                                                                        ParserConfigurationException, SAXException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url);
        doc.getDocumentElement().normalize();
        log.info("Root element " + doc.getDocumentElement().getNodeName());

        NodeList nodeList = doc.getElementsByTagName(dataTag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }

        throw new NullCommandException(String.format("Node \"%s\" could not be found in XML file specified", dataTag));
    }


    public static boolean printLineToFile(String fileName, String message) {
        File file = getFile(fileName, true);
        if (file == null) { return false; }

        try(FileWriter fw = new FileWriter(file, true)) {
            message += "\r\n";
            fw.write(message);
            fw.flush();
            return true;
        }
        catch(IOException e) {
            log.error("Cannot write to file {}", fileName, e);
        }

        return false;
    }

    public static File getFile(String name, boolean local) {
        HashMap<String,File> fileMap;
        if (local) {
            fileMap = localFileMap;
        } else {
            fileMap = sharedFileMap;
        }

        if (!fileMap.containsKey(name) || fileMap.get(name) == null) {
            String fileLoc;
            if (local) {
                fileLoc = SystemUtilities.getDataDirectory();
            } else {
                fileLoc = SystemUtilities.getSharedDirectory();
            }

            File locDir = new File(fileLoc);
            File file = new File(fileLoc + File.separator + name + ".dat");

            try {
                locDir.mkdirs();
                file.createNewFile();
            }
            catch(IOException e) {
                //failure is possible due to user permissions on shared files
                log.error("Cannot setup file {} ({})", name, local? "Local":"Shared", e);
            }

            if (file.exists()) {
                fileMap.put(name, file);
            }
        }

        return fileMap.get(name);
    }

    public static void deleteFile(String name) {
        File file = localFileMap.get(name);

        if (file != null && !file.delete()) {
            log.warn("Unable to delete file {}", name);
            file.deleteOnExit();
        }

        localFileMap.put(name, null);
    }

    public static boolean deleteFromFile(String fileName, String deleteLine) {
        File file = getFile(fileName, true);
        File temp = getFile(Constants.TEMP_FILE, true);

        try(BufferedReader br = new BufferedReader(new FileReader(file)); BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {
            String line;
            while((line = br.readLine()) != null) {
                if (!line.equals(deleteLine)) {
                    bw.write(line + "\r\n");
                }
            }

            bw.flush();
            bw.close();
            br.close();

            deleteFile(fileName);
            return temp.renameTo(file);
        }
        catch(IOException e) {
            log.error("Unable to delete line from file", e);
            return false;
        }
    }

}
