package qz.ui;

import com.github.zafarkhaja.semver.Version;
import org.codehaus.jettison.json.JSONArray;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.ui.component.IconCache;
import qz.ui.component.LinkLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tres on 2/26/2015.
 * Displays a basic about dialog
 */
public class AboutDialog extends BasicDialog implements Themeable {

    private static final Logger log = LoggerFactory.getLogger(AboutDialog.class);

    private Server server;
    private Version latestVersion;


    public AboutDialog(JMenuItem menuItem, IconCache iconCache) {
        super(menuItem, iconCache);

        try {
            URL api = new URL("https://api.github.com/repos/qzind/tray/releases");
            BufferedReader br = new BufferedReader(new InputStreamReader(api.openStream()));

            StringBuilder rawJson = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) {
                rawJson.append(line);
            }

            JSONArray json = new JSONArray(rawJson.toString());
            latestVersion = Version.valueOf(json.getJSONObject(0).getString("name"));

            log.trace("Found latest version: {}", latestVersion);
        }
        catch(Exception e) {
            log.error("Failed to get latest version info", e);
        }
    }

    public void setServer(Server server) {
        this.server = server;

        initComponents();
    }

    public void initComponents() {
        setIconImage(getImage(IconCache.Icon.ABOUT_ICON));

        JLabel lblAbout = new JLabel(Constants.ABOUT_TITLE);
        lblAbout.setFont(new Font(null, Font.PLAIN, 36));

        LinkLabel linkNew = new LinkLabel("What's New?");
        linkNew.setLinkLocation(Constants.ABOUT_VERSION_URL);
        LinkLabel linkLibrary = new LinkLabel("Detailed library information");
        linkLibrary.setLinkLocation(server.getURI().toString());

        JLabel lblUpdate = new JLabel();
        JButton updateButton = new JButton();
        updateButton.setVisible(false);
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try { Desktop.getDesktop().browse(new URL(Constants.ABOUT_URL + "/download").toURI()); }
                catch(Exception e) { log.error("", e); }
            }
        });

        if (latestVersion.greaterThan(Constants.VERSION)) {
            lblUpdate.setText("An update is available:");

            updateButton.setText("Download " + latestVersion.toString());
            updateButton.setVisible(true);
        } else {
            lblUpdate.setText("You have the latest version.");
        }

        //JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setPreferredSize(new Dimension(320, 260));

        infoPanel.add(lblAbout);
        infoPanel.add(Box.createVerticalGlue());
        Box versionBox = Box.createHorizontalBox();
        versionBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        versionBox.add(new JLabel(String.format("%s (Java)", Constants.VERSION.toString())));
        versionBox.add(Box.createHorizontalStrut(12));
        versionBox.add(linkNew);
        infoPanel.add(versionBox);
        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(lblUpdate);
        infoPanel.add(updateButton);
        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(new JLabel(String.format("<html>%s is written and supported by %s.</html>", Constants.ABOUT_TITLE, Constants.ABOUT_COMPANY)));
        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(new JLabel(String.format("<html>If using %s commercially, please first reach out to the website publisher for support issues.</html>", Constants.ABOUT_TITLE)));
        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(linkLibrary);


        JPanel aboutPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        aboutPanel.add(new JLabel(getIcon(IconCache.Icon.LOGO_ICON)));
        aboutPanel.add(infoPanel);


        //override font to remove underline for these links
        Font lblFont = new Font(null, Font.PLAIN, 12);
        Map<TextAttribute,Object> attributes = new HashMap<>(lblFont.getAttributes());
        attributes.remove(TextAttribute.UNDERLINE);
        lblFont = lblFont.deriveFont(attributes);

        LinkLabel lblLicensing = new LinkLabel("Licensing Information");
        lblLicensing.setLinkLocation(Constants.ABOUT_URL + "/licensing");
        lblLicensing.setFont(lblFont);

        LinkLabel lblSupport = new LinkLabel("Support Information");
        lblSupport.setLinkLocation(Constants.ABOUT_URL + "/support");
        lblSupport.setFont(lblFont);

        LinkLabel lblPrivacy = new LinkLabel("Privacy Policy");
        lblPrivacy.setLinkLocation(Constants.ABOUT_URL + "/privacy");
        lblPrivacy.setFont(lblFont);

        JPanel supportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 80, 10));
        supportPanel.add(lblLicensing);
        supportPanel.add(lblSupport);
        supportPanel.add(lblPrivacy);


        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(aboutPanel);
        panel.add(new JToolBar.Separator());
        panel.add(supportPanel);


        setContent(panel, true);
    }

}
