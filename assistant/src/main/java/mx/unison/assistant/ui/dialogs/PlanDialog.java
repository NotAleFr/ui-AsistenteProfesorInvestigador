package mx.unison.assistant.ui.dialogs;

import java.awt.*;
import java.time.Year;
import javax.swing.*;

import mx.unison.assistant.ui.common.DialogBar;
import mx.unison.assistant.ui.common.Ui;

public class PlanDialog extends JDialog {

    JTextField nameField = new JTextField("Plan #1");
    JTextField yearField = new JTextField(String.valueOf(Year.now().getValue()));
    JComboBox<String> periodCombo = new JComboBox<>();
    boolean accepted = false;

    public PlanDialog(Window owner) {
        super(owner, "Creación de Plan Académico", ModalityType.APPLICATION_MODAL);

        setLayout(new BorderLayout());
        setSize(420, 260);
        setResizable(false);
        setLocationRelativeTo(owner);

        updatePeriods();
        yearField.addActionListener(e -> updatePeriods());

        add(createForm(), BorderLayout.CENTER);
        add(DialogBar.footer(
            FlowLayout.RIGHT,
            Ui.secondaryButton("Cancelar", () -> dispose()),
            Ui.primaryButton("Aceptar", () -> onAccept())
        ), BorderLayout.SOUTH);
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new BorderLayout(0, 14));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JPanel secondRow = new JPanel(new GridLayout(1, 2, 12, 0));
        secondRow.setOpaque(false);
        secondRow.add(Ui.field("Año", yearField));
        secondRow.add(Ui.field("Periodo", periodCombo));

        form.add(Ui.field("Nombre del Plan", nameField), BorderLayout.NORTH);
        form.add(secondRow, BorderLayout.CENTER);
        return form;
    }

    private void onAccept() {
        if (nameField.getText().trim().isEmpty()) {
            Ui.info(this, "Aviso", "Escribe un nombre para el plan.");
            return;
        }
        accepted = true;
        dispose();
    }

    private void updatePeriods() {
        String year = yearField.getText().trim();
        if (year.isEmpty()) {
            year = String.valueOf(Year.now().getValue());
        }
        periodCombo.removeAllItems();
        periodCombo.addItem(year + "-1");
        periodCombo.addItem(year + "-2");
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String[] getPlan() {
        return new String[] {
            nameField.getText().trim(),
            yearField.getText().trim(),
            (String) periodCombo.getSelectedItem()
        };
    }
}
