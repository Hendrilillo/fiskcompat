package com.hendrilillo.fiskcompat.fsk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intérprete del lenguaje de animación .fsk de Fisk.
 *
 * <pre>
 * {var} = expr            asignar          {var} += expr / -= / *= / /=
 * {var} @ peso -&gt; expr     interpolar       {var} @ peso -'&gt; expr  (interpolación de ángulos)
 * out "canal" (a, b, ...)  salida para animaciones del cuerpo
 * ; comentario
 * </pre>
 * Expresiones: + - * / % ^, comparaciones, &amp;&amp; ||, !, multiplicación implícita ("5{data}"),
 * grados con ' ("90'" = π/2), funciones (sin, clamp, animate2, curve, if...) también sin paréntesis
 * ("curve {data}").
 */
public final class FskScript {
    /** Índices de variables de este script. */
    final Map<String, Integer> slots = new HashMap<>();
    final List<String> names = new ArrayList<>();
    private final List<Stmt> statements = new ArrayList<>();
    /** Líneas que no se pudieron compilar (para diagnóstico). */
    public final List<String> errors = new ArrayList<>();

    public static FskScript compile(String source) {
        FskScript s = new FskScript();
        for (String raw : source.split("\\r?\\n")) {
            String line = raw;
            int c = line.indexOf(';');
            if (c >= 0) line = line.substring(0, c);
            line = line.trim();
            if (line.isEmpty()) continue;
            try {
                s.statements.add(new Parser(s, line).statement());
            } catch (RuntimeException e) {
                s.errors.add(line + "  <- " + e.getMessage());
            }
        }
        return s;
    }

    public int slot(String name) {
        return slots.computeIfAbsent(name, n -> {
            names.add(n);
            return names.size() - 1;
        });
    }

    public Integer find(String name) {
        return slots.get(name);
    }

    public int size() {
        return names.size();
    }

    public List<String> names() {
        return names;
    }

    /** Ejecuta el script sobre las variables dadas (indexadas con slot()). */
    public void run(double[] vars, List<Output> outputs) {
        for (Stmt st : statements) st.exec(vars, outputs);
    }

    public record Output(String channel, double[] args) {}

    // ------------------------------------------------------------------ AST

    interface Expr {
        double eval(double[] v);
    }

    interface Stmt {
        void exec(double[] v, List<Output> out);
    }

    static double bool(boolean b) {
        return b ? 1 : 0;
    }

    static double interpolateRot(double a, double b, double t) {
        double d = b - a;
        while (d < -Math.PI) d += Math.PI * 2;
        while (d >= Math.PI) d -= Math.PI * 2;
        return a + t * d;
    }

    // ------------------------------------------------------------------ parser

    private static final class Parser {
        private final FskScript script;
        private final String s;
        private int i;

        Parser(FskScript script, String s) {
            this.script = script;
            this.s = s;
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private boolean peek(String t) {
            ws();
            return s.startsWith(t, i);
        }

        private boolean eat(String t) {
            if (peek(t)) {
                i += t.length();
                return true;
            }
            return false;
        }

        private void expect(String t) {
            if (!eat(t)) throw new IllegalStateException("Se esperaba " + t + " en: " + s);
        }

        private String varName() {
            expect("{");
            int end = s.indexOf('}', i);
            String name = s.substring(i, end).trim();
            i = end + 1;
            return name;
        }

        Stmt statement() {
            ws();
            if (s.startsWith("out", i) && !s.startsWith("out_", i)) {
                i += 3;
                ws();
                expect("\"");
                int end = s.indexOf('"', i);
                String channel = s.substring(i, end);
                i = end + 1;
                expect("(");
                List<Expr> args = new ArrayList<>();
                if (!peek(")")) {
                    do args.add(or()); while (eat(","));
                }
                expect(")");
                Expr[] a = args.toArray(new Expr[0]);
                return (v, out) -> {
                    if (out == null) return;
                    double[] vals = new double[a.length];
                    for (int k = 0; k < a.length; k++) vals[k] = a[k].eval(v);
                    out.add(new Output(channel, vals));
                };
            }
            int slot = script.slot(varName());
            if (eat("@")) {
                Expr weight = or();
                boolean rot;
                if (eat("-'>")) rot = true;
                else {
                    expect("->");
                    rot = false;
                }
                Expr target = or();
                return rot
                        ? (v, out) -> v[slot] = interpolateRot(v[slot], target.eval(v), weight.eval(v))
                        : (v, out) -> {
                            double w = weight.eval(v);
                            v[slot] = v[slot] + (target.eval(v) - v[slot]) * w;
                        };
            }
            String[] ops = {"+=", "-=", "*=", "/=", "="};
            for (String op : ops) {
                if (eat(op)) {
                    Expr e = or();
                    return switch (op) {
                        case "+=" -> (v, out) -> v[slot] += e.eval(v);
                        case "-=" -> (v, out) -> v[slot] -= e.eval(v);
                        case "*=" -> (v, out) -> v[slot] *= e.eval(v);
                        case "/=" -> (v, out) -> v[slot] /= e.eval(v);
                        default -> (v, out) -> v[slot] = e.eval(v);
                    };
                }
            }
            throw new IllegalStateException("Sentencia no válida: " + s);
        }

        Expr or() {
            Expr l = and();
            while (eat("||")) {
                Expr a = l, b = and();
                l = v -> bool(a.eval(v) != 0 || b.eval(v) != 0);
            }
            return l;
        }

        Expr and() {
            Expr l = cmp();
            while (eat("&&")) {
                Expr a = l, b = cmp();
                l = v -> bool(a.eval(v) != 0 && b.eval(v) != 0);
            }
            return l;
        }

        Expr cmp() {
            Expr a = add();
            String[] ops = {"==", "!=", "<=", ">=", "<", ">"};
            for (String op : ops) {
                if (peek(op) && !(op.equals(">") && false)) {
                    i += op.length();
                    Expr b = add();
                    Expr x = a;
                    return switch (op) {
                        case "==" -> v -> bool(x.eval(v) == b.eval(v));
                        case "!=" -> v -> bool(x.eval(v) != b.eval(v));
                        case "<=" -> v -> bool(x.eval(v) <= b.eval(v));
                        case ">=" -> v -> bool(x.eval(v) >= b.eval(v));
                        case "<" -> v -> bool(x.eval(v) < b.eval(v));
                        default -> v -> bool(x.eval(v) > b.eval(v));
                    };
                }
            }
            return a;
        }

        private boolean atBinaryMinus() {
            // "-" seguido de ">" o "'>" es una flecha, no una resta
            return peek("-") && !peek("->") && !peek("-'>") && !peek("-=");
        }

        Expr add() {
            Expr l = mul();
            while (true) {
                if (peek("+") && !peek("+=")) {
                    i++;
                    Expr a = l, b = mul();
                    l = v -> a.eval(v) + b.eval(v);
                } else if (atBinaryMinus()) {
                    i++;
                    Expr a = l, b = mul();
                    l = v -> a.eval(v) - b.eval(v);
                } else return l;
            }
        }

        private boolean startsPrimary() {
            ws();
            if (i >= s.length()) return false;
            char c = s.charAt(i);
            return c == '{' || c == '(' || Character.isDigit(c) || c == '.' || Character.isLetter(c);
        }

        Expr mul() {
            Expr l = unary();
            while (true) {
                if (peek("*") && !peek("*=")) {
                    i++;
                    Expr a = l, b = unary();
                    l = v -> a.eval(v) * b.eval(v);
                } else if (peek("/") && !peek("/=")) {
                    i++;
                    Expr a = l, b = unary();
                    l = v -> a.eval(v) / b.eval(v);
                } else if (peek("%")) {
                    i++;
                    Expr a = l, b = unary();
                    l = v -> a.eval(v) % b.eval(v);
                } else if (startsPrimary()) {
                    Expr a = l, b = pow();
                    l = v -> a.eval(v) * b.eval(v);
                } else return l;
            }
        }

        Expr unary() {
            if (atBinaryMinus()) {
                i++;
                Expr e = unary();
                return v -> -e.eval(v);
            }
            if (peek("!") && !peek("!=")) {
                i++;
                Expr e = unary();
                return v -> bool(e.eval(v) == 0);
            }
            return pow();
        }

        Expr pow() {
            Expr base = postfix();
            if (eat("^")) {
                Expr e = unary();
                return v -> Math.pow(base.eval(v), e.eval(v));
            }
            return base;
        }

        Expr postfix() {
            Expr e = primary();
            while (peek("'") && !peek("'>")) {
                i++;
                Expr x = e;
                e = v -> Math.toRadians(x.eval(v));
            }
            return e;
        }

        Expr primary() {
            ws();
            if (i >= s.length()) throw new IllegalStateException("Fin inesperado: " + s);
            char c = s.charAt(i);
            if (c == '(') {
                i++;
                Expr e = or();
                expect(")");
                return e;
            }
            if (c == '{') {
                int slot = script.slot(varName());
                return v -> v[slot];
            }
            if (Character.isDigit(c) || c == '.') {
                int start = i;
                while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
                if (i < s.length() && (s.charAt(i) == 'E' || s.charAt(i) == 'e')
                        && i + 1 < s.length() && (Character.isDigit(s.charAt(i + 1)) || s.charAt(i + 1) == '-')) {
                    i += 2;
                    while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
                }
                double d = Double.parseDouble(s.substring(start, i));
                return v -> d;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;
                String name = s.substring(start, i);
                switch (name) {
                    case "pi": if (peek("(") && s.startsWith("()", i)) i += 2; return v -> Math.PI;
                    case "true": return v -> 1;
                    case "false": return v -> 0;
                    default: break;
                }
                List<Expr> args = new ArrayList<>();
                if (peek("(")) {
                    expect("(");
                    if (!peek(")")) {
                        do args.add(or()); while (eat(","));
                    }
                    expect(")");
                } else {
                    args.add(postfix()); // función sin paréntesis: "curve {data}"
                }
                return Functions.create(name, args.toArray(new Expr[0]));
            }
            throw new IllegalStateException("Símbolo inesperado '" + c + "' en: " + s);
        }
    }
}
