package qz.ui;

import qz.auth.RequestState;
import qz.ui.component.CertificateTable;
import qz.ui.component.IconCache;
import qz.ui.component.RequestTable;

import javax.swing.*;

/**
 * Small <code>JPanel</code> container for <code>RequestTable</code> and <code>CertificateTable</code>
 */
public class DetailsDialog extends JPanel {
    private RequestTable requestTable;
    private CertificateTable certTable;

    public DetailsDialog(IconCache iconCache) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        initComponents(iconCache);
    }

    private void initComponents(IconCache iconCache) {
        JLabel requestLabel = new JLabel("Request");
        requestLabel.setAlignmentX(CENTER_ALIGNMENT);

        requestTable = new RequestTable(iconCache);
        JScrollPane reqScrollPane = new JScrollPane(requestTable);
        requestTable.getAccessibleContext().setAccessibleName(requestLabel.getText() + " Details");
        requestTable.getAccessibleContext().setAccessibleDescription("Signing details about this request.");
        requestLabel.setLabelFor(requestTable);

        JLabel certLabel = new JLabel("Certificate");
        certLabel.setAlignmentX(CENTER_ALIGNMENT);

        certTable = new CertificateTable(iconCache);
        JScrollPane certScrollPane = new JScrollPane(certTable);
        certTable.getAccessibleContext().setAccessibleName(certLabel.getText() + " Details");
        certTable.getAccessibleContext().setAccessibleDescription("Certificate details about this request.");
        certLabel.setLabelFor(certTable);

        add(requestLabel);
        add(reqScrollPane);

        add(new JToolBar.Separator());

        add(certLabel);
        add(certScrollPane);
    }

    public void setRequest(RequestState requestState) {
        requestTable.setRequest(requestState);
    }

    public void updateDisplay() {
        if(requestTable.getRequest() == null) return;

        certTable.setCertificate(requestTable.getRequest().getCertificate());
        certTable.autoSize();
        requestTable.autoSize();
    }

}
