package com.hendrilillo.fiskcompat;

import com.google.gson.JsonObject;
import com.hendrilillo.fiskcompat.hero.HeroDefinition;
import com.hendrilillo.fiskcompat.hero.HeroRegistry;
import com.hendrilillo.fiskcompat.js.Unimplemented;
import com.hendrilillo.fiskcompat.pack.HeroPack;
import com.hendrilillo.fiskcompat.pack.HeroPackManager;
import com.hendrilillo.fiskcompat.render.HeroRendererScript;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga los heropacks de la carpeta de prueba (../testgame/heropacks) fuera de Minecraft y
 * ejecuta todos los scripts de héroes y renderers. Imprime un informe de errores y de la API que falta.
 */
public class PackLoadingTest {
    @Test
    public void loadAllPacks() {
        Path gameDir = Path.of(System.getProperty("fiskcompat.testGameDir"));
        HeroPackManager.load(gameDir);
        HeroRegistry.loadAll();
        // Las clases de rayos no dependen del render: se pueden probar aquí
        HeroRendererScript.propertyFactory = (type, r) -> new com.hendrilillo.fiskcompat.client.beam.PropertyBinding(type);
        HeroRendererScript.resourceFactory = (spec, r) -> {
            String type = spec.substring(0, spec.indexOf('|')), path = spec.substring(spec.indexOf('|') + 1);
            return switch (type) {
                case "BEAM_CONSTELLATION" -> new com.hendrilillo.fiskcompat.client.beam.BeamConstellation();
                case "BEAM_RENDERER" -> com.hendrilillo.fiskcompat.client.beam.BeamRendererDef.get(path);
                default -> null;
            };
        };

        String only = System.getProperty("fiskcompat.onlyDomain", "sind");
        Map<String, String> rendererErrors = new LinkedHashMap<>();
        List<String> ids = new ArrayList<>();
        for (HeroDefinition h : HeroRegistry.all()) {
            if (h.id().startsWith(only + ":")) ids.add(h.id());
        }
        for (HeroPack pack : HeroPackManager.packs()) {
            if (!pack.domain().equals(only) || !pack.info().has("alts")) continue;
            JsonObject alts = pack.info().getAsJsonObject("alts");
            for (String hero : alts.keySet()) {
                for (String alt : alts.getAsJsonObject(hero).keySet()) {
                    if (!alt.startsWith("$")) ids.add(hero + "/" + alt);
                }
            }
        }
        int ok = 0;
        for (String id : ids) {
            try {
                HeroRendererScript r = HeroRendererScript.load(id);
                ok++;
                if (id.equals(only + ":mark7") || id.equals(only + ":mark85")) {
                    System.out.println(id + " anims=" + r.customAnimations().keySet());
                    for (Object o : r.properties()) {
                        if (o instanceof com.hendrilillo.fiskcompat.client.beam.PropertyBinding pb && pb.constellation != null) {
                            var en = pb.constellation.entries.isEmpty() ? null : pb.constellation.entries.get(0);
                            System.out.println(id + " " + pb.type + " anchor=" + pb.anchor + " color=" + Integer.toHexString(pb.color.rgb)
                                    + " beam=" + (pb.beam == null ? null : pb.beam.id + "/" + pb.beam.aspects.size())
                                    + " entries=" + pb.constellation.entries.size()
                                    + (en == null ? "" : " off=" + java.util.Arrays.toString(en.offset()) + " fp=" + java.util.Arrays.toString(en.firstPerson()) + " size=" + java.util.Arrays.toString(en.size())));
                        }
                    }
                }
                if (id.equals(only + ":mark2")) {
                    System.out.println("mark2 layer1 = " + r.texturePath(r.textureKey(new com.hendrilillo.fiskcompat.js.Dummy("entity"), HeroRendererScript.Slot.CHESTPLATE)));
                    System.out.println("mark2 icon0 = " + r.itemIcon(0));
                }
            } catch (Throwable e) {
                rendererErrors.put(id, String.valueOf(e.getMessage()));
            }
        }

        System.out.println("\n===== INFORME =====");
        System.out.println("Héroes cargados: " + HeroRegistry.all().size() + "  errores: " + HeroRegistry.errors().size());
        HeroRegistry.errors().forEach((k, v) -> System.out.println("  [hero] " + k + ": " + v));
        System.out.println("Renderers " + only + ": " + ok + "/" + ids.size() + " ok");
        rendererErrors.forEach((k, v) -> System.out.println("  [renderer] " + k + ": " + v));
        System.out.println("API sin implementar usada:");
        Unimplemented.snapshot().forEach((k, v) -> System.out.println("  " + k + " x" + v));
        HeroPackManager.closeAll();
    }
}
