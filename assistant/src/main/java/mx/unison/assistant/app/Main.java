package mx.unison.assistant.app;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import mx.unison.assistant.data.DatabaseConnection;
import mx.unison.assistant.data.DatabaseConnection.HealthResult;
import mx.unison.assistant.data.DatabaseConnection.HealthStatus;
import mx.unison.assistant.service.AppServices;
import mx.unison.assistant.ui.frame.MainWindow;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Dejenlo arriba para que todo tenga el estilo del flatlaf
        FlatInterFont.install();
        FlatLaf.registerCustomDefaultsSource("theme");

        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
        FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);

        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            if (prepareDatabase()) {
                new MainWindow(new AppServices()).show();
            }
        });
    }

    private static boolean prepareDatabase() {
        String retryOption = "Reintentar";
        String restoreOption = "Restaurar respaldo";
        String exitOption = "Salir";

        while (true) {
            HealthResult healthResult = DatabaseConnection.checkHealth();
            if (healthResult.status() == HealthStatus.OK) {
                try {
                    DatabaseConnection.createBackup();
                } catch (IOException | SQLException | SecurityException e) {
                    LOGGER.log(Level.WARNING, "No se pudo crear el respaldo de inicio.", e);
                }
                return true;
            }
            if (healthResult.cause() != null) {
                LOGGER.log(Level.WARNING, healthResult.message(), healthResult.cause());
            }

            List<String> options = new ArrayList<>();
            options.add(retryOption);
            if (healthResult.status() == HealthStatus.CORRUPT && hasBackups()) {
                options.add(restoreOption);
            }
            options.add(exitOption);

            int selectedOption = JOptionPane.showOptionDialog(null, healthResult.message(), "Base de datos",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options.toArray(), retryOption);
            if (selectedOption == JOptionPane.CLOSED_OPTION || exitOption.equals(options.get(selectedOption))) {
                return false;
            }
            if (restoreOption.equals(options.get(selectedOption))) {
                restoreBackup();
            }
        }
    }

    private static boolean hasBackups() {
        try {
            return !DatabaseConnection.listBackups().isEmpty();
        } catch (IOException | SecurityException e) {
            LOGGER.log(Level.WARNING, "No se pudieron consultar los respaldos.", e);
            return false;
        }
    }

    private static void restoreBackup() {
        String message;
        try {
            if (DatabaseConnection.restoreLatestBackup()) {
                return;
            }
            message = "No se encontró un respaldo válido.";
        } catch (IOException | SecurityException e) {
            LOGGER.log(Level.WARNING, "No se pudo restaurar el respaldo.", e);
            message = "No se pudo restaurar el respaldo. Comprueba los permisos y el espacio disponible.";
        }
        JOptionPane.showMessageDialog(null, message, "Restaurar respaldo", JOptionPane.ERROR_MESSAGE);
    }
}
