package mx.unison.assistant.ui.common;

import java.awt.*;
import javax.swing.*;

public class StatusBadge extends JLabel {

    Color pillBackground;

    public StatusBadge(String text, Color background, Color foreground) {
        super(text);
        this.pillBackground = background;

        setForeground(foreground);
        setOpaque(false);
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        putClientProperty("FlatLaf.style", "font: semibold, size: 12");
    }

    public static StatusBadge forStatus(String status) {
        if ("Entregado".equalsIgnoreCase(status)) {
            return new StatusBadge(
                status,
                UIManager.getColor("Status.delivered.background"),
                UIManager.getColor("Status.delivered.foreground")
            );
        }
        if ("Pendiente".equalsIgnoreCase(status)) {
            return new StatusBadge(
                status,
                UIManager.getColor("Status.pending.background"),
                UIManager.getColor("Status.pending.foreground")
            );
        }
        return new StatusBadge(
            status,
            UIManager.getColor("Status.inProgress.background"),
            UIManager.getColor("Status.inProgress.foreground")
        );
    }

    public static StatusBadge forFiles(int count) {
        if (count > 0) {
            return new StatusBadge(
                "Sí (" + count + ")",
                UIManager.getColor("Status.delivered.background"),
                UIManager.getColor("Status.delivered.foreground")
            );
        }
        return new StatusBadge(
            "Sin archivos",
            UIManager.getColor("Status.inProgress.background"),
            UIManager.getColor("Status.inProgress.foreground")
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(pillBackground);
        int arc = getHeight();
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
