package mx.unison.assistant.ui.views;

import java.awt.*;
import javax.swing.*;

import mx.unison.assistant.ui.common.PageHeader;

public class AlertasView extends JPanel {
    public AlertasView() {
        Insets padding = UIManager.getInsets("View.padding");

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));

        add(new PageHeader("Alertas"), BorderLayout.NORTH);
    }
}
