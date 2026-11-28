package mx.unison.assistant.ui.views;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import mx.unison.assistant.model.Documento;
import mx.unison.assistant.model.Evidencia;
import mx.unison.assistant.model.Fechas;
import mx.unison.assistant.model.Periodo;
import mx.unison.assistant.service.AppServices;
import mx.unison.assistant.service.EvidenciaService.ResumenIndicadores;
import mx.unison.assistant.ui.common.PageHeader;
import mx.unison.assistant.ui.common.StatsBar;
import mx.unison.assistant.ui.common.StatusBadge;
import mx.unison.assistant.ui.common.Ui;
import mx.unison.assistant.ui.dialogs.SeguimientoDialog;

public class SeguimientoView extends JPanel {

    private static final String TABLE_CARD = "TABLE";
    private static final String EMPTY_CARD = "EMPTY";
    private static final String NO_SCORE = "—";
    private static final int SCORE_COLUMN = 5;
    private static final int TABLE_MIN_FIT_WIDTH = 820;

    final AppServices services;

    JTextField searchField = new JTextField();
    JComboBox<String> yearCombo = new JComboBox<>();
    StatsBar statsBar = new StatsBar();
    JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
    CardLayout tableCards = new CardLayout();
    JPanel tableArea = new JPanel(tableCards);
    JLabel emptyStateLabel = new JLabel("", SwingConstants.CENTER);

    DefaultTableModel tableModel;
    JTable table;
    FiltrosPanel filtrosPanel;
    List<Evidencia> allRows = new ArrayList<>();
    List<Evidencia> visibleRows = new ArrayList<>();
    String selectedSemesterKey = null;
    boolean updatingYears = false;
    // true cuando el usuario ya eligió un año o semestre; antes de eso la vista sigue a los datos
    boolean periodChosenByUser = false;
    Runnable onDataChanged;

    public SeguimientoView(AppServices services) {
        this.services = services;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel mainArea = new JPanel(new BorderLayout());
        Ui.applyViewStyle(mainArea);

        PageHeader header = new PageHeader("Seguimiento de evidencias");
        header.addAction(Ui.primaryButton("Generar Concentrado", () -> onGenerateSummary()));

        // Toolbar, StatsBar y pestañas en filas separadas para que no se encimen al abrir Filtros a 1024 px
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setOpaque(false);
        controls.add(createToolbar());
        controls.add(createStatsRow());
        controls.add(createTabsRow());

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(controls, BorderLayout.CENTER);

        tableArea.setOpaque(false);
        tableArea.add(createTableScroll(), TABLE_CARD);
        tableArea.add(createEmptyStatePanel(), EMPTY_CARD);

        mainArea.add(north, BorderLayout.NORTH);
        mainArea.add(tableArea, BorderLayout.CENTER);

        filtrosPanel = new FiltrosPanel(services, () -> onSideFiltersChanged(), () -> onSideFiltersChanged());
        filtrosPanel.setVisible(false);

        add(mainArea, BorderLayout.CENTER);
        add(filtrosPanel, BorderLayout.EAST);

        reload();
    }

    private void loadYears(boolean useDefaultSelection) {
        String previousYear = useDefaultSelection ? null : (String) yearCombo.getSelectedItem();
        int currentCalendarYear = Periodo.yearOf(Periodo.current());
        Set<Integer> years = new TreeSet<>(Collections.reverseOrder());
        years.addAll(Periodo.recentYears());
        for (Evidencia item : allRows) {
            if (item.getAnio() > 0) {
                years.add(item.getAnio());
            }
        }

        String targetYear = resolveSelectedYear(previousYear, allRows, years, currentCalendarYear);
        updatingYears = true;
        try {
            yearCombo.removeAllItems();
            for (Integer year : years) {
                yearCombo.addItem(String.valueOf(year));
            }
            yearCombo.setSelectedItem(targetYear);
        } finally {
            updatingYears = false;
        }
    }

    static String resolveSelectedYear(
            String previousYear,
            List<Evidencia> rows,
            Set<Integer> availableYears,
            int currentCalendarYear) {
        if (previousYear != null) {
            for (Integer year : availableYears) {
                if (previousYear.equals(String.valueOf(year))) {
                    return previousYear;
                }
            }
        }

        int latestEligibleYear = -1;
        if (rows != null) {
            for (Evidencia item : rows) {
                int itemYear = item.getAnio();
                if (itemYear == currentCalendarYear) {
                    return String.valueOf(currentCalendarYear);
                }
                if (itemYear > 0 && itemYear < currentCalendarYear && itemYear > latestEligibleYear) {
                    latestEligibleYear = itemYear;
                }
            }
        }
        return String.valueOf(latestEligibleYear > 0 ? latestEligibleYear : currentCalendarYear);
    }

    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    public void reload() {
        loadRows();
        if (allRows.isEmpty()) {
            periodChosenByUser = false;
        }
        // Mientras el usuario no haya elegido año o semestre, la vista sigue al año/semestre con datos
        boolean useDefaultSelection = !periodChosenByUser && !allRows.isEmpty();
        if (useDefaultSelection) {
            selectedSemesterKey = null;
        }
        loadYears(useDefaultSelection);
        filtrosPanel.reloadCatalog();
        refreshFilterPeriodsForSelectedYear();
        rebuildSemesterTabs();
        filtrosPanel.setSelectedPeriod(selectedSemesterKey);
        applyFilter();
    }

    private void onSideFiltersChanged() {
        FiltrosPanel.FilterCriteria criteria = filtrosPanel.getAppliedFilter();
        if (!Objects.equals(criteria.period(), selectedSemesterKey)) {
            periodChosenByUser = true;
            selectedSemesterKey = criteria.period();
        }
        rebuildSemesterTabs();
        applyFilter();
    }

    private void refreshFilterPeriodsForSelectedYear() {
        String selectedYear = getSelectedYear();
        filtrosPanel.setAvailablePeriods(List.of(selectedYear + "-2", selectedYear + "-1"));
    }

    private String getSelectedYear() {
        String selectedYear = (String) yearCombo.getSelectedItem();
        if (selectedYear == null || selectedYear.isBlank()) {
            return String.valueOf(Periodo.yearOf(Periodo.current()));
        }
        return selectedYear;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setOpaque(false);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Ancho pensado para que el año siga visible a 1024 px con Filtros abierto
        searchField.setPreferredSize(new Dimension(180, 34));
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
            if (!updatingYears) {
                periodChosenByUser = true;
                refreshFilterPeriodsForSelectedYear();
                rebuildSemesterTabs();
                filtrosPanel.setSelectedPeriod(selectedSemesterKey);
                applyFilter();
            }
        });

        toolbar.add(searchField);
        toolbar.add(filterButton);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(yearLabel);
        toolbar.add(yearCombo);
        return toolbar;
    }

    private JPanel createStatsRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        row.add(statsBar, BorderLayout.WEST);
        return row;
    }

    private JPanel createTabsRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Panel.borderColor")),
            BorderFactory.createEmptyBorder(2, 0, 2, 0)
        ));

        tabsPanel.setOpaque(false);
        row.add(tabsPanel, BorderLayout.WEST);
        return row;
    }

    private void rebuildSemesterTabs() {
        tabsPanel.removeAll();
        String selectedYear = getSelectedYear();

        List<String> tabKeys = buildSemesterTabKeys(selectedYear);
        selectedSemesterKey = resolveSelectedSemesterKey(selectedYear, selectedSemesterKey, allRows, Periodo.current());

        ButtonGroup group = new ButtonGroup();
        for (String tabKey : tabKeys) {
            JToggleButton tab = new JToggleButton(formatSemesterTabLabel(tabKey));
            tab.putClientProperty("FlatLaf.styleClass", "tab");
            if (tabKey.equals(selectedSemesterKey)) {
                tab.setSelected(true);
            }
            tab.addActionListener(e -> {
                periodChosenByUser = true;
                selectedSemesterKey = tabKey;
                filtrosPanel.setSelectedPeriod(tabKey);
                applyFilter();
            });
            group.add(tab);
            tabsPanel.add(tab);
        }

        tabsPanel.revalidate();
        tabsPanel.repaint();
    }

    static List<String> buildSemesterTabKeys(String year) {
        return List.of(year + "-2", year + "-1", FiltrosPanel.ALL);
    }

    static String formatSemesterTabLabel(String semesterKey) {
        return FiltrosPanel.ALL.equals(semesterKey) ? FiltrosPanel.ALL : "Semestre " + semesterKey;
    }

    static String resolveSelectedSemesterKey(
            String year,
            String currentSelectionKey,
            List<Evidencia> rows,
            String currentPeriod) {
        List<String> tabKeys = buildSemesterTabKeys(year);
        if (currentSelectionKey != null && tabKeys.contains(currentSelectionKey)) {
            return currentSelectionKey;
        }

        String secondSemesterPeriod = year + "-2";
        String firstSemesterPeriod = year + "-1";
        boolean hasSecondSemester = false;
        boolean hasFirstSemester = false;
        if (rows != null) {
            for (Evidencia item : rows) {
                String period = item.getPeriodo();
                if (secondSemesterPeriod.equals(period)) {
                    hasSecondSemester = true;
                } else if (firstSemesterPeriod.equals(period)) {
                    hasFirstSemester = true;
                }
            }
        }

        if (hasSecondSemester) {
            return secondSemesterPeriod;
        }
        if (hasFirstSemester) {
            return firstSemesterPeriod;
        }
        if (currentPeriod != null && currentPeriod.startsWith(year + "-")) {
            return currentPeriod;
        }
        return secondSemesterPeriod;
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

        // Llena el ancho a partir de un tamaño legible (1280x720 sin filtros); en anchos chicos usa scroll horizontal
        table = new JTable(tableModel) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                Container viewport = getParent();
                return viewport != null && viewport.getWidth() >= TABLE_MIN_FIT_WIDTH;
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, Ui.tooltipCellRenderer());

        int[] widths = { 190, 95, 180, 70, 90, 65, 105, 105, 130 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        // "Dar seguimiento" no se corta aunque la tabla se angoste
        table.getColumnModel().getColumn(8).setMinWidth(widths[8]);

        // El modelo guarda el puntaje como número; solo la celda muestra la raya si no hay opción
        DefaultTableCellRenderer scoreRenderer = Ui.tooltipCellRenderer();
        table.getColumnModel().getColumn(SCORE_COLUMN).setCellRenderer((tbl, value, isSelected, hasFocus, row, column) -> {
            int modelRow = tbl.convertRowIndexToModel(row);
            Object shownValue = modelRow < visibleRows.size() ? formatScore(visibleRows.get(modelRow)) : value;
            return scoreRenderer.getTableCellRendererComponent(tbl, shownValue, isSelected, hasFocus, row, column);
        });

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

    static String formatScore(Evidencia item) {
        return item.getIdOpcion() > 0 ? String.valueOf(item.getPuntajeOpcion()) : NO_SCORE;
    }

    private JPanel createEmptyStatePanel() {
        JPanel emptyPanel = new JPanel(new GridBagLayout());
        emptyPanel.setBackground(Color.WHITE);
        emptyPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        emptyStateLabel.setForeground(UIManager.getColor("Text.secondaryForeground"));
        emptyStateLabel.putClientProperty("FlatLaf.style", "font: size: 14");
        emptyPanel.add(emptyStateLabel);
        return emptyPanel;
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

        if (rows.isEmpty()) {
            emptyStateLabel.setText(allRows.isEmpty()
                ? "No hay evidencias en seguimiento."
                : "No se encontraron evidencias con los filtros actuales.");
            tableCards.show(tableArea, EMPTY_CARD);
        } else {
            tableCards.show(tableArea, TABLE_CARD);
        }
    }

    public void onSearch() {
        applyFilter();
    }

    public void applyFilter() {
        String query = normalizeForSearch(searchField.getText().trim());
        String toolbarYear = getSelectedYear();
        FiltrosPanel.FilterCriteria criteria = filtrosPanel.getAppliedFilter();

        List<Evidencia> filtered = new ArrayList<>();
        for (Evidencia item : allRows) {
            if (matchesFilters(item, query, toolbarYear, criteria)) {
                filtered.add(item);
            }
        }
        loadTable(filtered);
    }

    static boolean matchesFilters(
            Evidencia item,
            String normalizedQuery,
            String toolbarYear,
            FiltrosPanel.FilterCriteria criteria) {
        String title = item.getTitulo() != null ? item.getTitulo() : "";
        String period = item.getPeriodo() != null ? item.getPeriodo() : "";
        String type = item.getNombreTipoEvidencia() != null ? item.getNombreTipoEvidencia() : "";
        String status = item.getNombreEstatus() != null ? item.getNombreEstatus() : "";
        List<Documento> docs = item.getDocumentos() != null ? item.getDocumentos() : List.of();

        if (!normalizedQuery.isEmpty() && !normalizeForSearch(title).contains(normalizedQuery)) {
            return false;
        }
        if (toolbarYear != null && !period.startsWith(toolbarYear + "-")) {
            return false;
        }
        if (!FiltrosPanel.ALL.equals(criteria.period()) && !period.equals(criteria.period())) {
            return false;
        }
        if (!FiltrosPanel.ALL.equals(criteria.type()) && !type.equals(criteria.type())) {
            return false;
        }
        if (!FiltrosPanel.ALL.equals(criteria.status()) && !status.equalsIgnoreCase(criteria.status())) {
            return false;
        }
        if (criteria.maxScore() != null && item.getPuntajeOpcion() > criteria.maxScore()) {
            return false;
        }
        if ("Con archivos".equals(criteria.fileCount()) && docs.isEmpty()) {
            return false;
        }
        if ("Sin archivos".equals(criteria.fileCount()) && !docs.isEmpty()) {
            return false;
        }
        if (!FiltrosPanel.ALL.equals(criteria.fileType()) && !hasFileWithExtension(docs, criteria.fileType())) {
            return false;
        }
        return true;
    }

    // Minúsculas y sin acentos, para que "investigacion" encuentre "Investigación"
    static String normalizeForSearch(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        StringBuilder withoutAccents = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char character = decomposed.charAt(i);
            if (Character.getType(character) != Character.NON_SPACING_MARK) {
                withoutAccents.append(character);
            }
        }
        return withoutAccents.toString().toLowerCase(Locale.ROOT);
    }

    private static boolean hasFileWithExtension(List<Documento> docs, String extension) {
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
        openTrackingDialog(visibleRows.get(row));
    }

    public void openEvidenceById(int idEvidencia) {
        try {
            Evidencia targetEvidence = services.evidencias.findById(idEvidencia).orElse(null);
            if (targetEvidence == null) {
                JOptionPane.showMessageDialog(this, "La evidencia seleccionada ya no existe.", "Aviso", JOptionPane.WARNING_MESSAGE);
                reload();
                return;
            }
            openTrackingDialog(targetEvidence);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudo cargar la evidencia seleccionada.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openTrackingDialog(Evidencia item) {
        SeguimientoDialog dialog = new SeguimientoDialog(SwingUtilities.getWindowAncestor(this), services);
        dialog.fill(item);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            reload();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        }
    }

    public void onGenerateSummary() {
        Ui.info(this, "Generar Concentrado", "Se generará el concentrado del periodo seleccionado (PDF + ZIP de adjuntos).");
    }
}
