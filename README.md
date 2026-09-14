# TripodDawn

A Fabric mod for Minecraft 1.21.11. War machines climb out of the ground at night and take the world
apart. The first horn sounds on night one, the first walker arrives around day eight, and the
pressure keeps rising after that. No new item, no new recipe, no new block: you fight it with the
weapons the game already gives you.

Needed on the server and on every client, since the machines are rendered by GeckoLib.

## What a world looks like from the first night

The invasion reads the world's own day counter. Nothing is stored per player, nothing is a game rule,
and the mod costs nothing on a server nobody is playing on: one roll per night, not one per tick.

Nothing comes up in daylight. A night opens between 13000 and 23000 on the world clock, once, and the
gate is the clock rather than how dark the sky looks, because a thunderstorm at noon is dark enough to
fool the second test and this mod starts a thunderstorm itself at the top of every invasion night. The
one exception is deliberate and is the command below.

| Day | Rung | Martians per player | Machines per night | Machines standing | Strikes | Species opened |
|---|---|---|---|---|---|---|
| 0 | the warning | 0 | 1 | 1 | 8 | tripod |
| 3 | the scouts | 3 | 1 | 2 | 12 | none |
| 8 | the walkers | 4 | 2 | 4 | 18 | none |
| 14 | the harvest | 5 | 3 | 6 | 24 | harvester |
| 20 | the siege | 6 | 4 | 8 | 34 | uberpod |
| 30 | the emperor | 8 | 5 | 10 | 48 | emperorpod, once only |
| 40, 50, 60 and on | one rung every ten days | +2 | +1 | +2 | +8 | none |
| 45 | not a rung | | | | | titan, one night in six |

Night one of a world already has a machine in it, under a horn and the ground moving. One, and
nothing on foot beside it: you meet the thing alone before you ever have to meet two.

The newest species open gets half the rolls and the older ones share the rest, so reaching a rung is
something you see rather than something you read. Past the siege the per-night number stops being the
limit and the standing number takes over.

The table stops at the emperor because there is no eighth species to open. The calendar does not stop
with it: from day 30 on, every ten days adds a rung, and a rung is more on foot, more a night, more
standing and more sky. Day 100 sends twenty-two martians and twelve machines at one player. The climb
flattens at 32 martians, 12 a night, 24 standing and 160 strikes, and that ceiling is a tick budget
rather than a difficulty decision: twenty-four machines walking is twenty-four of everything the
server does per machine.

Nothing the invasion does is written in the chat, and nothing wears a bar at the top of the screen,
the emperorpod and the titan included. A named health bar is writing too, and it would give away that
the shape on the horizon is the last one rather than another uberpod. You learn where you are by
listening, and by counting the sky.

## The sky, twenty seconds before anything walks

An invasion night puts the weather to thunder itself, then drops the strike count above around every
player, between 20 and 96 blocks out, spread over twenty seconds. Almost none of them has anything
under it. The four a machine lights on its way up are the same bolts, so watching the horizon tells
you a night has started and nothing more precise than that.

None of these carries fire or damage, at any difficulty. A rung that put twelve real bolts in a
forest would have you fighting the fire instead of the thing that lit it.

## What comes out of the ground

A machine arrives between 48 and 96 blocks away and spends thirty seconds climbing out, throwing soil
while it rises and sounding its horn when it reaches the surface. Four bolts come down over the spot
in the first eight seconds, the first one on the machine itself. Anyone within forty blocks of that
gets the earthquake effect: the camera moves a little, without taking your aim away from you. The
same thing happens once on night one, with no machine under it.

Once it is standing it comes for you. A machine picks a player out up to a hundred and twenty-eight
blocks away, walks the whole distance, and does not need to have seen you first: it is forty blocks
tall and one hill is not cover. That reach is longer than the ninety-six it can arrive at, so
the ones that land at the far edge start walking instead of strolling, and backing off a chunk does
not shake one loose. It stays where it came up, and walking away does not make it disappear. Martians
hold on to sixty-four blocks, and are ordinary night mobs that do despawn, which is what stops a week
of nights from piling up.

With no player in reach it shoots whatever else is alive within sixty-four blocks, so a village left
behind burns on its own and the cows in the field go with it. Two things are spared: the invasion
never fires on its own, and the night mobs are left to the player, since a machine clearing the
zombies off a roof would be helping rather than hunting.

| Creature | Height | Health | Armour | Stamp | Notes |
|---|---|---|---|---|---|
| Martian | 2.5 | 40 | 0 | 8 | on foot, comes at you from 64 blocks |
| Tripod, scout | 34 | 140 | 8 | 22 | heat ray, no plating, the fast one |
| Tripod | 40 | 200 | 15 | 30 | heat ray |
| Tripod, heavy | 48 | 320 | 24 | 44 | heat ray, double plating, slow |
| Harvester | 40 | 180 | 15 | 30 | heat ray |
| Uberpod | 44 | 280 | 25 | 40 | heat ray |
| Emperorpod | 44 | 450 | 25 | 60 | heat ray, one per world |
| Titan | 68 | 900 | 30 | 70 | heat ray, rarely, from day 45 |

Height is how tall the thing is drawn. The box it collides with is the legs and nothing else, six
blocks for a walker, and that is on purpose: a box the size of the model would have the server test
forty blocks of empty sky under every machine standing, every tick, and would wedge one under any
canopy tall enough to clear its hood. What a shot lands on is cut from the drawn height instead, so
nothing you can see is out of reach.

That cut is twenty boxes on a walker, not three, and their shape comes from a rule of the game rather
than from a choice about aiming. Whatever box an entity carries, it is only found by a shot that
passes within four blocks of the position it stands at, so one box thirty-two blocks tall answered
arrows around the feet and let everything above them through, and one box fourteen blocks wide
answered from the east and not from the west. Each box now stays inside that reach.

So the machine wears four columns of them: one up its own axis for the hull and the hood, and one on
each leg, out where the leg really is and turning with the body. A shot fired between two legs goes
through, which is what a walker standing on daylight should do. Where the arrow lands still decides
what it is worth: the legs take three fifths of it, the hull takes all of it, the hood takes two and
a half times.

A machine does not look for a way around anything, either. It picks a direction and walks, stepping
up to three blocks as it goes. That is what a thing taller than the trees would do, and it is also
the only version the server can afford: asking the game for a route costs one test per block the
walker fills, and a tall box spent more time deciding where to put its feet than the rest of the
invasion put together.

### Three walkers out of the same hole

A tripod comes up in one of three builds, and they read apart at distance. The scout has been
stripped of its plating and is six blocks shorter than the line machine: it dies faster than
anything else on the field and it arrives before you are ready for it. The heavy wears a second layer
of plating, stands eight blocks taller and walks at three quarters of the speed, because it does not
have to hurry. The line machine in the middle is the one everything else is measured against.

Which one turns up is a weight rather than a rung, so the mix shifts instead of switching. Before day
8 every tripod is a scout. Up to day 20 it is two scouts for one line machine. From day 20 the
heavies start, one roll in four, and half the rolls are line machines. Past day 40 it is heavies and
line machines, and the scouts have stopped coming.

The titan is the one that is not a tripod. Sixty-eight blocks, nearly twice the line machine, and the
first thing you learn about it is that it is on the horizon and the trees are not. It comes up between 72 and 112 blocks out, one night in six from day 45, and never
two at a time. Nothing announces it: no bar at the top of the screen, no line in the chat, the same
silence as everything else here.

### Two shots

The heat ray is the thing that kills you, not the stamp. A machine picks its shot by how far away you
are, and you can hear which one is coming: the quick shot is pitched high, the charged one low.

| | Wind-up | Reload | Direct hit | Splash | Radius |
|---|---|---|---|---|---|
| Quick | 0.4 s | 1.25 s | 25 | 8 | 2.5 |
| Charged | 2 s | 5 s | 500 | 40 | 4 |

Inside 36 blocks it fires quick, and the quick shot misses: it carries a spread, so it is a thing to
run under rather than a thing to dodge, and standing still in the open is what makes it land. Past 36
blocks it has the time to wind all the way up and it takes it, so the charged shot is what comes off
the horizon, aimed, and a direct one kills a player outright. Either way, what it misses it blows a
hole in and what it hits catches fire, three seconds on the quick shot and eight on the charged one.
Inside twelve blocks it stops firing and stamps instead, so the ground under a machine is the one
place its ray cannot reach.

You get that wind-up to read. The hood gathers a glow that starts wide and dim and closes to a point,
the shot leaves on a bloom thrown two blocks out in front, and the ray draws a continuous rope rather
than a dotted line: a white core inside an orange sleeve, laid down along the three blocks it crosses
each tick instead of once at its head. It goes through up to ten bodies, flashing on each, and ends in
a white flash, an orange ball thrown back the way it came, and smoke.

### Where you hit it

Machines take normal damage from everything: a sword, an axe, an arrow, a block of TNT. Where the hit
lands is what decides how much of it arrives. A machine is not one box, it is three stacked, and the
one you are aiming at changes the number by a factor of four.

| Zone | Height on the machine | What a hit is worth |
|---|---|---|
| Legs | the bottom 80 % | 0.6 |
| Hull | 76 % to 92 % | 1.0 |
| Hood | 88 % to the top | 2.5 |

Those percentages are of the drawn height, so on a line machine the hood starts thirty-five blocks
up. The three spans overlap at the seams on purpose: a shot arriving there belongs to whichever box
the game hands it, and both answers are defensible, where a gap would be a shot that hit nothing.

The legs are thin, far apart and mostly air, so little of what is aimed at them is load bearing.
They are also where almost everything on the ground lands, which is why four fifths of the machine is
worth a little over half. The hood is where the ray comes out, which is the one place the plating
cannot be, and getting anything up there is the problem: shoot it off a hill, off a tower, or with a
bow from a long way back. Killing one drops nothing and gives experience.

A machine that dies takes five seconds to fall, lies there venting for three more, and then scuttles
itself: a blast the size of three sticks of TNT, a crater, and smoke over the spot. Eight seconds,
because the kill and the crater should be the same moment. A wreck that simply blinked out would read
as the game forgetting it, and a wreck left lying turns a siege night into a scrapyard you cannot walk
through. It lights no fire, unlike the ray, since it goes off next to whoever just killed it. It obeys
the vanilla mob griefing rule like everything else this mod sets off, so a server that turned block
damage off has already said no to the crater.

### What you hear, and what you feel

A machine is loud before it is visible. Every time one has covered a fifth of its own height it puts
a leg down, and the footfall is measured off the ground it actually covered rather than read off the
walk animation, so a scout at three quarters speed takes more steps per second and leaves the same
distance between two of them. Close enough and you get the foot itself, with dirt thrown up around
it. Past sixty-four blocks the same step arrives as the far version, which has lost its top end and
gained a tail: what reaches you over that distance is the low end and a roll, never the metal.

The engine works the same way, near and far, and the horn is unchanged from the source mod. What is
new is everything under the machine, since the mod it came from had nothing at all down there: three
footfalls that rotate, the distant one, the stamp, the bed under a shake, and the ring a shot makes
on plating. None of them is a sample. `mod/scripts/steps.py` draws them out of sine sweeps and shaped
noise, which is the only way the repository can own them, and a forty block leg landing on stone is
not a thing anybody has a recording of anyway.

Both of those come with the ground moving. An arrival shakes everyone within forty blocks for twelve
seconds; a footfall shakes whoever stands inside three quarters of the machine's drawn height, for
long enough to read as a jolt and no longer; a stamp that connects shakes the full height. The
camera moves and your aim stays yours. The three durations are the only knob: the effect tapers on
its own, so a short shake is a weak one.

The ring a shot makes says where it landed. A strut answers low, the hull in the middle, the plate
over the hood high and thin, and it is played from the height of the box that was hit rather than
from the machine, so on a forty block walker the shins and the hood come from two different places.
Nothing else tells you that you found the weak point: no number comes up, and the machine's own hurt
sound is the same wherever it was hit.

A wreck keeps breathing while it burns. Every six and a half seconds it lets go of a lungful of gas
and smoke, right up to the blast.

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

`spawn` takes the six species, plus `scout` and `heavy` for the two tripod builds that are not the
line machine. `spawn tripod` gives the build the calendar is currently sending.

The six spawn eggs are in the creative tab and do the same thing by hand.

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

Three more scripts rebuild the assets rather than leaving them as files nobody can reproduce.
`mod/scripts/particles.py` draws the eleven dust and heat-ray frames. `mod/scripts/sounds.sh` takes a
folder of source recordings and writes seventeen of the `.ogg` files. `mod/scripts/steps.py` needs no
input at all and writes the other seven, the ones under the machine, from numbers.

## Licence

MIT, see [LICENSE](LICENSE). Nothing third-party is bundled in the jar.
