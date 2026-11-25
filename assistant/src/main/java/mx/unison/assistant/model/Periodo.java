package mx.unison.assistant.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Periodos escolares de RF-01: "2026-1" es de enero a junio y "2026-2" de julio a diciembre
public class Periodo {

    public static boolean isValid(String periodo) {
        if (periodo == null || periodo.length() != 6) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (!Character.isDigit(periodo.charAt(i))) {
                return false;
            }
        }
        if (periodo.charAt(4) != '-') {
            return false;
        }
        return periodo.endsWith("1") || periodo.endsWith("2");
    }

    public static int yearOf(String periodo) {
        return Integer.parseInt(periodo.substring(0, 4));
    }

    public static String current() {
        return of(LocalDate.now());
    }

    public static String of(LocalDate date) {
        if (date.getMonthValue() <= 6) {
            return date.getYear() + "-1";
        } else {
            return date.getYear() + "-2";
        }
    }

    public static List<Integer> recentYears() {
        int current = LocalDate.now().getYear();
        List<Integer> years = new ArrayList<>();
        years.add(current);
        years.add(current - 1);
        years.add(current - 2);
        return years;
    }

    public static List<String> periodsOf(int year) {
        List<String> periods = new ArrayList<>();
        periods.add(year + "-1");
        periods.add(year + "-2");
        return periods;
    }

    public static List<String> recentPeriods() {
        List<String> periods = new ArrayList<>();
        for (int year : recentYears()) {
            periods.add(year + "-1");
            periods.add(year + "-2");
        }
        return periods;
    }

    public static LocalDate startOf(String periodo) {
        int year = yearOf(periodo);
        if (periodo.endsWith("1")) {
            return LocalDate.of(year, 1, 1);
        } else {
            return LocalDate.of(year, 7, 1);
        }
    }

    // Fecha de finalización por defecto en Seguimiento (Tabla 3 del SRS)
    public static LocalDate endOf(String periodo) {
        int year = yearOf(periodo);
        if (periodo.endsWith("1")) {
            return LocalDate.of(year, 6, 30);
        } else {
            return LocalDate.of(year, 12, 31);
        }
    }
}
