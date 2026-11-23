package mx.unison.assistant.ui.dialogs;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import mx.unison.assistant.ui.common.DialogBar;
import mx.unison.assistant.ui.common.InfoBox;
import mx.unison.assistant.ui.common.SampleData;
import mx.unison.assistant.ui.common.Ui;

// Seguimiento de una evidencia con los campos de RF-04 y de la Tabla 3 de RF-13
public class SeguimientoDialog extends JDialog {

    JTextField titleField = new JTextField();
    JComboBox<String> periodCombo = new JComboBox<>(SampleData.getAllPeriods());
    JComboBox<String> statusCombo = new JComboBox<>(SampleData.getStatuses());
    JComboBox<String> categoryCombo = new JComboBox<>(SampleData.getCategories());
    JComboBox<String> accreditationCombo = new JComboBox<>(SampleData.getAccreditations());
    JComboBox<String> typeCombo = new JComboBox<>();
    JComboBox<String> optionCombo = new JComboBox<>();
    JTextField startDateField = new JTextField();
    JTextField dueDateField = new JTextField();

    JPanel documentsPanel = new JPanel();
    InfoBox infoBox = new InfoBox("Acreditación (PEDPD)");

    List<String> documentNames = new ArrayList<>();
    List<File> newFiles = new ArrayList<>();
    List<String> removedDocuments = new ArrayList<>();
    boolean saved = false;

    public SeguimientoDialog(Window owner) {
        super(owner, "Seguimiento de evidencia", ModalityType.APPLICATION_MODAL);

        setLayout(new BorderLayout());
        setSize(980, 600);
        setMinimumSize(new Dimension(880, 540));
        setLocationRelativeTo(owner);

        // La categoría recarga los tipos. El tipo recarga las opciones y el cuadro PEDPD (RF-04, RF-13)
        categoryCombo.addActionListener(e -> loadTypes());
        typeCombo.addActionListener(e -> onTypeChanged());
        loadTypes();

        add(DialogBar.header("Seguimiento de evidencia", () -> dispose()), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        add(DialogBar.footer(
            FlowLayout.LEFT,
            Ui.primaryButton("+  Guardar seguimiento", () -> onSave()),
            Ui.secondaryButton("Adjuntar archivo", () -> onAttachDocuments())
        ), BorderLayout.SOUTH);

        refreshDocumentsList();
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(4, 24, 16, 24));

        // Tres filas arriba para que los combos largos no se corten
        JPanel topFields = new JPanel(new GridLayout(3, 1, 0, 10));
        topFields.setOpaque(false);

        JPanel row1 = new JPanel(new GridLayout(1, 4, 12, 0));
        row1.setOpaque(false);
        row1.add(Ui.field("Título", titleField));
        row1.add(Ui.field("Periodo", periodCombo));
        row1.add(Ui.field("Estatus", statusCombo));
        row1.add(Ui.field("Categoría", categoryCombo));

        JPanel row2 = new JPanel(new GridLayout(1, 2, 12, 0));
        row2.setOpaque(false);
        row2.add(Ui.field("Tipo de Actividad", typeCombo));
        row2.add(Ui.field("Opción", optionCombo));

        JPanel row3 = new JPanel(new GridLayout(1, 3, 12, 0));
        row3.setOpaque(false);
        row3.add(Ui.field("Acreditación", accreditationCombo));
        row3.add(Ui.field("Fecha de inicio", startDateField));
        row3.add(Ui.field("Fecha de finalización", dueDateField));

        topFields.add(row1);
        topFields.add(row2);
        topFields.add(row3);

        // Dos columnas abajo, los documentos y la acreditación PEDPD
        JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0));
        columns.setOpaque(false);
        columns.add(createDocumentsBox());
        columns.add(infoBox);

        body.add(topFields, BorderLayout.NORTH);
        body.add(columns, BorderLayout.CENTER);
        return body;
    }

    private JPanel createDocumentsBox() {
        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setOpaque(false);

        JLabel title = new JLabel("Documentos adjuntos");
        title.putClientProperty("FlatLaf.style", "font: semibold, size: 13");

        documentsPanel.setLayout(new BoxLayout(documentsPanel, BoxLayout.Y_AXIS));
        documentsPanel.setBackground(Color.WHITE);
        documentsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.add(documentsPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(Ui.lineBorder());

        box.add(title, BorderLayout.NORTH);
        box.add(scroll, BorderLayout.CENTER);
        return box;
    }

    private void refreshDocumentsList() {
        documentsPanel.removeAll();
        if (documentNames.isEmpty()) {
            JLabel empty = new JLabel("Sin archivos adjuntos");
            empty.setForeground(UIManager.getColor("Text.tertiaryForeground"));
            documentsPanel.add(empty);
        } else {
            for (int i = 0; i < documentNames.size(); i++) {
                int index = i;
                documentsPanel.add(createDocumentPill(documentNames.get(i), index));
                documentsPanel.add(Box.createVerticalStrut(8));
            }
        }
        documentsPanel.revalidate();
        documentsPanel.repaint();
    }

    private JPanel createDocumentPill(String docName, int index) {
        JPanel pill = new JPanel(new BorderLayout(8, 0));
        pill.setBackground(UIManager.getColor("Note.background"));
        pill.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 6));
        pill.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JButton removeButton = Ui.button("X", "pillClose", () -> onRemoveDocument(index));

        pill.add(new JLabel(docName), BorderLayout.CENTER);
        pill.add(removeButton, BorderLayout.EAST);
        return pill;
    }

    private void onSave() {
        if (titleField.getText().trim().isEmpty()) {
            Ui.info(this, "Aviso", "El título de la evidencia es obligatorio.");
            return;
        }
        if (typeCombo.getSelectedItem() == null) {
            Ui.info(this, "Aviso", "Selecciona un tipo de actividad.");
            return;
        }
        // La opción solo es obligatoria si el tipo tiene opciones (Apéndice B)
        if (optionCombo.getItemCount() > 0 && optionCombo.getSelectedItem() == null) {
            Ui.info(this, "Aviso", "Selecciona una opción para este tipo de actividad.");
            return;
        }
        saved = true;
        dispose();
    }

    // Formatos permitidos por RF-14. El filtro por defecto es .pdf y .jpg, según el Artículo 24 del Reglamento PDEPD
    public void onAttachDocuments() {
        FileNameExtensionFilter preferred = new FileNameExtensionFilter("PDF o JPG (.pdf, .jpg, .jpeg)", "pdf", "jpg", "jpeg");
        FileNameExtensionFilter allowed = new FileNameExtensionFilter(
            "Formatos permitidos (.pdf, .jpg, .jpeg, .png, .docx, .xlsx)",
            "pdf", "jpg", "jpeg", "png", "docx", "xlsx"
        );

        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(preferred);
        chooser.addChoosableFileFilter(allowed);
        chooser.setFileFilter(preferred);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            // Conectar: DocumentoService.attach(...) valida, copia y calcula SHA256 al guardar
            for (File file : chooser.getSelectedFiles()) {
                newFiles.add(file);
                documentNames.add(file.getName());
            }
            refreshDocumentsList();
        }
    }

    public void onRemoveDocument(int index) {
        if (index >= 0 && index < documentNames.size()) {
            String removed = documentNames.remove(index);
            removedDocuments.add(removed);
            refreshDocumentsList();
        }
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
        onTypeChanged();
    }

    // Recarga las opciones del tipo y el cuadro PEDPD. Si el tipo no tiene opciones, el combo se desactiva
    private void onTypeChanged() {
        String type = (String) typeCombo.getSelectedItem();
        optionCombo.removeAllItems();
        if (type == null) {
            optionCombo.setEnabled(false);
            infoBox.setText("");
            return;
        }

        for (String option : SampleData.getOptions(type)) {
            optionCombo.addItem(option);
        }
        optionCombo.setEnabled(optionCombo.getItemCount() > 0);
        infoBox.setText(Ui.typeInfoText("", SampleData.getAccreditation(type)));
    }

    public void fill(
        String title,
        String category,
        String type,
        String option,
        String period,
        String startDate,
        String dueDate,
        String status,
        String accreditation,
        List<String> docs
    ) {
        titleField.setText(title);
        // Primero la categoría, que recarga los tipos. Luego el tipo, que recarga las opciones. Al final la opción
        categoryCombo.setSelectedItem(category);
        typeCombo.setSelectedItem(type);
        optionCombo.setSelectedItem(option);
        periodCombo.setSelectedItem(period);
        startDateField.setText(startDate);
        dueDateField.setText(dueDate);
        statusCombo.setSelectedItem(status);
        accreditationCombo.setSelectedItem(accreditation);

        documentNames.clear();
        if (docs != null) {
            documentNames.addAll(docs);
        }
        refreshDocumentsList();
    }

    public boolean isSaved() {
        return saved;
    }

    // [0]=titulo, [1]=categoria, [2]=tipo, [3]=opcion, [4]=periodo, [5]=fechaInicio, [6]=fechaFin, [7]=estatus, [8]=acreditacion
    public String[] getEvidence() {
        String option = (String) optionCombo.getSelectedItem();
        if (option == null) {
            option = "";
        }
        return new String[] {
            titleField.getText().trim(),
            (String) categoryCombo.getSelectedItem(),
            (String) typeCombo.getSelectedItem(),
            option,
            (String) periodCombo.getSelectedItem(),
            startDateField.getText().trim(),
            dueDateField.getText().trim(),
            (String) statusCombo.getSelectedItem(),
            (String) accreditationCombo.getSelectedItem()
        };
    }

    public List<String> getDocuments() {
        return new ArrayList<>(documentNames);
    }

    public List<File> getNewFiles() {
        return newFiles;
    }

    public List<String> getRemovedDocuments() {
        return removedDocuments;
    }
}
