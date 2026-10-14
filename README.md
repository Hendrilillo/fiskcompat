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
- **Apuntar / repulsor**: mantén la tecla de apuntar (normalmente C) y haz clic derecho para disparar.
- **Unibeam**: pulsa y mantén su tecla (normalmente V) para cargar; dispara solo al completarse la carga.
- **Equipo del traje** (Alt izquierdo): con un objeto válido en la mano lo guarda en el traje (p. ej. TNT, pólvora o cargas de fuego para los cohetes del brazo de Sind); con la mano vacía saca el último.
- **Cohetes del brazo y similares** (`lightning_cast`): clic derecho sin apuntar y con la mano vacía cuando el traje lo permite.
- **Rayos y láseres** (energy_projection, heat_vision): mientras mantienes su tecla. Algunos trajes los usan con el clic izquierdo (entonces el clic no golpea).
- **Modo centinela** (en Sind, la habilidad 3 / B de los trajes que lo tienen): el traje se abre y te deja fuera; se queda como un centinela que te sigue y ataca a quien te ataque. Clic derecho sobre él: abrir/cerrar. Agachado + clic derecho: volar/aterrizar. Para volver a ponértelo, ábrelo y métete dentro.

## Sonidos

Como en Fisk, los sonidos no van dentro del heropack: el heropack dice de dónde descargarlos (`"sounds"` en su `heropack.json`). La primera vez que arranca el juego con un heropack nuevo, el mod los descarga de GitHub y los guarda en `.minecraft/fiskcompat/sounds` (los del Sind ocupan unos 6 MB y los de Fisk unos 25 MB). Las siguientes veces ya no hace falta internet. Si estás sin conexión, puedes poner el zip a mano en esa carpeta con el nombre `<dominio>-<versión>.zip` (por ejemplo `sind-18.zip`).

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
| Animaciones de modelos (`.fsk`) | ✅ mismo resultado que el intérprete de Fisk en los 254 archivos del Sind y de Fisk |
| Animaciones del cuerpo (pose de vuelo, etc.) | ✅ |
| Variables de datos sincronizadas cliente/servidor | ✅ |
| Scripts por tick (`setTickHandler`) y teclas de los scripts | ✅ |
| Teclas de habilidad (C, V, B, N, G) y máscara (M) con lista en pantalla | ✅ |
| Vuelo (doble salto), transformación, cooldown, escudo, inmunidades, atributos | ✅ |
| Apuntar, repulsores, unibeam, rayos de energía y láseres (daño y dibujo) | ✅ |
| Animaciones en primera persona (brazo al apuntar, las dos manos cuando la animación las usa) | ✅ |
| `lightning_cast` (cohetes del brazo), equipo guardado en el traje, bengalas | ✅ |
| Láseres que derriten hielo y funden bloques (si `mobGriefing` está activado) | ✅ |
| `entity.world()` para los scripts (bloques, entidades cercanas, bioma) | ✅ |
| Sonidos de los heropacks (descarga, eventos de sonido, bucles, fundidos, funciones `override`, `entity.playSound`) | ✅ |
| Modo centinela (`sentry_mode`): salir del traje, centinela que sigue y dispara, volver a entrar | ✅ |
| Partículas, HUD de Fisk | ⏳ |

## Desarrollo

- `gradle build` → `build/libs/`.
- `gradle test` carga los heropacks de `../testgame/heropacks` fuera del juego e imprime los errores y la API de Fisk que aún falta.
- `gradle runAutotest` (con `run/saves/test` creado) abre el juego, monta una escena con trajes, hace capturas en `run/screenshots` y se cierra. Se puede elegir qué trajes con `-Dsuits=sind:mark2,sind:mark3`; con `-Dmode=attacks` prueba los ataques.
- `docs/api/` tiene las firmas de la API JS de Fisk (para saber qué implementar) y `docs/sind-inventory.md` lo que usa el pack Sind.
