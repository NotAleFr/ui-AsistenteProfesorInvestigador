package mx.unison.assistant.ui.views;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import mx.unison.assistant.ui.common.PageHeader;
import mx.unison.assistant.ui.common.SampleData;
import mx.unison.assistant.ui.common.Ui;

public class ReportesView extends JPanel {

    JComboBox<String> yearCombo = new JComboBox<>(SampleData.getYears());
    JLabel reportSubtitle = new JLabel();
    DefaultTableModel previewModel;
    int selectedYear = Integer.parseInt(SampleData.getYears()[0]);

    public ReportesView() {
        setLayout(new BorderLayout());
        Ui.applyViewStyle(this);

        PageHeader header = new PageHeader("Reporte de evidencia anual");
        header.addAction(Ui.primaryButton("Exportar reporte anual", () -> onExport()));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(createYearBar(), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(createReportPreview(), BorderLayout.CENTER);

        loadYear(selectedYear);
    }

    private JPanel createYearBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel yearLabel = new JLabel("Año del reporte:");
        yearLabel.setForeground(UIManager.getColor("Text.secondaryForeground"));

        yearCombo.setPreferredSize(new Dimension(100, 32));
        yearCombo.addActionListener(e -> loadYear(Integer.parseInt(String.valueOf(yearCombo.getSelectedItem()))));

        bar.add(yearLabel);
        bar.add(yearCombo);
        return bar;
    }

    private JScrollPane createReportPreview() {
        JPanel backdrop = new JPanel(new BorderLayout());
        backdrop.setBackground(UIManager.getColor("Alertas.background"));
        backdrop.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Panel.borderColor")),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JPanel sheet = new JPanel(new BorderLayout(0, 20));
        sheet.setBackground(Color.WHITE);
        sheet.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Panel.borderColor")),
            BorderFactory.createEmptyBorder(28, 32, 28, 32)
        ));

        sheet.add(createDocumentHeader(), BorderLayout.NORTH);
        sheet.add(createDeliveredTable(), BorderLayout.CENTER);
        backdrop.add(sheet, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(backdrop);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel createDocumentHeader() {
        JPanel docHeader = new JPanel();
        docHeader.setLayout(new BoxLayout(docHeader, BoxLayout.Y_AXIS));
        docHeader.setOpaque(false);

        JTextArea uniLabel = new JTextArea(
            "UNIVERSIDAD DE SONORA\n"
            + "FACULTAD INTERDISCIPLINARIA DE INGENIERÍA · DEPARTAMENTO DE INGENIERÍA INDUSTRIAL\n"
            + "CAMPUS HERMOSILLO"
        );
        uniLabel.setEditable(false);
        uniLabel.setFocusable(false);
        uniLabel.setLineWrap(true);
        uniLabel.setWrapStyleWord(true);
        uniLabel.setOpaque(false);
        uniLabel.setBorder(null);
        uniLabel.putClientProperty("FlatLaf.style", "font: semibold, size: 14");
        uniLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        reportSubtitle.putClientProperty("FlatLaf.style", "font: semibold, size: 15");
        reportSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel metaRow = new JPanel(new GridLayout(2, 2, 16, 6));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        metaRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Panel.borderColor")),
            BorderFactory.createEmptyBorder(10, 0, 10, 0)
        ));
        metaRow.add(new JLabel("<html><b>Académico:</b> Dra. Mery Helen Pesantes Espinoza</html>"));
        metaRow.add(new JLabel("<html><b>PRODEP Vigente:</b> Sí</html>"));
        metaRow.add(new JLabel("<html><b>Categoría:</b> Profesor-Investigador Titular</html>"));
        metaRow.add(new JLabel("<html><b>Formato:</b> Anexo 5 — Reglamento PEDPD</html>"));

        docHeader.add(uniLabel);
        docHeader.add(Box.createVerticalStrut(16));
        docHeader.add(reportSubtitle);
        docHeader.add(Box.createVerticalStrut(14));
        docHeader.add(metaRow);
        return docHeader;
    }

    private JPanel createDeliveredTable() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JLabel sectionTitle = new JLabel("Evidencias concluidas (Entregado)");
        sectionTitle.putClientProperty("FlatLaf.style", "font: semibold, size: 14");

        String[] columns = { "Categoría", "Título", "Tipo de actividad", "Periodo", "Puntaje", "Documentos" };
        previewModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(previewModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, Ui.tooltipCellRenderer());

        JPanel tableBox = new JPanel(new BorderLayout());
        tableBox.setBackground(Color.WHITE);
        tableBox.setBorder(Ui.lineBorder());
        tableBox.add(table.getTableHeader(), BorderLayout.NORTH);
        tableBox.add(table, BorderLayout.CENTER);

        panel.add(sectionTitle, BorderLayout.NORTH);
        panel.add(tableBox, BorderLayout.CENTER);
        return panel;
    }

    // Evidencias "Entregado" del año (RF-16). Conectar: ReporteService.getAnnualReport(year)
    public void loadYear(int year) {
        this.selectedYear = year;
        reportSubtitle.setText("Informe anual de las actividades académicas para el año escolar " + year);

        previewModel.setRowCount(0);
        for (Object[] row : SampleData.getAnnualReport(year)) {
            previewModel.addRow(row);
        }
    }

    // Conectar: ReporteService.generateAnnualReport(year)
    public void onExport() {
        Ui.info(this, "Exportar reporte anual", "Se exportará el reporte anual " + selectedYear + " (PDF + ZIP de adjuntos).");
    }
}
