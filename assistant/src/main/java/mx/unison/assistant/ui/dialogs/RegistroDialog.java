package mx.unison.assistant.ui.dialogs;
import mx.unison.assistant.model.Categoria;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Periodo;
import mx.unison.assistant.model.TipoEvidencia;
import mx.unison.assistant.service.AppServices;

import java.awt.*;
import java.sql.SQLException;

import javax.swing.*;

import mx.unison.assistant.ui.common.DialogBar;
import mx.unison.assistant.ui.common.InfoBox;
import mx.unison.assistant.ui.common.Ui;

// Crea o edita un registro del plan académico (RF-04, RF-05)
public class RegistroDialog extends JDialog {
    final AppServices services;

    JTextField titleField = new JTextField();
    JComboBox<String> periodCombo = new JComboBox<>();
    JComboBox<Categoria> categoryCombo = new JComboBox<>();
    JComboBox<TipoEvidencia> typeCombo = new JComboBox<>();
    JTextArea descriptionArea = new JTextArea(7, 24);
    InfoBox infoBox = new InfoBox("Definición y acreditación (PEDPD)");

    boolean saved = false;
    int editingIdEvidencia = 0;
    int editingIdPlanAcademico = 0;

    public RegistroDialog(Window owner, AppServices services) {
        super(owner, "Creación de registro", ModalityType.APPLICATION_MODAL);

        this.services = services;
        for (String p : Periodo.recentPeriods()) {
            periodCombo.addItem(p);
        }
        try {
            for (Categoria c : services.catalogo.getCategories()) {
                categoryCombo.addItem(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las categorías.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        setLayout(new BorderLayout());
        setSize(860, 520);
        setMinimumSize(new Dimension(760, 460));
        setLocationRelativeTo(owner);

        titleField.putClientProperty("JTextField.placeholderText", "Ej. Evidencia #10");

        // Los tipos dependen de la categoría y el cuadro PEDPD depende del tipo (RF-04)
        categoryCombo.addActionListener(e -> loadTypes());
        typeCombo.addActionListener(e -> updateInfoText());
        loadTypes();

        add(DialogBar.header("Creación de registro", () -> dispose()), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        add(DialogBar.footer(
            FlowLayout.LEFT,
            Ui.primaryButton("+  Guardar Registro", () -> onSave())
        ), BorderLayout.SOUTH);
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(8, 24, 16, 24));

        JPanel topRow = new JPanel(new GridLayout(1, 3, 16, 0));
        topRow.setOpaque(false);
        topRow.add(Ui.field("Título", titleField));
        topRow.add(Ui.field("Periodo", periodCombo));
        topRow.add(Ui.field("Categoría", categoryCombo));

        JPanel leftSide = new JPanel(new BorderLayout(0, 14));
        leftSide.setOpaque(false);
        leftSide.add(Ui.field("Tipo de Actividad", typeCombo), BorderLayout.NORTH);
        leftSide.add(Ui.noteBox("Descripción", descriptionArea), BorderLayout.CENTER);

        JPanel split = new JPanel(new GridLayout(1, 2, 20, 0));
        split.setOpaque(false);
        split.add(leftSide);
        split.add(infoBox);

        body.add(topRow, BorderLayout.NORTH);
        body.add(split, BorderLayout.CENTER);
        return body;
    }

    private void onSave() {
        if (titleField.getText().trim().isEmpty()) {
            Ui.info(this, "Aviso", "El título del registro es obligatorio.");
            return;
        }
        if (typeCombo.getSelectedItem() == null) {
            Ui.info(this, "Aviso", "Selecciona un tipo de actividad.");
            return;
        }
        saved = true;
        dispose();
    }

    // Recarga los tipos de actividad de la categoría seleccionada
    private void loadTypes() {
        Categoria category = (Categoria) categoryCombo.getSelectedItem();
        typeCombo.removeAllItems();
        if (category != null) {
            try {
                for (TipoEvidencia type : services.catalogo.getTypes(category.getIdCategoria())) {
                    typeCombo.addItem(type);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "No se pudieron cargar los tipos de actividad.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        updateInfoText();
    }

    private void updateInfoText() {
        TipoEvidencia type = (TipoEvidencia) typeCombo.getSelectedItem();
        if (type == null) {
            infoBox.setText("");
            return;
        }
        infoBox.setText(Ui.typeInfoText(type.getDefinicionTipoEvidencia(), type.getContenidoAcreditacion()));
    }

    public void setDefaultPeriod(String period) {
        if (period != null && !period.isEmpty()) {
            periodCombo.setSelectedItem(period);
        }
    }

    public void fill(Evidencia ev) {
        setTitle("Editar registro");
        editingIdEvidencia = ev.getIdEvidencia();
        editingIdPlanAcademico = ev.getIdPlanAcademico();
        titleField.setText(ev.getTitulo());
        periodCombo.setSelectedItem(ev.getPeriodo());
        descriptionArea.setText(ev.getDescripcion() != null ? ev.getDescripcion() : "");

        for (int i = 0; i < categoryCombo.getItemCount(); i++) {
            Categoria c = categoryCombo.getItemAt(i);
            if (c.getIdCategoria() == ev.getIdCategoria()) {
                categoryCombo.setSelectedIndex(i);
                break;
            }
        }
        loadTypes();
        for (int i = 0; i < typeCombo.getItemCount(); i++) {
            TipoEvidencia t = typeCombo.getItemAt(i);
            if (t.getIdTipoEvidencia() == ev.getIdTipoEvidencia()) {
                typeCombo.setSelectedIndex(i);
                break;
            }
        }
        updateInfoText();
    }

    public boolean isSaved() {
        return saved;
    }

    // [0]=titulo, [1]=categoria, [2]=tipo, [3]=periodo, [4]=descripcion
    public Evidencia getEvidence(int idPlanAcademico) {
        TipoEvidencia selectedType = (TipoEvidencia) typeCombo.getSelectedItem();
        Categoria selectedCategory = (Categoria) categoryCombo.getSelectedItem();

        Evidencia ev = new Evidencia();
        ev.setIdEvidencia(editingIdEvidencia);
        ev.setIdPlanAcademico(editingIdPlanAcademico > 0 ? editingIdPlanAcademico : idPlanAcademico);
        ev.setTitulo(titleField.getText().trim());
        ev.setDescripcion(descriptionArea.getText().trim());
        ev.setPeriodo((String) periodCombo.getSelectedItem());
        if (selectedType != null) {
            ev.setIdTipoEvidencia(selectedType.getIdTipoEvidencia());
            ev.setNombreTipoEvidencia(selectedType.getNombreTipoEvidencia());
        }
        if (selectedCategory != null) {
            ev.setIdCategoria(selectedCategory.getIdCategoria());
            ev.setNombreCategoria(selectedCategory.getNombreCategoria());
        }
        return ev;
    }
}
