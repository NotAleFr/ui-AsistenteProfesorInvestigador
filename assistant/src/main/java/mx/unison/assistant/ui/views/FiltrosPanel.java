package mx.unison.assistant.ui.views;

import java.awt.*;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.*;

import mx.unison.assistant.model.Estatus;
import mx.unison.assistant.model.TipoEvidencia;
import mx.unison.assistant.service.AppServices;
import mx.unison.assistant.ui.common.Ui;

// Panel lateral de filtros de Seguimiento (RF-11)
public class FiltrosPanel extends JPanel {

    static final String ALL = "Todos";
    static final String ANY_FILE_COUNT = "Cualquiera";

    public record FilterCriteria(
        String period,
        String type,
        String status,
        Double maxScore,
        String fileCount,
        String fileType
    ) {
        public static FilterCriteria defaults() {
            return new FilterCriteria(ALL, ALL, ALL, null, ANY_FILE_COUNT, ALL);
        }

        public FilterCriteria withPeriod(String nextPeriod) {
            return new FilterCriteria(nextPeriod, type, status, maxScore, fileCount, fileType);
        }
    }

    final AppServices services;

    JComboBox<String> periodCombo = new JComboBox<>();
    JComboBox<String> typeCombo = new JComboBox<>();
    JComboBox<String> statusCombo = new JComboBox<>();
    JTextField maxScoreField = new JTextField();
    JComboBox<String> fileCountCombo = new JComboBox<>(new String[] { ANY_FILE_COUNT, "Con archivos", "Sin archivos" });
    JComboBox<String> fileTypeCombo = new JComboBox<>(new String[] { ALL, ".pdf", ".jpg", ".jpeg", ".png", ".docx", ".xlsx" });

    FilterCriteria appliedCriteria = FilterCriteria.defaults();
    Runnable onApplyCallback;
    Runnable onClearCallback;

    public FiltrosPanel(AppServices services, Runnable onApplyCallback, Runnable onClearCallback) {
        this.services = services;
        this.onApplyCallback = onApplyCallback;
        this.onClearCallback = onClearCallback;

        // Los nombres de tipo son largos. Con un prototipo corto el panel no se ensancha
        typeCombo.setPrototypeDisplayValue("Tipo seleccionado");

        periodCombo.addItem(ALL);
        typeCombo.addItem(ALL);
        statusCombo.addItem(ALL);
        try {
            for (TipoEvidencia t : services.catalogo.getAllTypes()) {
                typeCombo.addItem(t.getNombreTipoEvidencia());
            }
            for (Estatus st : services.catalogo.getStatuses()) {
                statusCombo.addItem(st.getNombreEstatus());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los filtros del catálogo.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(230, 0));
        setBackground(UIManager.getColor("Filtros.background"));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Panel.borderColor")));

        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Panel.borderColor")),
            BorderFactory.createEmptyBorder(14, 16, 12, 16)
        ));

        JLabel title = new JLabel("Filtros");
        title.putClientProperty("FlatLaf.style", "font: bold, size: 16");

        header.add(title, BorderLayout.WEST);
        header.add(Ui.primaryButton("Aplicar", () -> onApply()), BorderLayout.EAST);
        return header;
    }

    private JScrollPane createBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        maxScoreField.putClientProperty("JTextField.placeholderText", "Ej. 50");

        JPanel[] groups = {
            Ui.field("Periodo", periodCombo),
            Ui.field("Tipo de evidencia", typeCombo),
            Ui.field("Estatus", statusCombo),
            Ui.field("Puntaje máx.", maxScoreField),
            Ui.field("Cantidad de archivos", fileCountCombo),
            Ui.field("Tipos de archivos", fileTypeCombo)
        };

        for (JPanel group : groups) {
            group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            body.add(group);
            body.add(Box.createVerticalStrut(12));
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(body, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Panel.borderColor")));
        footer.add(Ui.secondaryButton("Limpiar filtros", () -> onClearAll()));
        return footer;
    }

    public void setAvailablePeriods(Collection<String> periods) {
        Set<String> sortedPeriods = new TreeSet<>(Collections.reverseOrder());
        if (periods != null) {
            for (String period : periods) {
                if (period != null && !period.isBlank()) {
                    sortedPeriods.add(period.trim());
                }
            }
        }

        String previousPeriod = appliedCriteria.period();
        periodCombo.removeAllItems();
        periodCombo.addItem(ALL);
        for (String period : sortedPeriods) {
            periodCombo.addItem(period);
        }

        if (previousPeriod != null && sortedPeriods.contains(previousPeriod)) {
            periodCombo.setSelectedItem(previousPeriod);
        } else {
            periodCombo.setSelectedItem(ALL);
            appliedCriteria = appliedCriteria.withPeriod(ALL);
        }
    }

    public void setSelectedPeriod(String period) {
        String targetPeriod = (period != null && containsItem(periodCombo, period)) ? period : ALL;
        periodCombo.setSelectedItem(targetPeriod);
        appliedCriteria = appliedCriteria.withPeriod(targetPeriod);
    }

    public void onApply() {
        Double parsedMaxScore;
        try {
            parsedMaxScore = parseMaxScore(maxScoreField.getText());
        } catch (IllegalArgumentException e) {
            Ui.info(this, "Aviso", e.getMessage());
            maxScoreField.requestFocusInWindow();
            return;
        }
        appliedCriteria = new FilterCriteria(
            selectedOrDefault(periodCombo, ALL),
            selectedOrDefault(typeCombo, ALL),
            selectedOrDefault(statusCombo, ALL),
            parsedMaxScore,
            selectedOrDefault(fileCountCombo, ANY_FILE_COUNT),
            selectedOrDefault(fileTypeCombo, ALL)
        );
        if (onApplyCallback != null) {
            onApplyCallback.run();
        }
    }

    public void onClearAll() {
        periodCombo.setSelectedIndex(0);
        typeCombo.setSelectedIndex(0);
        statusCombo.setSelectedIndex(0);
        maxScoreField.setText("");
        fileCountCombo.setSelectedIndex(0);
        fileTypeCombo.setSelectedIndex(0);
        appliedCriteria = FilterCriteria.defaults();
        if (onClearCallback != null) {
            onClearCallback.run();
        }
    }

    public FilterCriteria getAppliedFilter() {
        return appliedCriteria;
    }

    static Double parseMaxScore(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return null;
        }
        String trimmed = rawText.trim();
        try {
            double parsed = Double.parseDouble(trimmed);
            if (!Double.isFinite(parsed) || parsed < 0) {
                throw new IllegalArgumentException("El puntaje máximo debe ser un número mayor o igual a 0.");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El puntaje máximo debe ser un valor numérico (por ejemplo, 50 o 12.5).");
        }
    }

    private static boolean containsItem(JComboBox<String> combo, String value) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (value.equals(combo.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String selectedOrDefault(JComboBox<String> combo, String fallback) {
        String selected = (String) combo.getSelectedItem();
        return selected != null ? selected : fallback;
    }
}