package com.hendrilillo.fiskcompat.fsk;

/** Funciones matemáticas de .fsk (equivalentes a FskMath y java.lang.Math). */
final class Functions {
    private Functions() {}

    static double curveCrests(double x) {
        return Math.sin(x * Math.PI / 2);
    }

    static double curve(double x) {
        return (curveCrests(x * 2 - 1) + 1) / 2;
    }

    static double clamp(double x, double min, double max) {
        return Math.min(Math.max(x, min), max);
    }

    /** Rampa de 0 a 1 entre "start" y "start + duration". */
    static double animate(double x, double duration, double start) {
        return x > start && x <= start + duration ? (x - start) / duration : 0;
    }

    /** Sube durante fadeIn, se mantiene y baja durante fadeOut, dentro de [start, start + duration). */
    static double animate(double x, double duration, double start, double fadeIn, double fadeOut) {
        fadeIn = clamp(fadeIn, 0, duration);
        fadeOut = clamp(fadeOut, 0, duration - fadeIn);
        if (x >= start && x < start + duration) {
            double t = x - start;
            if (t < fadeIn) return animate(t, fadeIn, 0);
            if (t >= duration - fadeOut) return 1 - animate(t, fadeOut, duration - fadeOut);
            return 1;
        }
        return 0;
    }

    static double wrapAngleTo180(double a) {
        a %= 360;
        if (a >= 180) a -= 360;
        if (a < -180) a += 360;
        return a;
    }

    static FskScript.Expr create(String name, FskScript.Expr[] a) {
        int n = a.length;
        return switch (name) {
            case "sin" -> v -> Math.sin(a[0].eval(v));
            case "cos" -> v -> Math.cos(a[0].eval(v));
            case "tan" -> v -> Math.tan(a[0].eval(v));
            case "asin" -> v -> Math.asin(a[0].eval(v));
            case "acos" -> v -> Math.acos(a[0].eval(v));
            case "atan" -> v -> Math.atan(a[0].eval(v));
            case "atan2" -> v -> Math.atan2(a[0].eval(v), a[1].eval(v));
            case "sinh" -> v -> Math.sinh(a[0].eval(v));
            case "cosh" -> v -> Math.cosh(a[0].eval(v));
            case "tanh" -> v -> Math.tanh(a[0].eval(v));
            case "sqrt" -> v -> Math.sqrt(a[0].eval(v));
            case "cbrt" -> v -> Math.cbrt(a[0].eval(v));
            case "exp" -> v -> Math.exp(a[0].eval(v));
            case "expm1" -> v -> Math.expm1(a[0].eval(v));
            case "log" -> v -> Math.log(a[0].eval(v));
            case "log10" -> v -> Math.log10(a[0].eval(v));
            case "log1p" -> v -> Math.log1p(a[0].eval(v));
            case "abs" -> v -> Math.abs(a[0].eval(v));
            case "signum" -> v -> Math.signum(a[0].eval(v));
            case "round" -> v -> Math.round(a[0].eval(v));
            case "floor" -> v -> Math.floor(a[0].eval(v));
            case "ceil" -> v -> Math.ceil(a[0].eval(v));
            case "pow" -> v -> Math.pow(a[0].eval(v), a[1].eval(v));
            case "hypot" -> v -> Math.hypot(a[0].eval(v), a[1].eval(v));
            case "logn" -> v -> Math.log(a[1].eval(v)) / Math.log(a[0].eval(v));
            case "root" -> v -> Math.pow(a[0].eval(v), 1 / a[1].eval(v));
            case "curve" -> v -> curve(a[0].eval(v));
            case "curveCrests" -> v -> curveCrests(a[0].eval(v));
            case "clamp" -> v -> clamp(a[0].eval(v), a[1].eval(v), a[2].eval(v));
            case "interpolate" -> v -> { double x = a[0].eval(v); return x + (a[1].eval(v) - x) * a[2].eval(v); };
            case "wrapAngleTo180" -> v -> wrapAngleTo180(a[0].eval(v));
            case "if" -> v -> a[0].eval(v) == 1 ? a[1].eval(v) : a[2].eval(v);
            case "animate" -> n >= 5
                    ? v -> animate(a[0].eval(v), a[1].eval(v), a[2].eval(v), a[3].eval(v), a[4].eval(v))
                    : v -> animate(a[0].eval(v), a[1].eval(v), a[2].eval(v));
            case "animate2" -> v -> animate(a[0].eval(v), a[1].eval(v), a[2].eval(v), a[3].eval(v), a[4].eval(v));
            case "min" -> v -> { double r = a[0].eval(v); for (int k = 1; k < n; k++) r = Math.min(r, a[k].eval(v)); return r; };
            case "max" -> v -> { double r = a[0].eval(v); for (int k = 1; k < n; k++) r = Math.max(r, a[k].eval(v)); return r; };
            case "sum" -> v -> { double r = 0; for (FskScript.Expr e : a) r += e.eval(v); return r; };
            default -> throw new IllegalStateException("Función desconocida: " + name);
        };
    }
}
