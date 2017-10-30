package qz.ui;

import org.eclipse.jetty.server.*;
import qz.common.Constants;
import qz.common.SecurityInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.SortedMap;

/**
 * Created by Tres on 2/26/2015.
 * Displays a basic about dialog
 */
public class AboutDialog extends BasicDialog {
    JPanel gridPanel;
    JLabel wssLabel;
    JLabel wsLabel;
    SortedMap<String, String> libVersions;
    String name;

    public AboutDialog(JMenuItem menuItem, IconCache iconCache, String name) {
        super(menuItem, iconCache);
        this.name = name;
        initComponents();
    }

    @Override
    public void setVisible(boolean b) {
        if (libVersions == null) {
            libVersions = SecurityInfo.getLibVersions();
            GridLayout layout = (GridLayout)gridPanel.getLayout();
            layout.setRows(layout.getRows() + libVersions.size());

            gridPanel.add(createLabel("Library Name:", true));
            gridPanel.add(createLabel("Version:", true));
            for (Map.Entry<String, String> entry: libVersions.entrySet()) {
                gridPanel.add(createLabel("    " + entry.getKey(), false));
                gridPanel.add(createLabel("    "  + (entry.getValue() == null ? "(unknown)" : entry.getValue()), false));
            }
            shadeComponents();
        }
        super.setVisible(b);
    }

    public void initComponents() {
        JComponent header = setHeader(new JLabel(getIcon(IconCache.Icon.BANNER_ICON)));
        header.setBorder(new EmptyBorder(Constants.BORDER_PADDING, 0, Constants.BORDER_PADDING, 0));

        gridPanel = new JPanel();

        JScrollPane pane = new JScrollPane(gridPanel);
        pane.getVerticalScrollBar().setUnitIncrement(8);
        pane.setPreferredSize(new Dimension(00, 100));
        gridPanel.setLayout(new GridLayout(5, 2));
        gridPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

        wsLabel = new JLabel("None");
        wssLabel = new JLabel("None.  No https support.");
        wssLabel.setForeground(Constants.WARNING_COLOR);
        wssLabel.setFont(wsLabel.getFont().deriveFont(Font.BOLD));

        gridPanel.add(createLabel("Software:", true));
        gridPanel.add(createLabel(name));

        gridPanel.add(createLabel("Secure Socket:", true));
        gridPanel.add(wssLabel);

        gridPanel.add(createLabel("Fallback Socket:", true));
        gridPanel.add(wsLabel);

        gridPanel.add(createLabel("Publisher:", true));
        try {
            gridPanel.add(new LinkLabel(new URL(Constants.ABOUT_URL)));
        }
        catch(MalformedURLException ex) {
            gridPanel.add(new LinkLabel(Constants.ABOUT_URL));
        }

        shadeComponents();
        setContent(pane, true);
    }

    public void shadeComponents() {
        for(int i = 0; i < gridPanel.getComponents().length; i++) {
            if (i % 4 == 0 || i % 4 == 1) {
                if (gridPanel.getComponent(i) instanceof JComponent) {
                    ((JComponent)gridPanel.getComponent(i)).setOpaque(true);
                    gridPanel.getComponent(i).setBackground(gridPanel.getComponent(i).getBackground().brighter());
                }
            }
            ((JComponent)gridPanel.getComponent(i)).setBorder(new EmptyBorder(0, Constants.BORDER_PADDING, 0, Constants.BORDER_PADDING));
        }
    }

    public JComponent createLabel(String text) {
        return createLabel(text, false);
    }

    public JComponent createLabel(String text, boolean isBold) {
        JLabel label = new JLabel(text);
        if (isBold) {
            label.setFont(label.getFont().deriveFont(Font.BOLD));
        }

        return label;
    }

    /**
     * Sets server for displaying port and socket secure/insecure information
     */
    public void setServer(Server server) {
        for(Connector c : server.getConnectors()) {
            for(ConnectionFactory f : c.getConnectionFactories()) {
                ServerConnector s = (ServerConnector)c;
                if (f instanceof SslConnectionFactory) {
                    wssLabel.setText("wss://localhost:" + s.getLocalPort());
                    wssLabel.setFont(wsLabel.getFont());
                    wssLabel.setForeground(wsLabel.getForeground());
                    break;
                } else {
                    wsLabel.setText("ws://localhost:" + s.getLocalPort());
                    break;
                }
            }
        }

        pack();
    }

    public JButton addPanelButton(JMenuItem menuItem) {
        JButton button = addPanelButton(menuItem.getText(), menuItem.getIcon(), menuItem.getMnemonic());
        button.addActionListener(menuItem.getActionListeners()[0]);
        return button;
    }
}
