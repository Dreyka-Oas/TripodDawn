# TripodDawn

A Fabric mod for Minecraft 1.21.11. War machines climb out of the ground at night and take the world
apart. The first horn sounds on night one, the first walker arrives around day eight, and the
pressure keeps rising after that. No new item, no new recipe, no new block: you fight it with the
weapons the game already gives you.

Needed on the server and on every client, since the machines are rendered by GeckoLib.

## What a world looks like from the first night

The invasion reads the world's own day counter. Nothing is stored per player, nothing is a game rule,
and the mod costs nothing on a server nobody is playing on: one roll per night, not one per tick.

| Day | Rung | Martians per player | Machines per night | Machines standing | Species opened |
|---|---|---|---|---|---|
| 0 | nothing yet | 0 | 0 | 0 | none |
| 1 | the warning | 0 | 0 | 0 | none |
| 3 | the scouts | 3 | 0 | 0 | none |
| 8 | the walkers | 4 | 1 | 2 | tripod |
| 14 | the harvest | 5 | 2 | 4 | harvester |
| 20 | the siege | 6 | 3 | 8 | uberpod |
| 30 | the emperor | 8 | 4 | 10 | emperorpod, once only |

Night one is a horn over the horizon and the ground moving under your feet. Nothing arrives.

The newest species open gets half the rolls and the older ones share the rest, so reaching a rung is
something you see rather than something you read. Past the siege the per-night number stops being the
limit and the standing number takes over.

Nothing the invasion does is written in the chat. You learn where you are by listening.

## What comes out of the ground

A machine arrives between 48 and 96 blocks away and spends thirty seconds climbing out, throwing soil
while it rises and sounding its horn when it reaches the surface. Anyone within forty blocks of that
gets the earthquake effect: the camera moves a little, without taking your aim away from you. The
same thing happens once on night one, with no machine under it.

Once it is standing it comes for you. A machine picks a player out up to a hundred blocks away, walks
the whole distance, and does not need to have seen you first: it is twenty-four blocks tall and one
hill is not cover. It stays where it came up, and walking away does not make it disappear. Martians
are ordinary night mobs and do despawn, which is what stops a week of nights from piling up.

| Creature | Health | Armour | Stamp | Notes |
|---|---|---|---|---|
| Martian | 40 | 0 | 8 | on foot, comes at you from 48 blocks |
| Tripod | 200 | 15 | 30 | heat ray |
| Harvester | 180 | 15 | 30 | heat ray |
| Uberpod | 280 | 25 | 40 | heat ray |
| Emperorpod | 450 | 25 | 60 | heat ray, one per world |

The heat ray is the thing that kills you, not the stamp. A machine takes aim for a second and a
quarter, with a sound to tell you so, then fires anything between 12 and 64 blocks as long as it can
see you. A direct hit kills a player outright and sets what is left on fire; what it misses it blows
a hole in. Inside twelve blocks it stops firing and stamps instead, so the ground under a machine is
the one place its ray cannot reach.

Machines take normal damage from everything: a sword, an axe, an arrow, a block of TNT. Their health
and their armour are the only thing between you and them. Killing one drops nothing and gives
experience.

A machine that dies falls over and stays on the ground as a wreck for five minutes before the world
cleans it up.

## The command

`/tripoddawn` needs operator level and is the only place in the mod that writes in the chat, to
whoever typed it and to nobody else.

| Subcommand | What it does |
|---|---|
| `day` | says which invasion day it is and which rung that is |
| `day <number>` | jumps to that day without touching the world clock |
| `wave` | plays a night straight away, whatever the sky says |
| `reset` | puts the invasion back on the world clock |
| `spawn <species>` | drops one creature 32 to 64 blocks away, machine already standing |

The five spawn eggs are in the creative tab and do the same thing by hand.

## Building it

Java 21, Gradle wrapper included, nothing else to install.

```bash
./gradlew build
```

The jar lands in `mod/build/libs/`. Versions are pinned in `mod/gradle.properties`: Fabric loader
0.19.3, Fabric API 0.141.4+1.21.11, GeckoLib 5.4.5.

`python3 mod/scripts/check.py` reads the resource pack against the Java that loads it: the two
language files key for key, every id a player sees against its translation, every sound and every
model against the file it points at. It returns non-zero on the first problem.

Two more scripts rebuild the assets rather than leaving them as files nobody can reproduce.
`mod/scripts/particles.py` draws the eleven dust and heat-ray frames, and `mod/scripts/sounds.sh`
takes a folder of source recordings and writes the seventeen `.ogg` files.

## Licence

MIT, see [LICENSE](LICENSE). Nothing third-party is bundled in the jar.
