package mx.unison.assistant.ui.common;

import java.awt.*;
import javax.swing.*;

// Barra superior e inferior reutilizable para los diálogos
public class DialogBar {

    public static JPanel header(String titleText, Runnable onCancel) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        JLabel title = new JLabel(titleText);
        title.putClientProperty("FlatLaf.styleClass", "h1");

        header.add(title, BorderLayout.WEST);
        header.add(Ui.secondaryButton("X  Cancelar", onCancel), BorderLayout.EAST);
        return header;
    }

    public static JPanel footer(int alignment, JButton... buttons) {
        JPanel footer = new JPanel(new FlowLayout(alignment, 12, 14));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Panel.borderColor")),
            BorderFactory.createEmptyBorder(0, 12, 0, 12)
        ));

        for (JButton button : buttons) {
            footer.add(button);
        }
        return footer;
    }
}
