package com.sctp.transporte.util;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

/** Días de operación codificados como "L,M,X,J,V,S,D" (X = miércoles). */
public final class Dias {
    private static final String ORDEN = "LMXJVSD";
    private static final String[] NOMBRES = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private Dias() {}

    public static char letra(DayOfWeek d) { return ORDEN.charAt(d.getValue() - 1); }

    public static boolean opera(String dias, char letra) { return dias.indexOf(letra) >= 0; }

    /** ¿Opera al menos un día de lunes a sábado? */
    public static boolean operaLunesASabado(String dias) {
        for (char c : "LMXJVS".toCharArray()) if (opera(dias, c)) return true;
        return false;
    }

    /** "L,M,X,J,V,S" → "Lun–Sáb"; "L,X,V" → "Lun, Mié, Vie". */
    public static String etiqueta(String dias) {
        List<String> partes = new ArrayList<>();
        int i = 0;
        while (i < 7) {
            if (!opera(dias, ORDEN.charAt(i))) { i++; continue; }
            int j = i;
            while (j + 1 < 7 && opera(dias, ORDEN.charAt(j + 1))) j++;
            if (j - i >= 2) partes.add(NOMBRES[i] + "–" + NOMBRES[j]);
            else for (int k = i; k <= j; k++) partes.add(NOMBRES[k]);
            i = j + 1;
        }
        return String.join(", ", partes);
    }
}
