package mx.unison.assistant.ui.frame;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;

public class Sidebar extends JPanel {

    Consumer<Section> onSelect;
    Runnable onToggleAlerts;
    ButtonGroup buttonGroup = new ButtonGroup();
    JPanel tilesPanel = new JPanel();
    Map<Section, JToggleButton> sectionTiles = new EnumMap<>(Section.class);
    JToggleButton alertsTile;

    public Sidebar(Consumer<Section> onSelect, Runnable onToggleAlerts) {
        this.onSelect = onSelect;
        this.onToggleAlerts = onToggleAlerts;

        Insets padding = UIManager.getInsets("Sidebar.padding");

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(UIManager.getInt("Sidebar.width"), 0));
        setBackground(UIManager.getColor("Sidebar.background"));
        setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));

        add(createHeader(), BorderLayout.NORTH);
        add(createTiles(), BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("<html>Asistente<br>Profesor-Investigador</html>");
        title.putClientProperty("FlatLaf.style", "font: bold, size: 15");

        JLabel welcome = new JLabel("Bienvenida, Dra. Pesantes");
        welcome.setForeground(UIManager.getColor("Text.secondaryForeground"));
        welcome.putClientProperty("FlatLaf.style", "font: size: 12");

        // Salto de línea fijo: un JLabel HTML en BoxLayout no se ajusta solo al ancho de la barra
        JLabel greeting = new JLabel("<html>Gestión de producción<br>y evidencias</html>");
        greeting.setForeground(UIManager.getColor("Text.tertiaryForeground"));
        greeting.putClientProperty("FlatLaf.style", "font: size: 12");

        header.add(title);
        header.add(Box.createVerticalStrut(14));
        header.add(welcome);
        header.add(Box.createVerticalStrut(4));
        header.add(greeting);
        header.add(Box.createVerticalStrut(28));
        return header;
    }

    private JPanel createTiles() {
        int gap = UIManager.getInt("Sidebar.tileGap");
        tilesPanel.setLayout(new GridLayout(0, 2, gap, gap));
        tilesPanel.setOpaque(false);

        for (Section section : Section.values()) {
            JToggleButton tile = createTile(section);
            if (section == Section.ACADEMIC_PLAN) {
                tile.setSelected(true);
            }
        }

        // Se mete en NORTH de otro panel para que los tiles no se estiren
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(tilesPanel, BorderLayout.NORTH);
        return wrapper;
    }

    private JToggleButton createTile(Section section) {
        JToggleButton tile = new JToggleButton(section.getLabel());

        tile.setPreferredSize(UIManager.getDimension("Sidebar.tileSize"));
        tile.setHorizontalTextPosition(SwingConstants.CENTER);
        tile.setVerticalTextPosition(SwingConstants.BOTTOM);
        tile.putClientProperty("FlatLaf.styleClass", "sidebarTile");

        if (section == Section.ALERTS) {
            // Alertas se abre encima de cualquier pantalla, no cambia de vista
            alertsTile = tile;
            tile.addActionListener(e -> onToggleAlerts.run());
        } else {
            tile.addActionListener(e -> onSelect.accept(section));
            buttonGroup.add(tile);
            sectionTiles.put(section, tile);
        }

        tilesPanel.add(tile);
        return tile;
    }

    public void selectSection(Section section) {
        JToggleButton tile = sectionTiles.get(section);
        if (tile != null) {
            tile.setSelected(true);
        }
    }

    public void setAlertsSelected(boolean selected) {
        if (alertsTile != null) {
            alertsTile.setSelected(selected);
        }
    }

    public void setAlertCount(int count) {
        if (alertsTile != null) {
            String baseLabel = Section.ALERTS.getLabel();
            alertsTile.setText(count > 0 ? baseLabel + " (" + count + ")" : baseLabel);
        }
    }
}
