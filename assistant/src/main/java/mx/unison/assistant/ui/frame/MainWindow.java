package mx.unison.assistant.ui.frame;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.*;

import mx.unison.assistant.service.AppServices;
import mx.unison.assistant.ui.views.*;

public class MainWindow {
    
    JFrame frame;
    Sidebar sidebar;
    CardLayout cards = new CardLayout();
    JPanel content = new JPanel(cards);
    AlertasPanel alertasPanel;
    JLayeredPane centerLayers = new JLayeredPane();
    final AppServices services;

    public MainWindow(AppServices services) {
        
        this.services = services;
        frame = new JFrame("Asistente Profesor-Investigador");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        PlanAcademicoView planView = new PlanAcademicoView(services);
        SeguimientoView seguimientoView = new SeguimientoView(services);
        ReportesView reportesView = new ReportesView();
        AjustesView ajustesView = new AjustesView(services);

        content.add(planView, Section.ACADEMIC_PLAN.name());
        content.add(seguimientoView, Section.TRACKING.name());
        content.add(reportesView, Section.REPORTS.name());
        content.add(ajustesView, Section.SETTINGS.name());

        sidebar = new Sidebar(section -> showSection(section), () -> toggleAlerts());
        alertasPanel = new AlertasPanel(services, () -> toggleAlerts());
        alertasPanel.setVisible(false);

        planView.setOnDataChanged(() -> {
            seguimientoView.reload();
            alertasPanel.refresh();
        });
        seguimientoView.setOnDataChanged(() -> {
            alertasPanel.refresh();
        });
        ajustesView.setOnDataChanged(() -> {
            planView.reload();
            seguimientoView.reload();
            alertasPanel.refresh();
        });

        // Usamos JLayeredPane para que la bandeja de alertas flote encima de la pantalla actual
        centerLayers.add(content, JLayeredPane.DEFAULT_LAYER);
        centerLayers.add(alertasPanel, JLayeredPane.PALETTE_LAYER);
        centerLayers.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutCenterLayers();
            }
        });

        frame.add(sidebar, BorderLayout.WEST);
        frame.add(centerLayers, BorderLayout.CENTER);

        frame.setSize(1280, 720);
        frame.setMinimumSize(new Dimension(1024, 640));
        frame.setLocationRelativeTo(null);
    }

    private void layoutCenterLayers() {
        int width = centerLayers.getWidth();
        int height = centerLayers.getHeight();
        int alertsWidth = UIManager.getInt("Alertas.width");
        content.setBounds(0, 0, width, height);
        alertasPanel.setBounds(0, 0, alertsWidth, height);
    }

    public void show() {
        frame.setVisible(true);
        layoutCenterLayers();
        SwingUtilities.invokeLater(() -> alertasPanel.showStartupReminders(frame));
    }

    public void showSection(Section section) {
        cards.show(content, section.name());
        sidebar.selectSection(section);
        if (alertasPanel.isVisible()) {
            alertasPanel.setVisible(false);
            sidebar.setAlertsSelected(false);
        }
    }

    public void toggleAlerts() {
        boolean nextVisible = !alertasPanel.isVisible();
        if (nextVisible) {
            alertasPanel.refresh();
            layoutCenterLayers();
        }
        alertasPanel.setVisible(nextVisible);
        sidebar.setAlertsSelected(nextVisible);
    }

    public JFrame getFrame() {
        return frame;
    }
}
