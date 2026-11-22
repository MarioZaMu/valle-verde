# Recursos gráficos y licencias — Valle Verde

> **Versión 2.0 · Fase 5.7c · HU-42, HU-44**

## 1. Qué se usa

Todos los sprites son **trabajo propio**: pixel-art dibujado por el equipo con el script [`tools/gen_sprites.py`](../tools/gen_sprites.py) (Pillow, semilla fija, salida idéntica en cada ejecución). **Ningún sprite viene de otro juego** (en particular, ninguno de Stardew Valley, cuyos gráficos tienen copyright de su autor): solo se tomó como referencia el *estilo* de granja pixel-art con paleta cálida.

- Cada imagen mide **32 × 32 px**; los edificios son hojas de tiles enteros (Gallinero y Almacén 64 × 64, Establo 96 × 64, Invernadero 96 × 96).
- El mapa se amplía con un **factor entero** (×2 = 64 px por parcela a 1024 × 640) y con interpolación de vecino más cercano, para que los píxeles queden nítidos; si no cabe un factor entero sin desperdiciar más de un cuarto del espacio, se usa la escala que cabe, también sin suavizar.
- Si falta cualquier imagen, el `AssetManager` lo registra y se dibuja el gráfico vectorial de respaldo (HU-42): el juego sigue siendo completo sin ninguna imagen.

## 2. Paquetes de imágenes y licencia (HU-42 · CA-4, HU-44 · CA-5)

Todo paquete debe constar aquí **en el mismo commit** que lo agrega. Solo se aceptan obra propia, **CC0** o **CC-BY** con su atribución. La prueba `HU42_ca4_everyImagePackageInTheResourcesIsListedInAssetsMd` falla si hay un archivo en `assets/` cuya carpeta no aparece en la tabla.

| Carpeta | Contenido | Autor y fuente | Licencia |
|---|---|---|---|
| `terrain/` | hierba de las 4 estaciones, tierra seca y regada, agua, árbol | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `crops/` | 5 cultivos × 5 etapas (semilla, brote, creciendo, maduro, marchito) | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `animals/` | gallina y vaca, 3 cuadros de animación cada una | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `buildings/` | Gallinero, Establo, Almacén e Invernadero | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `items/` | iconos de semillas, cosechas, huevo, leche, heno y medicina | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `characters/` | avatar (4 direcciones × 3 cuadros) y 4 personajes (Marta, Don Tomás, Dra. Lucía, Inés) | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `icons/` | herramientas, monedas, energía, estaciones y clima | trabajo propio, `tools/gen_sprites.py` | CC0 |

## 3. Nombres que lee el `AssetManager`

| Qué | Nombre | Ejemplo |
|---|---|---|
| Terreno | `terrain/<TERRENO>.png`, `terrain/GRASS_<ESTACIÓN>.png` | `terrain/SOIL_WATERED.png` |
| Cultivo | `crops/<CULTIVO>_<ETAPA>.png` | `crops/CARROT_MATURE.png` |
| Animal | `animals/<ANIMAL>_<CUADRO>.png` | `animals/HEN_1.png` |
| Edificio | `buildings/<EDIFICIO>.png` | `buildings/BARN.png` |
| Ítem | `items/<ÍTEM>.png` | `items/CARROT_SEED.png` |
| Personaje | `characters/AVATAR_<DIRECCIÓN>_<CUADRO>.png`, `characters/<NPC>.png` | `characters/MARTA.png` |
| Icono | `icons/<ID>.png` | `icons/RAINY.png` |

Para regenerar o comprobar: `python tools/gen_sprites.py` y `python tools/gen_sprites.py --check`.
