package mx.unison.assistant.ui.views;

import java.awt.*;
import javax.swing.*;

import mx.unison.assistant.ui.common.PageHeader;

public class PlanAcademicoView extends JPanel {
    public PlanAcademicoView() {
        Insets padding = UIManager.getInsets("View.padding");

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));

        add(new PageHeader("Plan académico"), BorderLayout.NORTH);
    }
}
