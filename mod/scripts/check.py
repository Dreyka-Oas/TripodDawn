"""Checks the resource pack against the Java that reads it.

Nothing here runs the game. It answers the questions a launch cannot: whether the two language files
still line up, whether every id a player sees has a name, whether a sound or a model points at a file
that exists, and whether a character that has no business in the source slipped into a string.

Exit code 0 when everything holds, 1 otherwise, so it can gate a build.

Usage: python3 scripts/check.py
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/tripoddawn"
JAVA = ROOT / "src/main/java/oas/dreyka/tripoddawn"

NAMESPACE = "tripoddawn"

problems = []


def fail(where, what):
    problems.append(f"{where}: {what}")


def load(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        fail(path.relative_to(ROOT), "missing")
    except json.JSONDecodeError as error:
        fail(path.relative_to(ROOT), f"not valid json, {error}")
    return {}


def java_ids(name):
    """The registry paths a registration file passes to its register call."""
    source = (JAVA / name).read_text(encoding="utf-8")
    return set(re.findall(r'register\w*\(\s*"([a-z0-9_.]+)"', source))


def check_lang(en, fr):
    only_en = sorted(set(en) - set(fr))
    only_fr = sorted(set(fr) - set(en))
    for key in only_en:
        fail("fr_fr.json", f"missing key {key}")
    for key in only_fr:
        fail("en_us.json", f"missing key {key}")

    # A translated line that dropped a %1$s renders the placeholder as text to the player, and the
    # game says nothing about it.
    for key in sorted(set(en) & set(fr)):
        slots_en = sorted(re.findall(r"%\d+\$s", en[key]))
        slots_fr = sorted(re.findall(r"%\d+\$s", fr[key]))
        if slots_en != slots_fr:
            fail(key, f"placeholders differ, {slots_en} against {slots_fr}")


def check_translated(en, fr, keys, what):
    for key in sorted(keys):
        for name, table in (("en_us.json", en), ("fr_fr.json", fr)):
            if key not in table:
                fail(name, f"no name for the {what} {key}")


def check_sounds(en, fr):
    entries = load(ASSETS / "sounds.json")
    declared = set(entries)
    registered = java_ids("sound/TripodDawnSounds.java")

    for path in sorted(registered - declared):
        fail("sounds.json", f"{path} is registered in Java and declared nowhere")
    for path in sorted(declared - registered):
        fail("sounds.json", f"{path} is declared and registered nowhere")

    for path, entry in sorted(entries.items()):
        subtitle = entry.get("subtitle")
        if subtitle is None:
            fail(f"sounds.json {path}", "no subtitle, so the sound is silent to a deaf player")
        else:
            for name, table in (("en_us.json", en), ("fr_fr.json", fr)):
                if subtitle not in table:
                    fail(name, f"no subtitle text for {subtitle}")
        for sound in entry.get("sounds", []):
            file = sound["name"].split(":", 1)[-1]
            if not (ASSETS / "sounds" / f"{file}.ogg").exists():
                fail(f"sounds.json {path}", f"points at sounds/{file}.ogg, which is not there")

    on_disk = {file.stem for file in (ASSETS / "sounds").glob("*.ogg")}
    used = {sound["name"].split(":", 1)[-1]
            for entry in entries.values() for sound in entry.get("sounds", [])}
    for file in sorted(on_disk - used):
        fail("sounds/", f"{file}.ogg ships and nothing plays it")


def check_geckolib():
    """The four machines and the martian, read off the renderer registrations."""
    source = (JAVA / "client/TripodDawnClient.java").read_text(encoding="utf-8")
    names = set()
    for match in re.findall(r'GeoModel<>\(([^)]*)\)', source):
        names.update(re.findall(r'"([a-z_]+)"', match))
    if not names:
        fail("TripodDawnClient.java", "no GeckoLib model name found, the check would pass on nothing")

    for name in sorted(names):
        for folder, suffix in (("models", ".geo.json"), ("animations", ".animation.json")):
            path = ASSETS / "geckolib" / folder / f"{name}{suffix}"
            if not path.exists():
                fail("geckolib", f"{name} has no {folder[:-1]} at {path.relative_to(ASSETS)}")
        if not (ASSETS / "textures/entity" / f"{name}.png").exists():
            fail("geckolib", f"{name} has no texture at textures/entity/{name}.png")


def check_particles():
    for path in sorted((ASSETS / "particles").glob("*.json")):
        for texture in load(path).get("textures", []):
            file = texture.split(":", 1)[-1]
            # A particle definition names its frames bare; the game looks them up under
            # textures/particle/ and nowhere else.
            if not (ASSETS / "textures/particle" / f"{file}.png").exists():
                fail(f"particles/{path.name}",
                     f"points at textures/particle/{file}.png, which is not there")


def check_items():
    for path in sorted((ASSETS / "items").glob("*.json")):
        if not (ASSETS / "models/item" / path.name).exists():
            fail(f"items/{path.name}", "has no matching model under models/item/")


def check_characters():
    """The house rules, applied to the files rather than to the intent."""
    banned = {
        " ": "no-break space",
        " ": "narrow no-break space",
        "​": "zero width space",
        "﻿": "byte order mark",
        "—": "em dash",
        "–": "en dash",
        "‘": "curly quote",
        "’": "curly apostrophe",
        "“": "curly double quote",
        "”": "curly double quote",
    }
    for path in sorted(list(RES.rglob("*.json")) + list(JAVA.rglob("*.java"))):
        text = path.read_text(encoding="utf-8")
        for character, name in banned.items():
            if character in text:
                line = text[:text.index(character)].count("\n") + 1
                fail(f"{path.relative_to(ROOT)}:{line}", f"holds a {name}")


def check_hardcoded():
    """A sentence built in Java never reaches fr_fr.json, so a French player reads English."""
    for path in sorted(JAVA.rglob("*.java")):
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            if "Component.literal(" in line and not line.lstrip().startswith("//"):
                fail(f"{path.relative_to(ROOT)}:{number}", "builds player text in Java")


def main():
    en = load(ASSETS / "lang/en_us.json")
    fr = load(ASSETS / "lang/fr_fr.json")

    check_lang(en, fr)
    check_translated(en, fr,
                     {f"entity.{NAMESPACE}.{path}" for path in java_ids("entity/TripodDawnEntities.java")},
                     "entity")
    check_translated(en, fr,
                     {f"item.{NAMESPACE}.{path}" for path in java_ids("item/TripodDawnItems.java")},
                     "item")
    check_translated(en, fr,
                     {f"effect.{NAMESPACE}.{path}" for path in java_ids("effect/TripodDawnEffects.java")},
                     "effect")
    check_sounds(en, fr)
    check_geckolib()
    check_particles()
    check_items()
    check_characters()
    check_hardcoded()

    if problems:
        for problem in problems:
            print(f"FAIL {problem}")
        print(f"\n{len(problems)} problemes")
        return 1
    print("tout est en place")
    return 0


if __name__ == "__main__":
    sys.exit(main())
