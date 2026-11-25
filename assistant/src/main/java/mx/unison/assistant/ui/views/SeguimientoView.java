package mx.unison.assistant.ui.views;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import mx.unison.assistant.model.Documento;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Periodo;
import mx.unison.assistant.service.AppServices;
import mx.unison.assistant.service.EvidenciaService.ResumenIndicadores;
import mx.unison.assistant.ui.common.Fechas;
import mx.unison.assistant.ui.common.PageHeader;
import mx.unison.assistant.ui.common.StatsBar;
import mx.unison.assistant.ui.common.StatusBadge;
import mx.unison.assistant.ui.common.Ui;
import mx.unison.assistant.ui.dialogs.SeguimientoDialog;

public class SeguimientoView extends JPanel {

    final AppServices services;

    JTextField searchField = new JTextField();
    JComboBox<String> yearCombo = new JComboBox<>();
    StatsBar statsBar = new StatsBar();
    JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));

    DefaultTableModel tableModel;
    JTable table;
    FiltrosPanel filtrosPanel;
    List<Evidencia> allRows = new ArrayList<>();
    List<Evidencia> visibleRows = new ArrayList<>();
    String selectedSemester = "Todos";
    Runnable onDataChanged;

    public SeguimientoView(AppServices services) {
        this.services = services;
        loadYears();

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

        filtrosPanel = new FiltrosPanel(services, () -> applyFilter(), () -> applyFilter());
        filtrosPanel.setVisible(false);

        add(mainArea, BorderLayout.CENTER);
        add(filtrosPanel, BorderLayout.EAST);

        loadRows();
        rebuildSemesterTabs();
        applyFilter();
    }

    private void loadYears() {
        yearCombo.removeAllItems();
        for (Integer year : Periodo.recentYears()) {
            yearCombo.addItem(String.valueOf(year));
        }
    }

    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    public void reload() {
        loadRows();
        applyFilter();
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

    private void loadRows() {
        try {
            allRows = services.evidencias.findForTracking();
        } catch (SQLException e) {
            e.printStackTrace();
            allRows = new ArrayList<>();
            JOptionPane.showMessageDialog(this, "No se pudieron cargar las evidencias en seguimiento.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadTable(List<Evidencia> rows) {
        visibleRows = rows;
        tableModel.setRowCount(0);

        for (Evidencia item : rows) {
            String category = item.getNombreCategoria() != null ? item.getNombreCategoria() : "";
            String type = item.getNombreTipoEvidencia() != null ? item.getNombreTipoEvidencia() : "";
            String period = item.getPeriodo() != null ? item.getPeriodo() : "";
            String dueDate = (item.getFechaFinalizacion() != null && !item.getFechaFinalizacion().isBlank())
                ? Fechas.toScreen(item.getFechaFinalizacion())
                : (Periodo.isValid(period) ? Fechas.toScreen(Periodo.endOf(period)) : "");
            double score = item.getPuntajeOpcion();
            String status = item.getNombreEstatus() != null ? item.getNombreEstatus() : "";
            int docCount = item.getDocumentos() != null ? item.getDocumentos().size() : 0;

            tableModel.addRow(new Object[] {
                item.getTitulo(), category, type, period,
                dueDate, score, status, docCount, "Dar seguimiento"
            });
        }

        ResumenIndicadores resumen = services.evidencias.countByStatus(rows);
        statsBar.setCounts(resumen.total(), resumen.entregadas(), resumen.pendientes(), resumen.enProceso());
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

        Double maxScore = null;
        if (!maxScoreText.isEmpty()) {
            try {
                maxScore = Double.parseDouble(maxScoreText);
            } catch (NumberFormatException ignored) {
                maxScore = null;
            }
        }

        List<Evidencia> filtered = new ArrayList<>();
        for (Evidencia item : allRows) {
            String title = item.getTitulo() != null ? item.getTitulo() : "";
            String period = item.getPeriodo() != null ? item.getPeriodo() : "";
            String type = item.getNombreTipoEvidencia() != null ? item.getNombreTipoEvidencia() : "";
            String status = item.getNombreEstatus() != null ? item.getNombreEstatus() : "";
            List<Documento> docs = item.getDocumentos() != null ? item.getDocumentos() : List.of();

            if (!query.isEmpty() && !title.toLowerCase().contains(query)) {
                continue;
            }

            String activeYear = !"Todos".equals(sideYear) ? sideYear : toolbarYear;
            if (!period.startsWith(activeYear)) {
                continue;
            }
            if (!"Todos".equals(selectedSemester) && !selectedSemester.endsWith(period)) {
                continue;
            }
            if (!"Todos".equals(sidePeriod) && !period.equals(sidePeriod)) {
                continue;
            }
            if (!"Todos".equals(sideType) && !type.equals(sideType)) {
                continue;
            }
            if (!"Todos".equals(sideStatus) && !status.equalsIgnoreCase(sideStatus)) {
                continue;
            }
            if (maxScore != null && item.getPuntajeOpcion() > maxScore) {
                continue;
            }
            if ("Con archivos".equals(fileCountFilter) && docs.isEmpty()) {
                continue;
            }
            if ("Sin archivos".equals(fileCountFilter) && !docs.isEmpty()) {
                continue;
            }
            if (!"Todos".equals(fileTypeFilter) && !hasFileWithExtension(docs, fileTypeFilter)) {
                continue;
            }

            filtered.add(item);
        }
        loadTable(filtered);
    }

    private boolean hasFileWithExtension(List<Documento> docs, String extension) {
        for (Documento doc : docs) {
            String name = doc.getNombreOriginal();
            if (name != null && name.toLowerCase().endsWith(extension.toLowerCase())) {
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

        Evidencia item = visibleRows.get(row);
        SeguimientoDialog dialog = new SeguimientoDialog(SwingUtilities.getWindowAncestor(this), services);
        dialog.fill(item);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            try {
                Evidencia updated = dialog.getEvidence();
                services.evidencias.updateTracking(updated, dialog.getNewFiles(), dialog.getRemovedDocuments());
                loadRows();
                applyFilter();
                if (onDataChanged != null) {
                    onDataChanged.run();
                }
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error de archivo", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "No se pudo guardar el seguimiento.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void onGenerateSummary() {
        Ui.info(this, "Generar Concentrado", "Se generará el concentrado del periodo seleccionado (PDF + ZIP de adjuntos).");
    }
}
