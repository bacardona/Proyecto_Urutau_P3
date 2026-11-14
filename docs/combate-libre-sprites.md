# Sprites de Combate Libre

Este informe registra la integración inicial. Las asignaciones de Placaje, el
bloqueo y las colisiones se actualizaron después: consulta el
[informe de bloqueo y colisiones](combate-libre-bloqueo-colisiones.md).

Los cuatro paquetes se leen desde `/sprites/fighting/<ESPECIE>/Sprites_nuevos/`.
Los PNG originales y sus XML se conservaron. La configuración está en
`FightingSpriteProfile`; `SpriteCollabAnimationSet` carga una vez cada paquete,
resuelve `CopyOf` después de cargar las hojas concretas y conserva sus duraciones,
direcciones y anclas. No se agregó otro reproductor ni una dependencia externa.

## Movimientos y secuencias

| Especie | Movimiento real | Animación | Motivo |
|---|---|---|---|
| Bulbasaur | TACKLE / Placaje | Attack | Contacto físico breve, con el cuerpo anclado. |
| Bulbasaur | VINE_WHIP / Látigo Cepa | Shoot | Postura de lanzamiento; conserva el efecto existente de las cepas. |
| Bulbasaur | SLEEP_POWDER / Somnífero | Shake | Sacude el bulbo para expulsar el polvo. |
| Bulbasaur | GROWTH / Desarrollo | DeepBreath | Preparación y expansión corporal del refuerzo. |
| Charmander | TACKLE / Placaje | Attack | Golpe corto con la pata delantera. |
| Charmander | EMBER / Ascuas | Shoot → Charge | Postura de exhalación; es el alias definido por este XML. |
| Charmander | GROWL / Gruñido | DeepBreath | Abre la boca y exhala; conserva las ondas existentes. |
| Charmander | FLY / Vuelo | Hop | Poses direccionales de despegue, vuelo y descenso, siguiendo la física existente. |
| Eevee | TACKLE / Placaje | Nod | Golpe corto con la cabeza. Attack contiene un salto ofensivo y no se eligió para este movimiento. |
| Eevee | QUICK_ATTACK / Ataque Rápido | Charge | Postura de avance rápido, compatible con su desplazamiento existente. |
| Eevee | GROWL / Gruñido | Shoot | Apertura de boca dirigida al rival; conserva las ondas existentes. |
| Eevee | TAIL_WHIP / Látigo | TailWhip | Secuencia específica de movimiento de cola. |
| Squirtle | TACKLE / Placaje | Attack | Conservado. |
| Squirtle | WATER_GUN / Pistola de Agua | Shoot | Conservado. |
| Squirtle | TAIL_WHIP / Látigo | Swing | Conservado. |
| Squirtle | BITE / Mordisco | Nod | Conservado. |

Cada especie tiene cuatro hojas diferentes para sus cuatro ataques, comprobado
por identidad de la animación cargada, incluyendo los alias. El cargador ya no
sobrescribe un Strike real con Attack: Charmander dispone de ambos por separado.

Idle y Walk conservan sus duraciones originales. Hop sigue la velocidad vertical
real. Se compensa el salto dibujado dentro de Hop para no sumarlo otra vez al de
la física. Los tres paquetes nuevos usan Cringe como postura defensiva y Squirtle
conserva Withdraw; la pose se mantiene al bloquear y se recupera al soltar.
Hurt usa un reloj visual por luchador que se reinicia al recibir daño. Faint tiene
prioridad y el reemplazo espera su duración leída del XML (34/60 s en estos paquetes).
La selección también contempla Sleep, sin volver al GIF.

Cringe y DeepBreath solo tienen una fila en los paquetes nuevos: se respeta la
pose frontal original. Las animaciones que ofrecen ocho direcciones utilizan las
filas originales este/oeste. No se inventaron direcciones ni fotogramas.

Los ataques nuevos se sincronizan con STARTUP, ACTIVE y RECOVERY utilizando
HitFrame y ReturnFrame cuando existen. Los que no tienen marcadores distribuyen
la secuencia original entre esas fases, conservando la proporción de duraciones
de sus cuadros. La recuperación visual de Gruñido y Desarrollo finaliza y vuelve
a Idle mientras continúa su cooldown original. Los ataques de Squirtle conservan
su temporización anterior. El render no mantiene un reloj global mutable: pintar
no avanza cuadros, cancelar cambia inmediatamente la secuencia y pausar detiene
los relojes de simulación.

## Escala y geometría

Escala entera 4×, nearest-neighbor y proporciones originales. Los indicadores
de equipo usan las mismas hojas a 2×. No se adapta el tamaño al rectángulo
transparente del fotograma. Se usan el centro del cuerpo y el marcador de sombra;
en salto se compensa la traslación interna con el ancla corporal de Idle.

| Especie base | Hitbox corporal anterior, ancho × alto | Hitbox corporal actual |
|---|---|---|
| Bulbasaur | 96 × 130 | 68 × 72 |
| Charmander | 96 × 140 | 52 × 84 |
| Eevee | 88 × 125 | 60 × 80 |
| Squirtle | 60 × 75 | 60 × 75 |

Las cajas son fijas por forma, con los pies en Fighter.y, y se comparten entre
colisiones de ataques y apoyo en plataformas. Cola, llama, sudor y espacio
transparente no aumentan la caja corporal durante una acción.

| Especie | Ataque | Área anterior, ancho × alto | Área actual | Borde superior sobre los pies |
|---|---|---|---|---|
| Bulbasaur | Placaje | 54 × 62 | 44 × 48 | 56 px |
| Bulbasaur | Látigo Cepa | 148 × 54 | 132 × 44 | 60 px |
| Charmander | Placaje | 54 × 62 | 44 × 56 | 64 px |
| Charmander | Vuelo | 84 × 90 | 66 × 64 | 76 px |
| Eevee | Placaje | 54 × 62 | 42 × 48 | 60 px |
| Eevee | Ataque Rápido | 58 × 76 | 56 × 44 | 60 px |
| Eevee | Látigo | 112 × 82 | 72 × 44 | 48 px |

Las áreas parten del borde corporal orientado al rival. La cepa dibujada utiliza
el alcance ajustado de 132 px y nace a la altura del cuerpo reducido.
Somnífero nace del bulbo: 12 px detrás del centro y 64 px sobre los pies.
Ascuas usa el marcador de cabeza del cuadro de impacto, más 8 px de salida
(hacia la derecha: +28 px, −32 px respecto al centro/pies). Se mantiene Pistola
de Agua en ±36 px, −24 px. Velocidad, vida y tamaño de los proyectiles permanecen
iguales. Las evoluciones conservan su geometría y sus recursos previos.

El juego ya permite cruzarse al caminar: no existía resolución de colisiones
horizontales entre rivales. Se conservaron esas reglas; las nuevas cajas se usan
en impactos, separación al bloquear y plataformas. Añadir una barrera corporal
permanente sería un cambio de mecánica distinto.

## Error al recibir ultis

La condición `(!inUltimateSequence || fighter.isDefeated())` excluía el reproductor
SpriteCollab para cualquier participante vivo de una ulti. El depurador confirmó
la llegada al cargador antiguo con `species=SQUIRTLE`, `evolved=false`,
`ultimate=true`, `stun=true`, tras un pulso real de Llamarada (183 HP restantes).

Ahora se elige el paquete según la especie realmente dibujada, también durante
los destellos de transformación. Participar como objetivo de una ulti no excluye
el paquete base. Hurt tiene prioridad ante el stun especial o el daño; Faint
prevalece al morir. El regreso legítimo a la base usa sus PNG, mientras las
evoluciones siguen usando sus propios GIF. No se cambiaron las reglas de las
ultis ni las de transformación.

## Archivos de esta tarea

- `src/main/java/com/pokemon/battle/fighting/FightingSpriteProfile.java`: configuración, geometría y orígenes por especie.
- `src/main/java/com/pokemon/battle/fighting/SpriteCollabAnimationSet.java`: carga general de hojas, alias y validación de anclas.
- `src/main/java/com/pokemon/battle/fighting/FightingRenderer.java`: selección común, prioridades, alineación e indicadores de equipo.
- `src/main/java/com/pokemon/battle/fighting/Fighter.java`: dimensiones base y reloj visual de daño.
- `src/main/java/com/pokemon/battle/fighting/FightingCombatSystem.java`: aplica exclusivamente la geometría de contacto y de proyectiles.
- `src/main/java/com/pokemon/battle/fighting/FightingGameSession.java`: extiende la espera visual de Faint al resto de paquetes.
- `src/test/java/com/pokemon/battle/fighting/FreeFightSpriteCheck.java`: verificación ejecutable sin Oracle ni bibliotecas nuevas.
- Este informe.

Se comparó con una copia de las clases al inicio de la tarea. Permanecen intactos
AttackDefinition, EvolutionProfile, UltimateController, UltimateDefinition,
PhysicsSystem, Platform, FightingInput, FightingTeam y FightingGamePanel, así como
las constantes de daño, fases y cooldowns de FightingCombatSystem. No hubo cambios
en Oracle, PvP/PvE, selección de entrenadores ni recursos originales. El árbol de
trabajo ya tenía cambios locales previos, que se conservaron.

Placaje sigue siendo compartido por los cuatro Pokémon. Gruñido de Charmander y
Eevee comparte la misma definición y Látigo de Eevee y Squirtle conserva su efecto
de reducción de defensa; no se cambió su equilibrio. Dentro de cada equipo de
cuatro movimientos de una especie no se encontraron dos definiciones idénticas.

## Verificación y reproducción

Compilación Maven `test-compile`: BUILD SUCCESS, 121 clases principales con
`release 17`. Se usó el Maven 3.9.16 incluido en IntelliJ, descargando dos
dependencias del plugin de recursos que faltaban en la caché. El JDK instalado
es 25.0.2; no se ejecutó con un runtime JDK 17 independiente.

La clase FreeFightSpriteCheck terminó con 5898 comprobaciones correctas:

- Estados Idle, Walk este/oeste, Hop, guardia, Hurt y Faint para las cuatro especies.
- Cuatro animaciones de ataque diferentes, reproducción por fases y ambos sentidos.
- Ataques ejecutados desde sesiones reales, fin de acción y Placaje sin traslación.
- Placaje acertado y fallado, bloqueo y cancelación hacia el siguiente ataque.
- Proyectiles conectando en ambos sentidos, conservación del efecto de Somnífero y orígenes a la altura del cuerpo.
- Caja corporal constante, exclusión del margen transparente y aterrizaje en plataformas.
- Pausa de locomoción y Faint, reemplazo automático y revancha desde el panel Swing.
- 32 combinaciones de emisor/receptor de ulti, con supervivencia y KO, sin solicitudes de GIF de ninguna forma base.
- Cuatro escenarios adicionales de KO por ulti desde una sesión real: espera de Faint y reemplazo posterior.
- Evolución, uso de recursos propios de la evolución y retorno a la base después de la ulti.
- Render completo de arena/HUD e inspección de la imagen generada `target/free-fight-sprite-preview.png`.
- `git diff --check` sin errores de espacios.

Para repetir desde la raíz del proyecto:

```powershell
mvn test-compile
java "-Djava.awt.headless=true" -cp "target/test-classes;target/classes" com.pokemon.battle.fighting.FreeFightSpriteCheck
```

En IntelliJ también se puede ejecutar el main de FreeFightSpriteCheck. Es una
comprobación independiente, no una prueba JUnit que Maven ejecute automáticamente.

Queda pendiente una partida interactiva con teclado para valorar el ritmo y la
sensación del combate. Ejecuta MainGUI, entra a Combate Libre y selecciona los
tres Pokémon nuevos y Squirtle. Prueba F/G/H/V e I/J/K/L mirando a ambos lados,
salta sobre plataformas y pulsa ESC durante una animación. Para revisar las
ultis rápidamente, añade `-Dpokemon.fighting.debug=true` a las opciones VM:
F8/F9 llenan la energía de J1/J2; evoluciona con R/P y usa la ulti con T/O.
Observa al receptor base antes, durante y después del golpe; termina la partida
y selecciona revancha. Esos controles de depuración ya existían.
