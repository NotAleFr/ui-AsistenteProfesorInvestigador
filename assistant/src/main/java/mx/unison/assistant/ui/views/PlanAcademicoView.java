package mx.unison.assistant.ui.views;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import mx.unison.assistant.ui.common.CategorySection;
import mx.unison.assistant.ui.common.PageHeader;
import mx.unison.assistant.ui.common.SampleData;
import mx.unison.assistant.ui.common.Ui;
import mx.unison.assistant.ui.dialogs.PlanDialog;
import mx.unison.assistant.ui.dialogs.RegistroDialog;

public class PlanAcademicoView extends JPanel {

    JComboBox<String> planCombo = new JComboBox<>();
    JLabel periodValue = new JLabel("-");
    JButton addRecordButton;
    Map<String, String> planPeriods = new LinkedHashMap<>();
    Map<String, CategorySection> sections = new LinkedHashMap<>();
    String currentPlan;
    Runnable onDataChanged;

    public PlanAcademicoView() {
        setLayout(new BorderLayout());
        Ui.applyViewStyle(this);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(new PageHeader("Plan académico"), BorderLayout.NORTH);
        top.add(createToolbar(), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(createCategoriesScroll(), BorderLayout.CENTER);
        add(createBottomActions(), BorderLayout.SOUTH);

        loadPlans();
    }

    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Panel.borderColor")),
            BorderFactory.createEmptyBorder(0, 0, 16, 0)
        ));

        planCombo.setPreferredSize(new Dimension(190, 34));
        planCombo.addActionListener(e -> onSelectPlan());
        periodValue.putClientProperty("FlatLaf.style", "font: semibold, size: 14");

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(Ui.field("Elige un plan", planCombo));
        left.add(Box.createHorizontalStrut(32));
        left.add(Ui.field("Periodo", periodValue));

        addRecordButton = Ui.primaryButton("Añadir Registro", () -> onAddRecord());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        right.setOpaque(false);
        right.add(Ui.primaryButton("Crear Plan", () -> onCreatePlan()));
        right.add(addRecordButton);

        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    // Una sección (tabla) por cada categoría del catálogo (RF-02)
    private JScrollPane createCategoriesScroll() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        list.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 8));

        String[] categories = SampleData.getCategories();
        for (int i = 0; i < categories.length; i++) {
            addSection(list, categories[i], Ui.categoryColor(i));
        }

        // Se mete en NORTH para que las tablas no se estiren con espacio blanco vacío
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(list, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private void addSection(JPanel parent, String name, Color color) {
        CategorySection section = new CategorySection(
            name,
            color,
            () -> onEditRecord(name),
            () -> onDeleteRecord(name)
        );
        sections.put(name, section);
        parent.add(section);
        parent.add(Box.createVerticalStrut(18));
    }

    private JPanel createBottomActions() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Panel.borderColor")));

        footer.add(Ui.primaryButton("Guardar / Dar de alta", () -> onActivatePlan()));
        footer.add(Ui.successButton("Guardar borrador", () -> onSave()));
        footer.add(Ui.dangerButton("Eliminar Plan", () -> onDeletePlan()));
        return footer;
    }

    // Llena el combo de planes. Conectar: PlanService.listPlans()
    private void loadPlans() {
        planPeriods.clear();
        planCombo.removeAllItems();
        for (String[] plan : SampleData.listPlans()) {
            planPeriods.put(plan[0], plan[1]);
            planCombo.addItem(plan[0]);
        }
        onSelectPlan();
    }

    // Muestra los registros del plan en la tabla de su categoría. Conectar: PlanService.getRecords(plan)
    private void loadRecords(String planName) {
        clearAllTables();
        List<String[]> records = SampleData.getRecords(planName);
        for (String[] record : records) {
            // record = { título, categoría, tipo, periodo, descripción }
            CategorySection section = sections.get(record[1]);
            if (section != null) {
                section.addRow(record[0], record[2], record[3], record[4]);
            }
        }
    }

    private void clearAllTables() {
        for (CategorySection section : sections.values()) {
            section.getModel().setRowCount(0);
        }
    }

    public void onCreatePlan() {
        PlanDialog dialog = new PlanDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            String[] plan = dialog.getPlan();
            String name = plan[0];
            String period = plan[2];

            if (planPeriods.containsKey(name)) {
                Ui.info(this, "Aviso", "Ya existe un plan con el nombre \"" + name + "\".");
                return;
            }

            // Conectar: PlanService.createPlan(...)
            planPeriods.put(name, period);
            planCombo.addItem(name);
            planCombo.setSelectedItem(name);
        }
    }

    public void onSelectPlan() {
        Object selected = planCombo.getSelectedItem();
        if (selected != null) {
            currentPlan = selected.toString();
            periodValue.setText(planPeriods.get(currentPlan));
            addRecordButton.setEnabled(true);
            loadRecords(currentPlan);
        } else {
            currentPlan = null;
            periodValue.setText("-");
            addRecordButton.setEnabled(false);
            clearAllTables();
        }
    }

    public void onAddRecord() {
        if (currentPlan == null) {
            Ui.info(this, "Aviso", "Primero crea o selecciona un plan académico.");
            return;
        }

        RegistroDialog dialog = new RegistroDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setDefaultPeriod(periodValue.getText());
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            // Conectar: PlanService.addRecord(...)
            String[] data = dialog.getEvidence();
            CategorySection target = sections.get(data[1]);
            if (target != null) {
                target.addRow(data[0], data[2], data[3], data[4]);
            }
        }
    }

    public void onEditRecord(String categoryName) {
        CategorySection section = sections.get(categoryName);
        int row = section.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Aviso", "Selecciona primero un registro de " + categoryName + ".");
            return;
        }

        // Columnas: 0=título, 1=tipo, 2=periodo, 3=descripción
        DefaultTableModel model = section.getModel();

        RegistroDialog dialog = new RegistroDialog(SwingUtilities.getWindowAncestor(this));
        dialog.fill(
            String.valueOf(model.getValueAt(row, 0)),
            categoryName,
            String.valueOf(model.getValueAt(row, 1)),
            String.valueOf(model.getValueAt(row, 2)),
            String.valueOf(model.getValueAt(row, 3))
        );
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            // Conectar: PlanService.updateRecord(...)
            String[] data = dialog.getEvidence();
            String newCategory = data[1];
            if (newCategory.equals(categoryName)) {
                model.setValueAt(data[0], row, 0);
                model.setValueAt(data[2], row, 1);
                model.setValueAt(data[3], row, 2);
                model.setValueAt(data[4], row, 3);
            } else {
                // Si cambió de categoría, lo movemos a la tabla nueva
                model.removeRow(row);
                CategorySection target = sections.get(newCategory);
                if (target != null) {
                    target.addRow(data[0], data[2], data[3], data[4]);
                }
            }
        }
    }

    public void onDeleteRecord(String categoryName) {
        CategorySection section = sections.get(categoryName);
        int row = section.getSelectedRow();
        if (row < 0) {
            Ui.info(this, "Aviso", "Selecciona el registro que deseas eliminar.");
            return;
        }
        if (Ui.confirm(this, "¿Deseas eliminar el registro seleccionado?")) {
            // Conectar: PlanService.deleteRecord(...)
            section.getModel().removeRow(row);
        }
    }

    public void onSave() {
        if (currentPlan == null) {
            Ui.info(this, "Aviso", "No hay ningún plan seleccionado.");
            return;
        }
        // Conectar: PlanService.savePlan(...)
        Ui.info(this, "Plan académico", "Borrador de \"" + currentPlan + "\" guardado.");
    }

    public void onActivatePlan() {
        if (currentPlan == null) {
            Ui.info(this, "Aviso", "No hay ningún plan seleccionado.");
            return;
        }
        if (Ui.confirm(this, "¿Deseas dar de alta \"" + currentPlan + "\" y pasar sus registros a Seguimiento?")) {
            // Conectar: PlanService.activatePlan(...)
            Ui.info(this, "Plan académico", "Plan dado de alta correctamente.");
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        }
    }

    public void onDeletePlan() {
        if (currentPlan == null) {
            Ui.info(this, "Aviso", "No hay ningún plan para eliminar.");
            return;
        }
        if (Ui.confirm(this, "¿Deseas eliminar \"" + currentPlan + "\" junto con todos sus registros?")) {
            // Conectar: PlanService.deletePlan(...)
            String deleted = currentPlan;
            planPeriods.remove(deleted);
            planCombo.removeItem(deleted);
            onSelectPlan();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        }
    }
}
