package qz.ui;

import com.github.zafarkhaja.semver.Version;
import org.eclipse.jetty.server.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.AboutInfo;
import qz.common.Constants;
import qz.ui.component.EmLabel;
import qz.ui.component.IconCache;
import qz.ui.component.LinkLabel;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketServer;
import qz.ws.substitutions.Substitutions;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Tres on 2/26/2015.
 * Displays a basic about dialog
 */
public class AboutDialog extends BasicDialog implements Themeable {

    private static final Logger log = LogManager.getLogger(AboutDialog.class);
    private final boolean limitedDisplay;
    private Server server;
    private JLabel lblUpdate;
    private JButton updateButton;

    private JPanel contentPanel;
    private JToolBar headerBar;
    private Border dropBorder;

    // Use <html> allows word wrapping on a standard JLabel
    static class TextWrapLabel extends JLabel {
        TextWrapLabel(String text) {
            super("<html>" + text + "</html>");
        }
    }

    public AboutDialog(JMenuItem menuItem, IconCache iconCache) {
        super(menuItem, iconCache);

        //noinspection ConstantConditions - white label support
        limitedDisplay = Constants.VERSION_CHECK_URL.isEmpty();
    }

    public void setServer(Server server) {
        this.server = server;

        initComponents();
    }

    public void initComponents() {
        JLabel lblAbout = new EmLabel(Constants.ABOUT_TITLE, 3);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        LinkLabel linkLibrary = getLinkLibrary();
        Box versionBox = Box.createHorizontalBox();
        versionBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        versionBox.add(new JLabel(String.format("%s (Java)", Constants.VERSION)));

        JPanel aboutPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel logo = new JLabel(getIcon(IconCache.Icon.LOGO_ICON));
        logo.setBorder(new EmptyBorder(0, 0, 0, limitedDisplay ? 0 : 20));
        aboutPanel.add(logo);

        if (!limitedDisplay) {
            LinkLabel linkNew = new LinkLabel("What's New?");
            linkNew.setLinkLocation(Constants.VERSION_DOWNLOAD_URL);

            lblUpdate = new JLabel();
            updateButton = new JButton();
            updateButton.setVisible(false);
            updateButton.addActionListener(evt -> {
                try { Desktop.getDesktop().browse(new URL(Constants.ABOUT_DOWNLOAD_URL).toURI()); }
                catch(Exception e) { log.error("", e); }
            });
            checkForUpdate();
            versionBox.add(Box.createHorizontalStrut(12));
            versionBox.add(linkNew);

            infoPanel.add(lblAbout);
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(versionBox);
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(lblUpdate);
            infoPanel.add(updateButton);
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(new TextWrapLabel(String.format("%s is written and supported by %s.", Constants.ABOUT_TITLE, Constants.ABOUT_COMPANY)));
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(new TextWrapLabel(String.format("If using %s commercially, please first reach out to the website publisher for support issues.", Constants.ABOUT_TITLE)));
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(linkLibrary);
            infoPanel.setPreferredSize(logo.getPreferredSize());
        } else {
            LinkLabel linkLabel = new LinkLabel(Constants.ABOUT_URL);
            linkLabel.setLinkLocation(Constants.ABOUT_URL);

            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(lblAbout);
            infoPanel.add(versionBox);
            infoPanel.add(Box.createVerticalStrut(16));
            infoPanel.add(linkLabel);
            infoPanel.add(Box.createVerticalStrut(8));
            infoPanel.add(linkLibrary);
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(Box.createHorizontalStrut(16));
        }

        aboutPanel.add(infoPanel);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));
        contentPanel.add(aboutPanel);
        contentPanel.add(new JSeparator());

        if (!limitedDisplay) {
            contentPanel.add(getSupportPanel());
        }

        setContent(contentPanel, true);
        contentPanel.setDropTarget(createDropTarget());
        setHeader(headerBar = getHeaderBar());
        refreshHeader();
    }

    private static JPanel getSupportPanel() {
        LinkLabel lblLicensing = new LinkLabel("Licensing Information", 0.9f, false);
        lblLicensing.setLinkLocation(Constants.ABOUT_LICENSING_URL);

        LinkLabel lblSupport = new LinkLabel("Support Information", 0.9f, false);
        lblSupport.setLinkLocation(Constants.ABOUT_SUPPORT_URL);

        LinkLabel lblPrivacy = new LinkLabel("Privacy Policy", 0.9f, false);
        lblPrivacy.setLinkLocation(Constants.ABOUT_PRIVACY_URL);

        JPanel supportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 80, 10));
        supportPanel.add(lblLicensing);
        supportPanel.add(lblSupport);
        supportPanel.add(lblPrivacy);
        return supportPanel;
    }

    private LinkLabel getLinkLibrary() {
        LinkLabel linkLibrary = new LinkLabel("Detailed library information");
        if(server != null && server.isRunning() && !server.isStopping()) {
            // Some OSs (e.g. FreeBSD) return null for server.getURI(), fallback to sane values
            URI uri = server.getURI();
            String scheme = uri == null ? "http" : uri.getScheme();
            int port = uri == null ? PrintSocketServer.getInsecurePortInUse(): uri.getPort();
            linkLibrary.setLinkLocation(String.format("%s://%s:%s", scheme, AboutInfo.getPreferredHostname(), port));
        }
        return linkLibrary;
    }

    private JToolBar getHeaderBar() {
        JToolBar headerBar = new JToolBar();
        headerBar.setBorderPainted(false);
        headerBar.setLayout(new FlowLayout());
        headerBar.setOpaque(true);
        headerBar.setFloatable(false);

        LinkLabel substitutionsLabel = new LinkLabel("Substitutions are in effect for this machine");
        JButton refreshButton = new JButton("", getIcon(IconCache.Icon.RELOAD_ICON));
        refreshButton.setOpaque(false);
        refreshButton.addActionListener(e -> {
            Substitutions.getInstance(true);
            refreshHeader();
        });

        substitutionsLabel.setLinkLocation(FileUtilities.SHARED_DIR.toFile());

        headerBar.add(substitutionsLabel);
        headerBar.add(refreshButton);
        return headerBar;
    }

    private DropTarget createDropTarget() {
        return new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                processDroppedFile(evt);
            }

            @Override
            public synchronized void dragEnter(DropTargetDragEvent dtde) {
                super.dragEnter(dtde);
                setDropBorder(true);
            }

            @Override
            public synchronized void dragExit(DropTargetEvent dte) {
                super.dragExit(dte);
                setDropBorder(false);
            }
        };
    }

    private void processDroppedFile(DropTargetDropEvent evt) {
        try {
            evt.acceptDrop(DnDConstants.ACTION_COPY);
            Object dropped = evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if(dropped instanceof List) {
                List<File> droppedFiles = (List<File>)dropped;
                for (File file : droppedFiles) {
                    if(file.getName().equals(Substitutions.FILE_NAME)) {
                        blinkDropBorder(true);
                        log.info("File drop accepted: {}", file);
                        Path source = file.toPath();
                        Path dest = FileUtilities.SHARED_DIR.resolve(file.getName());
                        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                        FileUtilities.inheritParentPermissions(dest);
                        Substitutions.getInstance(true);
                        refreshHeader();
                        break;
                    } else {
                        blinkDropBorder(false);
                        break;
                    }
                }
            }
            evt.dropComplete(true);
        } catch (Exception ex) {
            log.warn(ex);
        }
        setDropBorder(false);
    }

    private void checkForUpdate() {
        Version latestVersion = AboutInfo.findLatestVersion();
        if (latestVersion.greaterThan(Constants.VERSION)) {
            lblUpdate.setText("An update is available:");

            updateButton.setText("Download " + latestVersion);
            updateButton.setVisible(true);
        } else if (latestVersion.lessThan(Constants.VERSION)) {
            lblUpdate.setText("You are on a beta release.");

            updateButton.setText("Revert to stable " + latestVersion);
            updateButton.setVisible(true);
        } else {
            lblUpdate.setText("You have the latest version.");

            updateButton.setVisible(false);
        }
    }

    private void setDropBorder(boolean isShown) {
        if(isShown) {
            if(contentPanel.getBorder() == null) {
                dropBorder = BorderFactory.createDashedBorder(Constants.TRUSTED_COLOR, 3, 5, 5, true);
                contentPanel.setBorder(dropBorder);
            }
        } else {
            contentPanel.setBorder(null);
        }
    }

    private void blinkDropBorder(boolean success) {
        Color borderColor = success ? Color.GREEN : Constants.WARNING_COLOR;
        dropBorder = BorderFactory.createDashedBorder(borderColor, 3, 5, 5, true);
        AtomicBoolean toggled = new AtomicBoolean(true);
        int blinkCount = 3;
        int blinkDelay = 100; // ms
        for(int i = 0; i < blinkCount * 2; i++) {
            Timer timer = new Timer("blink" + i);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(() -> {
                        contentPanel.setBorder(toggled.getAndSet(!toggled.get())? dropBorder:null);
                    });
                }
            }, i * blinkDelay);
        }
    }

    private void refreshHeader() {
        headerBar.setBackground(SystemUtilities.isDarkDesktop() ?
                                        Constants.TRUSTED_COLOR.darker().darker() : Constants.TRUSTED_COLOR_DARK);
        headerBar.setVisible(Substitutions.getInstance() != null);
        pack();
    }


    @Override
    public void setVisible(boolean visible) {
        if (visible && !limitedDisplay) {
            checkForUpdate();
        }

        super.setVisible(visible);
    }

    @Override
    public void refresh() {
        refreshHeader();
        super.refresh();
    }
}
