package qz.ui.component.table;

import org.codehaus.jettison.json.JSONException;
import qz.auth.RequestState;
import qz.ui.Themeable;
import qz.ui.component.IconCache;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.Objects;

import static qz.ui.component.table.FieldStyle.*;


public class RequestTable extends FieldValueTable implements Themeable {
    private final static String NOT_REQUIRED = "Not Required";
    private final static String MISSING = "Missing";

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

        public String getValue(RequestState requestState) {
            String value = "";
            boolean initialConnect = false;
            if (requestState != null) {
                try {
                    if(!requestState.getRequestData().isNull(fieldName)) {
                        value = requestState.getRequestData().getString(fieldName);
                    }
                    initialConnect = requestState.isInitialConnect();
                } catch(JSONException ignore) {}
            }

            // Sanitize blank values for readability
            if(value.isBlank()) {
                switch(this) {
                    case CALL:
                        return initialConnect ? "connect" : value;
                    case PARAMS:
                        return "{}";
                    case SIGNATURE:
                        return initialConnect ? NOT_REQUIRED : MISSING;
                    case VALIDITY:
                        return Objects.requireNonNull(requestState).getValidity().getDescription();
                }
            }
            return value;
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

        /**
         * Validity has special JSON handling, so it's field name is null, but
         * this can cause iteration issues with headless dialogs.  We can
         * circumvent this by returning "validity" instead
         */
        public String getFieldName(boolean preventNull) {
            if(preventNull && fieldName == null) {
                return name().toLowerCase(Locale.ENGLISH);
            }
            return fieldName;
        }
    }

    private RequestState requestState;

    public RequestTable(IconCache iconCache) {
        super(iconCache);
        setDefaultRenderer(Object.class, new RequestTableCellRenderer());
    }

    public void setRequestState(RequestState requestState) {
        this.requestState = requestState;
    }

    public RequestState getRequestState() {
        return requestState;
    }

    public static FieldStyle getStyle(RequestState requestState, RequestField requestField) {
        switch(requestField) {
            case SIGNATURE:
                if(requestState.isInitialConnect()) {
                    return NORMAL;
                }
                if (requestState.getValidity() != RequestState.Validity.TRUSTED) {
                    return WARNING;
                }
                break;
            case TIMESTAMP:
                if (requestState.getValidity() == RequestState.Validity.EXPIRED) {
                    return WARNING;
                }
                break;
            case VALIDITY:
                return requestState.getValidity() == RequestState.Validity.TRUSTED? TRUSTED : WARNING;
        }
        return NORMAL;
    }

    @Override
    public void refreshComponents() {
        if (requestState == null) {
            return;
        }

        removeRows();

        for(RequestField field : RequestField.values()) {
            model.addRow(new Object[] {field, field.getValue(requestState)});
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
                label = stylizeLabel(NORMAL, label, isSelected);
                if (iconCache != null) {
                    label.setIcon(iconCache.getIcon(IconCache.Icon.FIELD_ICON));
                }
                return label;
            }

            // Second Column
            if (requestState == null || col < 1) { return stylizeLabel(NORMAL, label, isSelected); }

            RequestField field = (RequestField)table.getValueAt(row, col - 1);
            if (field == null) { return stylizeLabel(NORMAL, label, isSelected); }

            return stylizeLabel(getStyle(requestState, field), label, isSelected);
        }

    }

}
