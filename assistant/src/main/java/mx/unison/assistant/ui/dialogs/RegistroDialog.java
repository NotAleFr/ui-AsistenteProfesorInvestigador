package mx.unison.assistant.ui.dialogs;

import java.awt.*;
import javax.swing.*;

import mx.unison.assistant.ui.common.DialogBar;
import mx.unison.assistant.ui.common.InfoBox;
import mx.unison.assistant.ui.common.SampleData;
import mx.unison.assistant.ui.common.Ui;

// Crea o edita un registro del plan académico (RF-04, RF-05)
public class RegistroDialog extends JDialog {

    JTextField titleField = new JTextField();
    JComboBox<String> periodCombo = new JComboBox<>(SampleData.getAllPeriods());
    JComboBox<String> categoryCombo = new JComboBox<>(SampleData.getCategories());
    JComboBox<String> typeCombo = new JComboBox<>();
    JTextArea descriptionArea = new JTextArea(7, 24);
    InfoBox infoBox = new InfoBox("Definición y acreditación (PEDPD)");

    boolean saved = false;

    public RegistroDialog(Window owner) {
        super(owner, "Creación de registro", ModalityType.APPLICATION_MODAL);

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
        String category = (String) categoryCombo.getSelectedItem();
        typeCombo.removeAllItems();
        if (category != null) {
            for (String type : SampleData.getTypes(category)) {
                typeCombo.addItem(type);
            }
        }
        updateInfoText();
    }

    private void updateInfoText() {
        String type = (String) typeCombo.getSelectedItem();
        if (type == null) {
            infoBox.setText("");
            return;
        }
        infoBox.setText(Ui.typeInfoText(SampleData.getDefinition(type), SampleData.getAccreditation(type)));
    }

    public void setDefaultPeriod(String period) {
        if (period != null && !period.isEmpty()) {
            periodCombo.setSelectedItem(period);
        }
    }

    public void fill(String title, String category, String type, String period, String description) {
        setTitle("Editar registro");
        titleField.setText(title);
        // Primero la categoría, que recarga los tipos, y luego el tipo
        categoryCombo.setSelectedItem(category);
        typeCombo.setSelectedItem(type);
        periodCombo.setSelectedItem(period);
        descriptionArea.setText(description);
        updateInfoText();
    }

    public boolean isSaved() {
        return saved;
    }

    // [0]=titulo, [1]=categoria, [2]=tipo, [3]=periodo, [4]=descripcion
    public String[] getEvidence() {
        return new String[] {
            titleField.getText().trim(),
            (String) categoryCombo.getSelectedItem(),
            (String) typeCombo.getSelectedItem(),
            (String) periodCombo.getSelectedItem(),
            descriptionArea.getText().trim()
        };
    }
}
