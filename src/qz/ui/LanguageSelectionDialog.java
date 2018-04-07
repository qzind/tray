package qz.ui;

import qz.common.I18NLoader;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Locale;
import java.util.Objects;

import static qz.common.I18NLoader.gettext;

public class LanguageSelectionDialog extends BasicDialog {
    private JPanel gridPanel;
    private JButton saveButton;

    public LanguageSelectionDialog(JMenuItem menuItem, IconCache iconCache) {
        super(menuItem, iconCache);
        initComponents();
    }

    public void initComponents() {
        gridPanel = new JPanel();

        gridPanel.setLayout(new BoxLayout(gridPanel, BoxLayout.Y_AXIS));
        ButtonGroup buttonGroup = new ButtonGroup();

        I18NLoader.SUPPORTED_LOCALES.forEach(
                locale -> {
                    JRadioButton jRadioButton = new JRadioButton(locale.getDisplayName(I18NLoader.getCurrentLocale()));

                    jRadioButton.setSelected(Objects.equals(locale, I18NLoader.getCurrentLocale()));
                    jRadioButton.setActionCommand(locale.toLanguageTag());

                    buttonGroup.add(jRadioButton);
                    gridPanel.add(jRadioButton);
                }
        );

        saveButton = addPanelButton(gettext("Save"), IconCache.Icon.SAVED_ICON, KeyEvent.VK_S);
        saveButton.addActionListener(e -> I18NLoader.changeLocale(Locale.forLanguageTag(buttonGroup.getSelection().getActionCommand())));

        setContent(gridPanel, true);
    }
}
