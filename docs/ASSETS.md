# Recursos gráficos y licencias — Valle Verde

> **Versión 2.2 · Fase 5.7g (el valle) · HU-42, HU-44, HU-48, HU-50**

## 1. Qué se usa

Todos los sprites son **trabajo propio**: pixel-art dibujado por el equipo con el script [`tools/gen_sprites.py`](../tools/gen_sprites.py) (Pillow, semilla fija, salida idéntica en cada ejecución). **Ningún sprite viene de otro juego** (en particular, ninguno de Stardew Valley, cuyos gráficos tienen copyright de su autor): solo se tomó como referencia el *estilo* de granja pixel-art con paleta cálida.

- Cada imagen mide **32 × 32 px**, salvo las que se elevan sobre su parcela (vista 3/4), que miden tiles enteros: árboles y pinos 64 × 64 (dos tiles de alto), Gallinero y Almacén 64 × 96, Establo 96 × 96 e Invernadero 96 × 128 (un tile de tejado sobre el terreno que ocupan).
- Hierba, tierra y agua **encajan sin costura** (su textura se repite cada 32 px). El renderizador pinta un suelo continuo: donde la tierra o el agua tocan la hierba, la hierba invade el borde con un perfil irregular y proyecta una sombra corta, así que no se ven cuadros ni líneas de cuadrícula.
- El mapa se amplía con un **factor entero** (×2 = 64 px por parcela a 1024 × 640). Si no cabe un factor entero, cada sprite se amplía primero por el entero inmediato superior con vecino más cercano y luego se ajusta a su tamaño final (*escalado nítido*), de modo que los píxeles siguen siendo cuadrados del mismo tamaño también con la escala de pantalla de Windows (125 %, 150 %).
- Si falta cualquier imagen, el `GestorRecurso` lo registra y se dibuja el gráfico vectorial de respaldo (HU-42): el juego sigue siendo completo sin ninguna imagen.

## 2. Paquetes de imágenes y licencia (HU-42 · CA-4, HU-44 · CA-5)

Todo paquete debe constar aquí **en el mismo commit** que lo agrega. Solo se aceptan obra propia, **CC0** o **CC-BY** con su atribución. La prueba `HU42_ca4_everyImagePackageInTheResourcesIsListedInAssetsMd` falla si hay un archivo en `assets/` cuya carpeta no aparece en la tabla.

| Carpeta | Contenido | Autor y fuente | Licencia |
|---|---|---|---|
| `terrain/` | hierba de las 4 estaciones (4 variantes: lisa, matas, flores u hojas, piedritas), tierra seca y regada, agua (3 cuadros), roca, roble y pino de las 4 estaciones, arbusto, camino de tierra | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `crops/` | 5 cultivos × 5 etapas (semilla, brote, creciendo, maduro, marchito) | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `animals/` | gallina y vaca, 3 cuadros de animación cada una | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `buildings/` | Gallinero, Establo, Almacén e Invernadero | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `items/` | iconos de semillas, cosechas, huevo, leche, heno y medicina | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `characters/` | avatar (4 direcciones × 3 cuadros) y 4 personajes (Marta, Don Tomás, Dra. Lucía, Inés), de 32 × 64 (una parcela de ancho y dos de alto) | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `village/` | casas del valle: tu casa, la tienda de Marta, la casa de Don Tomás, la clínica de la Dra. Lucía y la casa de Inés (128 × 128 o 160 × 128, el tejado sube una parcela sobre su base) | trabajo propio, `tools/gen_sprites.py` | CC0 |
| `icons/` | herramientas, monedas, energía, estaciones y clima | trabajo propio, `tools/gen_sprites.py` | CC0 |

## 3. Nombres que lee el `GestorRecurso`

| Qué | Nombre | Ejemplo |
|---|---|---|
| Terreno | `terrain/<TERRENO>.png`, `terrain/GRASS_<ESTACIÓN>[_<VARIANTE>].png`, `terrain/WATER[_<CUADRO>].png` | `terrain/GRASS_AUTUMN_2.png` |
| Árboles y camino | `terrain/TREE_<ESTACIÓN>.png`, `terrain/PINE_<ESTACIÓN>.png`, `terrain/BUSH_<ESTACIÓN>.png`, `terrain/PATH.png` | `terrain/TREE_WINTER.png` |
| Cultivo | `crops/<CULTIVO>_<ETAPA>.png` | `crops/CARROT_MATURE.png` |
| Animal | `animals/<ANIMAL>_<CUADRO>.png` | `animals/HEN_1.png` |
| Edificio | `buildings/<EDIFICIO>.png` | `buildings/BARN.png` |
| Casa del valle | `village/<CASA>.png` | `village/SHOP.png` |
| Ítem | `items/<ÍTEM>.png` | `items/CARROT_SEED.png` |
| Personaje | `characters/AVATAR_<DIRECCIÓN>_<CUADRO>.png`, `characters/<NPC>.png` | `characters/MARTA.png` |
| Icono | `icons/<ID>.png` | `icons/RAINY.png` |

Para regenerar o comprobar: `python tools/gen_sprites.py` y `python tools/gen_sprites.py --check`; `--sheet archivo.png` además escribe una hoja con todos los sprites para revisarlos de un vistazo.
