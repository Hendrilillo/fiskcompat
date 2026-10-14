package com.hendrilillo.fiskcompat;

import com.hendrilillo.fiskcompat.fsk.FskScript;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FskTest {
    private static double run(String src, String var) {
        FskScript s = FskScript.compile(src);
        double[] v = new double[s.size() + 1];
        s.run(v, null);
        return v[s.find(var)];
    }

    @Test
    public void basics() {
        assertEquals(Math.PI / 2, run("{a} = 90'", "a"), 1e-9);
        assertEquals(6, run("{b} = 3\n{a} = 2{b}", "a"), 1e-9);
        assertEquals(-5, run("{a} += -5", "a"), 1e-9);
        assertEquals(7, run("{a} = if(2 > 1, 7, 3)", "a"), 1e-9);
        assertEquals(0.5, run("{a} = curve 0.5", "a"), 1e-9);
        assertEquals(5, run("{w} = 0.5\n{a} = 0\n{a} @ {w} -> 10", "a"), 1e-9);
        assertEquals(Math.PI - 0.5 * Math.PI, run("{a} = pi - pi / 2", "a"), 1e-9);
        assertEquals(1, run("{a} = clamp(5, 0, 1) ; comentario", "a"), 1e-9);
        assertEquals(-3, run("{x} = 1\n{a} = -(1 + 2){x}", "a"), 1e-9);
    }

    /** Compila todas las animaciones de los heropacks de prueba y muestra las líneas que fallan. */
    @Test
    public void compileAllPackAnimations() throws Exception {
        Path dir = Path.of(System.getProperty("fiskcompat.testGameDir", "../testgame"), "fsk");
        if (!Files.isDirectory(dir)) return;
        int files = 0, bad = 0;
        try (var s = Files.walk(dir)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".fsk")).toList()) {
                files++;
                List<String> errors = FskScript.compile(Files.readString(p)).errors;
                for (String e : errors) {
                    bad++;
                    if (bad < 30) System.out.println("[fsk] " + p.getFileName() + ": " + e);
                }
            }
        }
        System.out.println("[fsk] " + files + " archivos, " + bad + " líneas con error");
    }
}
