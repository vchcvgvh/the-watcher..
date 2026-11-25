# The Watcher — a Fabric horror mod for Minecraft 1.21.1

Something has been watching you since your first night. It's patient, and it gets closer.

The Watcher is a sculpted, smooth 3D mesh (~21,000 polygons, not Minecraft boxes). It stands about 3.4 blocks tall and is starved and lopsided.

- **Hair:** long, wet black hair made of ~170 layered locks of fine, see-through strands, as in modern games, over a hair-covered scalp. It's swept back from the face, but a few wet locks hang over the right side.
- **Head:** a sculpted skull with a heavy brow, sharp cheekbones running back to the ears, hollow cheeks and temples, and a flat, noseless face with two slits.
- **Eyes:** real eyeballs set in almond-shaped, lidless eye openings. They're bloodshot and yellowed, with cloudy irises and pupils that glow in the dark. The lower lids sag to show red. The left eye darts around tracking you, while the lazy right one drifts on its own.
- **Mouth:** a grin torn open across both cheeks, with two rows of needle teeth (some broken) and an underbite of more needles.
- **Body:** lumpy, bruised, veined, wrinkled skin. Shadows are baked into every crease, and ribs push through the skin. The spine is twisted, the left shoulder droops, and a torn wound in its side exposes raw flesh and bare ribs.
- **Limbs:** arms that hang past its knees, one longer than the other, with visible tendons and long jointed fingers ending in dirty hooked claws.
- **Animation:**
  - It breathes shallowly, limps, and every so often its whole body shudders.
  - Its fingers curl and twitch on their own, its head jerks, and sometimes it slowly tilts its head almost sideways.
  - When it hunts, it runs low with its claws reaching for you. When it screams, it folds down into your face with its jaw unhinged.

There are no PNG files in the mod. Skin, bone, flesh and eyes are all painted in code.

## How it plays

Each player has a hidden **dread** level that rises while you play. It rises faster at night, in the dark, and deep underground. Dread unlocks five stages, and each new stage is announced with one quiet line of text.

| Stage | Reached after* | What starts happening |
|---|---|---|
| 0 | start | Nothing. Enjoy it. |
| 1 – *You feel watched.* | ~3 min | Cave noises, footsteps behind you, torches going out, **the Watcher staring from a distance** |
| 2 – *Something is wrong here.* | ~10 min | Doors opening, fake chat, whispers, hotbar swaps, **Crawlers** in the dark. **Sleeping can get you taken.** |
| 3 – *It knows where you sleep.* | ~22 min | Jump scares, **the Faceless**, night **hunts**, the Watcher **blinks closer each time you look away**, **random abductions** |
| 4 – *It's done waiting.* | ~40 min | Constant hunts, **Hollow Hound packs** at night, stalking turns into a chase |

\*Real playtime in daylight on the surface. At night or in caves it's 2–4× faster. Events also come more often at higher stages, from every 1.5–3 minutes at stage 1 to every 20–55 seconds at stage 4.

### The creatures

- **The Watcher:** about 3.4 blocks tall, with wet black hair, a grin torn across its cheeks, and mismatched glowing eyes.
  - It stalks you from a distance. From stage 3, instead of vanishing when you look at it, it waits until you look away, then it's closer.
  - At night it hunts you, putting out torches around you as it comes. Sometimes when it catches you, it drags you into the Hollow.
- **Crawler:** a starved body bent backwards, spider-walking on its back with its head upside down. It climbs walls, pounces, and burns in daylight. It lives in caves, dark places and the Hallways.
- **The Faceless:** a tall robed figure with a blank, featureless head. It can't move while anyone is looking at it. Look away and it's fast, and every time you look back it's frozen in a different pose. It hits for 5 hearts.
- **Hollow Hound:** hairless and starved, with lipless jaws and flat human front teeth. It hunts in packs; hurt one and they all come for you.

### The places it takes you

You can get taken at random (stage 3+), while sleeping (stage 2+), or when the Watcher catches you during a hunt. Each place has a way out marked by **crying obsidian**, and a sound now and then from the direction of the exit.

- **The Hallways:** endless yellowed corridors under humming, flickering fluorescent lights. Damp stains, missing ceiling tiles, vents, holes punched through walls, and the occasional lone chair. Crawlers live here.
  - **Way out:** a room with a red floor and an open door. Listen for a door closing somewhere.
- **The Hollow:** a dead black forest in thick fog and falling ash. Fallen trees, rows of graves, a ruined cabin, black pools, and lanterns left hanging from branches. Hound packs roam and the Faceless wait between the trees.
  - **Way out:** follow the soul lanterns to the stone archway.
- **The Flesh:** living red caves with walls of meat threaded with veins, glowing growths, ribcage tunnels of bone, teeth hanging from the ceiling and jutting from the floor, and pools of blood. Crawlers climb the walls.
  - **Way out:** the mouth, a round chamber ringed with teeth. Follow the heartbeat.
- **The Still Water:** an endless, misty, knee-deep black lake. Drowned trees with lanterns in their branches, sunken ruins and arches, rotting fence lines, lily pads. The Faceless stand in the water.
  - **Way out:** follow the lantern posts to a little stone island. Listen for the bell.

If you can't find the way out, you eventually **wake up** back home, after 5 to 7 minutes depending on the place. Dying also brings you back, to your normal respawn point.

## Commands & settings (need cheats/OP)

```
/watcher status                  your dread and stage
/watcher stage @s 3              skip straight to a stage
/watcher trigger jumpscare       fire any event on yourself (tab-complete for the list)
/watcher send hallways           get taken (hallways, hollow, flesh, still_water)
/watcher escape                  come back
/gamerule watcherEnabled false   turn the whole thing off
/gamerule watcherGriefing false  no torch removal, door opening or hotbar swaps
```

Each creature has a spawn egg in the Spawn Eggs tab.

## Getting it into the Modrinth App (easiest)

> **Important:** `the-watcher.zip` (this folder) is the *source code*, not a modpack. Importing it gives
> "Modpack archive is missing manifest.json". You need the **`.mrpack`** file that the build produces.

The build makes a **Modrinth modpack** (`.mrpack`). Importing it creates a ready-to-play instance (Minecraft 1.21.1 + Fabric + Fabric API + The Watcher) in one step.

**1. Build it.** Pick either option:

- **No Java needed (GitHub):** create a new GitHub repository and upload the *contents* of this folder to it (so `build.gradle` sits at the top level). Wait about 5 minutes, then open the repo's **Releases** page (right-hand side of the repo page). Download **`The-Watcher-1.9.0.mrpack`** from there. It's the real file, ready to import.
- **On your PC:** install **JDK 21** (e.g. Temurin 21 from adoptium.net), then in this folder run:
  ```bash
  ./gradlew build          # Windows: gradlew.bat build
  ```
  The first build downloads Minecraft and Fabric, which takes a few minutes. You get `build/distributions/The-Watcher-1.9.0.mrpack` and the plain mod jar in `build/libs/`.

**2. Import it.** In the Modrinth App, click **+** (Create instance) and choose **From file** (or "Import"). Select `The-Watcher-1.9.0.mrpack`, or just drag the file onto the app window.

**3. Play.** Launch the new "The Watcher" instance and make a world (cheats on if you want to use the test commands).

**Already have a Fabric 1.21.1 instance in the Modrinth App?** Open that instance's **Content** tab, click **Add content → Add from file**, and pick `the-watcher-1.9.0.jar` from `build/libs/`. Fabric API needs to be installed in that instance too.

**Test in a dev client** without any launcher: `./gradlew runClient`.

## Installing manually (other launchers)

1. Install **Fabric Loader** for Minecraft 1.21.1 (fabricmc.net/use).
2. Put **Fabric API** (for 1.21.1) and `the-watcher-1.9.0.jar` in your `mods` folder.
3. For multiplayer, the server needs both jars too. Every player needs both jars so they can see the Watcher.

## Tweaking

- How fast dread rises / stage thresholds: `horror/DreadState.java` (`STAGE_THRESHOLDS`) and `HorrorDirector.dreadGain`
- How often things happen: `HorrorDirector.COOLDOWN`
- Event weights and the stage each event unlocks at: the list at the top of `horror/HorrorEvent.java`
- Chat lines and whispers: `CHAT_LINES` / `WHISPERS` in `HorrorEvent.java`
- **The 3D model:** `client/mesh/WatcherSkeleton.java`. Body parts are lofted tubes and sculpted ellipsoids. `headShape` and `chestShape` sculpt the skull and ribcage, and `pose()` holds all the animation. Because this code doesn't depend on Minecraft, you can tweak it and preview it outside the game.
- **Textures:** `client/mesh/TexturePainter.java`. Ten 512px materials are painted in code with embossed relief: skin, bone, flesh, hair, scalp, eyes, cloth, pale skin, claw and hide. There are no image files.
- **The realms:** `gen/RealmGen.java` builds them through a plain block interface, so the same code can run outside the game for testing.
- **Hair amount, length and parting:** the hair section of the `WatcherSkeleton` constructor.

## Gore (1.9.0)
Every creature is now visibly mutilated: open wounds with bare ribs, broken bones
stabbing out of the skin (Watcher's forearm and rib, Crawler's elbows, Hound's leg),
an exposed spine, torn-open skulls, blood-soaked jaws, blood tears and dripping blood.
In game, blood drips off the creatures and sprays when they hit you or get hit.

Turn the blood particles off with `/gamerule watcherGore false`.

## Visual overhaul (1.9.0)
- Limbs are starved and sinewy: tendon cords, bone ridges pressing through, knuckly elbows and knees.
- Spines are real vertebrae, not beads. Jaws are bony and V-shaped.
- Wounds are torn craters with a ragged lip of raw flesh, hanging shreds of muscle and curled skin flaps.
- Blood is darker and more realistic, with splattered edges and drip runs that flow downward.
- Creases in the skin flush red, like blood showing under thin skin.
- The Faceless has a skull-like face with its eyes and mouth sewn shut, bony hands and a hem of torn cloth strips.

## Screen scares and notes on your PC (1.9.0)
- **Jumpscares:** the creature's real 3D model lunges at your screen, screaming, while the screen shakes and flashes red with static. This happens at random, and every time you turn around and catch the Watcher behind you.
- **Pop-up text:** big shaking, glitching red words; subliminal flashes of giant white words; quiet lines that type themselves out at the bottom of the screen. Some of them know your name.
- **Notes on your PC:** now and then the Watcher leaves a `.txt` file on your desktop and opens it in Notepad (TextEdit on Mac). It writes at most 5 per session, at least 6 minutes apart. It only creates new text files; it never changes or deletes anything. Notes can include your computer's user name and the time. That's filled in on your PC and never sent anywhere.
- All scares happen much more often than before.

Test them with `/watcher trigger screamer`, `glitch_text`, `subliminal`, `typed` or `pc_note`.

Turn things off in `.minecraft/config/thewatcher.properties` (made the first time you launch):
`jumpscares=false`, `popupText=false`, `pcNotes=false`.

## Custom sounds (1.9.0)
Every sound below was synthesized from scratch (no samples) by `tools/sfx.py`:
jumpscare stinger, two Watcher screams, a distant scream, three whispers, breathing, heartbeat, static,
glitch, typing ticks, knocking, Crawler skittering and chittering, Hound growls and howl, the Faceless's
muffled hum (its mouth is sewn shut), the realm drone, and bones cracking. They replace the vanilla sounds
for the creatures, jumpscares, pop-up text, horror events and realms. Subtitles included.

## Four different jumpscares (1.9.0)
Each creature has its own jumpscare, drawn with its real 3D model:
- **The Watcher, "turn around":** it fades out of the dark, breathing, then snaps into your face. A white flash, its jaw unhinges, it screams, and blood splatters the screen and runs down it.
- **The Faceless, "don't blink":** strobe cuts in the dark. Every flicker it's somewhere else, frozen in a different pose, closer. Then its sewn face fills the screen and its neck snaps sideways, with two bone cracks.
- **The Crawler, "from above":** dust falls, then it drops from the top of the screen, bouncing on its legs, and scuttles at you in jerky stop-motion bursts, chattering.
- **The Hollow Hound, "the charge":** it gallops at you out of the dark and bites. The screen jolts, claw gashes rip across it and blood sprays.

All of them use film grain, a vignette and static. Test each one with `/watcher jumpscare watcher`, `faceless`, `crawler` or `hound`.

## More gore (1.9.0)
- **Watcher:** its belly is torn open with its guts hanging out in loops to its thighs, and the skin is ripped off one arm, showing the muscle fibres and tendons.
- **Crawler:** both sides are torn open with guts dangling under it, and one haunch is skinned.
- **Hollow Hound:** its belly is ripped open with guts dragging underneath. Half its muzzle is skinned, showing the muscle, raw gums and the teeth down to their roots.
- **Faceless:** its robe and chest are torn open over a black hollow with its ribs bowing across it, and its left hand is stripped down to bloody finger bones.

## Dread you can see (1.9.0)
- **The dread stage shows on screen:**
  - The edges of the screen darken more at every stage.
  - From stage 2, a figure flashes at the edge of your vision for three frames.
  - From stage 3, the picture tears and glitches.
  - At stage 4 you hear your own heartbeat.
- **When a creature is near (within 32 blocks):** the screen edges pulse red in time with a heartbeat that speeds up as it gets closer. Up close, your view trembles.
- **New event, "something turns your head":** the Watcher stands right behind you, breathing. You're slowed, your neck cracks, and your camera is turned toward it a few degrees at a time, straight into the jumpscare.
- **New event, fake death:** a perfect copy of the death screen, which then glitches into "not yet."
- More, creepier messages.

Try them: `/watcher trigger forced_look`, `/watcher trigger fake_death`, `/watcher stage @s 4`.
