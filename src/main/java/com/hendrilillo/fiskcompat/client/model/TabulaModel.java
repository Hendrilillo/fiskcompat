package com.hendrilillo.fiskcompat.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hendrilillo.fiskcompat.FiskCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Modelo de Tabula (.tbl) cargado desde models/tabula/. Soporta el formato nuevo (projVersion 5,
 * "parts" con "boxes") y el antiguo (projVersion 2, "cubes"). Las coordenadas quedan en el espacio
 * de la parte del cuerpo a la que se ancla el modelo, igual que en Fisk.
 */
public final class TabulaModel {
    private static final Map<String, Optional<TabulaModel>> CACHE = new ConcurrentHashMap<>();
    private static final EnumSet<Direction> ALL_FACES = EnumSet.allOf(Direction.class);

    public final List<ModelPart> roots = new ArrayList<>();
    /** Partes por nombre (puede haber varias con el mismo nombre). */
    public final Map<String, List<ModelPart>> parts = new LinkedHashMap<>();
    public final Map<ModelPart, PartPose> defaults = new HashMap<>();
    public int texWidth = 64, texHeight = 32;

    public static void clearCache() {
        CACHE.clear();
    }

    /** "sind:mk3/shield" -> sind:models/tabula/mk3/shield.tbl */
    public static TabulaModel get(String id) {
        return CACHE.computeIfAbsent(id, TabulaModel::load).orElse(null);
    }

    private static Optional<TabulaModel> load(String id) {
        int c = id.indexOf(':');
        ResourceLocation rl = ResourceLocation.tryBuild(c < 0 ? "fiskheroes" : id.substring(0, c),
                "models/tabula/" + (c < 0 ? id : id.substring(c + 1)) + ".tbl");
        if (rl == null) return Optional.empty();
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
        if (res.isEmpty()) {
            FiskCompat.LOGGER.warn("Modelo Tabula no encontrado: {}", rl);
            return Optional.empty();
        }
        try (ZipInputStream zip = new ZipInputStream(res.get().open())) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (e.getName().equals("model.json")) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(zip, StandardCharsets.UTF_8)).getAsJsonObject();
                    TabulaModel m = new TabulaModel();
                    if (json.has("parts")) m.readV5(json);
                    else m.readV2(json);
                    return Optional.of(m);
                }
            }
        } catch (Exception ex) {
            FiskCompat.LOGGER.error("Error leyendo el modelo {}: {}", rl, ex.toString());
        }
        return Optional.empty();
    }

    private static float f(JsonObject o, String k, float def) {
        return o.has(k) ? o.get(k).getAsFloat() : def;
    }

    private void register(String name, ModelPart part, PartPose pose) {
        part.setInitialPose(pose);
        part.loadPose(pose);
        defaults.put(part, pose);
        parts.computeIfAbsent(name, k -> new ArrayList<>()).add(part);
    }

    // ------------------------------------------------------------------ formato nuevo (v5)

    private void readV5(JsonObject json) {
        texWidth = json.get("texWidth").getAsInt();
        texHeight = json.get("texHeight").getAsInt();
        for (JsonElement p : json.getAsJsonArray("parts")) roots.add(partV5(p.getAsJsonObject()));
    }

    private ModelPart partV5(JsonObject o) {
        float tw = texWidth, th = texHeight;
        if (o.has("matchProject") && !o.get("matchProject").getAsBoolean()) {
            tw = f(o, "texWidth", tw);
            th = f(o, "texHeight", th);
        }
        int pu = o.has("texOffX") ? o.get("texOffX").getAsInt() : 0;
        int pv = o.has("texOffY") ? o.get("texOffY").getAsInt() : 0;
        boolean mirror = o.has("mirror") && o.get("mirror").getAsBoolean();
        List<ModelPart.Cube> cubes = new ArrayList<>();
        for (JsonElement be : o.getAsJsonArray("boxes")) {
            JsonObject b = be.getAsJsonObject();
            cubes.add(new ModelPart.Cube(pu + b.get("texOffX").getAsInt(), pv + b.get("texOffY").getAsInt(),
                    f(b, "posX", 0), f(b, "posY", 0), f(b, "posZ", 0),
                    f(b, "dimX", 0), f(b, "dimY", 0), f(b, "dimZ", 0),
                    f(b, "expandX", 0), f(b, "expandY", 0), f(b, "expandZ", 0),
                    mirror, tw, th, ALL_FACES));
        }
        Map<String, ModelPart> children = new LinkedHashMap<>();
        int i = 0;
        for (JsonElement ce : o.getAsJsonArray("children")) {
            children.put("c" + i++, partV5(ce.getAsJsonObject()));
        }
        ModelPart part = new ModelPart(cubes, children);
        part.visible = !o.has("showModel") || o.get("showModel").getAsBoolean();
        PartPose pose = PartPose.offsetAndRotation(f(o, "rotPX", 0), f(o, "rotPY", 0), f(o, "rotPZ", 0),
                (float) Math.toRadians(f(o, "rotAX", 0)), (float) Math.toRadians(f(o, "rotAY", 0)), (float) Math.toRadians(f(o, "rotAZ", 0)));
        register(o.has("name") ? o.get("name").getAsString() : "", part, pose);
        return part;
    }

    // ------------------------------------------------------------------ formato antiguo (v2)

    private void readV2(JsonObject json) {
        texWidth = json.get("textureWidth").getAsInt();
        texHeight = json.get("textureHeight").getAsInt();
        if (json.has("cubes")) for (JsonElement c : json.getAsJsonArray("cubes")) roots.add(cubeV2(c.getAsJsonObject()));
        if (json.has("cubeGroups")) for (JsonElement g : json.getAsJsonArray("cubeGroups")) groupV2(g.getAsJsonObject());
    }

    private void groupV2(JsonObject g) {
        if (g.has("cubes")) for (JsonElement c : g.getAsJsonArray("cubes")) roots.add(cubeV2(c.getAsJsonObject()));
        if (g.has("cubeGroups")) for (JsonElement c : g.getAsJsonArray("cubeGroups")) groupV2(c.getAsJsonObject());
    }

    private static float[] arr(JsonObject o, String k) {
        JsonArray a = o.getAsJsonArray(k);
        float[] r = new float[a.size()];
        for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
        return r;
    }

    private ModelPart cubeV2(JsonObject o) {
        float[] dim = arr(o, "dimensions"), pos = arr(o, "position"), off = arr(o, "offset"), rot = arr(o, "rotation");
        float[] tx = o.has("txOffset") ? arr(o, "txOffset") : new float[]{0, 0};
        float grow = f(o, "mcScale", 0);
        boolean mirror = o.has("txMirror") && o.get("txMirror").getAsBoolean();
        List<ModelPart.Cube> cubes = new ArrayList<>();
        if (dim[0] != 0 || dim[1] != 0 || dim[2] != 0) {
            cubes.add(new ModelPart.Cube((int) tx[0], (int) tx[1], off[0], off[1], off[2], dim[0], dim[1], dim[2],
                    grow, grow, grow, mirror, texWidth, texHeight, ALL_FACES));
        }
        Map<String, ModelPart> children = new LinkedHashMap<>();
        int i = 0;
        if (o.has("children")) for (JsonElement ce : o.getAsJsonArray("children")) children.put("c" + i++, cubeV2(ce.getAsJsonObject()));
        ModelPart part = new ModelPart(cubes, children);
        part.visible = !(o.has("hidden") && o.get("hidden").getAsBoolean());
        if (o.has("scale")) {
            float[] sc = arr(o, "scale");
            part.xScale = sc[0];
            part.yScale = sc[1];
            part.zScale = sc[2];
        }
        PartPose pose = PartPose.offsetAndRotation(pos[0], pos[1], pos[2],
                (float) Math.toRadians(rot[0]), (float) Math.toRadians(rot[1]), (float) Math.toRadians(rot[2]));
        register(o.get("name").getAsString(), part, pose);
        return part;
    }

    /** Vuelve todas las partes a su pose inicial. */
    public void reset() {
        defaults.forEach(ModelPart::loadPose);
    }
}
