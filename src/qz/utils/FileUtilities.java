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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import qz.App;
import qz.auth.Certificate;
import qz.auth.RequestState;
import qz.common.ByteArrayBuilder;
import qz.common.Constants;
import qz.common.PropertyHelper;
import qz.communication.FileIO;
import qz.communication.FileParams;
import qz.exception.NullCommandException;
import qz.installer.WindowsSpecialFolders;
import qz.installer.certificate.CertificateManager;
import qz.installer.provision.ProvisionInstaller;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static qz.common.Constants.*;

/**
 * Common static file i/o utilities
 *
 * @author Tres Finocchiaro
 */
public class FileUtilities {

    private static final Logger log = LogManager.getLogger(FileUtilities.class);
    public static final Path USER_DIR = getUserDirectory();
    public static final Path SHARED_DIR = getSharedDirectory();
    public static final Path TEMP_DIR = getTempDirectory();
    public static final char FILE_SEPARATOR = ';';
    public static final char FIELD_SEPARATOR = '|';
    public static final char ESCAPE_CHAR = '^';

    /**
     * Zips up the USER_DIR, places on desktop with timestamp
     */
    public static boolean zipLogs() {
        String date = new SimpleDateFormat("yyyy-MM-dd_HHmm").format(new Date());
        String filename = Constants.DATA_DIR + "-" + date + ".zip";
        Path destination = getUserDesktop().resolve(filename);

        try {
            zipDirectory(USER_DIR, destination);
            log.info("Zipped the contents of {} and placed the resulting files in {}", USER_DIR, destination);
            return true;
        } catch(IOException e) {
            log.warn("Could not create zip file: {}", destination, e);
        }
        return false;
    }

    protected static void zipDirectory(Path sourceDir, Path outputFile) throws IOException {
        final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(outputFile.toFile()));
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                try {
                    Path targetFile = sourceDir.relativize(file);
                    outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
                    byte[] bytes = Files.readAllBytes(file);
                    outputStream.write(bytes, 0, bytes.length);
                    outputStream.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return FileVisitResult.CONTINUE;
            }
        });
        outputStream.close();
    }

    /**
     * Location where user Desktop is located
     */
    private static Path getUserDesktop() {
        // OneDrive will override the default location on Windows
        if(SystemUtilities.isWindows()) {
            try {
                return Paths.get(WindowsSpecialFolders.DESKTOP.getPath());
            }
            catch(Throwable ignore) {}
        }
        return Paths.get(System.getProperty("user.home"), "Desktop");
    }

    /**
     * Location where preferences and logs are kept
     */
    private static Path getUserDirectory() {
        if(SystemUtilities.isWindows()) {
            try {
                return Paths.get(WindowsSpecialFolders.ROAMING_APPDATA.getPath(), Constants.DATA_DIR);
            } catch(Throwable ignore) {
                return Paths.get(System.getenv("APPDATA"), Constants.DATA_DIR);
            }
        } else if(SystemUtilities.isMac()) {
            return Paths.get(System.getProperty("user.home"), "/Library/Application Support/", Constants.DATA_DIR);
        } else {
            return Paths.get(System.getProperty("user.home"), "." + Constants.DATA_DIR);
        }
    }

    /**
     * Location where shared preferences are kept, such as .autostart
     */
    private static Path getSharedDirectory() {
        if(SystemUtilities.isWindows()) {
            try {
                return Paths.get(WindowsSpecialFolders.PROGRAM_DATA.getPath(), Constants.DATA_DIR);
            } catch(Throwable ignore) {
                return Paths.get(System.getenv("PROGRAMDATA"), Constants.DATA_DIR);
            }
        } else if(SystemUtilities.isMac()) {
            return Paths.get("/Library/Application Support/", Constants.DATA_DIR);
        } else {
            return Paths.get("/srv/", Constants.DATA_DIR);
        }
    }

    public static boolean childOf(File childFile, Path parentPath) {
        Path child = childFile.toPath().normalize().toAbsolutePath();
        Path parent = parentPath.normalize().toAbsolutePath();
        return child.startsWith(parent);
    }

    public static Path inheritParentPermissions(Path filePath) {
        if(SystemUtilities.isWindows()) {
            // assume permissions are inherited
        } else {
            // assume permissions are not inherited
            try {
                FileAttribute<Set<PosixFilePermission>> attributes = PosixFilePermissions.asFileAttribute(Files.getPosixFilePermissions(filePath.getParent()));
                Files.setPosixFilePermissions(filePath, attributes.value());
                // Remove execute flag
                filePath.toFile().setExecutable(false, false);
            } catch(IOException e) {
                log.warn("Unable to inherit file permissions {}", filePath, e);
            }
        }
        return filePath;

    }

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
            "app", "action", "command", "workflow", // Mac OS Application/Executable
            "sh", "ksh", "csh", "pl", "py", "bash", "run",  // Unix Script
            "ipa", "apk", // iOS/Android App
            "widget", // Yahoo Widget
            "url" // Internet Shortcut
    };

    private static final CharSequenceTranslator translator = new LookupTranslator(new String[][] {
            {"" + ESCAPE_CHAR, "" + ESCAPE_CHAR + ESCAPE_CHAR},
            {"\\", ESCAPE_CHAR + "b"},
            {"/", ESCAPE_CHAR + "f"},
            {":", ESCAPE_CHAR + "c"},
            {"*", ESCAPE_CHAR + "a"},
            {"?", ESCAPE_CHAR + "m"},
            {"\"", ESCAPE_CHAR + "q"},
            {"<", ESCAPE_CHAR + "g"},
            {">", ESCAPE_CHAR + "l"},
            {"|", ESCAPE_CHAR + "p"},
            {"" + (char)0x7f, ESCAPE_CHAR + "d"}
    });

    /* resource files */
    private static HashMap<String,File> localFileMap = new HashMap<>();
    private static HashMap<String,File> sharedFileMap = new HashMap<>();
    private static ArrayList<Map.Entry<Path,String>> whiteList;
    private static boolean FILE_IO_ENABLED = true;
    private static boolean FILE_IO_STRICT = false;

    public static void setFileIoEnabled(boolean enabled) {
        FILE_IO_ENABLED = enabled;
    }

    public static void setFileIoStrict(boolean strict) {
        FILE_IO_STRICT = strict;
    }

    /**
     * Performs security checks before allowing File IO operations:
     *    1. Is the request verified (was the signature OK?)?
     *    2. Is the certificate valid?
     *    3. Is the location whitelisted?
     *    4. Is the file extension permitted
     */
    private static void checkFileRequest(Path path, FileParams fp, RequestState request, boolean allowRootDir) throws AccessDeniedException {
        if(!FILE_IO_ENABLED) {
            throw new AccessDeniedException("File operations are disabled");
        } else if(!request.isVerified() && FILE_IO_STRICT) {
            throw new AccessDeniedException("File request is not verified");
        } else if(request.getCertUsed() == null || !request.getCertUsed().isTrusted()) {
            throw new AccessDeniedException("Certificate provided is not trusted");
        } else if(!isWhiteListed(path, allowRootDir, fp.isSandbox(), request)) {
            throw new AccessDeniedException("File operation is not in a permitted location");
        } else if(!allowRootDir && !Files.isDirectory(path)) {
            if (!isGoodExtension(path)) {
                throw new AccessDeniedException(path.toString());
            }
        }
    }

    public static Path getAbsolutePath(JSONObject params, RequestState request, boolean allowRootDir) throws JSONException, IOException {
        return getAbsolutePath(params, request, allowRootDir, false);
    }

    public static Path getAbsolutePath(JSONObject params, RequestState request, boolean allowRootDir, boolean createMissing) throws JSONException, IOException {
        FileParams fp = new FileParams(params);
        String commonName = request.isVerified()? escapeFileName(request.getCertName()):"UNTRUSTED";

        Path path = createAbsolutePath(fp, commonName);
        checkFileRequest(path, fp, request, allowRootDir);
        initializeRootFolder(fp, commonName);

        if (createMissing) {
            if (!SystemUtilities.isWindows()) {
                Path resolve;
                // Find existing parental directory
                for(resolve = path.getParent(); !Files.exists(resolve); resolve = resolve.getParent()) {
                    // do nothing
                }
                Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(resolve);
                FileAttribute<Set<PosixFilePermission>> fileAttributes = PosixFilePermissions.asFileAttribute(permissions);
                Files.createDirectories(path.getParent(), fileAttributes);
            } else {
                Files.createDirectories(path.getParent());
            }
        }

        return path;
    }

    private static void initializeRootFolder(FileParams fileParams, String commonName) throws IOException {
        Path parent = fileParams.isShared()? SHARED_DIR:USER_DIR;

        Path rootPath;
        if (fileParams.isSandbox()) {
            rootPath = Paths.get(parent.toString(), FileIO.SANDBOX_DATA_SUFFIX, commonName);
        } else {
            rootPath = Paths.get(parent.toString(), FileIO.GLOBAL_DATA_SUFFIX);
        }

        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
            if(fileParams.isShared()) {
                rootPath.toFile().setWritable(true, false);
            }
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
            Path parent = fileParams.isShared()? SHARED_DIR:USER_DIR;
            if (fileParams.isSandbox()) {
                sanitizedPath = Paths.get(parent.toString(), FileIO.SANDBOX_DATA_SUFFIX, commonName).resolve(fileParams.getPath());
            } else {
                sanitizedPath = Paths.get(parent.toString(), FileIO.GLOBAL_DATA_SUFFIX).resolve(fileParams.getPath());
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
            for(String bad : badExtensions) {
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
    public static boolean isWhiteListed(Path path, boolean allowRootDir, boolean sandbox, RequestState request) {
        String commonName = request.isVerified()? escapeFileName(request.getCertName()):"UNTRUSTED";
        if (whiteList == null) {
            whiteList = new ArrayList<>();
            //default sandbox locations. More can be added through the properties file
            whiteList.add(new AbstractMap.SimpleEntry<>(USER_DIR, FIELD_SEPARATOR + "sandbox" + FIELD_SEPARATOR));
            whiteList.add(new AbstractMap.SimpleEntry<>(SHARED_DIR, FIELD_SEPARATOR + "sandbox" + FIELD_SEPARATOR));
            whiteList.addAll(parseDelimitedPaths(getFileAllowProperty(App.getTrayProperties()).toString()));
        }

        Path cleanPath = path.normalize().toAbsolutePath();
        for(Map.Entry<Path,String> allowed : whiteList) {
            if (cleanPath.startsWith(allowed.getKey())) {
                if ("".equals(allowed.getValue()) || allowed.getValue().contains(FIELD_SEPARATOR + commonName + FIELD_SEPARATOR) && (allowRootDir || !cleanPath.equals(allowed.getKey()))) {
                    return true;
                } else if (allowed.getValue().contains(FIELD_SEPARATOR + "sandbox" + FIELD_SEPARATOR)) {
                    Path p;
                    if (sandbox) {
                        p = Paths.get(allowed.getKey().toString(), FileIO.SANDBOX_DATA_SUFFIX, commonName);
                    } else {
                        p = Paths.get(allowed.getKey().toString(), FileIO.GLOBAL_DATA_SUFFIX);
                    }
                    if (cleanPath.startsWith(p) && (allowRootDir || !cleanPath.equals(p))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static ArgParser.ExitStatus addFileAllowProperty(String path, String commonName) throws IOException {
        PropertyHelper props = new PropertyHelper(new File(CertificateManager.getWritableLocation(), Constants.PROPS_FILE + ".properties"));
        ArrayList<Map.Entry<Path, String>> paths = parseDelimitedPaths(getFileAllowProperty(props).toString(), false);
        Iterator<Map.Entry<Path, String>> iterator = paths.iterator();
        String commonNameEscaped = escapePathProperty(commonName);
        // First, iterate to see if the path already exists
        boolean found = false;
        boolean updated = false;
        while(iterator.hasNext()) {
            Map.Entry<Path, String> value = iterator.next();
            if(value.getKey().toString().equals(path)) {
                found = true;
                if(!commonNameEscaped.isEmpty() && !value.getValue().contains(commonNameEscaped)) {
                    value.setValue((value.getValue().isEmpty() ? FIELD_SEPARATOR : value.getValue()) + commonNameEscaped + FIELD_SEPARATOR);
                    updated = true;
                }
            }
        }
        if(!found) {
            paths.add(new AbstractMap.SimpleEntry<>(Paths.get(path).normalize().toAbsolutePath(), commonNameEscaped.isEmpty() ? "" : FIELD_SEPARATOR + commonNameEscaped + FIELD_SEPARATOR));
            updated = true;
        }
        if(updated) {
            if(saveFileAllowProperty(props, paths)) {
                log.info("Added \"file.allow\" entry to {}.properties.", Constants.PROPS_FILE);
                return ArgParser.ExitStatus.SUCCESS;
            }
            return ArgParser.ExitStatus.GENERAL_ERROR;
        } else {
            log.warn("Skipping \"file.allow\" entry in {}.properties, it already exist.", Constants.PROPS_FILE);
            return ArgParser.ExitStatus.SUCCESS;
        }
    }

    public static ArgParser.ExitStatus removeFileAllowProperty(String path) throws IOException {
        PropertyHelper props = new PropertyHelper(new File(CertificateManager.getWritableLocation(), Constants.PROPS_FILE + ".properties"));
        ArrayList<Map.Entry<Path, String>> paths = parseDelimitedPaths(getFileAllowProperty(props).toString(), false);

        int before = paths.size();
        Iterator<Map.Entry<Path, String>> iterator = paths.iterator();
        while(iterator.hasNext()) {
            Map.Entry<Path, String> value = iterator.next();
            if(value.getKey().toString().equals(path)) {
                iterator.remove();
            }
        }
        if(paths.size() != before) {
            if(saveFileAllowProperty(props, paths)) {
                log.info("Removed \"file.allow\" entry from {}.properties.", Constants.PROPS_FILE);
                return ArgParser.ExitStatus.SUCCESS;
            }
            return ArgParser.ExitStatus.GENERAL_ERROR;
        } else {
            log.warn("Skipping \"file.allow\" entry in {}.properties, it doesn't exist.", Constants.PROPS_FILE);
            return ArgParser.ExitStatus.SUCCESS;
        }
    }

    private static boolean saveFileAllowProperty(PropertyHelper props, ArrayList<Map.Entry<Path, String>> paths) {
        StringBuilder fileAllow = new StringBuilder();
        for(Map.Entry<Path, String> path : paths) {
            fileAllow
                    .append(escapePathProperty(path.getKey().toString()))
                    .append(path.getValue())
                    .append(FILE_SEPARATOR);
        }
        props.remove("file.whitelist");
        props.remove("file.allow");
        if(fileAllow.length() > 0) {
            props.setProperty("file.allow", fileAllow.toString());
        }
        return props.save();
    }

    /**
     * Escapes <code>ESCAPE_CHARACTER</code>, <code>FILE_SEPARATOR</code> and <code>FIELD_SEPARATOR</code>
     * so it a multi-value file path can be safely parsed later
     */
    private static String escapePathProperty(String path) {
        return path == null ? "" : path
                .replace("" + ESCAPE_CHAR, ("" + ESCAPE_CHAR) + ESCAPE_CHAR)
                .replace("" + FILE_SEPARATOR, ("" + ESCAPE_CHAR) + FILE_SEPARATOR)
                .replace("" + FIELD_SEPARATOR, ("" + ESCAPE_CHAR) + FIELD_SEPARATOR);
    }

    private static StringBuilder getFileAllowProperty(Properties props) {
        StringBuilder propString = new StringBuilder();
        if(props != null) {
            propString.append(props.getProperty("file.allow", ""));
            if (propString.length() == 0) {
                // Deprecated
                propString.append(props.getProperty("file.whitelist", ""));
                if (propString.length() > 0) {
                    log.warn("Property \"file.whitelist\" is deprecated and will be removed in a future version.  Please use \"file.allow\" instead.");
                }
            }
        }
        return propString;
    }

    /**
     * Parses semi-colon delimited paths with optional pipe-delimited descriptions
     * e.g. C:\file1.txt;C:\file2.txt
     *      C:\file1.txt|ABC Inc.;C:\file2.txt|XYZ Inc.
     */
    public static ArrayList<Map.Entry<Path, String>> parseDelimitedPaths(String delimited, boolean escapeCommonNames) {
        ArrayList<Map.Entry<Path, String>> foundPaths = new ArrayList<>();
        if (delimited != null) {
            StringBuilder propString = new StringBuilder(delimited);
            boolean escaped = false;
            boolean resetPending = false, tokenPending = false;
            ArrayList<String> tokens = new ArrayList<>();
            //unescape and tokenize
            for(int i = 0; i < propString.length(); i++) {
                char iteratingChar = propString.charAt(i);
                //if the char before this was an escape char, we are no longer escaped and we skip delimiter detection
                if (escaped) {
                    escaped = false;
                } else {
                    if (iteratingChar == ESCAPE_CHAR) {
                        escaped = true;
                        propString.deleteCharAt(i);
                        i--;
                    } else {
                        tokenPending = iteratingChar == FIELD_SEPARATOR || iteratingChar == FILE_SEPARATOR;
                        resetPending = iteratingChar == FILE_SEPARATOR;
                    }
                }
                boolean lastChar = (i == propString.length() - 1);
                //if a delimiter is found, save string to token and delete it from propString
                if (tokenPending || lastChar) {
                    String token = propString.substring(0, lastChar && !tokenPending ? i + 1 : i);
                    if (!token.isEmpty()) tokens.add(token);
                    propString.delete(0, i + 1);
                    i = -1;
                    tokenPending = false;
                }
                //if a semicolon was found or we are on the last char of the string, dump the tokens into a pair and add it to whiteList
                if (resetPending || lastChar) {
                    resetPending = false;
                    String commonNames = tokens.size() > 1? "" + FIELD_SEPARATOR:"";
                    for(int n = 1; n < tokens.size(); n++) {
                        if(escapeCommonNames) {
                            commonNames += escapeFileName(tokens.get(n)) + FIELD_SEPARATOR;
                        } else {
                            // We still need to maintain some level of escaping for reserved characters
                            // this is just going to cause headaches later, but we don't have much choice
                            commonNames += escapePathProperty(tokens.get(n));
                            if(!commonNames.endsWith("" + FIELD_SEPARATOR)) {
                                commonNames += FIELD_SEPARATOR;
                            }
                        }
                    }
                    foundPaths.add(new AbstractMap.SimpleEntry<>(Paths.get(tokens.get(0)).normalize().toAbsolutePath(), commonNames));
                    tokens.clear();
                }
            }
        }
        return foundPaths;
    }

    public static ArrayList<Map.Entry<Path, String>> parseDelimitedPaths(Properties props, String key) {
        return parseDelimitedPaths(props == null ? null : props.getProperty(key));
    }

    public static ArrayList<Map.Entry<Path, String>> parseDelimitedPaths(String delimited) {
        return parseDelimitedPaths(delimited, false);
    }

    /**
     * Returns whether or not the supplied path is restricted, such as the qz-tray data directory
     * Warning:  This does not follow symlinks
     *
     * @param path File or Directory path to test
     * @return {@code true} if restricted, {@code false} otherwise
     */
    public static boolean isBadPath(String path) {
        return childOf(new File(path), USER_DIR);
    }

    /**
     * Escapes invalid chars from filenames. This does not cause collisions. Escape char is <code>ESCAPE_CHAR</code>
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
                returnStringBuilder.replace(n, n + 1, ESCAPE_CHAR + String.format("%02d", (int)c));
            }
        }
        return returnStringBuilder.toString();
    }

    public static String readLocalFile(String file) throws IOException {
        return new String(readFile(new DataInputStream(new FileInputStream(file))), Charsets.UTF_8);
    }

    public static String readLocalFile(Path path) throws IOException {
        return new String(readFile(new DataInputStream(new FileInputStream(path.toFile()))), Charsets.UTF_8);
    }

    public static byte[] readRawFile(String url) throws IOException {
        return readFile(new DataInputStream(ConnectionUtilities.getInputStream(url, true)));
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

        return cmds.toByteArray();
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


    public static synchronized boolean printLineToFile(String fileName, String message, boolean local) {
        File file = getFile(fileName, local);
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

    public static boolean printLineToFile(String fileName, String message) {
        return printLineToFile(fileName, message, true);
    }

    public static File getFile(String name, boolean local) {
        HashMap<String,File> fileMap;
        if (local) {
            fileMap = localFileMap;
        } else {
            fileMap = sharedFileMap;
        }

        if (!fileMap.containsKey(name) || fileMap.get(name) == null) {
            File path = local ? USER_DIR.toFile() : SHARED_DIR.toFile();
            File dat = Paths.get(path.toString(),  name + ".dat").toFile();

            try {
                path.mkdirs();
                dat.createNewFile();
                if(!local) {
                    dat.setReadable(true, false);
                    dat.setWritable(true, false);
                }
            }
            catch(IOException e) {
                //failure is possible due to user permissions on shared files
                if (local || (!name.equals(Constants.ALLOW_FILE) && !name.equals(Constants.BLOCK_FILE))) {
                    log.warn("Cannot setup file {} ({})", dat, local? "Local":"Shared", e);
                }
            }

            if (dat.exists()) {
                fileMap.put(name, dat);
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

    public static ArgParser.ExitStatus addToCertList(String list, File certFile) throws Exception {
        FileReader fr = new FileReader(certFile);
        Certificate cert = new Certificate(IOUtils.toString(fr));
        if(FileUtilities.printLineToFile(list, cert.data(), !SystemUtilities.isAdmin())) {
            log.info("Successfully added {} to {} list", cert.getOrganization(), ALLOW_FILE);
            return ArgParser.ExitStatus.SUCCESS;
        }
        log.error("Failed to add {} to {} list", cert.getOrganization(), ALLOW_FILE);
        return ArgParser.ExitStatus.GENERAL_ERROR;
    }

    public static synchronized boolean deleteFromFile(String fileName, String deleteLine, boolean local) {
        File file = getFile(fileName, local);
        File temp = getFile(Constants.TEMP_FILE, local);

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

    /**
     *
     * @return First line of ".autostart" file in user or shared space or "0" if blank.  If neither are found, returns "1".
     * @throws IOException
     */
    private static String readAutoStartFile() throws IOException {
        log.debug("Checking for {} preference in user directory {}...", Constants.AUTOSTART_FILE, USER_DIR);
        Path userAutoStart = Paths.get(USER_DIR.toString(), Constants.AUTOSTART_FILE);
        List<String> lines = null;
        if (Files.exists(userAutoStart)) {
            lines = Files.readAllLines(userAutoStart);
        } else {
            log.debug("Checking for {} preference in shared directory {}...", Constants.AUTOSTART_FILE, SHARED_DIR);
            Path sharedAutoStart = Paths.get(SHARED_DIR.toString(), Constants.AUTOSTART_FILE);
            if (Files.exists(sharedAutoStart)) {
                lines = Files.readAllLines(sharedAutoStart);
            }
        }
        if (lines == null) {
            return "1";
        } else if (lines.isEmpty()) {
            log.warn("File {} is empty, this shouldn't happen.", Constants.AUTOSTART_FILE);
            return "0";
        } else {
            String val = lines.get(0).trim();
            log.debug("Autostart preference {} contains {}", Constants.AUTOSTART_FILE, val);
            return val;
        }
    }

    private static synchronized boolean writeAutoStartFile(String mode) throws IOException {
        Path autostartFile = Paths.get(USER_DIR.toString(), Constants.AUTOSTART_FILE);
        Files.write(autostartFile, mode.getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        return readAutoStartFile().equals(mode);
    }

    public static boolean setAutostart(boolean autostart) {
        try {
            return writeAutoStartFile(autostart ? "1": "0");
        }
        catch(IOException e) {
            return false;
        }
    }

    public static boolean isAutostart() {
        try {
            return "1".equals(readAutoStartFile());
        }
        catch(IOException e) {
            return false;
        }
    }

    /**
     * Configures the given embedded resource file using qz.common.Constants combined with the provided
     * HashMap and writes to the specified location
     *
     * Will look for resource relative to relativeClass package location.
     */
    public static synchronized void configureAssetFile(String relativeAsset, File dest, HashMap<String, String> additionalMappings, Class relativeClass) throws IOException {
        // Static fields, parsed from qz.common.Constants
        List<Field> fields = new ArrayList<>();
        HashMap<String, String> allMappings = (HashMap<String, String>)additionalMappings.clone();
        fields.addAll(Arrays.asList(Constants.class.getFields())); // public only
        for(Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) { // static only
                try {
                    String key = "%" + field.getName() + "%";
                    Object value = field.get(null);
                    if (value != null) {
                        if (value instanceof String) {
                            allMappings.putIfAbsent(key, (String)value);
                        } else if(value instanceof Boolean) {
                            allMappings.putIfAbsent(key, "" + field.getBoolean(null));
                        }
                    }
                }
                catch(IllegalAccessException e) {
                    // This should never happen; we are only using public fields
                    log.warn("{} occurred fetching a value for {}", e.getClass().getName(), field.getName(), e);
                }
            }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(relativeClass.getResourceAsStream(relativeAsset)));
        BufferedWriter writer = new BufferedWriter(new FileWriter(dest));

        String line;
        while((line = reader.readLine()) != null) {
            for(Map.Entry<String, String> mapping : allMappings.entrySet()) {
                if (line.contains(mapping.getKey())) {
                    line = line.replaceAll(mapping.getKey(), mapping.getValue());
                }
            }
            writer.write(line + "\n");
        }
        reader.close();
        writer.close();
    }

    public static void configureAssetFile(String relativeAsset, Path dest, HashMap<String, String> additionalMappings, Class relativeClass) throws IOException {
        configureAssetFile(relativeAsset, dest.toFile(), additionalMappings, relativeClass);
    }

    private static Path getTempDirectory() {
        try {
            return Files.createTempDirectory(Constants.DATA_DIR + "_data_");
        } catch(IOException e) {
            log.warn("We couldn't get a temp directory for writing.  This could cause some items to break");
        }
        return null;
    }

    public static void setPermissionsParentally(Path toTraverse, boolean worldWrite) {
        Path stepper = toTraverse.toAbsolutePath();
        // Assume we shouldn't go higher than 2nd-level (e.g. "/etc", "C:\Program Files\", etc)
        while(stepper.getParent() != null && !stepper.getRoot().equals(stepper.getParent())) {
            File file = stepper.toFile();
            file.setReadable(true, false);
            file.setExecutable(true, false);
            file.setWritable(true, !worldWrite);
            if (SystemUtilities.isWindows() && worldWrite) {
                WindowsUtilities.setWritable(stepper);
            }
            stepper = stepper.getParent();
        }
    }

    public static void setPermissionsRecursively(Path toRecurse, boolean worldWrite) {
        try (Stream<Path> paths = Files.walk(toRecurse)) {
            paths.forEach((path)->{
                if(SystemUtilities.isWindows() && worldWrite) {
                    // By default, NSIS sets owner to "Administrator", preventing non-admins from writing
                    // Add "Authenticated Users" write permission using
                    WindowsUtilities.setWritable(path);
                }
                if (path.toFile().isDirectory()) {
                    // Executable bit in Unix allows listing files
                    path.toFile().setExecutable(true, false);
                }
                path.toFile().setReadable(true, false);
                path.toFile().setWritable(true, !worldWrite);
            });
        } catch (IOException e) {
            log.warn("An error occurred setting permissions: {}", toRecurse);
        }
    }

    public static void setExecutableRecursively(Path toRecurse, boolean ownerOnly) {
        File folder = toRecurse.toFile();
        if(SystemUtilities.isWindows() || !folder.exists() || !folder.isDirectory()) {
            return;
        }

        // "provision.json" found, assume we're in the provisioning directory, only process scripts and installers
        boolean isProvision = toRecurse.resolve(PROVISION_FILE).toFile().exists();

        try (Stream<Path> paths = Files.walk(toRecurse)) {
            paths.forEach((path)->{
                if (path.toFile().isDirectory()) {
                    // Executable bit in Unix allows listing files
                    path.toFile().setExecutable(true, ownerOnly);
                } else if(!isProvision || ProvisionInstaller.shouldBeExecutable(path)) {
                    path.toFile().setExecutable(true, ownerOnly);
                }
            });
        } catch (IOException e) {
            log.warn("An error occurred setting permissions: {}", toRecurse);
        }
    }

    public static void cleanup() {
        if(FileUtilities.TEMP_DIR != null) {
            FileUtils.deleteQuietly(FileUtilities.TEMP_DIR.toFile());
        }
    }
}
