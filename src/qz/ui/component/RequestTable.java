package qz.ui.component;

import org.codehaus.jettison.json.JSONException;
import qz.auth.RequestState;
import qz.ui.Themeable;

import javax.swing.*;
import java.awt.*;

public class RequestTable extends DisplayTable implements Themeable {

    public enum RequestField {
        CALL("Call", "call"),
        PARAMS("Parameters", "params"),
        SIGNATURE("Signature", "signature"),
        TIMESTAMP("Timestamp", "timestamp"),
        VALIDITY("Validity", null);

        final String label;
        final String fieldName;

        RequestField(String label, String fieldName) {
            this.label = label;
            this.fieldName = fieldName;
        }

        public String getValue(RequestState request) {
            if (request != null && !request.getRequestData().isNull(fieldName)) {
                try { return request.getRequestData().getString(fieldName); }
                catch(JSONException ignore) {}
            }

            return "";
        }

        @Override
        public String toString() {
            return label;
        }

        public static int size() {
            return values().length;
        }

        public String getLabel() {
            return label;
        }

        public String getFieldName() {
            return fieldName;
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

    public RequestState getRequest() {
        return request;
    }

    @Override
    public void refreshComponents() {
        if (request == null) {
            return;
        }

        removeRows();

        for(RequestField field : RequestField.values()) {
            if (field == RequestField.VALIDITY) {
                model.addRow(new Object[] {field, request.getValidity().getDescription()});
            } else {
                model.addRow(new Object[] {field, field.getValue(request)});
            }
        }

        repaint();
    }

    @Override
    public void refresh() {
        refreshComponents();
        ((StyledTableCellRenderer)getDefaultRenderer(Object.class)).refresh();
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
                label = stylizeLabel(STATUS_NORMAL, label, isSelected);
                if (iconCache != null) {
                    label.setIcon(iconCache.getIcon(IconCache.Icon.FIELD_ICON));
                }
                return label;
            }

            // Second Column
            if (request == null || col < 1) { return stylizeLabel(STATUS_NORMAL, label, isSelected); }

            RequestField field = (RequestField)table.getValueAt(row, col - 1);
            if (field == null) { return stylizeLabel(STATUS_NORMAL, label, isSelected); }

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
                    if (request.isVerified()) {
                        style = STATUS_TRUSTED;
                    } else if (request.getValidity() != RequestState.Validity.EXPIRED) {
                        style = STATUS_WARNING;
                    }

                    if (label.getText().isEmpty()) {
                        if (request.isInitialConnect()) {
                            label.setText("Not Required");
                            style = STATUS_NORMAL;
                        } else {
                            label.setText("Missing");
                        }
                    }
                    break;
                case TIMESTAMP:
                    if (request.getValidity() == RequestState.Validity.EXPIRED) {
                        style = STATUS_WARNING;
                    }
                    break;
                case VALIDITY:
                    style = request.isVerified()? STATUS_TRUSTED:STATUS_WARNING;
                    break;
            }

            return stylizeLabel(style, label, isSelected);
        }

    }

}
