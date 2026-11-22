# Recursos gráficos y licencias — Valle Verde

> **Versión 1.0 · Fase 5.6 · HU-42**

## 1. Qué se usa hoy

**Ningún paquete de imágenes.** Todo lo que se ve en el juego (terrenos, cultivos, edificios, animales, lluvia, nieve, avisos) se dibuja con formas vectoriales de Java2D en `MapRenderer`; los colores están en `Palette`. Por eso el repositorio no incluye ni sprites de terceros ni imágenes con licencia que cumplir, y el juego se puede jugar completo sin ninguna imagen (HU-42 · CA-3).

## 2. Cómo se agregan sprites

El `AssetManager` busca, dentro de `src/main/resources/assets/`, los archivos siguientes; si no existe uno, el renderizador usa el gráfico vectorial y deja un aviso en el registro (HU-42 · CA-2). Cada archivo se lee una sola vez y se guarda en memoria (CA-1).

| Qué | Nombre del archivo | Ejemplo |
|---|---|---|
| Cultivo en una etapa | `crops/<CULTIVO>_<ETAPA>.png` | `crops/CARROT_MATURE.png` |
| Animal en un cuadro de su animación (0, 1 o 2) | `animals/<ANIMAL>_<CUADRO>.png` | `animals/HEN_0.png` |

## 3. Regla de licencias (HU-42 · CA-4)

Todo paquete de imágenes que se agregue debe quedar en la tabla siguiente **en el mismo commit**, con su fuente y su licencia. Solo se aceptan **CC0** o **CC-BY** (con la atribución que pida el autor); no se usan sprites de otros juegos comerciales. La prueba `HU42_ca4_everyImagePackageInTheResourcesIsListedInAssetsMd` falla si hay un archivo en `assets/` que no aparece aquí.

| Carpeta | Autor y fuente | Licencia |
|---|---|---|
| — | Ningún paquete incluido | — |

Licencia del código y de los gráficos vectoriales del proyecto: trabajo propio del equipo.
