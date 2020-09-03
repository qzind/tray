package qz.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.auth.Certificate;
import qz.common.Constants;
import qz.common.PropertyHelper;
import qz.ui.component.CertificateDisplay;
import qz.ui.component.CertificateTable;
import qz.ui.component.ContainerList;
import qz.ui.component.IconCache;
import qz.utils.FileUtilities;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Tres on 2/23/2015.
 */
public class SiteManagerDialog extends BasicDialog implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SiteManagerDialog.class);

    private JSplitPane splitPane;

    private JTabbedPane tabbedPane;

    private ContainerList<CertificateDisplay> allowList;
    private ContainerList<CertificateDisplay> blockList;

    private CertificateTable certTable;
    private IconCache iconCache;
    private PropertyHelper prefs;

    private JButton addButton;
    private JButton deleteButton;

    private Thread readerThread;
    private AtomicBoolean threadRunning;
    private AtomicReference<CertificateDisplay> deleteCertificate;

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
        addButton.setFont(addFont.deriveFont(Font.BOLD, addFont.getSize() * 1.50f));
        addButton.setForeground(Constants.TRUSTED_COLOR);
        addButton.setBorderPainted(false);
        addButton.addActionListener(e -> {
            File chooseFolder = null;
            for(String folder : new String[] { "Downloads", "Desktop", ""}) {
                Path folderFile = Paths.get(System.getProperty("user.home"), folder);
                System.out.println(folderFile);
                if(folderFile.toFile().exists()) {
                    chooseFolder = folderFile.toFile();
                    break;
                }
            }
            FileDialog fileDialog = new java.awt.FileDialog(this);
            fileDialog.setDirectory(chooseFolder.toString());
            fileDialog.setMultipleMode(false);
            fileDialog.setVisible(true);
            try {
                for(File cert : fileDialog.getFiles()) {
                    Certificate importCert = new Certificate(cert.toPath());
                    if (importCert.isValid()) {
                        CertificateDisplay certificate = new CertificateDisplay(importCert, true);
                        if (!allowList.contains(importCert) && !Certificate.UNKNOWN.equals(certificate.getCert())) {
                            FileUtilities.printLineToFile(Constants.ALLOW_FILE, certificate.getCert().data());
                            allowList.add(certificate);
                        }
                    }
                }
            } catch(CertificateException | IOException ex) {
                log.warn("Could not read certificate", ex);
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
            deleteCertificate.set(getSelectedCertificate());
            deleteButton.setEnabled(false);
            clearSelection();
        });
        deleteButton.setEnabled(false);
        addKeyListener(KeyEvent.VK_DELETE, deleteButton);
        addKeyListener(KeyEvent.VK_BACK_SPACE, deleteButton);

        JPanel tabbedPanePanel = new JPanel();
        tabbedPanePanel.setLayout(new BoxLayout(tabbedPanePanel, BoxLayout.Y_AXIS));
        tabbedPanePanel.add(tabbedPane);
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(addButton, LEFT_ALIGNMENT);
        toolBar.add(deleteButton, LEFT_ALIGNMENT);
        tabbedPanePanel.add(toolBar);
        splitPane.add(tabbedPanePanel);
        splitPane.add(new JScrollPane(certTable));
        splitPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        certTable.autoSize();

        readerThread = new Thread(this);
        threadRunning = new AtomicBoolean(false);
        deleteCertificate = new AtomicReference<>(null);

        JCheckBox strictModeCheckBox = new JCheckBox(Constants.STRICT_MODE_LABEL, prefs.getBoolean(Constants.PREFS_STRICT_MODE, false));
        strictModeCheckBox.setToolTipText(Constants.STRICT_MODE_TOOLTIP);
        strictModeCheckBox.addActionListener(e -> {
            if(strictModeCheckBox.isSelected() && !new ConfirmDialog(null, "Please Confirm", iconCache).prompt(Constants.STRICT_MODE_CONFIRM)) {
                strictModeCheckBox.setSelected(false);
                return;
            }
            Certificate.setTrustBuiltIn(!strictModeCheckBox.isSelected());
            prefs.setProperty(Constants.PREFS_STRICT_MODE, strictModeCheckBox.isSelected());
            certTable.refreshComponents();
        });

        // Add checkbox near "close" button
        addPanelComponent(strictModeCheckBox);

        setContent(splitPane, true);
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

    /**
     * Thread safe remove certificate from GUI and filesystem
     */
    public SiteManagerDialog removeCertificate(CertificateDisplay certificate) {
        final ContainerList<CertificateDisplay> certList = getSelectedList();
        if (certificate != null && FileUtilities.deleteFromFile(certList.getTag().toString(), certificate.getCert().data())) {
            certList.remove(certificate);
        } else {
            log.warn("Error removing {} from the list of {} sites", certificate, getSelectedTabName().toLowerCase(Locale.ENGLISH));
        }

        return this;
    }

    private CertificateDisplay getSelectedCertificate() {
        return (CertificateDisplay)getSelectedList().getList().getSelectedValue();
    }

    private String getSelectedTabName() {
        if (tabbedPane.getSelectedIndex() >= 0) {
            return tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
        }

        return "";
    }

    private ContainerList<CertificateDisplay> getSelectedList() {
        if (tabbedPane.getSelectedIndex() == 0) {
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
                if (deleteCertificate.get() != null) {
                    removeCertificate(deleteCertificate.getAndSet(null));
                } else if (allowFile.lastModified() > allowTick
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

}
