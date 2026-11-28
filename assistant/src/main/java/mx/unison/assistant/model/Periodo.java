package mx.unison.assistant.model;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

// Periodos escolares de RF-01: "2026-1" es de enero a junio y "2026-2" de julio a diciembre
public class Periodo {

    private static final MonthDay SEMESTER_1_START_FROM = MonthDay.of(1, 1);
    private static final MonthDay SEMESTER_1_START_TO = MonthDay.of(1, 20);
    private static final MonthDay SEMESTER_2_START_FROM = MonthDay.of(8, 1);
    private static final MonthDay SEMESTER_2_START_TO = MonthDay.of(8, 20);
    private static final MonthDay SEMESTER_1_MID_FROM = MonthDay.of(3, 15);
    private static final MonthDay SEMESTER_1_MID_TO = MonthDay.of(3, 31);
    private static final MonthDay SEMESTER_2_MID_FROM = MonthDay.of(10, 1);
    private static final MonthDay SEMESTER_2_MID_TO = MonthDay.of(10, 15);
    private static final MonthDay SEMESTER_1_END_FROM = MonthDay.of(6, 1);
    private static final MonthDay SEMESTER_1_END_TO = MonthDay.of(6, 30);
    private static final MonthDay SEMESTER_2_END_FROM = MonthDay.of(11, 25);
    private static final MonthDay SEMESTER_2_END_TO = MonthDay.of(12, 20);

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
        requireValid(periodo);
        return Integer.parseInt(periodo.substring(0, 4));
    }

    private static void requireValid(String periodo) {
        if (!isValid(periodo)) {
            throw new IllegalArgumentException("El periodo debe tener el formato AAAA-1 o AAAA-2.");
        }
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

    public static String previous(String periodo) {
        requireValid(periodo);
        return periodo.endsWith("-2") ? periodo.substring(0, 4) + "-1" : (yearOf(periodo) - 1) + "-2";
    }

    // Julio queda entre periodos y no activa recordatorios de inicio, mitad ni cierre.
    public static boolean isPeriodStart(LocalDate date) {
        return inWindow(date, SEMESTER_1_START_FROM, SEMESTER_1_START_TO)
                || inWindow(date, SEMESTER_2_START_FROM, SEMESTER_2_START_TO);
    }

    public static boolean isMidPeriod(LocalDate date) {
        return inWindow(date, SEMESTER_1_MID_FROM, SEMESTER_1_MID_TO)
                || inWindow(date, SEMESTER_2_MID_FROM, SEMESTER_2_MID_TO);
    }

    public static boolean isPeriodEnd(LocalDate date) {
        return inWindow(date, SEMESTER_1_END_FROM, SEMESTER_1_END_TO)
                || inWindow(date, SEMESTER_2_END_FROM, SEMESTER_2_END_TO);
    }

    private static boolean inWindow(LocalDate date, MonthDay start, MonthDay end) {
        MonthDay monthDay = MonthDay.from(date);
        return !monthDay.isBefore(start) && !monthDay.isAfter(end);
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
