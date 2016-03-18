package qz.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.auth.Certificate;
import qz.common.Constants;
import qz.utils.FileUtilities;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Tres on 2/23/2015.
 */
public class SiteManagerDialog extends BasicDialog implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SiteManagerDialog.class);

    private JSplitPane splitPane;

    private JTabbedPane tabbedPane;

    private ContainerList<Certificate> allowList;
    private ContainerList<Certificate> blockList;

    private CertificateTable certTable;

    private JButton deleteButton;

    private Thread readerThread;
    private AtomicBoolean threadRunning;
    private AtomicReference<Certificate> deleteCertificate;

    private long allowTick = -1;
    private long blockTick = -1;


    public SiteManagerDialog(JMenuItem caller, IconCache iconCache) {
        super(caller, iconCache);
        certTable = new CertificateTable(null, iconCache);
        initComponents();
    }

    public void initComponents() {
        allowList = new ContainerList<>();
        allowList.setTag(Constants.ALLOW_FILE);
        blockList = new ContainerList<>();
        blockList.setTag(Constants.BLOCK_FILE);

        setIconImage(getImage(IconCache.Icon.SAVED_ICON));
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        tabbedPane = new JTabbedPane();
        appendListTab(allowList.getList(), Constants.ALLOWED, IconCache.Icon.ALLOW_ICON, KeyEvent.VK_A);
        appendListTab(blockList.getList(), Constants.BLOCKED, IconCache.Icon.BLOCK_ICON, KeyEvent.VK_B);

        setHeader(tabbedPane.getSelectedIndex() == 0? Constants.WHITE_SITES:Constants.BLACK_SITES);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                clearSelection();

                switch(tabbedPane.getSelectedIndex()) {
                    case 1: setHeader(Constants.BLACK_SITES);
                        blockList.getList().setSelectedIndex(0);
                        break;
                    default:
                        setHeader(Constants.WHITE_SITES);
                        allowList.getList().setSelectedIndex(0);
                }
            }
        });

        final ListModel allowListModel = allowList.getList().getModel();
        final ListModel blockListModel = blockList.getList().getModel();

        allowListModel.addListDataListener(new ListDataListener() {
            @Override public void intervalAdded(ListDataEvent e) { refreshTabTitle(); }
            @Override public void intervalRemoved(ListDataEvent e) { refreshTabTitle(); }
            @Override public void contentsChanged(ListDataEvent e) { refreshTabTitle(); }
            public void refreshTabTitle() {
                String title = Constants.ALLOWED + (String.format(allowListModel.getSize() > 0? " (%s)":"", allowListModel.getSize()));
                tabbedPane.setTitleAt(0, title);
            }
        });

        blockList.getList().getModel().addListDataListener(new ListDataListener() {
            @Override public void intervalAdded(ListDataEvent e) { refreshTabTitle(); }
            @Override public void intervalRemoved(ListDataEvent e) { refreshTabTitle(); }
            @Override public void contentsChanged(ListDataEvent e) { refreshTabTitle(); }
            public void refreshTabTitle() {
                String title = Constants.BLOCKED + (String.format(blockListModel.getSize() > 0? " (%s)":"", blockListModel.getSize()));
                tabbedPane.setTitleAt(1, title);
            }
        });

        // TODO:  Add certificate manual import capabilities
        deleteButton = addPanelButton("Delete", IconCache.Icon.DELETE_ICON, KeyEvent.VK_D);
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteCertificate.set(getSelectedCertificate());
                deleteButton.setEnabled(false);
                clearSelection();
            }
        });
        deleteButton.setEnabled(false);
        addKeyListener(KeyEvent.VK_DELETE, deleteButton);

        splitPane.add(tabbedPane);
        splitPane.add(new JScrollPane(certTable));
        splitPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        certTable.autoSize();

        readerThread = new Thread(this);
        threadRunning = new AtomicBoolean(false);
        deleteCertificate = new AtomicReference<>(null);

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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getSelectedList().getList().setSelectedIndex(0);
            }
        });

        return this;
    }

    private void addCertificateSelectionListener(final JList list) {
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (list.getSelectedValue() instanceof Certificate) {
                    certTable.setCertificate((Certificate)list.getSelectedValue());
                    deleteButton.setEnabled(true);
                } else {
                    deleteButton.setEnabled(false);
                }
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
            if (value instanceof Certificate) {
                label.setIcon(SiteManagerDialog.super.getIcon(IconCache.Icon.SAVED_ICON));
            } else {
                label.setIcon(null);
            }
            return label;
        }
    }

    /**
     * Thread safe remove certificate from GUI and filesystem
     */
    public SiteManagerDialog removeCertificate(Certificate certificate) {
        final ContainerList<Certificate> certList = getSelectedList();
        if (certificate != null && FileUtilities.deleteFromFile(certList.getTag().toString(), certificate.data())) {
            certList.remove(certificate);
        } else {
            log.warn("Error removing {} from the list of {} sites", certificate, getSelectedTabName().toLowerCase());
        }

        return this;
    }

    private Certificate getSelectedCertificate() {
        return (Certificate)getSelectedList().getList().getSelectedValue();
    }

    private String getSelectedTabName() {
        if (tabbedPane.getSelectedIndex() >= 0) {
            return tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
        }

        return "";
    }

    private ContainerList<Certificate> getSelectedList() {
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

        File allowFile = FileUtilities.getFile(Constants.ALLOW_FILE);
        File blockFile = FileUtilities.getFile(Constants.BLOCK_FILE);

        boolean initialSelection = true;

        allowTick = allowTick < 0? 0:allowTick;
        blockTick = blockTick < 0? 0:blockTick;

        // Reads the certificate allowed/blocked files and updates the certificate listing
        while(threadRunning.get()) {
            if (isVisible()) {
                if (deleteCertificate.get() != null) {
                    removeCertificate(deleteCertificate.getAndSet(null));
                } else if (allowFile.lastModified() > allowTick) {
                    allowTick = allowFile.lastModified();
                    readCertificates(allowList, allowFile);
                } else if (blockFile.lastModified() > blockTick) {
                    blockTick = blockFile.lastModified();
                    readCertificates(blockList, blockFile);
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
    public ArrayList<Certificate> readCertificates(ArrayList<Certificate> certList, File file) {
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                String[] data = line.split("\\t");

                if (data.length == Certificate.saveFields.length) {
                    HashMap<String,String> dataMap = new HashMap<>();
                    for(int i = 0; i < data.length; i++) {
                        dataMap.put(Certificate.saveFields[i], data[i]);
                    }

                    Certificate certificate = Certificate.loadCertificate(dataMap);
                    // Don't include the unsigned certificate if we are blocking it, there is a menu option instead
                    if (!certList.contains(certificate) && !Certificate.UNKNOWN.equals(certificate)) {
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
