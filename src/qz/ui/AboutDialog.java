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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tres on 2/26/2015.
 * Displays a basic about dialog
 */
public class AboutDialog extends BasicDialog implements Themeable {

    private static final Logger log = LogManager.getLogger(AboutDialog.class);

    private Server server;

    private boolean limitedDisplay;

    private JLabel lblUpdate;
    private JButton updateButton;

    // Use <html> allows word wrapping on a standard JLabel
    class TextWrapLabel extends JLabel {
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

        LinkLabel linkLibrary = new LinkLabel("Detailed library information");
        if(server != null && server.isRunning() && !server.isStopping()) {
            linkLibrary.setLinkLocation(String.format("%s://%s:%s", server.getURI().getScheme(), AboutInfo.getPreferredHostname(), server.getURI().getPort()));
        }
        Box versionBox = Box.createHorizontalBox();
        versionBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        versionBox.add(new JLabel(String.format("%s (Java)", Constants.VERSION.toString())));


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

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(aboutPanel);
        panel.add(new JSeparator());

        if (!limitedDisplay) {
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

            panel.add(supportPanel);
        }

        setContent(panel, true);
    }

    private void checkForUpdate() {
        Version latestVersion = AboutInfo.findLatestVersion();
        if (latestVersion.greaterThan(Constants.VERSION)) {
            lblUpdate.setText("An update is available:");

            updateButton.setText("Download " + latestVersion.toString());
            updateButton.setVisible(true);
        } else if (latestVersion.lessThan(Constants.VERSION)) {
            lblUpdate.setText("You are on a beta release.");

            updateButton.setText("Revert to stable " + latestVersion.toString());
            updateButton.setVisible(true);
        } else {
            lblUpdate.setText("You have the latest version.");

            updateButton.setVisible(false);
        }
    }


    @Override
    public void setVisible(boolean visible) {
        if (visible && !limitedDisplay) {
            checkForUpdate();
        }

        super.setVisible(visible);
    }


}
