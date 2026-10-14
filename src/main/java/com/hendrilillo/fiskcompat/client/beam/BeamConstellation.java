package com.hendrilillo.fiskcompat.client.beam;

import com.hendrilillo.fiskcompat.js.Dummy;
import com.hendrilillo.fiskcompat.js.JsFunction;
import org.openjdk.nashorn.api.scripting.AbstractJSObject;
import org.openjdk.nashorn.api.scripting.JSObject;

import java.util.ArrayList;
import java.util.List;

/** createResource("BEAM_CONSTELLATION"): los puntos de los que sale un rayo. */
public final class BeamConstellation extends AbstractJSObject {
    /** offset/firstPerson en píxeles (como Fisk), size = ancho y alto del rayo en píxeles. */
    public record Entry(float[] offset, float[] firstPerson, float[] size, String anchor) {}

    public final List<Entry> entries = new ArrayList<>();

    @Override
    public Object getMember(String name) {
        if (name.equals("bindBeam")) return new JsFunction(name, a -> {
            if (a.length > 0 && a[0] instanceof JSObject o) {
                entries.add(new Entry(vec(o, "offset", 3), vec(o, "firstPerson", 3), vec(o, "size", 2),
                        o.hasMember("anchor") && o.getMember("anchor") != null ? String.valueOf(o.getMember("anchor")) : null));
            }
            return this;
        });
        return new Dummy("constellation." + name);
    }

    private static float[] vec(JSObject o, String key, int n) {
        if (!o.hasMember(key)) return null;
        Object v = o.getMember(key);
        if (!(v instanceof JSObject arr)) return null;
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            Object x = arr.getSlot(i);
            out[i] = x instanceof Number num ? num.floatValue() : 0;
        }
        return out;
    }
}
