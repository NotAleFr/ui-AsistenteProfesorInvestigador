package mx.unison.assistant.ui.common;

import java.awt.*;
import javax.swing.*;

public class PageHeader extends JPanel {
    JPanel actionsPanel = new JPanel();

    public PageHeader(String title) {
        int bottomGap = UIManager.getInt("PageHeader.bottomGap");

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, bottomGap, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.putClientProperty("FlatLaf.styleClass", "h1");

        actionsPanel.setLayout(new GridBagLayout());
        actionsPanel.setOpaque(false);

        add(titleLabel, BorderLayout.WEST);
        add(actionsPanel, BorderLayout.EAST);
    }

    public void addAction(JButton button) {
        if (actionsPanel.getComponentCount() > 0) {
            actionsPanel.add(Box.createHorizontalStrut(8));
        }
        actionsPanel.add(button);
    }
}
