package mx.unison.assistant.ui.frame;

import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;

public class Sidebar extends JPanel {

    Consumer<Section> onSelect;
    ButtonGroup buttonGroup = new ButtonGroup();
    JPanel tilesPanel = new JPanel();

    public Sidebar(Consumer<Section> onSelect) {
        this.onSelect = onSelect;

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

        JLabel greeting = new JLabel("<html>Gestión de producción y evidencias</html>");
        greeting.setForeground(UIManager.getColor("Text.secondaryForeground"));

        header.add(title);
        header.add(Box.createVerticalStrut(10));
        header.add(greeting);
        header.add(Box.createVerticalStrut(30));
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
        tile.putClientProperty("FlatLaf.styleClass", "sidebarTile");
        tile.addActionListener(e -> onSelect.accept(section));
        tile.setVerticalTextPosition(SwingConstants.BOTTOM);

        buttonGroup.add(tile);
        tilesPanel.add(tile);
        return tile;
    }

}
