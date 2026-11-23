package mx.unison.assistant.ui.common;

import java.awt.*;
import javax.swing.*;

// Caja gris de solo lectura para mostrar texto con ajuste automático de líneas
public class InfoBox extends JPanel {

    JTextArea textArea = new JTextArea();

    public InfoBox(String titleText) {
        setLayout(new BorderLayout(0, 6));
        setOpaque(false);

        JLabel title = new JLabel(titleText);
        title.putClientProperty("FlatLaf.style", "font: semibold, size: 13");

        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(UIManager.getColor("Panel.subtleBackground"));
        textArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setBorder(Ui.lineBorder());

        add(title, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    public void setText(String text) {
        textArea.setText(text);
        textArea.setCaretPosition(0);
    }
}
