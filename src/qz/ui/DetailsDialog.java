package qz.ui;

import qz.auth.RequestState;
import qz.ui.component.CertificateTable;
import qz.ui.component.IconCache;
import qz.ui.component.RequestTable;

import javax.swing.*;

public class DetailsDialog extends JPanel {

    private JLabel requestLabel;
    private JScrollPane reqScrollPane;
    private RequestTable requestTable;

    private JLabel certLabel;
    private JScrollPane certScrollPane;
    private CertificateTable certTable;


    public DetailsDialog(IconCache iconCache) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        initComponents(iconCache);
    }

    private void initComponents(IconCache iconCache) {
        requestLabel = new JLabel("Request");
        requestLabel.setAlignmentX(CENTER_ALIGNMENT);

        requestTable = new RequestTable(iconCache);
        reqScrollPane = new JScrollPane(requestTable);

        certLabel = new JLabel("Certificate");
        certLabel.setAlignmentX(CENTER_ALIGNMENT);

        certTable = new CertificateTable(iconCache);
        certScrollPane = new JScrollPane(certTable);

        add(requestLabel);
        add(reqScrollPane);

        add(new JToolBar.Separator());

        add(certLabel);
        add(certScrollPane);
    }

    public void updateDisplay(RequestState request) {
        certTable.setCertificate(request.getCertUsed());
        certTable.autoSize();

        requestTable.setRequest(request);
        requestTable.autoSize();
    }

}
