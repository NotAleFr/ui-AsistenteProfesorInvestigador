package mx.unison.assistant.ui.common;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

// Helpers chicos para no repetir tanto código de Swing en cada vista y diálogo
public class Ui {

    // Cantidad de colores de categoría en FlatLaf.properties, de Category.color1 a Category.color5
    static final int CATEGORY_COLOR_COUNT = 5;

    // Color de la barra de una categoría según su posición en el catálogo. La posición 0 es la primera categoría
    public static Color categoryColor(int index) {
        int number = (index % CATEGORY_COLOR_COUNT) + 1;
        return UIManager.getColor("Category.color" + number);
    }

    // Texto del cuadro PEDPD con la definición y la acreditación. No imprime las partes vacías
    public static String typeInfoText(String definition, String accreditation) {
        String text = "";
        if (definition != null && !definition.isBlank()) {
            text = definition + "\n\n";
        }
        if (accreditation != null && !accreditation.isBlank()) {
            text = text + "Acreditación:\n" + accreditation;
        }
        if (text.isEmpty()) {
            text = "Sin información de definición o acreditación para este tipo de actividad.";
        }
        return text;
    }

    public static void applyViewStyle(JPanel panel) {
        Insets padding = UIManager.getInsets("View.padding");
        panel.setBackground(UIManager.getColor("Panel.background"));
        panel.setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));
    }

    public static Border lineBorder() {
        return BorderFactory.createLineBorder(UIManager.getColor("Panel.borderColor"));
    }

    public static JButton button(String text, String styleClass, Runnable onClick) {
        JButton btn = new JButton(text);
        if (styleClass != null) {
            btn.putClientProperty("FlatLaf.styleClass", styleClass);
        }
        if (onClick != null) {
            btn.addActionListener(e -> onClick.run());
        }
        return btn;
    }

    public static JButton primaryButton(String text, Runnable onClick) {
        return button(text, "primary", onClick);
    }

    public static JButton secondaryButton(String text, Runnable onClick) {
        return button(text, "secondary", onClick);
    }

    public static JButton successButton(String text, Runnable onClick) {
        return button(text, "success", onClick);
    }

    public static JButton dangerButton(String text, Runnable onClick) {
        return button(text, "danger", onClick);
    }

    public static JButton linkButton(String text, Runnable onClick) {
        JButton btn = button(text, "link", onClick);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // Etiqueta arriba + campo abajo (estándar de 34px de alto)
    public static JPanel field(String labelText, JComponent input) {
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setForeground(UIManager.getColor("Text.secondaryForeground"));
        label.putClientProperty("FlatLaf.style", "font: size: 12");

        if (!(input instanceof JLabel)) {
            input.setPreferredSize(new Dimension(input.getPreferredSize().width, 34));
        }
        box.add(label, BorderLayout.NORTH);
        box.add(input, BorderLayout.CENTER);
        return box;
    }

    // Cuadro amarillo de descripción o notas
    public static JPanel noteBox(String labelText, JTextArea area) {
        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.putClientProperty("FlatLaf.style", "font: semibold, size: 13");

        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(UIManager.getColor("Note.background"));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(lineBorder());

        box.add(label, BorderLayout.NORTH);
        box.add(scroll, BorderLayout.CENTER);
        return box;
    }

    // Renderer simple que agrega tooltip a las celdas por si el texto largo se recorta
    public static DefaultTableCellRenderer tooltipCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setToolTipText(value == null ? null : value.toString());
                return label;
            }
        };
    }

    // Confirmación segura: "Cancelar" es el botón por defecto (RNF-SEG-01)
    public static boolean confirm(Component parent, String message) {
        String[] options = { "Sí", "Cancelar" };
        int choice = JOptionPane.showOptionDialog(
            parent,
            message,
            "¡Aviso!",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[1]
        );
        return choice == 0;
    }

    public static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
