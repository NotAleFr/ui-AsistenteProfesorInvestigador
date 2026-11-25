package mx.unison.assistant.ui.views;
import mx.unison.assistant.service.AppServices;

import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.*;

import mx.unison.assistant.ui.common.PageHeader;
import mx.unison.assistant.ui.common.Ui;

public class AjustesView extends JPanel {
    final AppServices services;

    Runnable onDataChanged;

    public AjustesView(AppServices services) {
        this.services = services;
        setLayout(new BorderLayout());
        Ui.applyViewStyle(this);

        add(new PageHeader("Ajustes"), BorderLayout.NORTH);
        add(createOptionsPanel(), BorderLayout.CENTER);
    }

    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    private JPanel createOptionsPanel() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        list.add(createOptionRow(
            "Restaurar catálogo por defecto",
            "Esta opción establecerá las tablas Categoría, TipoEvidencia, Opción y Estatus exactamente a los valores originales del Reglamento PEDPD.",
            Ui.secondaryButton("Restaurar catálogo", () -> onRestoreCatalog())
        ));
        list.add(Box.createVerticalStrut(20));
        list.add(createOptionRow(
            "Restablecer y eliminar toda la información del sistema",
            "Esta opción eliminará todas las evidencias, registros y archivos adjuntos, y restaurará las tablas a un estado limpio de fábrica.",
            Ui.dangerButton("Restablecer todo", () -> onDeleteAll())
        ));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(list, BorderLayout.NORTH);
        return wrapper;
    }

    private JPanel createOptionRow(String titleText, String descText, JButton actionButton) {
        JPanel row = new JPanel(new BorderLayout(24, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Panel.borderColor")),
            BorderFactory.createEmptyBorder(18, 4, 18, 4)
        ));

        JPanel textPanel = new JPanel(new BorderLayout(0, 6));
        textPanel.setOpaque(false);

        JLabel title = new JLabel(titleText);
        title.putClientProperty("FlatLaf.style", "font: semibold, size: 15");

        JTextArea description = new JTextArea(descText);
        description.setEditable(false);
        description.setFocusable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setOpaque(false);
        description.setForeground(UIManager.getColor("Text.secondaryForeground"));
        description.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        description.setSize(new Dimension(520, Short.MAX_VALUE));

        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(description, BorderLayout.CENTER);

        JPanel buttonWrapper = new JPanel(new GridBagLayout());
        buttonWrapper.setOpaque(false);
        buttonWrapper.add(actionButton);

        row.add(textPanel, BorderLayout.CENTER);
        row.add(buttonWrapper, BorderLayout.EAST);
        return row;
    }

    public void onRestoreCatalog() {
        if (Ui.confirm(this, "¿Deseas restaurar el catálogo PEDPD a sus valores por defecto?")) {
            try {
                services.ajustes.restoreCatalog();
                Ui.info(this, "Ajustes", "Catálogo restaurado correctamente.");
                if (onDataChanged != null) {
                    onDataChanged.run();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                    this,
                    "No se pudo restaurar el catálogo.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    public void onDeleteAll() {
        // Doble confirmación como pide RF-23
        if (!Ui.confirm(this, "¿Deseas restablecer y eliminar toda la información del sistema?")) {
            return;
        }
        if (Ui.confirm(this, "Esta acción borrará todos los planes, evidencias y archivos adjuntos de forma permanente.\n¿Estás completamente segura de continuar?")) {
            try {
                services.ajustes.deleteAll();
                Ui.info(this, "Ajustes", "Sistema restablecido a estado de fábrica.");
                if (onDataChanged != null) {
                    onDataChanged.run();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                    this,
                    "No se pudo restablecer la información del sistema.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            } catch (IOException e) {
                JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
