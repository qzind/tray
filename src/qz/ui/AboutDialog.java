package qz.ui;

import com.github.zafarkhaja.semver.Version;
import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.ui.component.IconCache;
import qz.ui.component.LinkLabel;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
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

    private int connectionPort;
    private Version latestVersion;

    JEditorPane versionInfoBlock;
    JEditorPane productInfoBlock;


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

    public void usePort(int port) {
        connectionPort = port;

        initComponents();
    }

    public void initComponents() {
        setIconImage(getImage(IconCache.Icon.ABOUT_ICON));

        //language=HTML
        String styleBlock = "<style> " +
                "  body { font-family: 'sans-serif'; font-size: 10px; } " +
                "  h1 { font-size: 36px; font-weight: normal; } " +
                "  div { margin: 5px 0; width: 100%; } " +
                "</style>";

        //language=HTML
        String infoText = "<h1>%title%</h1>" +
                "<div>%version% (Java) <a href='%version_url%'>What's New?</a></div>" +
                "<div>%update_action%</div>" +
                "#split#" + //Split text to accommodate swing buttons
                "<div>%title% is written and supported by %company%.</div>" +
                "<div>If using %title% commercially, please first reach out to the website publisher for support issues.</div>" +
                "<div><a href='%library_url%'>Detailed library information \uD83D\uDDD7</a></div>";

        infoText = infoText.replaceAll("%title%", Constants.ABOUT_TITLE)
                .replaceAll("%company%", Constants.ABOUT_COMPANY)
                .replaceAll("%version%", Constants.VERSION.toString())
                .replaceAll("%library_url%", "https://localhost:" + connectionPort)
                .replaceAll("%version_url%", "https://github.com/qzind/tray/releases");


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
            infoText = infoText.replaceAll("%update_action%", "An update is available");

            updateButton.setText("Download " + latestVersion.toString());
            updateButton.setVisible(true);
        } else {
            infoText = infoText.replaceAll("%update_action%", "You have the latest version.");
        }

        HyperlinkListener linkListener = new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent evt) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(evt.getEventType())) {
                    try { Desktop.getDesktop().browse(evt.getURL().toURI()); }
                    catch(Exception e) { log.error("", e); }
                }
            }
        };


        String[] infoBlocks = infoText.split("#split#");

        versionInfoBlock = new JEditorPane("text/html", "<html>" + styleBlock + infoBlocks[0] + "</html>");
        versionInfoBlock.setMaximumSize(new Dimension(320, 200));
        versionInfoBlock.setEditable(false);
        versionInfoBlock.setOpaque(false);
        versionInfoBlock.addHyperlinkListener(linkListener);
        versionInfoBlock.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        productInfoBlock = new JEditorPane("text/html", "<html>" + styleBlock + infoBlocks[1] + "</html>");
        productInfoBlock.setMaximumSize(new Dimension(320, 200));
        productInfoBlock.setEditable(false);
        productInfoBlock.setOpaque(false);
        productInfoBlock.addHyperlinkListener(linkListener);
        productInfoBlock.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        ((HTMLEditorKit)productInfoBlock.getEditorKit()).setStyleSheet(((HTMLEditorKit)versionInfoBlock.getEditorKit()).getStyleSheet());

        JPanel infoBlock = new JPanel();
        infoBlock.setPreferredSize(new Dimension(320, 300));
        infoBlock.setLayout(new BoxLayout(infoBlock, BoxLayout.Y_AXIS));
        infoBlock.add(versionInfoBlock);
        Box btnBox = Box.createHorizontalBox();
        btnBox.add(updateButton);
        btnBox.add(Box.createHorizontalGlue());
        infoBlock.add(btnBox);
        infoBlock.add(productInfoBlock);

        JPanel aboutPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        aboutPanel.add(new JLabel(getIcon(IconCache.Icon.LOGO_ICON)));
        aboutPanel.add(infoBlock);


        //override font to remove underline for these links
        Font lblFont = new Font(null, Font.PLAIN, 12);
        Map<TextAttribute,Object> attributes = new HashMap<>(lblFont.getAttributes());
        attributes.remove(TextAttribute.UNDERLINE);
        lblFont = lblFont.deriveFont(attributes);

        LinkLabel lblLicensing = new LinkLabel("Licensing Information", Constants.ABOUT_URL + "/licensing ");
        lblLicensing.setFont(lblFont);
        LinkLabel lblSupport = new LinkLabel("Support Information", Constants.ABOUT_URL + "/support");
        lblSupport.setFont(lblFont);
        LinkLabel lblPrivacy = new LinkLabel("Privacy Policy", Constants.ABOUT_URL + "/privacy");
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

    @Override
    public void refresh() {
        super.refresh();

        //ensure jeditorpane link colors are updated..
        HTMLEditorKit kit = (HTMLEditorKit)versionInfoBlock.getEditorKit();
        StyleSheet ss = kit.getStyleSheet();

        //FIXME - broken changing lite -> dark
        String hexFormat = String.format("%02x%02x%02x", Constants.TRUSTED_COLOR.getRed(), Constants.TRUSTED_COLOR.getGreen(), Constants.TRUSTED_COLOR.getBlue());
        ss.addRule("a { color: #" + hexFormat + "; text-decoration: underline;}");
    }

}
