package mx.unison.assistant.ui.views;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.*;

import mx.unison.assistant.model.AlertaItem;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.RecordatorioItem;
import mx.unison.assistant.service.AppServices;
import mx.unison.assistant.ui.common.Ui;
import mx.unison.assistant.ui.dialogs.RecordatoriosDialog;

public class AlertasPanel extends JPanel {
    final AppServices services;

    JPanel listPanel = new JPanel();
    Runnable onClose;
    Consumer<AlertaItem> onSelectAlert;
    IntConsumer onCountChanged;

    public AlertasPanel(AppServices services, Runnable onClose,
                        Consumer<AlertaItem> onSelectAlert, IntConsumer onCountChanged) {
        this.services = services;
        this.onClose = onClose;
        this.onSelectAlert = onSelectAlert;
        this.onCountChanged = onCountChanged;

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
        int alertCount = 0;

        try {
            List<AlertaItem> alertItems = services.alertas.refreshAlertItems();
            alertCount = alertItems.size();
            if (alertItems.isEmpty()) {
                listPanel.add(createStaticCard("No hay alertas"));
            } else {
                for (AlertaItem alertItem : alertItems) {
                    listPanel.add(createClickableAlertCard(alertItem));
                    listPanel.add(Box.createVerticalStrut(10));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            listPanel.add(createStaticCard("No se pudieron cargar las alertas."));
        }

        if (onCountChanged != null) {
            onCountChanged.accept(alertCount);
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createStaticCard(String message) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UIManager.getColor("Alertas.cardBackground"));
        card.setBorder(BorderFactory.createCompoundBorder(
            Ui.lineBorder(),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JTextArea text = createCardText(message, "font: size: 13");
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JPanel createClickableAlertCard(AlertaItem alertItem) {
        Color defaultBackground = UIManager.getColor("Alertas.cardBackground");
        Color hoverBackground = UIManager.getColor("Panel.subtleBackground");

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(defaultBackground);
        card.setBorder(BorderFactory.createCompoundBorder(
            Ui.lineBorder(),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JTextArea titleText = createCardText(alertItem.titulo(), "font: semibold, size: 13");
        titleText.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea messageText = createCardText(alertItem.mensaje(), "font: size: 12");
        messageText.setForeground(UIManager.getColor("Text.secondaryForeground"));
        messageText.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton actionButton = Ui.linkButton(
            alertItem.planActivo() ? "Dar seguimiento" : "Ver en Plan académico",
            () -> openAlertItem(alertItem)
        );
        actionButton.putClientProperty("FlatLaf.style", "font: semibold, size: 12; focusWidth: 1; margin: 2,2,2,2; background: #00000000; hoverBackground: #0000000D");
        actionButton.setHorizontalAlignment(SwingConstants.LEFT);
        actionButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleText);
        card.add(Box.createVerticalStrut(4));
        card.add(messageText);
        card.add(Box.createVerticalStrut(6));
        card.add(actionButton);

        MouseAdapter interactionHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openAlertItem(alertItem);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(hoverBackground);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(defaultBackground);
            }
        };
        card.addMouseListener(interactionHandler);
        titleText.addMouseListener(interactionHandler);
        messageText.addMouseListener(interactionHandler);
        return card;
    }

    private void openAlertItem(AlertaItem alertItem) {
        if (onSelectAlert != null) {
            onSelectAlert.accept(alertItem);
        }
    }

    private JTextArea createCardText(String content, String flatLafStyle) {
        JTextArea text = new JTextArea(content);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setOpaque(false);
        text.setBorder(null);
        text.putClientProperty("FlatLaf.style", flatLafStyle);
        // Le damos el ancho interior real para que calcule bien su altura al envolver líneas
        text.setSize(new Dimension(UIManager.getInt("Alertas.width") - 56, Short.MAX_VALUE));
        return text;
    }

    public void showStartupReminders(Component parent, Consumer<Evidencia> onOpenEvidence, Runnable onGoToTracking) {
        try {
            List<RecordatorioItem> reminders = services.alertas.getStartupReminderItems();
            if (!reminders.isEmpty()) {
                Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
                new RecordatoriosDialog(owner, reminders, onOpenEvidence, onGoToTracking).setVisible(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
