#!/usr/bin/env bash
# Rebuilds the mod's audio from the source mod's recordings.
#
# Every sound stays mono at 48 kHz: Minecraft only attenuates a mono sound with distance, and a
# stereo file plays flat wherever the player stands. The width a modern mix would give these is
# bought back with the tone and dynamics below rather than with the channel count.
#
# Usage: sounds.sh <folder holding the source .ogg files>
set -euo pipefail

SRC="${1:?donner le dossier des sons d'origine}"
OUT="$(cd "$(dirname "$0")/.." && pwd)/src/main/resources/assets/tripoddawn/sounds"
mkdir -p "$OUT"

enc() { ffmpeg -hide_banner -loglevel error -y -i "$SRC/$1.ogg" -af "$2" -ac 1 -ar 48000 -c:a libvorbis -q:a 5 "$OUT/$3.ogg"; }

# Metal on a scale nothing in the game has: sub weight, the 400 Hz box taken out, presence and air
# put back, then glued so the tail carries instead of dropping off.
BIG="highpass=f=28,equalizer=f=70:width_type=q:w=0.8:g=4,equalizer=f=400:width_type=q:w=1.1:g=-3,equalizer=f=3200:width_type=q:w=1.0:g=2.5,equalizer=f=11000:width_type=q:w=0.7:g=2.5,acompressor=threshold=-20dB:ratio=3:attack=8:release=300:makeup=2,alimiter=limit=0.95,loudnorm=I=-14:TP=-1.5:LRA=9"

# The engine sits under everything else, so it is quieter and its top end is pulled back: a loop
# that competes with the horn turns the whole scene to noise.
ENGINE="highpass=f=30,equalizer=f=75:width_type=q:w=0.9:g=4,equalizer=f=340:width_type=q:w=1.2:g=-3.5,equalizer=f=6000:width_type=q:w=0.8:g=-2,acompressor=threshold=-24dB:ratio=4:attack=20:release=400:makeup=3,alimiter=limit=0.92,loudnorm=I=-20:TP=-2:LRA=7"

# A voice has nothing below eighty, and what makes it read at a distance is the 3 kHz band.
VOICE="highpass=f=85,equalizer=f=350:width_type=q:w=1.2:g=-3,equalizer=f=2800:width_type=q:w=1.0:g=3.5,equalizer=f=9000:width_type=q:w=0.8:g=2,acompressor=threshold=-18dB:ratio=3.5:attack=5:release=150:makeup=2,alimiter=limit=0.95,loudnorm=I=-16:TP=-1.5:LRA=8"

# The shot has to snap. Fast attack, hard top end, no low end to slow it down.
BEAM="highpass=f=45,equalizer=f=500:width_type=q:w=1.3:g=-2.5,equalizer=f=4800:width_type=q:w=0.9:g=4,equalizer=f=12000:width_type=q:w=0.7:g=3,acompressor=threshold=-16dB:ratio=5:attack=1:release=90:makeup=2.5,alimiter=limit=0.95,loudnorm=I=-15:TP=-1.5:LRA=7"

# The night-one warning is the one sound meant to be far away, so it loses its top and gains a tail.
FAR="highpass=f=35,lowpass=f=4200,equalizer=f=90:width_type=q:w=0.8:g=3,equalizer=f=1800:width_type=q:w=1.0:g=-2,aecho=0.8:0.85:420|900:0.28|0.16,acompressor=threshold=-22dB:ratio=3:attack=15:release=500:makeup=2,alimiter=limit=0.9,loudnorm=I=-19:TP=-2:LRA=9"

enc martian_growl     "$VOICE"  martian_growl
enc martian_hurt      "$VOICE"  martian_hurt
enc martian_death     "$VOICE"  martian_death

enc tripod.spawn      "$BIG"    machine_spawn
enc tripod.death      "$BIG"    machine_death
enc tripod.hurt       "$BIG"    machine_hurt
enc tripod.gas        "$BIG"    machine_gas

enc tripod.shoot      "$BEAM"   machine_shoot
enc tripod.heat_ray   "$BEAM"   machine_heat_ray

enc tripod_engine     "$ENGINE" machine_engine
enc tripod_engine_far "$ENGINE" machine_engine_far

enc tripod.horn        "$BIG"   tripod_horn
enc tripod.broken_horn "$BIG"   tripod_horn_broken
enc uberpod.horn       "$BIG"   uberpod_horn
enc emperorpod.horn    "$BIG"   emperorpod_horn

enc distant_horn1      "$FAR"   distant_horn_1
enc distant_horn.2     "$FAR"   distant_horn_2

ls -1 "$OUT"
