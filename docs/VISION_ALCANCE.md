# Visión y alcance — Valle Verde

> **Versión 0.1 · 2026-11-22 · Fase 1 (Análisis)**
> Proyecto final de *Diseño de Software* — Ingeniería de Software, UCC Pasto.
> «Valle Verde» es un **nombre provisional**: el nombre visible vive en una sola constante del código y el paquete raíz es neutro (`edu.ucc.farmsim`), de modo que renombrarlo no exige refactorizar.

## 1. Contexto académico

| Aspecto | Decisión |
|---|---|
| Fuente de verdad | `docs/entrada/Modelo de presentación de diseño.pptx`: define las **5 vistas** que se entregan (contexto, funcional, lógica, desarrollo, despliegue). No hay rúbrica ni enunciado adicional. |
| Material de apoyo | `Diagramas-de-despliegue-UML.pdf`, `De-la-Web-a-una-API-REST-con-Spring-Boot-y-MongoDB.pdf` y `Taller practico bloque 7.docx` (notación UML, capas, trazabilidad). |
| Valor del proyecto | El tema es libre: lo que se evalúa es la **calidad del diseño** y la **coherencia** entre documentos, diagramas y código. |
| Documentos adicionales | Requerimientos, reglas, historias y casos de uso son iniciativa del equipo; se mantienen livianos pero trazables (IDs `RF-`, `RNF-`, `RN-`, `CU-`, `HU-`). |
| Pendiente de confirmar | Fecha de entrega, si el trabajo es individual o en pareja y si hay exposición oral (la plantilla incluye el campo «Nombre del moderador»). |

## 2. Visión

**Para** estudiantes y jugadores casuales que disfrutan de los juegos de granja relajados,
**Valle Verde** es **un simulador de granja y comercio 2D por turnos (un turno = un día)**
**que** permite sembrar, regar y cosechar cultivos, criar animales, construir edificios y vender en un mercado con precios dinámicos, a lo largo de un ciclo de estaciones y clima cambiante.
**A diferencia de** los juegos de granja en tiempo real, cada decisión se toma sin presión de reloj: el estado del juego avanza únicamente cuando el jugador termina el día.
**Nuestro producto** demuestra, además, una arquitectura limpia y verificable (capas, patrones de diseño, pruebas deterministas).

**Inspiración visual:** vista cenital con pixel art (juegos de granja clásicos). Solo es una referencia de estilo; **no se usan sprites, nombres ni textos de ningún juego comercial** (ver §6, restricción R-05).

## 3. Objetivos medibles

| ID | Objetivo | Medida de éxito |
|---|---|---|
| OBJ-01 | Entregar las 5 vistas del PPTX, coherentes con el código | 5 diagramas renderizados; cada clase y componente dibujado existe en el código |
| OBJ-02 | Juego completo y jugable en un ciclo anual | Se puede jugar 112 días (Año 1) sin errores no controlados |
| OBJ-03 | Calidad verificable | Cobertura ≥ 70 % en dominio y aplicación; las 7 pruebas críticas en verde |
| OBJ-04 | Diseño por patrones justificado | 7 patrones aplicados (Singleton, Observer, State, Strategy, Factory Method, Builder, Memento), cada uno con justificación escrita |
| OBJ-05 | Trazabilidad completa | Cada RF → HU → diagrama → clases → pruebas, sin huecos |
| OBJ-06 | Entregable en todo momento | `mvn clean package` genera un `.jar` ejecutable al cerrar cada fase |

## 4. Alcance

### 4.1 Dentro del alcance (MVP = prioridad *Must*)

| Épica | Capacidades |
|---|---|
| Cultivo | Sembrar, regar, crecer por etapas, cosechar, marchitarse; consultar el estado de cada parcela |
| Tiempo y clima | Terminar el día, ciclo de 4 estaciones de 28 días, clima por estación con pronóstico, heladas, lluvia y sequía, energía diaria |
| Economía | Inventario limitado, compra de insumos, venta con precio dinámico (temporada + saturación) |
| Construcción y animales | Construir Gallinero y Establo, comprar animales, alimentarlos, recolectar huevos y leche, estados *hambriento* y *enfermo* |
| Progresión y persistencia | Logros con recompensa, nueva partida, guardar y cargar en 3 ranuras, rechazo seguro de archivos corruptos o inexistentes |
| Interfaz | Pantalla de inicio, HUD, mapa Java2D con clic por herramienta, inventario en grilla, mercado modal |

### 4.2 Alcance incremental (prioridades *Should* y *Could*)

Limpiar parcelas marchitas, tendencia de precios, curar animales, Almacén, desbloqueo por logros, ranuras con metadatos, *toasts*, resumen del día, Invernadero, efectos visuales (tinte día/noche, partículas), animaciones y sprites con `AssetManager`.

> Si el tiempo se agota, estas capacidades se recortan **sin afectar el diseño base**: la interfaz visual avanzada es la última fase.

### 4.3 Fuera de alcance (*Won't*)

| Excluido | Motivo |
|---|---|
| Multijugador y red | Complejidad ajena al objetivo de diseño; el despliegue es un solo equipo local |
| Audio (música y efectos) | No aporta a la evaluación del diseño |
| Base de datos y servicios externos | La persistencia es un archivo JSON local |
| NPC y misiones de personajes | Solo si el alcance lo permite y el equipo lo pide explícitamente |
| Personaje que camina por el mapa | La acción es *clic sobre la parcela con la herramienta elegida* |
| Tiempo real | El juego avanza únicamente por turnos |
| Modificaciones (*mods*), tienda en línea, cuentas de usuario | Sin red ni servicios |

## 5. Actores e interesados

### 5.1 Actores

| Actor | Tipo | Descripción | Interactúa con |
|---|---|---|---|
| **Jugador** | Primario (humano) | Único usuario. Administra la granja, la economía y el progreso | Todos los casos de uso |
| **Sistema de archivos local** | Secundario (sistema) | Almacena y devuelve los archivos JSON de las partidas | CU-14 Guardar, CU-15 Cargar |
| **Paquete de assets gráficos** | Secundario (recurso) | Imágenes CC0 / CC-BY que consume la interfaz; si falta, se dibuja con gráficos vectoriales | CU-18 Observar la granja |

> El **reloj del juego** no es un actor externo: avanza solo cuando el Jugador termina el día.

### 5.2 Interesados

| Interesado | Interés |
|---|---|
| Docente evaluador | Calidad del diseño, notación UML correcta, coherencia documentos–diagramas–código |
| Equipo desarrollador | Un proyecto terminable en el plazo, mantenible y demostrable en cada fase |
| Compañeros de clase | Poder ejecutar el juego con `java -jar` y entender la arquitectura |

## 6. Supuestos y restricciones

| ID | Tipo | Descripción |
|---|---|---|
| R-01 | Tecnología | Java 17, Maven, Swing + Java2D, JUnit 5 y Gson; todo gratuito |
| R-02 | Arquitectura | `ui → application → domain`; `infrastructure → domain`. El dominio no importa Swing, Gson ni E/S de archivos |
| R-03 | Arquitectura | `GameController` solo orquesta y devuelve un `Result`; nunca lanza excepciones hacia la UI |
| R-04 | Arquitectura | La UI se actualiza por eventos (Observer); toda aleatoriedad recibe un `Random` inyectado |
| R-05 | Licencias | Solo assets CC0 o CC-BY registrados en `docs/ASSETS.md`; respaldo vectorial dibujado por código |
| R-06 | Convenciones | Código, comentarios, pruebas y commits en inglés; documentación y textos del jugador en español |
| R-07 | Plataforma | Windows 10+ (equipo del desarrollador), JRE 17 o superior, resolución mínima 1024 × 768 |
| R-08 | Proceso | Se avanza por fases y historias; una fase no empieza sin visto bueno del estudiante |
| S-01 | Supuesto | El juego es de un solo jugador y un solo proceso (un hilo de lógica) |
| S-02 | Supuesto | El profesor acepta una aplicación de escritorio, ya que el tema es libre; las capas se nombran como en el curso (*Controller → Service → Repository*, dominio aparte) |

## 7. Glosario

| Término | Definición |
|---|---|
| **Turno / día** | Unidad de tiempo del juego. Termina cuando el Jugador pulsa «Terminar día» |
| **Parcela** | Casilla de la cuadrícula de la granja. Puede ser tierra de cultivo, hierba, agua u obstáculo |
| **Cultivo** | Planta sembrada en una parcela de tierra; avanza por etapas (semilla, brote, crecimiento, maduro) o se marchita |
| **Energía** | Recurso diario que consumen las acciones del Jugador |
| **Pronóstico** | Clima del día siguiente, visible desde que se sortea |
| **Saturación** | Caída del precio de un producto cuando se venden muchas unidades seguidas |
| **Ranura** | Cada una de las 3 posiciones de guardado |
| **Logro** | Meta de progresión que otorga una recompensa al cumplirse |
| **Moneda** | Unidad monetaria del juego (símbolo «m») |

## 8. Riesgos principales

| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| La interfaz visual consume demasiado tiempo | Alta | Alto | UI mínima funcional desde cada historia; UI visual es la última fase y recortable |
| Assets con licencia incompatible | Media | Medio | Solo CC0 / CC-BY, registro en `ASSETS.md`, respaldo vectorial |
| Reglas de negocio incoherentes entre documentos | Media | Alto | Valores únicos en `REGLAS_DE_NEGOCIO.md`; constantes en una sola clase de configuración |
| Aleatoriedad que vuelve las pruebas frágiles | Media | Medio | `Random` inyectado con semilla fija en todas las pruebas |
| Alcance excesivo (42 RF) | Media | Medio | MoSCoW: 30 *Must* forman el MVP; el resto se recorta sin afectar el diseño |
