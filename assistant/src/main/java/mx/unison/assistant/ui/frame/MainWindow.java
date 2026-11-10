package mx.unison.assistant.ui.frame;

import java.awt.*;
import javax.swing.*;

import mx.unison.assistant.ui.views.*;

public class MainWindow {

    CardLayout cards = new CardLayout();
    JPanel content = new JPanel(cards);

    public void show() {
        JFrame frame = new JFrame("Asistente Profesor-Investigador");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        content.add(new PlanAcademicoView(), Section.ACADEMIC_PLAN.name());
        content.add(new EvidenciasView(), Section.EVIDENCE.name());
        content.add(new AlertasView(), Section.ALERTS.name());
        content.add(new ReportesView(), Section.REPORTS.name());
        content.add(new TiposEvidenciaView(), Section.EVIDENCE_TYPES.name());

        Sidebar sidebar = new Sidebar(section -> cards.show(content, section.name()));

        frame.add(sidebar, BorderLayout.WEST);
        frame.add(content, BorderLayout.CENTER);

        frame.setSize(1280, 720);
        frame.setMinimumSize(new Dimension(1024, 640));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
