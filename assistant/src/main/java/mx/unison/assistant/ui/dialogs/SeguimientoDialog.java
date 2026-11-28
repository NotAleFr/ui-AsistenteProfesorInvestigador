package mx.unison.assistant.ui.dialogs;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import mx.unison.assistant.model.*;
import mx.unison.assistant.service.AppServices;
import mx.unison.assistant.ui.common.DialogBar;
import mx.unison.assistant.ui.common.InfoBox;
import mx.unison.assistant.ui.common.Ui;

// Seguimiento de una evidencia con los campos de RF-04 y de la Tabla 3 de RF-13
public class SeguimientoDialog extends JDialog {

    private static final int NONE_OPTION_ID = 0;

    final AppServices services;

    JTextField titleField = new JTextField();
    JComboBox<String> periodCombo = new JComboBox<>();
    JComboBox<Estatus> statusCombo = new JComboBox<>();
    JComboBox<Categoria> categoryCombo = new JComboBox<>();
    JComboBox<Acreditacion> accreditationCombo = new JComboBox<>();
    JComboBox<TipoEvidencia> typeCombo = new JComboBox<>();
    JComboBox<Opcion> optionCombo = new JComboBox<>();
    JTextField startDateField = new JTextField();
    JTextField dueDateField = new JTextField();

    JPanel documentsPanel = new JPanel();
    JTextArea notesArea = new JTextArea(4, 24);
    InfoBox infoBox = new InfoBox("Definición y acreditación (PEDPD)");

    Evidencia sourceEvidence;
    List<Documento> existingDocuments = new ArrayList<>();
    List<File> newFiles = new ArrayList<>();
    List<Documento> removedDocuments = new ArrayList<>();
    boolean saved = false;
    boolean populating = false;

    public SeguimientoDialog(Window owner, AppServices services) {
        super(owner, "Seguimiento de evidencia", ModalityType.APPLICATION_MODAL);

        this.services = services;
        for (String period : Periodo.recentPeriods()) {
            periodCombo.addItem(period);
        }
        try {
            for (Estatus st : services.catalogo.getStatuses()) {
                statusCombo.addItem(st);
            }
            for (Categoria cat : services.catalogo.getCategories()) {
                categoryCombo.addItem(cat);
            }
            for (Acreditacion acc : services.catalogo.getAccreditations()) {
                accreditationCombo.addItem(acc);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los catálogos.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        setLayout(new BorderLayout());
        setSize(980, 640);
        setMinimumSize(new Dimension(880, 560));
        setLocationRelativeTo(owner);

        // La categoría recarga los tipos. El tipo recarga las opciones y el cuadro PEDPD (RF-04, RF-13)
        categoryCombo.addActionListener(e -> {
            if (!populating) {
                loadTypes();
            }
        });
        typeCombo.addActionListener(e -> {
            if (!populating) {
                onTypeChanged();
            }
        });
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

        // Dos columnas abajo: documentos + notas a la izquierda, acreditación PEDPD a la derecha
        JPanel leftColumn = new JPanel(new GridLayout(2, 1, 0, 12));
        leftColumn.setOpaque(false);
        leftColumn.add(createDocumentsBox());
        leftColumn.add(Ui.noteBox("Descripción / Notas adicionales", notesArea));

        JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0));
        columns.setOpaque(false);
        columns.add(leftColumn);
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
        int totalItems = existingDocuments.size() + newFiles.size();
        if (totalItems == 0) {
            JLabel empty = new JLabel("Sin archivos adjuntos");
            empty.setForeground(UIManager.getColor("Text.tertiaryForeground"));
            documentsPanel.add(empty);
        } else {
            for (int i = 0; i < existingDocuments.size(); i++) {
                int idx = i;
                Documento existingDocument = existingDocuments.get(i);
                String name = existingDocument.getNombreOriginal();
                documentsPanel.add(createDocumentPill(
                    name,
                    () -> openFile(services.documentos.resolveFile(existingDocument)),
                    () -> onRemoveExistingDocument(idx)
                ));
                documentsPanel.add(Box.createVerticalStrut(8));
            }
            for (int i = 0; i < newFiles.size(); i++) {
                int idx = i;
                File pendingFile = newFiles.get(i);
                String name = pendingFile.getName();
                documentsPanel.add(createDocumentPill(
                    name,
                    () -> openFile(pendingFile.toPath()),
                    () -> onRemoveNewFile(idx)
                ));
                documentsPanel.add(Box.createVerticalStrut(8));
            }
        }
        documentsPanel.revalidate();
        documentsPanel.repaint();
    }

    private JPanel createDocumentPill(String docName, Runnable onOpen, Runnable onRemove) {
        JPanel pill = new JPanel(new BorderLayout(8, 0));
        pill.setBackground(UIManager.getColor("Note.background"));
        pill.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 6));
        pill.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel nameLabel = new JLabel(docName);
        nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nameLabel.setToolTipText("Abrir archivo");
        nameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onOpen.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                nameLabel.setText("<html><u>" + escapeHtml(docName) + "</u></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                nameLabel.setText(docName);
            }
        });

        JButton removeButton = Ui.button("X", "pillClose", onRemove);

        pill.add(nameLabel, BorderLayout.CENTER);
        pill.add(removeButton, BorderLayout.EAST);
        return pill;
    }

    private void openFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            JOptionPane.showMessageDialog(this, "El archivo ya no existe en el equipo.", "Archivo no encontrado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            JOptionPane.showMessageDialog(this, "Este sistema no permite abrir archivos directamente desde la aplicación.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().open(filePath.toFile());
        } catch (IOException | UnsupportedOperationException e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir el archivo seleccionado.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
        String startDateIso;
        String dueDateIso;
        try {
            startDateIso = Fechas.toIso(startDateField.getText());
            dueDateIso = Fechas.toIso(dueDateField.getText());
            if (startDateIso != null && dueDateIso != null && dueDateIso.compareTo(startDateIso) < 0) {
                Ui.info(this, "Aviso", "La fecha de finalización no puede ser anterior a la fecha de inicio.");
                return;
            }
        } catch (IllegalArgumentException e) {
            Ui.info(this, "Aviso", e.getMessage());
            return;
        }
        Evidencia updatedEvidence = buildEvidence(startDateIso, dueDateIso);
        try {
            services.evidencias.updateTracking(updatedEvidence, newFiles, removedDocuments);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error de archivo", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudo guardar el seguimiento.", "Error", JOptionPane.ERROR_MESSAGE);
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
            if (addUniqueStagedFiles(newFiles, chooser.getSelectedFiles())) {
                refreshDocumentsList();
            }
        }
    }

    static boolean addUniqueStagedFiles(List<File> stagedFiles, File[] selectedFiles) {
        if (selectedFiles == null || selectedFiles.length == 0) {
            return false;
        }
        boolean addedAny = false;
        for (File candidateFile : selectedFiles) {
            if (candidateFile == null || isFileAlreadyStaged(stagedFiles, candidateFile)) {
                continue;
            }
            stagedFiles.add(candidateFile);
            addedAny = true;
        }
        return addedAny;
    }

    private static boolean isFileAlreadyStaged(List<File> stagedFiles, File candidateFile) {
        Path candidatePath = candidateFile.toPath().toAbsolutePath().normalize();
        for (File stagedFile : stagedFiles) {
            if (stagedFile != null && stagedFile.toPath().toAbsolutePath().normalize().equals(candidatePath)) {
                return true;
            }
        }
        return false;
    }

    private void onRemoveExistingDocument(int index) {
        if (index >= 0 && index < existingDocuments.size()) {
            Documento removed = existingDocuments.remove(index);
            removedDocuments.add(removed);
            refreshDocumentsList();
        }
    }

    private void onRemoveNewFile(int index) {
        if (index >= 0 && index < newFiles.size()) {
            newFiles.remove(index);
            refreshDocumentsList();
        }
    }

    // Recarga los tipos de actividad de la categoría seleccionada
    private void loadTypes() {
        Categoria category = (Categoria) categoryCombo.getSelectedItem();
        boolean wasPopulating = populating;
        populating = true;
        try {
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
        } finally {
            populating = wasPopulating;
        }
        if (!populating) {
            onTypeChanged();
        }
    }

    // Recarga las opciones del tipo y el cuadro PEDPD. Si el tipo no tiene opciones, el combo se desactiva
    private void onTypeChanged() {
        TipoEvidencia type = (TipoEvidencia) typeCombo.getSelectedItem();
        optionCombo.removeAllItems();
        if (type == null) {
            optionCombo.setEnabled(false);
            infoBox.setText("");
            return;
        }

        try {
            for (Opcion opt : services.catalogo.getOptions(type.getIdTipoEvidencia())) {
                optionCombo.addItem(opt);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las opciones del tipo de actividad.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        optionCombo.setEnabled(optionCombo.getItemCount() > 0);
        if (!populating && optionCombo.getItemCount() > 0) {
            optionCombo.setSelectedIndex(0);
        }
        infoBox.setText(Ui.typeInfoText(type.getDefinicionTipoEvidencia(), type.getContenidoAcreditacion()));
    }

    public void fill(Evidencia ev) {
        this.sourceEvidence = ev;
        populating = true;
        try {
            titleField.setText(ev.getTitulo() != null ? ev.getTitulo() : "");

            String period = ev.getPeriodo();
            if (period != null && !period.isBlank()) {
                boolean foundPeriod = false;
                for (int i = 0; i < periodCombo.getItemCount(); i++) {
                    if (period.equals(periodCombo.getItemAt(i))) {
                        periodCombo.setSelectedIndex(i);
                        foundPeriod = true;
                        break;
                    }
                }
                if (!foundPeriod) {
                    periodCombo.addItem(period);
                    periodCombo.setSelectedItem(period);
                }
            }

            for (int i = 0; i < statusCombo.getItemCount(); i++) {
                if (statusCombo.getItemAt(i).getIdEstatus() == ev.getIdEstatus()) {
                    statusCombo.setSelectedIndex(i);
                    break;
                }
            }

            // Primero la categoría, que recarga los tipos. Luego el tipo, que recarga las opciones. Al final la opción
            for (int i = 0; i < categoryCombo.getItemCount(); i++) {
                if (categoryCombo.getItemAt(i).getIdCategoria() == ev.getIdCategoria()) {
                    categoryCombo.setSelectedIndex(i);
                    break;
                }
            }

            loadTypes();
            for (int i = 0; i < typeCombo.getItemCount(); i++) {
                if (typeCombo.getItemAt(i).getIdTipoEvidencia() == ev.getIdTipoEvidencia()) {
                    typeCombo.setSelectedIndex(i);
                    break;
                }
            }

            onTypeChanged();
            if (ev.getIdOpcion() > 0 && optionCombo.getItemCount() > 0) {
                for (int i = 0; i < optionCombo.getItemCount(); i++) {
                    if (optionCombo.getItemAt(i).getIdOpcion() == ev.getIdOpcion()) {
                        optionCombo.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (optionCombo.getItemCount() > 0) {
                optionCombo.setSelectedIndex(0);
            }

            for (int i = 0; i < accreditationCombo.getItemCount(); i++) {
                if (accreditationCombo.getItemAt(i).getIdAcreditacion() == ev.getIdAcreditacion()) {
                    accreditationCombo.setSelectedIndex(i);
                    break;
                }
            }

            String activePeriod = (String) periodCombo.getSelectedItem();
            LocalDate defaultDueDate = (activePeriod != null && Periodo.isValid(activePeriod))
                ? Periodo.endOf(activePeriod)
                : null;
            LocalDate effectiveDueDate = parseIsoDateOrNull(ev.getFechaFinalizacion());
            if (effectiveDueDate == null) {
                effectiveDueDate = defaultDueDate;
            }

            String startScreen;
            if (ev.getFechaInicio() != null && !ev.getFechaInicio().isBlank()) {
                startScreen = Fechas.toScreen(ev.getFechaInicio());
            } else {
                // Si la fecha límite efectiva ya pasó, evitamos poner hoy para no romper inicio <= fin
                LocalDate today = LocalDate.now();
                LocalDate defaultStartDate = today;
                if (effectiveDueDate != null && today.isAfter(effectiveDueDate)) {
                    LocalDate periodStart = (activePeriod != null && Periodo.isValid(activePeriod))
                        ? Periodo.startOf(activePeriod)
                        : effectiveDueDate;
                    defaultStartDate = effectiveDueDate.isBefore(periodStart) ? effectiveDueDate : periodStart;
                }
                startScreen = Fechas.toScreen(defaultStartDate);
            }
            startDateField.setText(startScreen);

            String dueScreen;
            if (ev.getFechaFinalizacion() != null && !ev.getFechaFinalizacion().isBlank()) {
                dueScreen = Fechas.toScreen(ev.getFechaFinalizacion());
            } else {
                dueScreen = defaultDueDate != null ? Fechas.toScreen(defaultDueDate) : "";
            }
            dueDateField.setText(dueScreen);
            notesArea.setText(ev.getDescripcion() != null ? ev.getDescripcion() : "");

            existingDocuments.clear();
            newFiles.clear();
            removedDocuments.clear();
            if (ev.getDocumentos() != null) {
                existingDocuments.addAll(ev.getDocumentos());
            }
            refreshDocumentsList();
        } finally {
            populating = false;
        }
    }

    private LocalDate parseIsoDateOrNull(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(isoDate.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private Evidencia buildEvidence(String startDateIso, String dueDateIso) {
        Evidencia ev = new Evidencia();
        if (sourceEvidence != null) {
            ev.setIdEvidencia(sourceEvidence.getIdEvidencia());
            ev.setIdPlanAcademico(sourceEvidence.getIdPlanAcademico());
            ev.setDocumentos(sourceEvidence.getDocumentos());
        }

        ev.setTitulo(titleField.getText().trim());
        String notes = notesArea.getText();
        ev.setDescripcion(notes.isBlank() ? null : notes);
        ev.setPeriodo((String) periodCombo.getSelectedItem());

        Estatus selectedStatus = (Estatus) statusCombo.getSelectedItem();
        if (selectedStatus != null) {
            ev.setIdEstatus(selectedStatus.getIdEstatus());
            ev.setNombreEstatus(selectedStatus.getNombreEstatus());
        }

        Categoria selectedCategory = (Categoria) categoryCombo.getSelectedItem();
        if (selectedCategory != null) {
            ev.setIdCategoria(selectedCategory.getIdCategoria());
            ev.setNombreCategoria(selectedCategory.getNombreCategoria());
        }

        TipoEvidencia selectedType = (TipoEvidencia) typeCombo.getSelectedItem();
        if (selectedType != null) {
            ev.setIdTipoEvidencia(selectedType.getIdTipoEvidencia());
            ev.setNombreTipoEvidencia(selectedType.getNombreTipoEvidencia());
        }

        Opcion selectedOption = (Opcion) optionCombo.getSelectedItem();
        if (optionCombo.isEnabled() && selectedOption != null) {
            ev.setIdOpcion(selectedOption.getIdOpcion());
            ev.setNombreOpcion(selectedOption.getNombreOpcion());
            ev.setPuntajeOpcion(selectedOption.getPuntajeOpcion());
        } else {
            ev.setIdOpcion(NONE_OPTION_ID);
        }

        Acreditacion selectedAcc = (Acreditacion) accreditationCombo.getSelectedItem();
        if (selectedAcc != null) {
            ev.setIdAcreditacion(selectedAcc.getIdAcreditacion());
            ev.setNombreAcreditacion(selectedAcc.getNombreAcreditacion());
        }

        ev.setFechaInicio(startDateIso);
        ev.setFechaFinalizacion(dueDateIso);
        return ev;
    }
}