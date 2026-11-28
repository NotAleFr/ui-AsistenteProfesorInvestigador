package mx.unison.assistant.ui.dialogs;

import java.awt.*;
import java.time.Year;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import mx.unison.assistant.model.Periodo;
import mx.unison.assistant.service.PlanService;
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
        yearField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePeriods();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePeriods();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePeriods();
            }
        });

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
        Integer parsedYear = parseValidYear();
        if (parsedYear == null) {
            Ui.info(this, "Aviso", "El año debe tener 4 dígitos entre " + PlanService.MIN_YEAR + " y " + PlanService.MAX_YEAR + ".");
            return;
        }
        updatePeriodsForYear(parsedYear);
        accepted = true;
        dispose();
    }

    private void updatePeriods() {
        Integer year = parseValidYear();
        if (year != null) {
            updatePeriodsForYear(year);
        }
    }

    private Integer parseValidYear() {
        String yearText = yearField.getText().trim();
        if (yearText.length() != 4) {
            return null;
        }
        int year;
        try {
            year = Integer.parseInt(yearText);
        } catch (NumberFormatException e) {
            return null;
        }
        if (year < PlanService.MIN_YEAR || year > PlanService.MAX_YEAR) {
            return null;
        }
        return year;
    }

    private void updatePeriodsForYear(int year) {
        String previousSelection = (String) periodCombo.getSelectedItem();
        boolean preferSecondSemester = previousSelection != null && previousSelection.endsWith("-2");
        periodCombo.removeAllItems();
        for (String period : Periodo.periodsOf(year)) {
            periodCombo.addItem(period);
        }
        if (preferSecondSemester && periodCombo.getItemCount() > 1) {
            periodCombo.setSelectedIndex(1);
        }
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
