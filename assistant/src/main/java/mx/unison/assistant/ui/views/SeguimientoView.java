package mx.unison.assistant.ui.views;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import mx.unison.assistant.ui.common.PageHeader;
import mx.unison.assistant.ui.common.SampleData;
import mx.unison.assistant.ui.common.SampleData.TrackingRow;
import mx.unison.assistant.ui.common.StatsBar;
import mx.unison.assistant.ui.common.StatusBadge;
import mx.unison.assistant.ui.common.Ui;
import mx.unison.assistant.ui.dialogs.SeguimientoDialog;

public class SeguimientoView extends JPanel {

    JTextField searchField = new JTextField();
    JComboBox<String> yearCombo = new JComboBox<>(SampleData.getYears());
    StatsBar statsBar = new StatsBar();
    JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));

    DefaultTableModel tableModel;
    JTable table;
    FiltrosPanel filtrosPanel;
    List<TrackingRow> allRows = new ArrayList<>();
    List<TrackingRow> visibleRows = new ArrayList<>();
    String selectedSemester = "Todos";
    Runnable onDataChanged;

    public SeguimientoView() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel mainArea = new JPanel(new BorderLayout());
        Ui.applyViewStyle(mainArea);

        PageHeader header = new PageHeader("Seguimiento de evidencias");
        header.addAction(Ui.primaryButton("Generar Concentrado", () -> onGenerateSummary()));

        // Toolbar + StatsBar + Tabs en filas limpias para que nunca se encimen al abrir Filtros
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setOpaque(false);
        controls.add(createToolbar());
        controls.add(createTabsAndStatsRow());

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(controls, BorderLayout.CENTER);

        mainArea.add(north, BorderLayout.NORTH);
        mainArea.add(createTableScroll(), BorderLayout.CENTER);

        filtrosPanel = new FiltrosPanel(() -> applyFilter(), () -> applyFilter());
        filtrosPanel.setVisible(false);

        add(mainArea, BorderLayout.CENTER);
        add(filtrosPanel, BorderLayout.EAST);

        loadRows();
        rebuildSemesterTabs();
        applyFilter();
    }

    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setOpaque(false);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField.setPreferredSize(new Dimension(210, 34));
        searchField.putClientProperty("JTextField.placeholderText", "Buscar por título...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onSearch();
            }
        });

        JButton filterButton = Ui.secondaryButton("Filtrar", () -> toggleFilters());
        filterButton.setPreferredSize(new Dimension(82, 34));

        JLabel yearLabel = new JLabel("Año:");
        yearLabel.setForeground(UIManager.getColor("Text.secondaryForeground"));
        yearCombo.setPreferredSize(new Dimension(84, 34));
        yearCombo.addActionListener(e -> {
            rebuildSemesterTabs();
            applyFilter();
        });

        toolbar.add(searchField);
        toolbar.add(filterButton);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(yearLabel);
        toolbar.add(yearCombo);
        return toolbar;
    }

    private JPanel createTabsAndStatsRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Panel.borderColor")),
            BorderFactory.createEmptyBorder(6, 0, 6, 0)
        ));

        tabsPanel.setOpaque(false);
        row.add(tabsPanel, BorderLayout.WEST);
        row.add(statsBar, BorderLayout.EAST);
        return row;
    }

    private void rebuildSemesterTabs() {
        tabsPanel.removeAll();
        String year = String.valueOf(yearCombo.getSelectedItem());
        String[] options = { "Todos", "Semestre " + year + "-1", "Semestre " + year + "-2" };
        selectedSemester = "Todos";

        ButtonGroup group = new ButtonGroup();
        for (String option : options) {
            JToggleButton tab = new JToggleButton(option);
            tab.putClientProperty("FlatLaf.styleClass", "tab");
            if (option.equals("Todos")) {
                tab.setSelected(true);
            }
            tab.addActionListener(e -> {
                selectedSemester = option;
                applyFilter();
            });
            group.add(tab);
            tabsPanel.add(tab);
        }

        tabsPanel.revalidate();
        tabsPanel.repaint();
    }

    private JScrollPane createTableScroll() {
        String[] columns = {
            "Título", "Categoría", "Tipo de actividad", "Periodo",
            "Fecha límite", "Puntaje", "Estatus", "Archivo", "Acciones"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, Ui.tooltipCellRenderer());

        int[] widths = { 190, 95, 180, 70, 90, 65, 105, 105, 115 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.getColumnModel().getColumn(6).setCellRenderer((tbl, value, isSelected, hasFocus, row, column) -> {
            return badgeCell(StatusBadge.forStatus(String.valueOf(value)), tbl, isSelected);
        });

        table.getColumnModel().getColumn(7).setCellRenderer((tbl, value, isSelected, hasFocus, row, column) -> {
            int count = (value instanceof Integer) ? (Integer) value : 0;
            return badgeCell(StatusBadge.forFiles(count), tbl, isSelected);
        });

        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, "<html><nobr><u>Dar seguimiento</u></nobr></html>", isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int viewCol = table.columnAtPoint(e.getPoint());
                int modelCol = viewCol >= 0 ? table.convertColumnIndexToModel(viewCol) : -1;
                table.setCursor(Cursor.getPredefinedCursor(modelCol == 8 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                int modelCol = viewCol >= 0 ? table.convertColumnIndexToModel(viewCol) : -1;
                if (row >= 0 && (modelCol == 8 || e.getClickCount() == 2)) {
                    onTrackEvidence(row);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel badgeCell(StatusBadge badge, JTable tbl, boolean isSelected) {
        JPanel cell = new JPanel(new GridBagLayout());
        cell.setBackground(isSelected ? tbl.getSelectionBackground() : Color.WHITE);
        cell.add(badge);
        return cell;
    }

    // Carga las evidencias activas. Conectar: EvidenciaService.findForTracking()
    private void loadRows() {
        allRows = SampleData.findForTracking();
    }

    public void loadTable(List<TrackingRow> rows) {
        visibleRows = rows;
        tableModel.setRowCount(0);

        int delivered = 0;
        int pending = 0;
        int inProgress = 0;

        for (TrackingRow item : rows) {
            tableModel.addRow(new Object[] {
                item.title, item.category, item.type, item.period,
                item.dueDate, item.score, item.status, item.docs.size(), "Dar seguimiento"
            });

            if ("Entregado".equalsIgnoreCase(item.status)) {
                delivered++;
            } else if ("Pendiente".equalsIgnoreCase(item.status)) {
                pending++;
            } else {
                inProgress++;
            }
        }
        statsBar.setCounts(rows.size(), delivered, pending, inProgress);
    }

    public void onSearch() {
        applyFilter();
    }

    public void applyFilter() {
        String query = searchField.getText().trim().toLowerCase();
        String toolbarYear = String.valueOf(yearCombo.getSelectedItem());
        String[] side = filtrosPanel.getFilter();
        String sideYear = side[0];
        String sidePeriod = side[1];
        String sideType = side[2];
        String sideStatus = side[3];
        String maxScoreText = side[4];
        String fileCountFilter = side[5];
        String fileTypeFilter = side[6];

        Integer maxScore = null;
        if (!maxScoreText.isEmpty()) {
            try {
                maxScore = Integer.parseInt(maxScoreText);
            } catch (NumberFormatException ignored) {
                maxScore = null;
            }
        }

        List<TrackingRow> filtered = new ArrayList<>();
        for (TrackingRow item : allRows) {
            if (!query.isEmpty() && !item.title.toLowerCase().contains(query)) {
                continue;
            }

            String activeYear = !"Todos".equals(sideYear) ? sideYear : toolbarYear;
            if (!item.period.startsWith(activeYear)) {
                continue;
            }
            if (!"Todos".equals(selectedSemester) && !selectedSemester.endsWith(item.period)) {
                continue;
            }
            if (!"Todos".equals(sidePeriod) && !item.period.equals(sidePeriod)) {
                continue;
            }
            if (!"Todos".equals(sideType) && !item.type.equals(sideType)) {
                continue;
            }
            if (!"Todos".equals(sideStatus) && !item.status.equalsIgnoreCase(sideStatus)) {
                continue;
            }
            if (maxScore != null && item.score > maxScore) {
                continue;
            }
            if ("Con archivos".equals(fileCountFilter) && item.docs.isEmpty()) {
                continue;
            }
            if ("Sin archivos".equals(fileCountFilter) && !item.docs.isEmpty()) {
                continue;
            }
            if (!"Todos".equals(fileTypeFilter) && !hasFileWithExtension(item.docs, fileTypeFilter)) {
                continue;
            }

            filtered.add(item);
        }
        loadTable(filtered);
    }

    private boolean hasFileWithExtension(List<String> docs, String extension) {
        for (String doc : docs) {
            if (doc.toLowerCase().endsWith(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public void toggleFilters() {
        filtrosPanel.setVisible(!filtrosPanel.isVisible());
        revalidate();
        repaint();
    }

    public void onTrackEvidence(int row) {
        if (row < 0 || row >= visibleRows.size()) {
            return;
        }

        TrackingRow item = visibleRows.get(row);
        SeguimientoDialog dialog = new SeguimientoDialog(SwingUtilities.getWindowAncestor(this));
        dialog.fill(
            item.title, item.category, item.type, item.option, item.period,
            item.startDate, item.dueDate, item.status, item.accreditation,
            item.docs
        );
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            // Conectar: EvidenciaService.updateTracking(...), DocumentoService.attach(...) y DocumentoService.remove(...)
            String[] updated = dialog.getEvidence();
            item.title = updated[0];
            item.category = updated[1];
            item.type = updated[2];
            item.option = updated[3];
            item.period = updated[4];
            item.startDate = updated[5];
            item.dueDate = updated[6];
            item.status = updated[7];
            item.accreditation = updated[8];
            item.docs = dialog.getDocuments();

            applyFilter();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        }
    }

    public void onGenerateSummary() {
        Ui.info(this, "Generar Concentrado", "Se generará el concentrado del periodo seleccionado (PDF + ZIP de adjuntos).");
    }
}
