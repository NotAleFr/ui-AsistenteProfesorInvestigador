package mx.unison.assistant.ui.views;

import java.awt.*;
import javax.swing.*;

import mx.unison.assistant.ui.common.PageHeader;

public class TiposEvidenciaView extends JPanel {
    public TiposEvidenciaView() {
        Insets padding = UIManager.getInsets("View.padding");

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));

        add(new PageHeader("Tipos de evidencia"), BorderLayout.NORTH);
    }
}
