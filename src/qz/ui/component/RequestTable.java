package qz.ui.component;

import org.codehaus.jettison.json.JSONException;
import qz.auth.RequestState;

import javax.swing.*;
import java.awt.*;

public class RequestTable extends DisplayTable {

    enum RequestField {
        CALL("Call", "call"),
        PARAMS("Parameters", "params"),
        SIGNATURE("Signature", "signature"),
        TIMESTAMP("Timestamp", "timestamp");

        String description;
        String fieldName;

        RequestField(String description, String fieldName) {
            this.description = description;
            this.fieldName = fieldName;
        }

        public String getValue(RequestState request) {
            if (request == null) {
                return "";
            }

            try {
                return request.getRequestData().getString(fieldName);
            }
            catch(JSONException e) {
                return "";
            }
        }

        @Override
        public String toString() {
            return description;
        }

        public static int size() {
            return values().length;
        }
    }

    private RequestState request;

    public RequestTable(IconCache iconCache) {
        super(iconCache);
        setDefaultRenderer(Object.class, new RequestTableCellRenderer());
    }

    public void setRequest(RequestState request) {
        this.request = request;
    }

    @Override
    public void refreshComponents() {
        if (request == null) {
            return;
        }

        removeRows();

        for(RequestField field : RequestField.values()) {
            model.addRow(new Object[] {field, field.getValue(request)});
        }

        repaint();
    }

    public void autoSize() {
        super.autoSize(RequestField.size(), 2);
    }


    private class RequestTableCellRenderer extends StyledTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            // First Column
            if (value instanceof RequestField) {
                stylizeLabel(STATUS_NORMAL, label, isSelected);
                if (iconCache != null) {
                    label.setIcon(iconCache.getIcon(IconCache.Icon.FIELD_ICON));
                }
                return label;
            }

            // Second Column
            if (request == null || col < 1) { return stylizeLabel(STATUS_NORMAL, label, isSelected); }

            RequestField field = (RequestField)table.getValueAt(row, col - 1);
            int style = STATUS_NORMAL;
            switch(field) {
                case CALL:
                    if (label.getText().isEmpty() && request.isInitialConnect()) {
                        //only time call can be empty is when setting up the connection
                        label.setText("connect");
                    }
                    break;
                case PARAMS:
                    if (label.getText().isEmpty()) {
                        label.setText("{}");
                    }
                    break;
                case SIGNATURE:
                    style = request.isTrusted()? STATUS_TRUSTED:STATUS_WARNING;

                    System.out.println(request.getStatus());
                    if (label.getText().isEmpty()) {
                        if (request.isInitialConnect()) {
                            label.setText("Not Required");
                        } else {
                            label.setText("Missing");
                        }
                    } else if (!request.isTrusted()) {
                        String issue;
                        switch(request.getStatus()) {
                            case EXPIRED:
                                issue = "Expired";
                                break;
                            case UNSIGNED: default:
                                issue = "Invalid";
                                break;
                        }
                        label.setText("(" + issue + ") " + label.getText());
                    }
                    break;
            }

            return stylizeLabel(style, label, isSelected);
        }

    }

}
