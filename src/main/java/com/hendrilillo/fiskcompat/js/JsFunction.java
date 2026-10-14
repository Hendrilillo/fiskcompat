package com.hendrilillo.fiskcompat.js;

import org.openjdk.nashorn.api.scripting.AbstractJSObject;

/** Función global expuesta a los scripts, implementada en Java. */
public final class JsFunction extends AbstractJSObject {
    public interface Body {
        Object call(Object[] args) throws Exception;
    }

    private final String name;
    private final Body body;

    public JsFunction(String name, Body body) {
        this.name = name;
        this.body = body;
    }

    @Override
    public Object call(Object thiz, Object... args) {
        try {
            return body.call(args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(name + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isFunction() {
        return true;
    }

    @Override
    public String getClassName() {
        return "Function";
    }

    @Override
    public String toString() {
        return "function " + name + "() { [native code] }";
    }
}
