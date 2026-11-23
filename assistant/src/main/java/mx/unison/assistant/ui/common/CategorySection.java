package mx.unison.assistant.ui.common;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

// Bloque de una categoría en Plan Académico: barra de color + tabla + acciones
public class CategorySection extends JPanel {

    String categoryName;
    DefaultTableModel model;
    JTable table;

    public CategorySection(String categoryName, Color bannerColor, Runnable onEdit, Runnable onDelete) {
        this.categoryName = categoryName;

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);

        add(createBanner(categoryName, bannerColor), BorderLayout.NORTH);
        add(createTableBox(), BorderLayout.CENTER);
        add(createRowActions(onEdit, onDelete), BorderLayout.SOUTH);
    }

    private JPanel createBanner(String name, Color color) {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(color);
        banner.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel title = new JLabel(name);
        title.setForeground(Color.WHITE);
        title.putClientProperty("FlatLaf.style", "font: bold, size: 13");
        banner.add(title, BorderLayout.WEST);
        return banner;
    }

    private JPanel createTableBox() {
        // Columnas de RF-03
        String[] columns = { "Título", "Tipo de actividad", "Periodo", "Descripción" };
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowSelectionAllowed(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, Ui.tooltipCellRenderer());

        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(Color.WHITE);
        box.setBorder(Ui.lineBorder());
        box.add(table.getTableHeader(), BorderLayout.NORTH);
        box.add(table, BorderLayout.CENTER);
        return box;
    }

    private JPanel createRowActions(Runnable onEdit, Runnable onDelete) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        actions.setOpaque(false);

        JButton editButton = Ui.linkButton("Editar seleccionado", onEdit);
        JButton deleteButton = Ui.linkButton("Eliminar", onDelete);
        deleteButton.setForeground(UIManager.getColor("Status.error.foreground"));

        actions.add(editButton);
        actions.add(deleteButton);
        return actions;
    }

    // Agrega un registro con las columnas de RF-03
    public void addRow(String title, String type, String period, String description) {
        model.addRow(new Object[] { title, type, period, description });
    }

    public int getSelectedRow() {
        return table.getSelectedRow();
    }

    public DefaultTableModel getModel() {
        return model;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
