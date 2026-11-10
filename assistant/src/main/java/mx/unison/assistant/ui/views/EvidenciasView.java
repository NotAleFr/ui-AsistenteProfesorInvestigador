package mx.unison.assistant.ui.views;

import java.awt.*;
import javax.swing.*;

import mx.unison.assistant.ui.common.PageHeader;

public class EvidenciasView extends JPanel {
    public EvidenciasView() {
        Insets padding = UIManager.getInsets("View.padding");

        PageHeader header = new PageHeader("Evidencias Institucionales");
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));

        JButton registerButton = new JButton("+ Registrar evidencia");
        registerButton.putClientProperty("FlatLaf.styleClass", "primary");
        header.addAction(registerButton);

        add(header, BorderLayout.NORTH);
    }
}
