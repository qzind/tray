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
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import qz.auth.Certificate;
import qz.auth.RequestState;
import qz.common.ByteArrayBuilder;
import qz.common.Constants;
import qz.communication.FileIO;
import qz.communication.FileParams;
import qz.exception.NullCommandException;
import qz.ws.PrintSocketServer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
            "dll", // Microsoft shared library
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

    private static final CharSequenceTranslator translator = new LookupTranslator(new String[][] {
            {"^", "^^"},
            {"\\", "^b"},
            {"/", "^f"},
            {":", "^c"},
            {"*", "^a"},
            {"?", "^m"},
            {"\"", "^q"},
            {"<", "^g"},
            {">", "^l"},
            {"|", "^p"},
            {"" + (char)0x7f, "^d"}
    });

    /* resource files */
    private static HashMap<String,File> localFileMap = new HashMap<>();
    private static HashMap<String,File> sharedFileMap = new HashMap<>();
    private static ArrayList<Map.Entry<Path,String>> whiteList;

    public static Path getAbsolutePath(JSONObject params, RequestState request, boolean allowRootDir) throws JSONException, IOException {
        FileParams fp = new FileParams(params);
        String commonName = request.isTrusted()? escapeFileName(request.getCertName()):"UNTRUSTED";

        Path path = createAbsolutePath(fp, commonName);
        initializeRootFolder(fp, commonName);

        if (!isWhiteListed(path, allowRootDir, fp.isSandbox(), request.getCertUsed())) {
            throw new AccessDeniedException(path.toString());
        }

        if (!allowRootDir && !Files.isDirectory(path)) {
            if (!isGoodExtension(path)) {
                throw new AccessDeniedException(path.toString());
            }
        }

        return path;
    }

    private static void initializeRootFolder(FileParams fileParams, String commonName) throws IOException {
        String parent = fileParams.isShared()? SystemUtilities.getSharedDataDirectory():SystemUtilities.getDataDirectory();

        Path rootPath;
        if (fileParams.isSandbox()) {
            rootPath = Paths.get(parent, Constants.SANDBOX_DIR, commonName);
        } else {
            rootPath = Paths.get(parent, Constants.NOT_SANDBOX_DIR);
        }

        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
        }
    }

    /**
     * Returns a normalised and absolute path. If the input path was relative,
     * the root may reside in one of four locations, based on the sandbox and
     * shared flags. If the input path was absolute, the path will be
     * normalised and returned without any further changes.
     *
     * @param fileParams File or Directory to sandbox
     * @param commonName Common name of the associated certificate for use with sandbox location
     * @return absolute path of input, with relative location's root being determined by the {@code sandbox} and {@code shared} flags.
     */
    public static Path createAbsolutePath(FileParams fileParams, String commonName) {
        Path sanitizedPath;
        if (fileParams.getPath().isAbsolute()) {
            sanitizedPath = fileParams.getPath();
        } else {
            String parent = fileParams.isShared()? SystemUtilities.getSharedDataDirectory():SystemUtilities.getDataDirectory();
            if (fileParams.isSandbox()) {
                sanitizedPath = Paths.get(parent, Constants.SANDBOX_DIR, commonName).resolve(fileParams.getPath());
            } else {
                sanitizedPath = Paths.get(parent, Constants.NOT_SANDBOX_DIR).resolve(fileParams.getPath());
            }
        }

        return sanitizedPath.normalize();
    }

    /**
     * Checks a path's extension against a list of forbidden extensions. If a match is found, false is returned.
     */
    public static boolean isGoodExtension(Path path) {
        String fileName = path.getFileName().toString();

        //"foo.exe." is valid on windows, but is immediately changed to "foo.exe" by the os
        //this is undocumented behavior, therefore, rather than trying to support it, we fail it.
        if (SystemUtilities.isWindows() && fileName.endsWith(".")) { return false; }

        String[] tokens = fileName.split("\\.(?=[^.]+$)");
        if (tokens.length == 2) {
            String extension = tokens[1];
            for(String bad : FileUtilities.badExtensions) {
                if (bad.equalsIgnoreCase(extension)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns whether or not the given file or folder is white-listed for File IO
     * Currently hard-coded to the QZ data directory or anything provided by qz-tray.properties
     * e.g. %APPDATA%/qz/data or $HOME/.qz/data, etc
     */
    public static boolean isWhiteListed(Path path, boolean allowRootDir, boolean sandbox, Certificate cert) {
        String commonName = cert.isTrusted()? escapeFileName(cert.getCommonName()):"UNTRUSTED";
        if (whiteList == null) {
            populateWhiteList();
        }

        Path cleanPath = path.normalize().toAbsolutePath();
        for(Map.Entry<Path,String> allowed : whiteList) {
            if (cleanPath.startsWith(allowed.getKey())) {
                if ("".equals(allowed.getValue()) || allowed.getValue().contains("|" + commonName + "|") && (allowRootDir || !cleanPath.equals(allowed.getKey()))) {
                    return true;
                } else if (allowed.getValue().contains("|sandbox|")) {
                    Path p;
                    if (sandbox) {
                        p = Paths.get(allowed.getKey().toString(), Constants.SANDBOX_DIR, commonName);
                    } else {
                        p = Paths.get(allowed.getKey().toString(), Constants.NOT_SANDBOX_DIR);
                    }
                    if (cleanPath.startsWith(p) && (allowRootDir || !cleanPath.equals(p))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void populateWhiteList() {
        whiteList = new ArrayList<>();
        //default sandbox locations. More can be added through the properties file
        whiteList.add(new AbstractMap.SimpleEntry<>(Paths.get(SystemUtilities.getDataDirectory()), "|sandbox|"));
        whiteList.add(new AbstractMap.SimpleEntry<>(Paths.get(SystemUtilities.getSharedDataDirectory()), "|sandbox|"));

        Properties props = PrintSocketServer.getTrayProperties();
        if (props != null) {
            StringBuilder propString = new StringBuilder(props.getProperty("file.whitelist", ""));
            boolean escaped = false;
            boolean resetPending = false, tokenPending = false;
            ArrayList<String> tokens = new ArrayList<>();
            //unescaper and tokenizer
            for(int i = 0; i < propString.length(); i++) {
                char iteratingChar = propString.charAt(i);
                //if the char before this was an escape char, we are no longer escaped and we skip delimiter detection
                if (escaped) {
                    escaped = false;
                } else {
                    if (iteratingChar == '^') {
                        escaped = true;
                        propString.deleteCharAt(i);
                        i--;
                    } else {
                        tokenPending = iteratingChar == '|' || iteratingChar == ';';
                        resetPending = iteratingChar == ';';
                    }
                }
                //If the last char isn't a ; or |
                if (i == propString.length() - 1) {
                    tokenPending = true;
                    resetPending = true;
                }
                //if a delimiter is found, save string to token and delete it from propString
                if (tokenPending) {
                    tokenPending = false;
                    tokens.add(propString.substring(0, i));
                    propString.delete(0, i + 1);
                    i = -1;
                }
                //if a semicolon was found or we are on the last char of the string, dump the tokens into a pair and add it to whiteList
                if (resetPending) {
                    resetPending = false;
                    String commonNames = tokens.size() > 1? "|":"";
                    for(int n = 1; n < tokens.size(); n++) {
                        commonNames += escapeFileName(tokens.get(n)) + "|";
                    }
                    whiteList.add(new AbstractMap.SimpleEntry<>(Paths.get(tokens.get(0)).normalize().toAbsolutePath(), commonNames));
                    tokens.clear();
                }
            }
        }
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

    /**
     * Escapes invalid chars from filenames. This does not cause collisions. Escape char is "^"
     * Characters escaped, ^ \ / : * ? " < > |</>
     * Warning: Restricted filenames such as lpt1, com1, aux... are not escaped by this function
     *
     * @param fileName file name to escape
     * @return escaped string
     */
    public static String escapeFileName(String fileName) {
        StringBuilder returnStringBuilder = new StringBuilder(translator.translate(fileName));
        for(int n = returnStringBuilder.length() - 1; n >= 0; n--) {
            char c = returnStringBuilder.charAt(n);
            if (c < 0x20) {
                returnStringBuilder.replace(n, n + 1, "^" + String.format("%02d", (int)c));
            }
        }
        return returnStringBuilder.toString();
    }

    public static boolean isSymlink(String filePath) {
        log.info("Verifying symbolic link: {}", filePath);
        boolean returnVal = false;
        if (filePath != null) {
            File f = new File(filePath);
            if (f.exists()) {
                try {
                    File canonicalFile = (f.getParent() == null? f:f.getParentFile().getCanonicalFile());
                    returnVal = !canonicalFile.getCanonicalFile().equals(canonicalFile.getAbsoluteFile());
                }
                catch(IOException ex) {
                    log.error("IOException checking for symlink", ex);
                }
            }
        }

        log.info("Symbolic link result: {}", returnVal);
        return returnVal;
    }

    public static String readLocalFile(String file) throws IOException {
        return new String(readFile(new DataInputStream(new FileInputStream(file))), Charsets.UTF_8);
    }

    public static byte[] readRawFile(String url) throws IOException {
        return readFile(new DataInputStream(ConnectionUtilities.getInputStream(url)));
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


    public static void setupListener(FileIO fileIO) throws IOException {
        FileWatcher.startWatchThread();
        FileWatcher.registerWatch(fileIO);
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
                if (local || (!name.equals(Constants.ALLOW_FILE) && !name.equals(Constants.BLOCK_FILE))) {
                    log.warn("Cannot setup file {} ({})", fileLoc, local? "Local":"Shared", e);
                }
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
