package com.hendrilillo.fiskcompat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hendrilillo.fiskcompat.FiskCompat;
import com.hendrilillo.fiskcompat.js.ArrowFunctionConverter;
import com.hendrilillo.fiskcompat.js.ScriptEngines;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Texturas dinámicas de Fisk (.tx.json): eligen la imagen según el estado de la entidad
 * ("mask_&lt;OPEN&gt;" -> "mask_true"/"mask_false") y pueden generar imágenes nuevas combinando
 * otras (XOR = borrar, OR = superponer, STITCH = recolocar trozos...).
 */
public final class TxTextures {
    private static final Map<String, Optional<Tx>> DEFINITIONS = new HashMap<>();
    private static final Map<String, Optional<ResourceLocation>> GENERATED = new HashMap<>();

    private TxTextures() {}

    public static void clear() {
        DEFINITIONS.clear();
        for (Optional<ResourceLocation> rl : GENERATED.values()) {
            rl.ifPresent(r -> Minecraft.getInstance().getTextureManager().release(r));
        }
        GENERATED.clear();
    }

    /** Resuelve una textura .tx.json para una entidad. fiskPath es "ns:ruta/archivo.tx.json". */
    public static ResourceLocation resolve(String fiskPath, Object entity) {
        Tx tx = DEFINITIONS.computeIfAbsent(fiskPath, TxTextures::load).orElse(null);
        if (tx == null) return null;
        Map<String, String> vars = new HashMap<>();
        for (Map.Entry<String, Getter> g : tx.getters.entrySet()) {
            vars.put(g.getKey(), g.getValue().eval(entity));
        }
        if (tx.texture != null) {
            String path = substitute(tx.texture, vars);
            return ClientHeroes.heroTexture(path, entity);
        }
        String out = substitute(tx.out, vars);
        return GENERATED.computeIfAbsent(out, o -> generate(tx, vars, o)).orElse(null);
    }

    private static String substitute(String s, Map<String, String> vars) {
        for (Map.Entry<String, String> e : vars.entrySet()) s = s.replace("<" + e.getKey() + ">", e.getValue());
        return s;
    }

    private static Optional<Tx> load(String fiskPath) {
        int c = fiskPath.indexOf(':');
        String ns = c < 0 ? "fiskheroes" : fiskPath.substring(0, c);
        String path = c < 0 ? fiskPath : fiskPath.substring(c + 1);
        ResourceLocation rl = ResourceLocation.tryBuild(ns, "textures/heroes/" + path);
        if (rl == null) return Optional.empty();
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isEmpty()) return Optional.empty();
            JsonObject json;
            try (InputStream in = res.get().open()) {
                json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            Tx tx = new Tx();
            JsonElement t = json.get("texture");
            if (t.isJsonPrimitive()) {
                tx.texture = t.getAsString();
            } else {
                JsonObject o = t.getAsJsonObject();
                tx.in = o.get("in").getAsString();
                tx.out = o.has("out") ? o.get("out").getAsString() : fiskPath + "#out";
                if (o.has("transform")) {
                    for (JsonElement e : o.getAsJsonArray("transform")) {
                        JsonObject op = e.getAsJsonObject();
                        tx.ops.add(new String[]{op.get("operation").getAsString(), op.has("supply") ? op.get("supply").getAsString() : ""});
                    }
                }
            }
            if (json.has("getters")) {
                JsonObject getters = json.getAsJsonObject("getters");
                for (String key : getters.keySet()) {
                    tx.getters.put(key, new Getter(getters.getAsJsonObject(key)));
                }
            }
            return Optional.of(tx);
        } catch (Exception e) {
            FiskCompat.LOGGER.error("Error leyendo la textura dinámica {}: {}", fiskPath, e.toString());
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------ generación de imágenes

    private static Optional<ResourceLocation> generate(Tx tx, Map<String, String> vars, String out) {
        NativeImage image = readHeroImage(substitute(tx.in, vars));
        if (image == null) return Optional.empty();
        try {
            for (String[] op : tx.ops) {
                String supply = substitute(op[1], vars);
                image = switch (op[0]) {
                    case "STITCH" -> stitch(image, supply);
                    case "XOR", "OR", "AND", "MULTIPLY", "ADD" -> combine(image, readHeroImage(supply), op[0]);
                    default -> image;
                };
            }
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(FiskCompat.MODID,
                    "generated/" + out.replace(':', '/').toLowerCase().replaceAll("[^a-z0-9/._-]", "_"));
            Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(image));
            return Optional.of(rl);
        } catch (Exception e) {
            FiskCompat.LOGGER.error("Error generando la textura {}: {}", out, e.toString());
            return Optional.empty();
        }
    }

    private static NativeImage readHeroImage(String fiskPath) {
        if (fiskPath == null) return null;
        int c = fiskPath.indexOf(':');
        String ns = c < 0 ? "fiskheroes" : fiskPath.substring(0, c);
        String path = c < 0 ? fiskPath : fiskPath.substring(c + 1);
        ResourceLocation rl = ResourceLocation.tryBuild(ns, "textures/heroes/" + path + ".png");
        if (rl == null) return null;
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
        if (res.isEmpty()) return null;
        try (InputStream in = res.get().open()) {
            return NativeImage.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static int alpha(int abgr) {
        return (abgr >>> 24) & 0xFF;
    }

    /** Operaciones píxel a píxel; la imagen "supply" se escala al tamaño de la de entrada. */
    private static NativeImage combine(NativeImage in, NativeImage supply, String op) {
        if (supply == null) return in;
        int w = in.getWidth(), h = in.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = in.getPixelRGBA(x, y);
                int b = supply.getPixelRGBA(x * supply.getWidth() / w, y * supply.getHeight() / h);
                int r = switch (op) {
                    case "XOR" -> alpha(b) > 0 ? 0 : a;
                    case "OR" -> alpha(b) > 0 ? b : a;
                    case "AND" -> (a & 0xFF000000) | (b & 0x00FFFFFF);
                    case "MULTIPLY" -> channels(a, b, true);
                    case "ADD" -> channels(a, b, false);
                    default -> a;
                };
                in.setPixelRGBA(x, y, r);
            }
        }
        supply.close();
        return in;
    }

    private static int channels(int a, int b, boolean multiply) {
        int out = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            double ca = ((a >>> shift) & 0xFF) / 255.0, cb = ((b >>> shift) & 0xFF) / 255.0;
            double v = multiply ? ca * cb : Math.min(ca + cb, 1);
            if (!multiply && shift == 24) v = ca;
            out |= ((int) Math.round(v * 255) & 0xFF) << shift;
        }
        return out;
    }

    /** STITCH: copia trozos de la imagen a otras posiciones según textures/stitchers/&lt;nombre&gt;.json. */
    private static NativeImage stitch(NativeImage in, String stitcher) throws Exception {
        int c = stitcher.indexOf(':');
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(c < 0 ? "fiskheroes" : stitcher.substring(0, c),
                "textures/stitchers/" + (c < 0 ? stitcher : stitcher.substring(c + 1)) + ".json");
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
        if (res.isEmpty()) return in;
        JsonObject json;
        try (InputStream is = res.get().open()) {
            json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonArray dims = json.getAsJsonArray("dimensions");
        int scale = Math.max(1, in.getWidth() / 64);
        NativeImage out = new NativeImage(dims.get(0).getAsInt() * scale, dims.get(1).getAsInt() * scale, true);
        for (JsonElement e : json.getAsJsonArray("stitch")) {
            JsonObject s = e.getAsJsonObject();
            int sw = s.getAsJsonArray("size").get(0).getAsInt() * scale, sh = s.getAsJsonArray("size").get(1).getAsInt() * scale;
            int fx = s.getAsJsonArray("from").get(0).getAsInt() * scale, fy = s.getAsJsonArray("from").get(1).getAsInt() * scale;
            int tx = s.getAsJsonArray("to").get(0).getAsInt() * scale, ty = s.getAsJsonArray("to").get(1).getAsInt() * scale;
            for (int y = 0; y < sh; y++) {
                for (int x = 0; x < sw; x++) {
                    if (fx + x >= in.getWidth() || fy + y >= in.getHeight() || tx + x >= out.getWidth() || ty + y >= out.getHeight()) continue;
                    out.setPixelRGBA(tx + x, ty + y, in.getPixelRGBA(fx + x, fy + y));
                }
            }
        }
        in.close();
        return out;
    }

    // ------------------------------------------------------------------ definiciones

    private static final class Tx {
        String texture;
        String in;
        String out;
        final List<String[]> ops = new ArrayList<>();
        final Map<String, Getter> getters = new LinkedHashMap<>();
    }

    private static final class Getter {
        private final ScriptObjectMirror func;
        private final Integer min, max;

        Getter(JsonObject json) throws Exception {
            String expr = ArrowFunctionConverter.convert(json.get("key").getAsString());
            ScriptObjectMirror global = ScriptEngines.newGlobal();
            ScriptEngines.eval("function $get(entity) { return (" + expr + "); }", "tx-getter", global);
            func = (ScriptObjectMirror) global.get("$get");
            JsonElement values = json.get("values");
            if (values != null && values.isJsonObject()) {
                min = values.getAsJsonObject().get("min").getAsInt();
                max = values.getAsJsonObject().get("max").getAsInt();
            } else {
                min = max = null;
            }
        }

        String eval(Object entity) {
            Object v;
            try {
                v = func.call(null, entity);
            } catch (Exception e) {
                v = 0;
            }
            if (v instanceof Number n) {
                double d = n.doubleValue();
                long l = (long) d;
                if (min != null) l = Math.max(min, Math.min(max, l));
                return d == Math.floor(d) || min != null ? Long.toString(l) : Double.toString(d);
            }
            return String.valueOf(v);
        }
    }
}
