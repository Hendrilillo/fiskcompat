# FiskCompat

Mod para **Minecraft 1.21.1 (NeoForge)** que carga **heropacks de Fisk's Superheroes** (hechos para la 1.7.10) sin modificarlos.

> Proyecto no oficial y sin relación con FiskFille ni con los autores de los heropacks. No incluye ningún heropack ni código de Fisk's Superheroes.

## Instalación

1. Instala NeoForge para 1.21.1 y pon `fiskcompat-x.y.z.jar` en la carpeta `mods`.
2. Arranca el juego una vez: se crea la carpeta `.minecraft/heropacks`.
3. Pon en **`.minecraft/heropacks`** (¡no en `mods`!):
   - el heropack que quieras (por ejemplo `sind2.2.0.zip`), y
   - el jar original de Fisk's Superheroes (`FiskHeroes-1.7.10-2.4.0_1.jar`). Muchos heropacks usan sus scripts y texturas base; el mod solo los lee.
4. Los trajes salen en la pestaña creativa **"FiskCompat - Heropacks"**.

## Controles

- **Volar**: doble salto en el aire. Adelante vuela hacia donde miras, correr = turbo, salto/agacharse = subir/bajar. Al tocar el suelo aterrizas.
- **Habilidades 1-5**: C, V, B, N, G (se pueden cambiar en Controles). Arriba a la izquierda sale qué hace cada una con el traje puesto.
- **Máscara**: M.

## Estado

| Parte | Estado |
|---|---|
| Cargar heropacks (zip, jar, carpeta) | ✅ |
| Scripts de héroes (`data/heroes/*.js`) | ✅ 149/149 del Sind + Fisk cargan |
| Scripts de renderers (`renderers/heroes/*.js`) | ✅ cargan; efectos parciales |
| Armaduras 64x64 de dos capas, luces | ✅ |
| Texturas dinámicas `.tx.json` (XOR/OR/STITCH) | ✅ |
| Casco que se abre (`opening_mask`), `overlay` | ✅ |
| Iconos de inventario | ✅ |
| Brazo en primera persona | ✅ |
| Modelos Tabula (`.tbl`, formatos nuevo y antiguo) | ✅ |
| Animaciones de modelos (`.fsk`) | ✅ 162/162 compilan |
| Animaciones del cuerpo (pose de vuelo, etc.) | ✅ |
| Variables de datos sincronizadas cliente/servidor | ✅ |
| Scripts por tick (`setTickHandler`) y teclas de los scripts | ✅ |
| Teclas de habilidad (C, V, B, N, G) y máscara (M) con lista en pantalla | ✅ |
| Vuelo (doble salto), transformación, cooldown, escudo, inmunidades, atributos | ✅ |
| Repulsores, rayos, unibeam, cohetes... | ⏳ |
| Sonidos, HUD, partículas | ⏳ |

## Desarrollo

- `gradle build` → `build/libs/`.
- `gradle test` carga los heropacks de `../testgame/heropacks` fuera del juego e imprime los errores y la API de Fisk que aún falta.
- `gradle runAutotest` (con `run/saves/test` creado) abre el juego, monta una escena con trajes, hace capturas en `run/screenshots` y se cierra. Se puede elegir qué trajes con `-Dsuits=sind:mark2,sind:mark3`.
- `docs/api/` tiene las firmas de la API JS de Fisk (para saber qué implementar) y `docs/sind-inventory.md` lo que usa el pack Sind.
