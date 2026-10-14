package com.hendrilillo.fiskcompat.client.beam;

import com.hendrilillo.fiskcompat.js.Dummy;
import com.hendrilillo.fiskcompat.js.JsFunction;
import org.openjdk.nashorn.api.scripting.AbstractJSObject;
import org.openjdk.nashorn.api.scripting.JSObject;

import java.util.HashMap;
import java.util.Map;

/**
 * renderer.bindProperty("fiskheroes:heat_vision"...) — lo que Fisk llama "render prop".
 * De momento se dibujan los rayos (charged_beam, energy_projection, heat_vision, repulsor_blast);
 * el resto de métodos se aceptan sin hacer nada.
 */
public final class PropertyBinding extends AbstractJSObject {
    public final String type;
    public String anchor;
    public BeamConstellation constellation;
    public BeamRendererDef beam;
    public final ColorRef color = new ColorRef();
    private Object condition;
    private final Map<String, Object> members = new HashMap<>();

    public PropertyBinding(String type) {
        this.type = type;
    }

    public boolean test(Object jsEntity) {
        if (condition instanceof JSObject f && f.isFunction()) {
            try {
                Object ok = f.call(null, jsEntity);
                return ok instanceof Boolean b ? b : ok instanceof Number n && n.doubleValue() != 0;
            } catch (Throwable t) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object getMember(String name) {
        switch (name) {
            case "color": return color;
            case "setAnchor": return new JsFunction(name, a -> { anchor = a.length > 0 && a[0] != null ? String.valueOf(a[0]) : null; return this; });
            case "setConstellation": return new JsFunction(name, a -> { if (a.length > 0 && a[0] instanceof BeamConstellation c) constellation = c; return this; });
            case "setRenderer": return new JsFunction(name, a -> { if (a.length > 0 && a[0] instanceof BeamRendererDef b) beam = b; return this; });
            case "setCondition": return new JsFunction(name, a -> { condition = a.length > 0 ? a[0] : null; return this; });
            default:
                Object v = members.get(name);
                if (v != null) return v;
                if (name.startsWith("set") || name.startsWith("add") || name.startsWith("bind")) {
                    return new JsFunction(name, a -> this);
                }
                Dummy d = new Dummy("property[" + type + "]." + name);
                members.put(name, d);
                return d;
        }
    }

    @Override
    public void setMember(String name, Object value) {
        members.put(name, value);
    }

    @Override
    public boolean hasMember(String name) {
        return true;
    }

    /** prop.color.set(0xRRGGBB) */
    public static final class ColorRef extends AbstractJSObject {
        public int rgb = 0xFFFFFF;

        @Override
        public Object getMember(String name) {
            if (name.equals("set")) return new JsFunction("set", a -> {
                if (a.length >= 3 && a[0] instanceof Number r && a[1] instanceof Number g && a[2] instanceof Number b) {
                    rgb = (int) (r.doubleValue() * 255) << 16 | (int) (g.doubleValue() * 255) << 8 | (int) (b.doubleValue() * 255);
                } else if (a.length > 0 && a[0] instanceof Number n) {
                    rgb = (int) n.longValue() & 0xFFFFFF;
                }
                return null;
            });
            return new Dummy("color." + name);
        }
    }
}
