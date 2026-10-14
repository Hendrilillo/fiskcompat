package com.hendrilillo.fiskcompat.js;

import org.openjdk.nashorn.api.scripting.AbstractJSObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Objeto "comodín" para partes de la API de Fisk que aún no están implementadas.
 * Acepta cualquier propiedad o llamada sin romper el script, y recuerda los valores asignados.
 * Las llamadas quedan registradas para saber qué falta por implementar.
 */
public class Dummy extends AbstractJSObject {
    private final String path;
    private final Map<String, Object> members = new HashMap<>();

    public Dummy(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    @Override
    public Object getMember(String name) {
        Object v = members.get(name);
        if (v != null) return v;
        if (name.equals("toString") || name.equals("valueOf")) return new JsFunction(name, a -> path);
        Dummy child = new Dummy(path + "." + name);
        members.put(name, child);
        return child;
    }

    @Override
    public void setMember(String name, Object value) {
        members.put(name, value);
    }

    @Override
    public boolean hasMember(String name) {
        return true;
    }

    @Override
    public Set<String> keySet() {
        return members.keySet();
    }

    @Override
    public Object call(Object thiz, Object... args) {
        Unimplemented.report(path + "()");
        return new Dummy(path + "()");
    }

    @Override
    public Object newObject(Object... args) {
        return new Dummy("new " + path);
    }

    @Override
    public boolean isFunction() {
        return true;
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == String.class) return "";
        return 0;
    }

    @Override
    public String toString() {
        return "[stub " + path + "]";
    }
}
