package qz.ui;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.auth.Certificate;
import qz.common.Constants;
import qz.common.PropertyHelper;
import qz.installer.certificate.CertificateChainBuilder;
import qz.installer.certificate.CertificateManager;
import qz.installer.certificate.KeyPairWrapper;
import qz.ui.component.*;
import qz.utils.FileUtilities;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Tres on 2/23/2015.
 */
public class SiteManagerDialog extends BasicDialog implements Runnable {

    private static final Logger log = LogManager.getLogger(SiteManagerDialog.class);

    private static final String IMPORT_NEEDED = "The provided certificate \"%s\" is unrecognized and not yet trusted.\n"  +
            "Would you like to automatically copy it to \"%s\"?";
    private static final String IMPORT_FAILED = "Failed to import certificate.  Please import manually.";
    private static final String INVALID_CERTIFICATE = "An exception occurred importing the certificate.  Please check the logs for details.";
    private static final String IMPORT_QUESTION = "Successfully created a new demo keypair.  Automatically install?";

    private static final String DEMO_CERT_QUESTION = "Create a new demo keypair for %s?\n" +
            "* This keypair will only work on this computer.\n" +
            "* This should only be done by developers.\n" +
            "* See also https://qz.io/wiki/signing";
    private static final String DEMO_CERT_NAME = String.format("%s Demo Cert", Constants.ABOUT_TITLE);

    private JSplitPane splitPane;

    private JTabbedPane tabbedPane;
    private Border plainBorder;
    private Color plainBackground;
    private Border dragBorder;

    private ContainerList<CertificateDisplay> allowList;
    private ContainerList<CertificateDisplay> blockList;

    private CertificateTable certTable;
    private IconCache iconCache;
    private PropertyHelper prefs;

    private JButton addButton;
    private JButton deleteButton;
    private JCheckBox strictModeCheckBox;

    private Thread readerThread;
    private AtomicBoolean threadRunning;

    private long allowTick = -1;
    private long blockTick = -1;


    public SiteManagerDialog(JMenuItem caller, IconCache iconCache, PropertyHelper prefs) {
        super(caller, iconCache);
        this.iconCache = iconCache;
        this.prefs = prefs;
        certTable = new CertificateTable(iconCache);
        initComponents();
    }

    public void initComponents() {
        allowList = new ContainerList<>();
        allowList.setTag(Constants.ALLOW_FILE);
        blockList = new ContainerList<>();
        blockList.setTag(Constants.BLOCK_FILE);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        tabbedPane = new JTabbedPane();
        appendListTab(allowList.getList(), Constants.ALLOWED, IconCache.Icon.ALLOW_ICON, KeyEvent.VK_A);
        appendListTab(blockList.getList(), Constants.BLOCKED, IconCache.Icon.BLOCK_ICON, KeyEvent.VK_B);

        setHeader(tabbedPane.getSelectedIndex() == 0? Constants.ALLOW_SITES_LABEL:Constants.BLOCK_SITES_LABEL);

        tabbedPane.addChangeListener(e -> {
            clearSelection();

            switch(tabbedPane.getSelectedIndex()) {
                case 1: setHeader(Constants.BLOCK_SITES_LABEL);
                    blockList.getList().setSelectedIndex(0);
                    break;
                default:
                    setHeader(Constants.ALLOW_SITES_LABEL);
                    allowList.getList().setSelectedIndex(0);
            }
        });
        plainBorder = tabbedPane.getBorder();
        plainBackground = tabbedPane.getBackground();
        dragBorder = BorderFactory.createLineBorder(Constants.TRUSTED_COLOR);

        final ListModel allowListModel = allowList.getList().getModel();
        final ListModel blockListModel = blockList.getList().getModel();

        allowListModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) { refreshTabTitle(); }

            @Override
            public void intervalRemoved(ListDataEvent e) { refreshTabTitle(); }

            @Override
            public void contentsChanged(ListDataEvent e) { refreshTabTitle(); }

            public void refreshTabTitle() {
                String title = Constants.ALLOWED + (String.format(allowListModel.getSize() > 0? " (%s)":"", allowListModel.getSize()));
                tabbedPane.setTitleAt(0, title);
            }
        });

        blockList.getList().getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) { refreshTabTitle(); }

            @Override
            public void intervalRemoved(ListDataEvent e) { refreshTabTitle(); }

            @Override
            public void contentsChanged(ListDataEvent e) { refreshTabTitle(); }

            public void refreshTabTitle() {
                String title = Constants.BLOCKED + (String.format(blockListModel.getSize() > 0? " (%s)":"", blockListModel.getSize()));
                tabbedPane.setTitleAt(1, title);
            }
        });

        addButton = new JButton("+");
        Font addFont = addButton.getFont();
        JPopupMenu addMenu = new JPopupMenu();
        JMenuItem browseItem = new JMenuItem("Browse...", iconCache.getIcon(IconCache.Icon.FOLDER_ICON));
        browseItem.setToolTipText("Browse for a certificate to import.");
        browseItem.setMnemonic(KeyEvent.VK_B);
        browseItem.addActionListener(e -> {
            File chooseFolder = null;
            for(String folder : new String[] { "Downloads", "Desktop", ""}) {
                Path folderFile = Paths.get(System.getProperty("user.home"), folder);
                if(folderFile.toFile().exists()) {
                    chooseFolder = folderFile.toFile();
                    break;
                }
            }
            FileDialog fileDialog = new java.awt.FileDialog(this);
            fileDialog.setDirectory(chooseFolder.toString());
            fileDialog.setMultipleMode(false);
            fileDialog.setVisible(true);
            addCertificates(fileDialog.getFiles(), getSelectedList(), true);
        });
        JMenuItem createNewItem = new JMenuItem("Create New...", iconCache.getIcon(IconCache.Icon.SETTINGS_ICON));
        createNewItem.setToolTipText("Developers only: Create and import a new demo keypair for signing.");
        createNewItem.setMnemonic(KeyEvent.VK_N);
        createNewItem.addActionListener(e -> {
            int generateCert = JOptionPane.showConfirmDialog(this, String.format(DEMO_CERT_QUESTION, Constants.ABOUT_TITLE), "Please Confirm", JOptionPane.YES_NO_OPTION);
            if(generateCert != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                Path created = createDemoCertificate();
                int installKeypair = JOptionPane.showConfirmDialog(this, IMPORT_QUESTION, "Keypair Created", JOptionPane.YES_NO_OPTION);
                if(installKeypair == JOptionPane.YES_OPTION) {
                    addCertificates(new File[] {created.resolve(Constants.SIGNING_CERTIFICATE).toFile()}, allowList, true);
                }
                ShellUtilities.browseDirectory(created);
            }
            catch(Throwable t) {
                JOptionPane.showMessageDialog(this, "Sorry, an error occurred, please check the logs.");
                log.error("An exception occurred creating or installing the demo certificate", t);
            }
        });
        addMenu.add(browseItem);
        addMenu.add(createNewItem);
        addButton.setFont(addFont.deriveFont(Font.BOLD, addFont.getSize() * 1.50f));
        addButton.setForeground(Constants.TRUSTED_COLOR);
        addButton.setBorderPainted(false);
        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                addMenu.show(addButton, e.getX(), e.getY());
            }
        });
        addButton.setEnabled(true);
        addKeyListener(KeyEvent.VK_PLUS, addButton);

        deleteButton = new JButton("-");
        Font deleteFont = deleteButton.getFont();
        deleteButton.setFont(deleteFont.deriveFont(Font.BOLD, deleteFont.getSize() * 1.50f));
        deleteButton.setForeground(Constants.WARNING_COLOR);
        addButton.setBorderPainted(false);
        deleteButton.addActionListener(e -> {
            removeCertificate(getSelectedCertificate(), getSelectedList());
            deleteButton.setEnabled(false);
            clearSelection();
        });
        deleteButton.setEnabled(false);
        addKeyListener(KeyEvent.VK_DELETE, deleteButton);
        addKeyListener(KeyEvent.VK_BACK_SPACE, deleteButton);

        // Fixes alignment issues with +/-
        JSeparator separator = new JSeparator();
        separator.setOpaque(false);
        separator.setForeground(new Color(0, 0, 0, 0));

        JPanel tabbedPanePanel = new JPanel();
        tabbedPanePanel.setLayout(new BoxLayout(tabbedPanePanel, BoxLayout.Y_AXIS));
        tabbedPanePanel.add(tabbedPane);
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(addButton, LEFT_ALIGNMENT);
        toolBar.add(deleteButton, LEFT_ALIGNMENT);
        toolBar.add(separator, LEFT_ALIGNMENT);
        tabbedPanePanel.add(toolBar, LEFT_ALIGNMENT);
        splitPane.add(tabbedPanePanel);
        splitPane.add(new JScrollPane(certTable));
        splitPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        certTable.autoSize();

        readerThread = new Thread(this);
        threadRunning = new AtomicBoolean(false);

        strictModeCheckBox = new JCheckBox(Constants.STRICT_MODE_LABEL, prefs.getBoolean(Constants.PREFS_STRICT_MODE, false));
        strictModeCheckBox.setToolTipText(Constants.STRICT_MODE_TOOLTIP);
        strictModeCheckBox.addActionListener(e -> {
            if (strictModeCheckBox.isSelected() && !new ConfirmDialog(null, "Please Confirm", iconCache).prompt(Constants.STRICT_MODE_CONFIRM)) {
                strictModeCheckBox.setSelected(false);
                return;
            }
            Certificate.setTrustBuiltIn(!strictModeCheckBox.isSelected());
            prefs.setProperty(Constants.PREFS_STRICT_MODE, strictModeCheckBox.isSelected());
            certTable.refreshComponents();
        });
        refreshStrictModeCheckbox();

        setContent(splitPane, true);

        // Register drag/drop events
        allowList.getList().setDragEnabled(true);
        blockList.getList().setDragEnabled(true);
        tabbedPane.setDropTarget(new DropTarget() {
            @Override
            public synchronized void dragEnter(DropTargetDragEvent e) {
                for(DataFlavor flavor : e.getTransferable().getTransferDataFlavors()) {
                    if(flavor.equals(DataFlavor.javaFileListFlavor)) {
                        // Dragged from file system
                        tabbedPane.setBorder(dragBorder);
                        e.acceptDrag(DnDConstants.ACTION_COPY);
                        return;
                    } else if(flavor.equals(DataFlavor.stringFlavor)) {
                        // Dragged from JList
                        Component target = e.getDropTargetContext().getComponent();
                        if(target instanceof JTabbedPane) {
                            target.setBackground(Constants.TRUSTED_COLOR);
                            e.acceptDrag(DnDConstants.ACTION_MOVE);
                        }
                    }
                }
            }

            @Override
            public synchronized void dragExit(DropTargetEvent e) {
                tabbedPane.setBorder(plainBorder);
                tabbedPane.setBackground(plainBackground);
            }

            @Override
            public synchronized void drop(DropTargetDropEvent e) {
                tabbedPane.setBorder(plainBorder);
                tabbedPane.setBackground(plainBackground);
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    addCertificates(e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor), getSelectedList(), true);
                    return;
                }
                catch(IOException | UnsupportedFlavorException ignore) {}

                e.acceptDrop(DnDConstants.ACTION_MOVE);
                Component targetComponent = e.getDropTargetContext().getComponent();
                if(targetComponent instanceof JTabbedPane) {
                    JTabbedPane tabbedPane = (JTabbedPane)targetComponent;
                    CertificateDisplay selectedCert = getSelectedCertificate();
                    int targetIndex = tabbedPane.indexAtLocation(e.getLocation().x, e.getLocation().y);
                    ContainerList<CertificateDisplay> target = getListByIndex(targetIndex);
                    ContainerList<CertificateDisplay> source = getSelectedList();
                    if(source != target) {
                        addCertificate(selectedCert, target, false);
                        removeCertificate(selectedCert, source);
                        clearSelection();
                    }
                }
            }
        });
    }

    private void refreshStrictModeCheckbox() {
        // Hide strict-mode checkbox for standard configurations
        if(Certificate.hasAdditionalCAs() || strictModeCheckBox.isSelected()) {
            // Add checkbox near "close" button
            addPanelComponent(strictModeCheckBox);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && !readerThread.isAlive()) {
            threadRunning.set(true);
            readerThread = new Thread(this);
            readerThread.start();
        } else {
            threadRunning.set(false);
        }

        if (visible && getSelectedList().getList().getSelectedIndex() < 0) {
            selectFirst();
        }

        super.setVisible(visible);
    }

    public SiteManagerDialog selectFirst() {
        SwingUtilities.invokeLater(() -> getSelectedList().getList().setSelectedIndex(0));

        return this;
    }

    private void addCertificateSelectionListener(final JList list) {
        list.addListSelectionListener(e -> {
            if (list.getSelectedValue() instanceof CertificateDisplay) {
                certTable.setCertificate(((CertificateDisplay)list.getSelectedValue()).getCert());
                deleteButton.setEnabled(true);
            } else {
                deleteButton.setEnabled(false);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void appendListTab(JList list, String title, IconCache.Icon icon, int mnemonic) {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        JScrollPane scrollPane = new JScrollPane(list);
        tabbedPane.addTab(title, getIcon(icon), scrollPane);
        tabbedPane.setMnemonicAt(tabbedPane.indexOfComponent(scrollPane), mnemonic);
        addCertificateSelectionListener(list);
        list.setCellRenderer(new CertificateListCellRenderer());
    }

    private class CertificateListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof CertificateDisplay) {
                if (((CertificateDisplay)value).isLocal()) {
                    label.setIcon(SiteManagerDialog.super.getIcon(IconCache.Icon.SAVED_ICON));
                } else {
                    label.setIcon(SiteManagerDialog.super.getIcon(IconCache.Icon.DESKTOP_ICON));
                }
            } else {
                label.setIcon(null);
            }
            return label;
        }
    }

    private CertificateDisplay getSelectedCertificate() {
        return (CertificateDisplay)getSelectedList().getList().getSelectedValue();
    }

    @SuppressWarnings("unused")
    private String getSelectedTabName() {
        if (tabbedPane.getSelectedIndex() >= 0) {
            return tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
        }

        return "";
    }

    private void removeCertificate(CertificateDisplay certDisplay, ContainerList<CertificateDisplay> list) {
        if (list.contains(certDisplay)) {
            String saveFile = (list == allowList ? Constants.ALLOW_FILE : Constants.BLOCK_FILE);
            if(certDisplay != null && certDisplay.getCert() != null
                    && FileUtilities.deleteFromFile(saveFile, certDisplay.getCert().data())) {
                list.remove(certDisplay);
            } else {
                log.warn("Error removing {} from the list of {} sites", certDisplay, saveFile);
            }
        }
    }

    private void addCertificate(CertificateDisplay certDisplay, ContainerList<CertificateDisplay> list, boolean selectWhenDone) {
        if (!list.contains(certDisplay) && !Certificate.UNKNOWN.equals(certDisplay.getCert())) {
            FileUtilities.printLineToFile(list == allowList ? Constants.ALLOW_FILE : Constants.BLOCK_FILE, certDisplay.getCert().data());
            list.addAndCallback(certDisplay, selectWhenDone ? () -> {
                list.getList().setSelectedValue(certDisplay, true);
                return null;
            } : null);
        }
    }

    private void addCertificates(Object dragged, ContainerList<CertificateDisplay> list, boolean selectWhenDone) {
        if(dragged instanceof java.util.List) {
            java.util.List certFiles = (java.util.List)dragged;
            if(certFiles.size() > 0) {
                if(certFiles.get(0) instanceof File) {
                    addCertificates((File[])certFiles.toArray(new File[certFiles.size()]), list, selectWhenDone);
                } else {
                    System.out.println("Nope: " + certFiles.get(0).getClass().getName());
                }

            }

        } else {
            log.warn("Could not convert certificate to from unknown type: {}", dragged.getClass().getCanonicalName());
        }
    }

    private void addCertificates(File[] certFiles, ContainerList<CertificateDisplay> list, boolean selectWhenDone) {
        for(File file : certFiles) {
            try {
                Certificate importCert = new Certificate(file.toPath());
                if (importCert.isValid()) {
                    addCertificate(new CertificateDisplay(importCert, true), list, selectWhenDone);
                    continue;
                }
                // Warn of any invalid certs
                showInvalidCertWarning(file, importCert);
            }
            catch(CertificateException | IOException e) {
                log.warn("Unable to import cert {}", file, e);
                JOptionPane.showMessageDialog(this, String.format(INVALID_CERTIFICATE), "Import failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showInvalidCertWarning(File file, Certificate cert) {
        Path override = SystemUtilities.getJarParentPath().resolve(Constants.OVERRIDE_CERT);
        String message = String.format(IMPORT_NEEDED,
                                       cert.getCommonName(),
                                       override);
        int copyAnswer = JOptionPane.showConfirmDialog(this, message, "Unrecognized Certificate", JOptionPane.YES_NO_OPTION);
        if(copyAnswer == JOptionPane.YES_OPTION) {
            Cursor backupCursor = getCursor();
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            boolean copySuccess = ShellUtilities.elevateCopy(file.toPath(), SystemUtilities.getJarParentPath().resolve(Constants.OVERRIDE_CERT));
            setCursor(backupCursor);
            if(copySuccess) {
                Certificate.scanAdditionalCAs();
                addCertificates(new File[] { file }, allowList, true);
                refreshStrictModeCheckbox();
            } else {
                JOptionPane.showMessageDialog(this, String.format(IMPORT_FAILED), "Import failed", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private ContainerList<CertificateDisplay> getSelectedList() {
        return getListByIndex(tabbedPane.getSelectedIndex());
    }

    private ContainerList<CertificateDisplay> getListByIndex(int index) {
        if (index == 0) {
            return allowList;
        }

        return blockList;
    }

    private void clearSelection() {
        certTable.setCertificate(null);
        allowList.getList().clearSelection();
        blockList.getList().clearSelection();
    }

    public void run() {
        threadRunning.set(true);

        File allowFile = FileUtilities.getFile(Constants.ALLOW_FILE, true);
        File allowFileShare = FileUtilities.getFile(Constants.ALLOW_FILE, false);

        File blockFile = FileUtilities.getFile(Constants.BLOCK_FILE, true);
        File blockFileShare = FileUtilities.getFile(Constants.BLOCK_FILE, false);

        boolean initialSelection = true;

        allowTick = allowTick < 0? 0:allowTick;
        blockTick = blockTick < 0? 0:blockTick;

        // Reads the certificate allowed/blocked files and updates the certificate listing
        while(threadRunning.get()) {
            if (isVisible()) {
                if (allowFile.lastModified() > allowTick
                        || (allowFileShare != null && allowFileShare.lastModified() > allowTick)) {
                    allowTick = Math.max(allowFile.lastModified(), (allowFileShare == null? 0:allowFileShare.lastModified()));
                    readCertificates(allowList, allowFileShare, false);
                    readCertificates(allowList, allowFile, true);
                } else if (blockFile.lastModified() > blockTick
                        || (blockFileShare != null && blockFileShare.lastModified() > blockTick)) {
                    blockTick = Math.max(blockFile.lastModified(), (blockFileShare == null? 0:blockFileShare.lastModified()));
                    readCertificates(blockList, blockFileShare, false);
                    readCertificates(blockList, blockFile, true);
                } else {
                    sleep(2000);
                }

                if (initialSelection) {
                    selectFirst();
                    initialSelection = false;
                }
            }
        }
        threadRunning.set(false);
    }

    public void sleep(int millis) {
        try { Thread.sleep(millis); } catch(InterruptedException ignore) {}
    }

    /**
     * Reads a certificate data file and updates the corresponding {@code ArrayList}
     *
     * @param certList The {@code ArrayList} requiring updating
     * @param file     The data file containing allow/block certificate information
     */
    public ArrayList<CertificateDisplay> readCertificates(ArrayList<CertificateDisplay> certList, File file, boolean local) {
        if (file == null) { return certList; }

        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                if (line.startsWith("#")) { continue; } //treat these lines as comments
                String[] data = line.split("\\t");

                if (data.length == Certificate.saveFields.length) {
                    HashMap<String,String> dataMap = new HashMap<>();
                    for(int i = 0; i < data.length; i++) {
                        dataMap.put(Certificate.saveFields[i], data[i]);
                    }

                    CertificateDisplay certificate = new CertificateDisplay(Certificate.loadCertificate(dataMap), local);

                    // Don't include the unsigned certificate if we are blocking it, there is a menu option instead
                    if (!certList.contains(certificate) && !Certificate.UNKNOWN.equals(certificate.getCert())) {
                        certList.add(certificate);
                    }
                }
            }
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }

        return certList;
    }

    /**
     * Creates a demo cert and key, returns the parent folder where they were created
     */
    private static Path createDemoCertificate() throws Throwable {
        CertificateChainBuilder chainBuilder = new CertificateChainBuilder(DEMO_CERT_NAME);

        KeyPairWrapper keyPair = chainBuilder.createCaCert();
        // Some locations a user might be happy with
        Path homeDir = Paths.get(System.getProperty("user.home"));
        Path[] paths = { homeDir.resolve("Desktop"),  homeDir.resolve("Downloads"),  homeDir };
        for(Path path : paths) {
            File file = path.toAbsolutePath().normalize().toFile();
            if(file.isDirectory() && file.canWrite()) {
                Path certData = file.toPath().resolve(DEMO_CERT_NAME);
                if(certData.toFile().mkdir() || (certData.toFile().isDirectory() && certData.toFile().exists())) {
                    // Write our PKCS#8 PEM file
                    File privateKey = certData.resolve(Constants.SIGNING_PRIVATE_KEY).toFile();
                    PemObject pemObject = new PemObject("PRIVATE KEY", keyPair.getKey().getEncoded());
                    log.info("Writing PKCS#8 Private Key: {}", privateKey);
                    FileWriter writer = new FileWriter(privateKey);
                    PemWriter pemWriter = new PemWriter(writer);
                    pemWriter.writeObject(pemObject);
                    pemWriter.flush();

                    // Write our x509 certificate file
                    log.info("Writing x509 Certificate: {}", privateKey);
                    File certificate = certData.resolve(Constants.SIGNING_CERTIFICATE).toFile();
                    CertificateManager.writeCert(keyPair.getCert(), certificate);
                    return certData;
                }
            }
        }
        throw new CertificateException("Can't create certificate");
    }

}
