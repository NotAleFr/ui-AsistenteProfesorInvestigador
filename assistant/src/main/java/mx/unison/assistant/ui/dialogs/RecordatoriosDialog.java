package mx.unison.assistant.ui.dialogs;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Fechas;
import mx.unison.assistant.model.Periodo;
import mx.unison.assistant.model.RecordatorioItem;
import mx.unison.assistant.ui.common.DialogBar;
import mx.unison.assistant.ui.common.Ui;

// Diálogo emergente de recordatorios al iniciar la aplicación (RF-21)
public class RecordatoriosDialog extends JDialog {

    private final Consumer<Evidencia> onOpenEvidence;
    private final Runnable onGoToTracking;

    public RecordatoriosDialog(Window owner, List<RecordatorioItem> reminders,
                               Consumer<Evidencia> onOpenEvidence, Runnable onGoToTracking) {
        super(owner, "Recordatorios de inicio", ModalityType.APPLICATION_MODAL);
        this.onOpenEvidence = onOpenEvidence;
        this.onGoToTracking = onGoToTracking;

        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        add(createHeader(), BorderLayout.NORTH);
        add(createBody(reminders), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        pack();
        int width = Math.max(520, Math.min(600, getPreferredSize().width));
        int height = Math.max(240, Math.min(540, getPreferredSize().height + 12));
        setSize(width, height);
        setMinimumSize(new Dimension(480, 230));
        setLocationRelativeTo(owner);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(UIManager.getColor("Panel.background"));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Panel.borderColor")),
            BorderFactory.createEmptyBorder(20, 24, 14, 24)
        ));

        JLabel title = new JLabel("Recordatorios de inicio");
        title.putClientProperty("FlatLaf.style", "font: bold, size: 20");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        String subtitleText = "Periodo actual: " + Periodo.current() + "  ·  " + Fechas.toScreen(LocalDate.now());
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setForeground(UIManager.getColor("Text.secondaryForeground"));
        subtitle.putClientProperty("FlatLaf.style", "font: size: 12");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        return header;
    }

    private JScrollPane createBody(List<RecordatorioItem> reminders) {
        Color dialogBackground = UIManager.getColor("Panel.background");

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(dialogBackground);
        list.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        for (int i = 0; i < reminders.size(); i++) {
            list.add(createReminderCard(reminders.get(i)));
            if (i < reminders.size() - 1) {
                list.add(Box.createVerticalStrut(12));
            }
        }

        ViewportWidthPanel wrapper = new ViewportWidthPanel(new BorderLayout());
        wrapper.setBackground(dialogBackground);
        wrapper.add(list, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(dialogBackground);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel createReminderCard(RecordatorioItem reminder) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UIManager.getColor("Panel.subtleBackground"));
        card.setBorder(BorderFactory.createCompoundBorder(
            Ui.lineBorder(),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(reminder.titulo());
        title.putClientProperty("FlatLaf.style", "font: semibold, size: 14");
        if (reminder.urgente()) {
            title.setForeground(UIManager.getColor("Status.error.foreground"));
        }
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea detail = new JTextArea(reminder.detalle());
        detail.setEditable(false);
        detail.setFocusable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setOpaque(false);
        detail.setBorder(null);
        detail.setForeground(UIManager.getColor("Text.secondaryForeground"));
        detail.putClientProperty("FlatLaf.style", "font: size: 13");
        detail.setSize(new Dimension(440, Short.MAX_VALUE));
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(detail);

        if (reminder.evidencias() != null && !reminder.evidencias().isEmpty()) {
            card.add(Box.createVerticalStrut(10));
            for (Evidencia evidencia : reminder.evidencias()) {
                card.add(createEvidenceRow(evidencia));
                card.add(Box.createVerticalStrut(6));
            }
        }
        return card;
    }

    private JPanel createEvidenceRow(Evidencia evidencia) {
        Color defaultBackground = UIManager.getColor("Alertas.cardBackground");
        Color hoverBackground = UIManager.getColor("Alertas.background");

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(defaultBackground);
        row.setBorder(BorderFactory.createCompoundBorder(
            Ui.lineBorder(),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);
        textCol.setMinimumSize(new Dimension(0, 0));

        String evidenceTitle = (evidencia.getTitulo() != null && !evidencia.getTitulo().isBlank())
            ? evidencia.getTitulo()
            : "Evidencia sin título";
        JLabel title = new JLabel(evidenceTitle);
        title.putClientProperty("FlatLaf.style", "font: semibold, size: 13");
        title.setToolTipText(evidenceTitle);
        title.setMinimumSize(new Dimension(0, 0));

        String status = (evidencia.getNombreEstatus() != null && !evidencia.getNombreEstatus().isBlank())
            ? evidencia.getNombreEstatus()
            : "Pendiente";
        String dateOrPeriod = formatDueDateOrPeriod(evidencia);
        String metaText = dateOrPeriod.isEmpty() ? status : status + "  ·  " + dateOrPeriod;
        JLabel meta = new JLabel(metaText);
        meta.setForeground(UIManager.getColor("Text.secondaryForeground"));
        meta.putClientProperty("FlatLaf.style", "font: size: 12");
        meta.setMinimumSize(new Dimension(0, 0));

        textCol.add(title);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(meta);

        JButton actionButton = Ui.linkButton("Dar seguimiento", () -> openEvidence(evidencia));
        actionButton.putClientProperty("FlatLaf.style", "font: semibold, size: 12; focusWidth: 1; margin: 2,4,2,4; background: #00000000; hoverBackground: #0000000D");

        row.add(textCol, BorderLayout.CENTER);
        row.add(actionButton, BorderLayout.EAST);

        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openEvidence(evidencia);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                row.setBackground(hoverBackground);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                row.setBackground(defaultBackground);
            }
        };
        row.addMouseListener(clickHandler);
        title.addMouseListener(clickHandler);
        meta.addMouseListener(clickHandler);
        return row;
    }

    private void openEvidence(Evidencia evidencia) {
        dispose();
        if (onOpenEvidence != null) {
            onOpenEvidence.accept(evidencia);
        }
    }

    private String formatDueDateOrPeriod(Evidencia evidencia) {
        String endDate = evidencia.getFechaFinalizacion();
        if (endDate != null && !endDate.isBlank()) {
            try {
                LocalDate parsedEndDate = LocalDate.parse(endDate.trim());
                return "Vence: " + Fechas.toScreen(parsedEndDate);
            } catch (DateTimeParseException ignored) {
                // Si la fecha guardada no es válida, mostramos periodo o estatus sin bloquear el diálogo
            }
        }
        if (evidencia.getPeriodo() != null && !evidencia.getPeriodo().isBlank()) {
            return "Periodo: " + evidencia.getPeriodo();
        }
        return "";
    }

    private static final class ViewportWidthPanel extends JPanel implements Scrollable {
        ViewportWidthPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private JPanel createFooter() {
        JButton trackingButton = Ui.secondaryButton("Ir a Seguimiento", () -> {
            dispose();
            if (onGoToTracking != null) {
                onGoToTracking.run();
            }
        });
        JButton okButton = Ui.primaryButton("Entendido", () -> dispose());
        getRootPane().setDefaultButton(okButton);

        return DialogBar.footer(FlowLayout.RIGHT, trackingButton, okButton);
    }
}
