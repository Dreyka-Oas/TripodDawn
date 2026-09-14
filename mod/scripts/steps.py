#!/usr/bin/env python3
"""
Draws the sounds the ground makes, since no recording of them exists.

The source mod had a horn, an engine and a voice, and nothing at all under the machine: no footfall,
no stamp, no rumble. Those four are built here from sine sweeps and shaped noise rather than sampled,
which is the only way the repository can own them: a forty block leg landing on stone is not a thing
anybody has a recording of, and a library clip would drag a licence in behind it.

Everything stays mono at 48 kHz. Minecraft only attenuates a mono sound with distance, so a stereo
file would play flat wherever the player stands.

Usage: steps.py
"""

import math
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

SR = 48000
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/tripoddawn/sounds"

# The same master chains as sounds.sh, so a made sound and a recorded one sit in the same mix.
#
# The low end is held back rather than pushed. A footfall built around its sub measures impressive
# and disappears on the speakers most people play through: what carries an impact on a laptop is the
# body between eighty and twelve hundred, and the sub is only what makes it feel big where there is
# something to reproduce it.
STEP = ("highpass=f=26,equalizer=f=55:width_type=q:w=0.8:g=1.5,"
        "equalizer=f=170:width_type=q:w=1.0:g=3,"
        "equalizer=f=700:width_type=q:w=1.1:g=2.5,"
        "equalizer=f=2600:width_type=q:w=1.0:g=2,"
        "acompressor=threshold=-20dB:ratio=3.5:attack=4:release=260:makeup=2,"
        "alimiter=limit=0.95,loudnorm=I=-17:TP=-1.5:LRA=9")

# Distance takes the top off before it takes the level off, and it adds a tail the near sound has not
# got. Both are written here rather than left to the attenuation, which only changes the volume.
FAR = ("highpass=f=24,lowpass=f=1500,equalizer=f=48:width_type=q:w=0.7:g=1.5,"
       "equalizer=f=200:width_type=q:w=1.0:g=3,"
       "aecho=0.85:0.7:260|560:0.3|0.16,"
       "acompressor=threshold=-24dB:ratio=3:attack=20:release=600:makeup=3,"
       "alimiter=limit=0.9,loudnorm=I=-21:TP=-2:LRA=10")

# The rumble is a bed. It has to be felt under the scene without ever being the thing you hear.
RUMBLE = ("highpass=f=18,lowpass=f=140,equalizer=f=40:width_type=q:w=0.6:g=4,"
          "acompressor=threshold=-28dB:ratio=6:attack=60:release=800:makeup=4,"
          "alimiter=limit=0.9,loudnorm=I=-24:TP=-3:LRA=6")

# A shot skidding off plating is all top end and no body, and it has to cut through a fight.
DEFLECT = ("highpass=f=400,equalizer=f=900:width_type=q:w=1.2:g=-4,"
           "equalizer=f=4200:width_type=q:w=0.9:g=4,"
           "equalizer=f=9500:width_type=q:w=0.8:g=2.5,"
           "acompressor=threshold=-16dB:ratio=4:attack=1:release=80:makeup=2,"
           "alimiter=limit=0.95,loudnorm=I=-16:TP=-1.5:LRA=6")


def ticks(seconds):
    return int(seconds * SR)


def decay(n, attack, half):
    """Fast in, exponential out. `half` is the time the level takes to halve, in seconds."""
    t = np.arange(n) / SR
    rise = np.clip(t / max(attack, 1e-5), 0.0, 1.0)
    return rise * np.exp(-t * math.log(2.0) / half)


def sweep(n, start, end, seconds):
    """A sine falling from one frequency to another, phase integrated so it never clicks."""
    t = np.arange(n) / SR
    f = start * (end / start) ** np.clip(t / seconds, 0.0, 1.0)
    return np.sin(2.0 * np.pi * np.cumsum(f) / SR)


def band(x, low, high, slope=4.0):
    """Shapes a signal in the frequency domain, which needs no filter design to stay stable."""
    spectrum = np.fft.rfft(x)
    freq = np.fft.rfftfreq(len(x), 1.0 / SR)
    safe = np.maximum(freq, 1e-3)
    gain = 1.0 / np.sqrt(1.0 + (low / safe) ** (2 * slope))
    gain *= 1.0 / np.sqrt(1.0 + (safe / high) ** (2 * slope))
    return np.fft.irfft(spectrum * gain, n=len(x))


def grains(n, rng, count, low, high, half):
    """
    Debris: short bursts scattered through the tail rather than one long noise fade.

    A fading noise bed reads as wind. What falls off a leg is a handful of separate impacts, so the
    tail is built as impacts and thins out on its own.
    """
    out = np.zeros(n)
    for _ in range(count):
        start = int(rng.random() ** 1.7 * n * 0.85)
        length = ticks(rng.uniform(0.008, 0.045))
        if start + length >= n:
            continue
        grain = band(rng.normal(0.0, 1.0, length), low, high)
        grain *= decay(length, 0.0008, rng.uniform(0.004, 0.02))
        out[start:start + length] += grain * rng.uniform(0.2, 1.0)
    return out * decay(n, 0.001, half)


def metal(n, rng, partials, low, high, half):
    """The leg's own ring. Detuned on purpose: exact harmonics sound like a bell, not like a strut."""
    out = np.zeros(n)
    t = np.arange(n) / SR
    for _ in range(partials):
        f = rng.uniform(low, high)
        glide = f * (1.0 - 0.04 * np.clip(t / half, 0.0, 1.0))
        out += np.sin(2.0 * np.pi * np.cumsum(glide) / SR) * decay(n, 0.0005, half * rng.uniform(0.4, 1.3))
    return out / partials


def normalise(x, peak=0.92):
    top = np.max(np.abs(x))
    return x if top < 1e-9 else x * (peak / top)


def footfall(seed, pitch):
    """One leg landing. Sub for the mass, a body thump for the ground, metal for the leg."""
    rng = np.random.default_rng(seed)
    n = ticks(0.95)
    sub = sweep(n, 50.0 * pitch, 26.0 * pitch, 0.22) * decay(n, 0.002, 0.13) * 0.30
    body = band(rng.normal(0.0, 1.0, n), 95.0 * pitch, 1100.0 * pitch) * decay(n, 0.0012, 0.065) * 1.0
    ring = metal(n, rng, 5, 1300.0 * pitch, 3400.0 * pitch, 0.09) * 0.55
    tail = grains(n, rng, 14, 1200.0, 6500.0, 0.22) * 0.40
    return normalise(sub + body + ring + tail)


def stamp():
    """The attack a machine uses underfoot. Everything the footfall has, lower and longer."""
    rng = np.random.default_rng(4041)
    n = ticks(1.7)
    sub = sweep(n, 42.0, 18.0, 0.5) * decay(n, 0.003, 0.32) * 0.38
    crack = band(rng.normal(0.0, 1.0, n), 300.0, 9000.0) * decay(n, 0.0004, 0.006) * 1.0
    body = band(rng.normal(0.0, 1.0, n), 85.0, 900.0) * decay(n, 0.0015, 0.16) * 1.0
    ring = metal(n, rng, 6, 900.0, 2600.0, 0.16) * 0.45
    rubble = grains(n, rng, 34, 300.0, 4200.0, 0.5) * 0.5
    return normalise(sub + crack + body + ring + rubble)


def rumble():
    """
    The bed under a shake. Four seconds, faded at both ends so a repeat never clicks.

    Modulated slowly rather than held flat: ground moving is never one steady level, and a constant
    band of noise stops registering after a second or two.
    """
    rng = np.random.default_rng(909)
    n = ticks(4.0)
    bed = band(rng.normal(0.0, 1.0, n), 22.0, 95.0)
    slow = np.interp(np.arange(n), np.linspace(0, n, 26), rng.uniform(0.35, 1.0, 26))
    fade = np.clip(np.minimum(np.arange(n), n - np.arange(n)) / (0.35 * SR), 0.0, 1.0)
    return normalise(bed * slow * fade, 0.85)


def deflect():
    """A shot skidding off plating. High, short, and gone before the next arrow lands."""
    rng = np.random.default_rng(77)
    n = ticks(0.45)
    ring = metal(n, rng, 7, 2400.0, 7200.0, 0.055)
    tick = band(rng.normal(0.0, 1.0, n), 1800.0, 12000.0) * decay(n, 0.0002, 0.0035) * 0.7
    return normalise(ring + tick)


def encode(samples, chain, name):
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as raw:
        with wave.open(raw.name, "wb") as handle:
            handle.setnchannels(1)
            handle.setsampwidth(2)
            handle.setframerate(SR)
            handle.writeframes((np.clip(samples, -1.0, 1.0) * 32767.0).astype("<i2").tobytes())
        subprocess.run(
            ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", raw.name,
             "-af", chain, "-ac", "1", "-ar", str(SR), "-c:a", "libvorbis", "-q:a", "5",
             str(OUT / f"{name}.ogg")],
            check=True)
    Path(raw.name).unlink(missing_ok=True)
    print(f"{name}.ogg")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    # Three footfalls rather than one: a machine takes hundreds of steps in a night and a single
    # clip turns into a metronome by the second one.
    for index, (seed, pitch) in enumerate(((101, 1.0), (202, 0.93), (303, 1.08)), start=1):
        encode(footfall(seed, pitch), STEP, f"machine_step_{index}")
    encode(footfall(404, 0.88), FAR, "machine_step_far")
    encode(stamp(), STEP, "machine_stamp")
    encode(rumble(), RUMBLE, "machine_rumble")
    encode(deflect(), DEFLECT, "machine_deflect")


if __name__ == "__main__":
    sys.exit(main())
