package com.hendrilillo.fiskcompat.js;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nashorn (modo ES5, igual que en Fisk) no entiende funciones flecha. Los heropacks las usan mucho,
 * así que, como hacía Fisk, se convierten a "function(...) {...}" antes de compilar.
 * Formas admitidas: (a, b) =&gt; ... y a =&gt; ...
 */
public final class ArrowFunctionConverter {
    private static final Pattern ARROW = Pattern.compile("(?:\\(\\s*((?:[\\w$]+\\s*(?:,\\s*[\\w$]+\\s*)*)?)\\)|(?<![\\w$.])([A-Za-z_$][\\w$]*))\\s*=>");

    private ArrowFunctionConverter() {}

    public static String convert(String src) {
        if (!src.contains("=>")) return src;
        int from = 0;
        while (true) {
            Matcher m = ARROW.matcher(src);
            int start = -1;
            while (m.find(from)) {
                if (isCode(src, m.start())) {
                    start = m.start();
                    break;
                }
                from = m.end();
            }
            if (start < 0) return src;
            String params = (m.group(1) != null ? m.group(1) : m.group(2)).trim();
            int after = m.end();
            int bodyStart = skipWs(src, after);
            if (bodyStart < src.length() && src.charAt(bodyStart) == '{') {
                src = src.substring(0, start) + "function(" + params + ") " + src.substring(bodyStart);
            } else {
                int end = expressionEnd(src, bodyStart);
                String expr = src.substring(bodyStart, end).trim();
                src = src.substring(0, start) + "function(" + params + ") { return " + expr + "; }" + src.substring(end);
            }
            from = start;
        }
    }

    private static int skipWs(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    /** Devuelve true si la posición no está dentro de un string o comentario. */
    private static boolean isCode(String s, int pos) {
        int i = 0;
        while (i < pos) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'' || c == '`') {
                i = skipString(s, i);
                if (i > pos) return false;
                continue;
            }
            if (c == '/' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == '/') {
                    int e = s.indexOf('\n', i);
                    if (e < 0) e = s.length();
                    if (e > pos) return false;
                    i = e;
                    continue;
                }
                if (n == '*') {
                    int e = s.indexOf("*/", i + 2);
                    e = e < 0 ? s.length() : e + 2;
                    if (e > pos) return false;
                    i = e;
                    continue;
                }
            }
            i++;
        }
        return true;
    }

    private static int skipString(String s, int i) {
        char q = s.charAt(i++);
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            i++;
            if (c == q) break;
        }
        return i;
    }

    private static boolean isOperatorChar(char c) {
        return "+-*/%&|^!=<>?:.,(".indexOf(c) >= 0;
    }

    /** Fin de una expresión que actúa de cuerpo de una flecha. */
    private static int expressionEnd(String s, int i) {
        int depth = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'' || c == '`') {
                i = skipString(s, i);
                continue;
            }
            if (c == '/' && i + 1 < s.length() && (s.charAt(i + 1) == '/' || s.charAt(i + 1) == '*')) {
                if (depth == 0) return i;
                if (s.charAt(i + 1) == '/') {
                    int e = s.indexOf('\n', i);
                    i = e < 0 ? s.length() : e;
                } else {
                    int e = s.indexOf("*/", i + 2);
                    i = e < 0 ? s.length() : e + 2;
                }
                continue;
            }
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') {
                if (depth == 0) return i;
                depth--;
            } else if (depth == 0 && (c == ',' || c == ';')) {
                return i;
            } else if (depth == 0 && c == '\n') {
                int prev = i - 1;
                while (prev >= 0 && Character.isWhitespace(s.charAt(prev))) prev--;
                int next = skipWs(s, i);
                boolean continues = (prev >= 0 && isOperatorChar(s.charAt(prev)))
                        || (next < s.length() && isOperatorChar(s.charAt(next)) && s.charAt(next) != '(');
                if (!continues) return i;
            }
            i++;
        }
        return i;
    }
}
