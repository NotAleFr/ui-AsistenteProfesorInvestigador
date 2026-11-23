package mx.unison.assistant.ui.common;

import java.awt.*;
import javax.swing.*;

// Barra de contadores (Total, Entregadas, Pendientes, En proceso)
public class StatsBar extends JPanel {

    JLabel totalLabel = new JLabel("0");
    JLabel deliveredLabel = new JLabel("0");
    JLabel pendingLabel = new JLabel("0");
    JLabel inProgressLabel = new JLabel("0");

    public StatsBar() {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        setOpaque(false);

        add(createItem("Total", totalLabel));
        add(createDivider());
        add(createItem("Entregadas", deliveredLabel));
        add(createDivider());
        add(createItem("Pendientes", pendingLabel));
        add(createDivider());
        add(createItem("En proceso", inProgressLabel));
    }

    private JPanel createItem(String titleText, JLabel valueLabel) {
        JPanel item = new JPanel();
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        item.setOpaque(false);

        JLabel title = new JLabel(titleText);
        title.setForeground(UIManager.getColor("Text.secondaryForeground"));
        title.putClientProperty("FlatLaf.style", "font: size: 12");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        valueLabel.putClientProperty("FlatLaf.style", "font: semibold, size: 22");
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        item.add(title);
        item.add(Box.createVerticalStrut(2));
        item.add(valueLabel);
        return item;
    }

    private JSeparator createDivider() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 36));
        separator.setForeground(UIManager.getColor("Panel.borderColor"));
        return separator;
    }

    public void setCounts(int total, int delivered, int pending, int inProgress) {
        totalLabel.setText(String.valueOf(total));
        deliveredLabel.setText(String.valueOf(delivered));
        pendingLabel.setText(String.valueOf(pending));
        inProgressLabel.setText(String.valueOf(inProgress));
    }
}
