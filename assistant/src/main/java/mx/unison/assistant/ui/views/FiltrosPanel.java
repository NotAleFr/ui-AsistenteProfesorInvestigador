package mx.unison.assistant.ui.views;

import java.awt.*;
import javax.swing.*;

import mx.unison.assistant.ui.common.SampleData;
import mx.unison.assistant.ui.common.Ui;

// Panel lateral de filtros de Seguimiento (RF-11)
public class FiltrosPanel extends JPanel {

    static final String ALL = "Todos";

    JComboBox<String> yearCombo = new JComboBox<>(withAll(SampleData.getYears()));
    JComboBox<String> periodCombo = new JComboBox<>(withAll(SampleData.getAllPeriods()));
    JComboBox<String> typeCombo = new JComboBox<>(withAll(SampleData.getAllTypes()));
    JComboBox<String> statusCombo = new JComboBox<>(withAll(SampleData.getStatuses()));
    JTextField maxScoreField = new JTextField();
    JComboBox<String> fileCountCombo = new JComboBox<>(new String[] { "Cualquiera", "Con archivos", "Sin archivos" });
    JComboBox<String> fileTypeCombo = new JComboBox<>(new String[] { ALL, ".pdf", ".jpg", ".jpeg", ".png", ".docx", ".xlsx" });

    Runnable onApplyCallback;
    Runnable onClearCallback;

    public FiltrosPanel(Runnable onApplyCallback, Runnable onClearCallback) {
        this.onApplyCallback = onApplyCallback;
        this.onClearCallback = onClearCallback;

        // Los nombres de tipo son largos. Con un prototipo corto el panel no se ensancha
        typeCombo.setPrototypeDisplayValue("Tipo seleccionado");

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(230, 0));
        setBackground(UIManager.getColor("Filtros.background"));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Panel.borderColor")));

        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
    }

    // Agrega "Todos" al inicio de la lista
    private static String[] withAll(String[] items) {
        String[] result = new String[items.length + 1];
        result[0] = ALL;
        for (int i = 0; i < items.length; i++) {
            result[i + 1] = items[i];
        }
        return result;
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
            Ui.field("Año", yearCombo),
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

    public void onApply() {
        if (onApplyCallback != null) {
            onApplyCallback.run();
        }
    }

    public void onClearAll() {
        yearCombo.setSelectedIndex(0);
        periodCombo.setSelectedIndex(0);
        typeCombo.setSelectedIndex(0);
        statusCombo.setSelectedIndex(0);
        maxScoreField.setText("");
        fileCountCombo.setSelectedIndex(0);
        fileTypeCombo.setSelectedIndex(0);
        if (onClearCallback != null) {
            onClearCallback.run();
        }
    }

    // [0]=año, [1]=periodo, [2]=tipo, [3]=estatus, [4]=puntajeMax, [5]=archivos, [6]=tipoArchivo
    // Conectar: se convierte en model.EvidenceFilter
    public String[] getFilter() {
        return new String[] {
            (String) yearCombo.getSelectedItem(),
            (String) periodCombo.getSelectedItem(),
            (String) typeCombo.getSelectedItem(),
            (String) statusCombo.getSelectedItem(),
            maxScoreField.getText().trim(),
            (String) fileCountCombo.getSelectedItem(),
            (String) fileTypeCombo.getSelectedItem()
        };
    }
}
