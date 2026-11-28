package mx.unison.assistant.ui.views;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import mx.unison.assistant.model.Categoria;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.PlanAcademico;
import mx.unison.assistant.service.AppServices;
import mx.unison.assistant.ui.common.CategorySection;
import mx.unison.assistant.ui.common.PageHeader;
import mx.unison.assistant.ui.common.StatusBadge;
import mx.unison.assistant.ui.common.Ui;
import mx.unison.assistant.ui.dialogs.PlanDialog;
import mx.unison.assistant.ui.dialogs.RegistroDialog;

public class PlanAcademicoView extends JPanel {

    final AppServices services;

    JComboBox<PlanAcademico> planCombo = new JComboBox<>();
    JLabel periodValue = new JLabel("-");
    JPanel planStatusSlot = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
    JButton addRecordButton;
    JButton activatePlanButton;
    JButton saveDraftButton;
    JButton deletePlanButton;
    Map<Integer, CategorySection> sections = new LinkedHashMap<>();
    Map<Integer, Categoria> categoriesById = new LinkedHashMap<>();
    Map<Integer, List<Evidencia>> recordsByCategory = new LinkedHashMap<>();
    PlanAcademico currentPlan;
    boolean populatingPlans = false;
    Runnable onDataChanged;

    public PlanAcademicoView(AppServices services) {
        this.services = services;
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

    public void reload() {
        loadPlans();
    }

    public void selectPlanById(int idPlanAcademico) {
        for (int i = 0; i < planCombo.getItemCount(); i++) {
            if (planCombo.getItemAt(i).getIdPlanAcademico() == idPlanAcademico) {
                planCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Panel.borderColor")),
            BorderFactory.createEmptyBorder(0, 0, 16, 0)
        ));

        planCombo.setPreferredSize(new Dimension(190, 34));
        planCombo.addActionListener(e -> {
            if (!populatingPlans) {
                onSelectPlan();
            }
        });
        periodValue.putClientProperty("FlatLaf.style", "font: semibold, size: 14");

        planStatusSlot.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(Ui.field("Elige un plan", planCombo));
        left.add(Box.createHorizontalStrut(32));
        left.add(Ui.field("Periodo", periodValue));
        left.add(Box.createHorizontalStrut(32));
        left.add(Ui.field("Estado", planStatusSlot));

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

        try {
            List<Categoria> categories = services.catalogo.getCategories();
            for (int i = 0; i < categories.size(); i++) {
                Categoria cat = categories.get(i);
                categoriesById.put(cat.getIdCategoria(), cat);
                recordsByCategory.put(cat.getIdCategoria(), new ArrayList<>());
                addSection(list, cat, Ui.categoryColor(i));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showDbError("No se pudieron cargar las categorías.");
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

    private void addSection(JPanel parent, Categoria category, Color color) {
        int idCategoria = category.getIdCategoria();
        CategorySection section = new CategorySection(
            category.getNombreCategoria(),
            color,
            () -> onEditRecord(idCategoria),
            () -> onDeleteRecord(idCategoria)
        );
        sections.put(idCategoria, section);
        parent.add(section);
        parent.add(Box.createVerticalStrut(18));
    }

    private JPanel createBottomActions() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Panel.borderColor")));

        activatePlanButton = Ui.primaryButton("Guardar / Dar de alta", () -> onActivatePlan());
        saveDraftButton = Ui.successButton("Guardar borrador", () -> onSave());
        deletePlanButton = Ui.dangerButton("Eliminar Plan", () -> onDeletePlan());

        footer.add(activatePlanButton);
        footer.add(saveDraftButton);
        footer.add(deletePlanButton);
        return footer;
    }

    private void loadPlans() {
        PlanAcademico previousSelected = (PlanAcademico) planCombo.getSelectedItem();
        populatingPlans = true;
        try {
            planCombo.removeAllItems();
            try {
                List<PlanAcademico> plans = services.planes.listPlans();
                for (PlanAcademico plan : plans) {
                    planCombo.addItem(plan);
                }
                if (previousSelected != null) {
                    for (int i = 0; i < planCombo.getItemCount(); i++) {
                        if (planCombo.getItemAt(i).getIdPlanAcademico() == previousSelected.getIdPlanAcademico()) {
                            planCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showDbError("No se pudieron cargar los planes académicos.");
            }
        } finally {
            populatingPlans = false;
        }

        onSelectPlan();
    }

    private void loadRecords(int idPlanAcademico) {
        clearAllTables();
        try {
            List<Evidencia> records = services.planes.getRecords(idPlanAcademico);
            for (Evidencia ev : records) {
                int idCat = ev.getIdCategoria();
                CategorySection section = sections.get(idCat);
                List<Evidencia> catRecords = recordsByCategory.get(idCat);
                if (section != null && catRecords != null) {
                    catRecords.add(ev);
                    String typeName = ev.getNombreTipoEvidencia() != null ? ev.getNombreTipoEvidencia() : "";
                    String desc = ev.getDescripcion() != null ? ev.getDescripcion() : "";
                    section.addRow(ev.getTitulo(), typeName, ev.getPeriodo(), desc);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showDbError("No se pudieron cargar los registros del plan.");
        }
    }

    private void clearAllTables() {
        for (List<Evidencia> list : recordsByCategory.values()) {
            list.clear();
        }
        for (CategorySection section : sections.values()) {
            section.getModel().setRowCount(0);
        }
    }

    public void onCreatePlan() {
        PlanDialog dialog = new PlanDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            String[] planData = dialog.getPlan();
            String name = planData[0];
            String period = planData[2];
            int year;
            try {
                year = Integer.parseInt(planData[1].trim());
            } catch (NumberFormatException e) {
                Ui.info(this, "Aviso", "El año debe ser un número entero válido.");
                return;
            }

            try {
                PlanAcademico created = services.planes.createPlan(name, year, period);
                populatingPlans = true;
                try {
                    planCombo.addItem(created);
                    planCombo.setSelectedItem(created);
                } finally {
                    populatingPlans = false;
                }
                onSelectPlan();
                if (onDataChanged != null) {
                    onDataChanged.run();
                }
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException e) {
                e.printStackTrace();
                showDbError("No se pudo crear el plan académico.");
            }
        }
    }

    public void onSelectPlan() {
        Object selected = planCombo.getSelectedItem();
        if (selected instanceof PlanAcademico) {
            currentPlan = (PlanAcademico) selected;
            periodValue.setText(currentPlan.getPeriodoPlan());
            updatePlanStateUi();
            loadRecords(currentPlan.getIdPlanAcademico());
        } else {
            currentPlan = null;
            periodValue.setText("-");
            updatePlanStateUi();
            clearAllTables();
        }
    }

    private void updatePlanStateUi() {
        boolean hasPlan = currentPlan != null;
        addRecordButton.setEnabled(hasPlan);
        activatePlanButton.setEnabled(hasPlan && !currentPlan.isActivo());
        saveDraftButton.setEnabled(hasPlan);
        deletePlanButton.setEnabled(hasPlan);

        planStatusSlot.removeAll();
        if (hasPlan) {
            planStatusSlot.add(StatusBadge.forPlanState(currentPlan.isActivo()));
        } else {
            JLabel emptyStatus = new JLabel("-");
            emptyStatus.putClientProperty("FlatLaf.style", "font: semibold, size: 14");
            planStatusSlot.add(emptyStatus);
        }
        planStatusSlot.revalidate();
        planStatusSlot.repaint();
    }

    public void onAddRecord() {
        if (currentPlan == null) {
            Ui.info(this, "Aviso", "Primero crea o selecciona un plan académico.");
            return;
        }

        RegistroDialog dialog = new RegistroDialog(SwingUtilities.getWindowAncestor(this), services);
        dialog.setDefaultPeriod(currentPlan.getPeriodoPlan());
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            try {
                Evidencia draft = dialog.getEvidence(currentPlan.getIdPlanAcademico());
                services.planes.addRecord(draft);
                loadRecords(currentPlan.getIdPlanAcademico());
                if (onDataChanged != null) {
                    onDataChanged.run();
                }
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException e) {
                e.printStackTrace();
                showDbError("No se pudo guardar el registro.");
            }
        }
    }

    public void onEditRecord(int idCategoria) {
        CategorySection section = sections.get(idCategoria);
        Categoria cat = categoriesById.get(idCategoria);
        String categoryName = cat != null ? cat.getNombreCategoria() : "esta categoría";
        List<Evidencia> catRecords = recordsByCategory.get(idCategoria);

        int row = section != null ? section.getSelectedRow() : -1;
        if (row < 0 || catRecords == null || row >= catRecords.size()) {
            Ui.info(this, "Aviso", "Selecciona primero un registro de " + categoryName + ".");
            return;
        }

        Evidencia selectedRecord = catRecords.get(row);
        RegistroDialog dialog = new RegistroDialog(SwingUtilities.getWindowAncestor(this), services);
        dialog.fill(selectedRecord);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            try {
                Evidencia updated = dialog.getEvidence(currentPlan != null ? currentPlan.getIdPlanAcademico() : selectedRecord.getIdPlanAcademico());
                updated.setIdEvidencia(selectedRecord.getIdEvidencia());
                services.planes.updateRecord(updated);
                if (currentPlan != null) {
                    loadRecords(currentPlan.getIdPlanAcademico());
                }
                if (onDataChanged != null) {
                    onDataChanged.run();
                }
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException e) {
                e.printStackTrace();
                showDbError("No se pudo actualizar el registro.");
            }
        }
    }

    public void onDeleteRecord(int idCategoria) {
        CategorySection section = sections.get(idCategoria);
        List<Evidencia> catRecords = recordsByCategory.get(idCategoria);
        int row = section != null ? section.getSelectedRow() : -1;
        if (row < 0 || catRecords == null || row >= catRecords.size()) {
            Ui.info(this, "Aviso", "Selecciona el registro que deseas eliminar.");
            return;
        }

        Evidencia selectedRecord = catRecords.get(row);
        if (Ui.confirm(this, "¿Deseas eliminar el registro seleccionado?")) {
            try {
                services.planes.deleteRecord(selectedRecord.getIdEvidencia());
                if (currentPlan != null) {
                    loadRecords(currentPlan.getIdPlanAcademico());
                }
                if (onDataChanged != null) {
                    onDataChanged.run();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showDbError("No se pudo eliminar el registro.");
            }
        }
    }

    public void onSave() {
        if (currentPlan == null) {
            Ui.info(this, "Aviso", "No hay ningún plan seleccionado.");
            return;
        }
        Ui.info(this, "Plan académico", "Los registros de \"" + currentPlan.getNombrePlan() + "\" se guardan automáticamente al agregarlos o editarlos.");
    }

    public void onActivatePlan() {
        if (currentPlan == null) {
            Ui.info(this, "Aviso", "No hay ningún plan seleccionado.");
            return;
        }
        if (currentPlan.isActivo()) {
            Ui.info(this, "Plan académico", "Este plan ya está activo en Seguimiento.");
            return;
        }
        if (Ui.confirm(this, "¿Deseas dar de alta \"" + currentPlan.getNombrePlan() + "\" y pasar sus registros a Seguimiento?")) {
            try {
                services.planes.activatePlan(currentPlan.getIdPlanAcademico());
                currentPlan.setActivo(true);
                updatePlanStateUi();
                Ui.info(this, "Plan académico", "Plan dado de alta correctamente.");
                if (onDataChanged != null) {
                    onDataChanged.run();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showDbError("No se pudo dar de alta el plan.");
            }
        }
    }

    public void onDeletePlan() {
        if (currentPlan == null) {
            Ui.info(this, "Aviso", "No hay ningún plan para eliminar.");
            return;
        }
        if (Ui.confirm(this, "¿Deseas eliminar \"" + currentPlan.getNombrePlan() + "\" junto con todos sus registros?")) {
            try {
                services.planes.deletePlan(currentPlan.getIdPlanAcademico());
                loadPlans();
                if (onDataChanged != null) {
                    onDataChanged.run();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showDbError("No se pudo eliminar el plan.");
            }
        }
    }

    private void showDbError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}