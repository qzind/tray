package qz.ui;

import qz.auth.Request;
import qz.ui.component.table.CertificateTable;
import qz.ui.component.IconCache;
import qz.ui.component.table.RequestTable;

import javax.swing.*;

/**
 * Small <code>JPanel</code> container for <code>RequestTable</code> and <code>CertificateTable</code>
 */
public class DetailsDialog extends JPanel {
    public static final String REQUEST_TABLE_LABEL = "Request";
    public static final String REQUEST_TABLE_NAME = String.format("%s Details", REQUEST_TABLE_LABEL);
    public static final String REQUEST_TABLE_DESCRIPTION = "Signing details about this request.";

    public static final String CERT_TABLE_LABEL = "Certificate";
    public static final String CERT_TABLE_NAME = String.format("%s Details", CERT_TABLE_LABEL);
    public static final String CERT_TABLE_DESCRIPTION = "Certificate details about this request.";

    private RequestTable requestTable;
    private CertificateTable certTable;

    public DetailsDialog(IconCache iconCache) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        initComponents(iconCache);
    }

    private void initComponents(IconCache iconCache) {
        JLabel requestLabel = new JLabel(REQUEST_TABLE_LABEL);
        requestLabel.setAlignmentX(CENTER_ALIGNMENT);

        requestTable = new RequestTable(iconCache);
        JScrollPane reqScrollPane = new JScrollPane(requestTable);
        requestTable.getAccessibleContext().setAccessibleName(REQUEST_TABLE_DESCRIPTION);
        requestTable.getAccessibleContext().setAccessibleDescription(REQUEST_TABLE_NAME);
        requestLabel.setLabelFor(requestTable);

        JLabel certLabel = new JLabel(CERT_TABLE_LABEL);
        certLabel.setAlignmentX(CENTER_ALIGNMENT);

        certTable = new CertificateTable(iconCache);
        JScrollPane certScrollPane = new JScrollPane(certTable);
        certTable.getAccessibleContext().setAccessibleName(CERT_TABLE_NAME);
        certTable.getAccessibleContext().setAccessibleDescription(CERT_TABLE_DESCRIPTION);
        certLabel.setLabelFor(certTable);

        add(requestLabel);
        add(reqScrollPane);

        add(new JToolBar.Separator());

        add(certLabel);
        add(certScrollPane);
    }

    public void setRequest(Request request) {
        requestTable.setRequest(request);
    }

    public void updateDisplay() {
        if(requestTable.getRequest() == null) return;

        certTable.setCertificate(requestTable.getRequest().getCertificate());
        certTable.autoSize();
        requestTable.autoSize();
    }

}
