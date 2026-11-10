package mx.unison.assistant.app;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import mx.unison.assistant.ui.frame.MainWindow;

public class Main {

    public static void main(String[] args) {
        // Dejenlo arriba para que todo tenga el estilo del flatlaf
        FlatInterFont.install();
        FlatLaf.registerCustomDefaultsSource("theme");

        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
        FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);

        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            new MainWindow().show();
        });
    }
}
