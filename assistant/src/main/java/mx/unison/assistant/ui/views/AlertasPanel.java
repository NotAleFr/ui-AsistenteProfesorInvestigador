package mx.unison.assistant.ui.views;
import mx.unison.assistant.service.AppServices;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;

import mx.unison.assistant.ui.common.Ui;

public class AlertasPanel extends JPanel {
    final AppServices services;

    JPanel listPanel = new JPanel();
    Runnable onClose;

    public AlertasPanel(AppServices services, Runnable onClose) {
        this.services = services;
        this.onClose = onClose;

        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Alertas.background"));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, UIManager.getColor("Panel.borderColor")));
        setPreferredSize(new Dimension(UIManager.getInt("Alertas.width"), 0));

        add(createHeader(), BorderLayout.NORTH);
        add(createAlertsList(), BorderLayout.CENTER);

        refresh();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 12));

        JLabel title = new JLabel("Bandeja de alertas");
        title.putClientProperty("FlatLaf.style", "font: semibold, size: 15");

        JButton closeButton = Ui.button("X", "close", () -> onClose.run());

        header.add(title, BorderLayout.WEST);
        header.add(closeButton, BorderLayout.EAST);
        return header;
    }

    private JScrollPane createAlertsList() {
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setBorder(BorderFactory.createEmptyBorder(4, 14, 16, 14));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(listPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    public void refresh() {
        listPanel.removeAll();

        try {
            List<String> alerts = services.alertas.checkAlerts();
            if (alerts.isEmpty()) {
                listPanel.add(createAlertCard("No hay alertas"));
            } else {
                for (String message : alerts) {
                    listPanel.add(createAlertCard(message));
                    listPanel.add(Box.createVerticalStrut(10));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            listPanel.add(createAlertCard("No se pudieron cargar las alertas."));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createAlertCard(String message) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UIManager.getColor("Alertas.cardBackground"));
        card.setBorder(BorderFactory.createCompoundBorder(
            Ui.lineBorder(),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JTextArea text = new JTextArea(message);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setOpaque(false);
        text.setBorder(null);
        text.putClientProperty("FlatLaf.style", "font: size: 13");
        // Le damos el ancho interior real para que calcule bien su altura al envolver líneas
        text.setSize(new Dimension(UIManager.getInt("Alertas.width") - 56, Short.MAX_VALUE));

        card.add(text, BorderLayout.CENTER);
        return card;
    }

    public void showStartupReminders(Component parent) {
        try {
            List<String> reminders = services.alertas.getStartupReminders();
            if (!reminders.isEmpty()) {
                Ui.info(parent, "Recordatorios de inicio", "• " + String.join("\n• ", reminders));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
