/*
 * Copyright (c) 2026, AJD
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.retronpcswapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;

/**
 * Registry class that maps Modern NPC IDs and Names to Retro (2004/2005) model & animation data,
 * with Category-Scoped modern animation sets.
 */
public class RetroNpcMapping
{
	private static final Map<Integer, RetroNpcData> ID_MAPPINGS = new HashMap<>();
	private static final Map<String, RetroNpcData> NAME_MAPPINGS = new HashMap<>();

	/**
	 * NPCs that must never be swapped, whatever the name and id tables say.
	 *
	 * <p>Matching is mostly by name, which is what lets a 2005 name keep working across every
	 * modern variant of an NPC - but a name is not always a costume. Several NPCs called "Guard"
	 * carry a bow, and handing them the 2005 sword-and-shield kit takes the bow away.
	 */
	private static final Set<Integer> EXCLUDED_IDS = Set.of(
		// Exclude specific Falador guards/Varrock guards
		NpcID.FAI_FALADOR_GUARD2, NpcID.FAI_FALADOR_GUARD2_F,
		NpcID.FAI_FALADOR_GUARD4, NpcID.FAI_FALADOR_GUARD5, NpcID.FAI_FALADOR_GUARD6,
		NpcID.FAI_VARROCK_GUARD,
		// Named "Cow" so the name row reaches it, but it is model 14102 on anims 180/229 - a mount,
		// not a cow, and it would be handed the retro cow mesh
		NpcID.OSB8_COW,
		// Exclude goblins/zombies from the Surprise Exam random event
		NpcID.PATTERN_GOBLIN1_DESK, NpcID.PATTERN_GOBLIN2_DESK, NpcID.PATTERN_ZOMBIE_DESK,
		// Exclude Vetion summons
		NpcID.VETION_HELLHOUND_JNR, NpcID.VETION_HELLHOUND_JNR_SINGLES
	);

	/**
	 * Categories that resolve from the registered id list only, never from a name.
	 *
	 * <p>Suppressed at lookup rather than by dropping the name row, because
	 * {@link #applyCacheDefinitions} walks {@code NAME_MAPPINGS} to graft the opcode 40 pairs
	 * onto the archetypes.
	 */
	private static final Set<RetroNpcCategory> ID_ONLY_CATEGORIES =
		Set.of(RetroNpcCategory.GUARDS);

	// Category-Scoped Modern Animation Sets. Values are gameval AnimationID constants where the
	// modern cache has them; retro 2005 sequence IDs used elsewhere in this class stay numeric
	// where no gameval name exists

	public static final Set<Integer> DEMON_MODERN_ATTACKS = Set.of(
		AnimationID.DEMON_ATTACK_GREATER,
		AnimationID.DEMON_UPDATE_ATTACK, AnimationID.DEMON_UPDATE_ATTACK_GREATER,
		AnimationID.DEMON_UPDATE_ATTACK_LESSER, AnimationID.DEMONS_ATTACK,
		// The DT2_SCAR_MAZE mage and ranged demons are registered to LESSER_DEMON_DEFAULT, so
		// their cast and swipe belong here too, or they would play un-intercepted
		AnimationID.DEMON_UPDATE_SWIPE, AnimationID.DEMON_UPDATE_FIREBALL_CAST
	);
	public static final Set<Integer> DEMON_MODERN_DEFENDS = Set.of(
		AnimationID.DEMON_UPDATE_DEFEND
	);
	public static final Set<Integer> DEMON_MODERN_DEATHS = Set.of(
		AnimationID.DEMON_UPDATE_DEATH, AnimationID.DEMONS_DEATH
	);

	// Dragons kept their 2005 sequences: DRAGON_WALK/ATTACK/BLOCK/READY/HEAD_ATTACK/DEATH are
	// 79/80/89/90/91/92, the very IDs the 2005 cache uses, and there is no DRAGON_UPDATE_* rework
	// family of the kind skeletons, zombies and giants got. Confirmed against the live cache: every
	// adult dragon definition still has standingAnim 90 and walkingAnim 79.
	public static final Set<Integer> DRAGON_MODERN_ATTACKS = Set.of(
		AnimationID.DRAGON_RANGED_ATTACKS
	);
	// The King Black Dragon is the one adult dragon with a post-2005 sequence of its own: it blocks
	// on DRAGON_BLOCK_KBD 4638 rather than the 2005 DRAGON_BLOCK 89.
	public static final Set<Integer> DRAGON_MODERN_DEFENDS = Set.of(
		AnimationID.DRAGON_BLOCK_KBD
	);
	public static final Set<Integer> DRAGON_MODERN_DEATHS = Set.of();

	public static final Set<Integer> GOBLIN_MODERN_ATTACKS = Set.of(
		AnimationID.SLICE_SURFACE_GOBLIN_SQUAT_UNARMED_ATTACK, AnimationID.SLICE_SURFACE_GOBLIN_ARMED_ATTACK,
			AnimationID.SLICE_SURFACE_GOBLIN_SQUAT_ATTACK_SPEAR, AnimationID.SLICE_SURFACE_GOBLIN_SERGENT_ATTACK,
			AnimationID.GOBLIN_ATTACK_UNARMED, AnimationID.GOBLIN_ATTACK_ARMED,
			// The shield-and-spear goblin is its own pose family - GOBLIN_RED_SOLDIER_5 and
			// GOBLIN_GREEN_SOLDIER_4 stand on 6200 and walk on 6201 where every other goblin uses
			// 6181/6186 - and its attack was the one left un-intercepted, so those two swung a
			// modern animation on a retro mesh
			AnimationID.SLICE_SURFACE_GOBLIN_SQUAT_SPEAR_ATTACK_SHIELD
	);
	public static final Set<Integer> GOBLIN_MODERN_DEFENDS = Set.of(
		AnimationID.SLICE_SURFACE_GOBLIN_DEFEND, AnimationID.GOBLIN_BLOCK,
			AnimationID.SLICE_SURFACE_GOBLIN_DEFEND_SPEAR, AnimationID.SLICE_SURFACE_GOBLIN_SERGENT_DEFEND
	);
	public static final Set<Integer> GOBLIN_MODERN_DEATHS = Set.of(
		AnimationID.SLICE_SURFACE_GOBLIN_DEATH, AnimationID.SLICE_SURFACE_GOBLIN_DEATH_SPEAR,
			AnimationID.SLICE_ARROW_DEATH, AnimationID.GOBLIN_DEATH, AnimationID.SLICE_SURFACE_GOBLIN_SERGENT_DEATH,
			AnimationID.SLICE_SURFACE_GOBLIN_DEATH_BY_ARROW, AnimationID.SLICE_SURFACE_GOBLIN_DEATH_BY_FIREBOLT
	);

	public static final Set<Integer> GUARD_MODERN_ATTACKS = Set.of(
		AnimationID.HUMAN_UNARMEDPUNCH, AnimationID.HUMAN_UNARMEDKICK
	);
	public static final Set<Integer> GUARD_MODERN_DEFENDS = Set.of(
		AnimationID.HUMAN_UNARMEDBLOCK
	);
	public static final Set<Integer> GUARD_MODERN_DEATHS = Set.of(
		AnimationID.HUMAN_DEATH
	);

	public static final Set<Integer> IMP_MODERN_ATTACKS = Set.of(
		AnimationID.IMP_ATTACK
	);
	public static final Set<Integer> IMP_MODERN_DEFENDS = Set.of(
		AnimationID.IMP_BLOCK
	);
	public static final Set<Integer> IMP_MODERN_DEATHS = Set.of(
		AnimationID.IMP_DEATH
	);

	public static final Set<Integer> SKELETON_MODERN_ATTACKS = Set.of(
		AnimationID.SKELETON_ATTACK, AnimationID.HUMAN_SWORD_SLASH, AnimationID.HUMAN_BLUNT_SPIKE,
		AnimationID.HUMAN_BLUNT_POUND, AnimationID.HUMAN_STAFF_SPIKE, AnimationID.HUMAN_STAFF_PUMMEL,
		AnimationID.HUMAN_UNARMEDPUNCH, AnimationID.HUMAN_UNARMEDKICK, AnimationID.HUMAN_BOW,
		AnimationID.HUMAN_SPEAR_SPIKE, AnimationID.HUMAN_SCYTHE_SWEEP,
		AnimationID.SKELETON_UPDATE_ATTACK_WEAPON, AnimationID.SKELETON_UPDATE_ATTACK_WEAPON_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_ATTACK_SWORD, AnimationID.SKELETON_UPDATE_ATTACK_SWORD_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_GIANT_ATTACK, AnimationID.SKELETON_UPDATE_GIANT_ATTACK_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_GIANT_VARY3_ATTACK, AnimationID.SKELETON_UPDATE_CHAMPION_ATTACK
	);
	public static final Set<Integer> SKELETON_MODERN_DEFENDS = Set.of(
		AnimationID.SKELETON_BLOCK,
		AnimationID.HUMAN_UNARMEDBLOCK, AnimationID.HUMAN_UNARMED_DEF, AnimationID.HUMAN_SHIELD_DEFENCE,
		AnimationID.SKELETON_UPDATE_DEFEND, AnimationID.SKELETON_UPDATE_DEFEND_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_GIANT_DEFEND, AnimationID.SKELETON_UPDATE_GIANT_DEFEND_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_GIANT_VARY3_DEFEND
	);
	public static final Set<Integer> SKELETON_MODERN_DEATHS = Set.of(
		 AnimationID.SKELETON_DEATH, AnimationID.HUMAN_DEATH,
		AnimationID.SKELETON_UPDATE_DEATH, AnimationID.SKELETON_UPDATE_DEATH_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_GIANT_DEATH, AnimationID.SKELETON_UPDATE_GIANT_DEATH_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_GIANT_VARY3_DEATH,
		AnimationID.GODWARS_GOBLIN_UPDATE_BANNER_DEATH
	);

	public static final Set<Integer> ZOMBIE_MODERN_ATTACKS = Set.of(
		AnimationID.ZOMBIE_UPDATE_ATTACK_NORMAL, AnimationID.ZOMBIE_UPDATE_ATTACK_WEAPON,
		AnimationID.ZOMBIE_UPDATE_ATTACK_DRAGGING, AnimationID.ZOMBIE_UPDATE_ATTACK_CHAMPION,
		AnimationID.HUMAN_UNARMEDPUNCH, AnimationID.HUMAN_UNARMEDKICK, AnimationID.HUMAN_STAFF_SPIKE,
		AnimationID.SKELETON_UPDATE_CHAMPION_ATTACK, AnimationID.ZOMBIE_ATTACK
	);
	public static final Set<Integer> ZOMBIE_MODERN_DEFENDS = Set.of(
		AnimationID.ZOMBIE_UPDATE_DEFEND_NORMAL, AnimationID.ZOMBIE_UPDATE_DEFEND_WEAPON,
		AnimationID.ZOMBIE_UPDATE_DEFEND_DRAGGING,
		AnimationID.HUMAN_UNARMEDBLOCK, AnimationID.HUMAN_UNARMED_DEF, AnimationID.HUMAN_SHIELD_DEFENCE,
		AnimationID.ZOMBIE_BLOCK
	);
	public static final Set<Integer> ZOMBIE_MODERN_DEATHS = Set.of(
		AnimationID.ZOMBIE_UPDATE_DEATH_NORMAL, AnimationID.ZOMBIE_UPDATE_DEATH_WEAPON,
		AnimationID.ZOMBIE_UPDATE_DEATH_DRAGGING, AnimationID.ZOMBIE_UPDATE_DESPAWN,
		AnimationID.HUMAN_DEATH, AnimationID.ZOMBIE_DEATH
	);

	// GHOST_ATTACK/GHOST_BLOCK/GHOST_DEATH (123/124/126) are deliberately absent here: they are
	// the surviving 2005 sequences themselves - the retro targets, not modern anims to intercept.
	public static final Set<Integer> GHOST_MODERN_ATTACKS = Set.of(
		AnimationID.GHOST_UPDATE_NORMAL_ATTACK, AnimationID.BOSSGHOST_ATTACK
	);
	public static final Set<Integer> GHOST_MODERN_DEFENDS = Set.of(
		AnimationID.GHOST_UPDATE_NORMAL_DEFEND, AnimationID.BOSSGHOST_FADE_OUT
	);
	public static final Set<Integer> GHOST_MODERN_DEATHS = Set.of(
		AnimationID.GHOST_UPDATE_NORMAL_DEATH, AnimationID.BOSSGHOST_DEATH
	);

	public static final Set<Integer> GIANT_MODERN_ATTACKS = Set.of(
		AnimationID.GIANT_UPDATE_BASIC_ATTACK, AnimationID.GIANT_UPDATE_MOSS_ATTACK,
		AnimationID.GIANT_UPDATE_FIRE_ATTACK, AnimationID.GIANT_UPDATE_FIRE_SWORD_ATTACK,
		AnimationID.GIANT_UPDATE_ICE_ATTACK, AnimationID.GIANT_ATTACK
	);
	public static final Set<Integer> GIANT_MODERN_DEFENDS = Set.of(
		AnimationID.GIANT_UPDATE_BASIC_DEFEND, AnimationID.GIANT_UPDATE_MOSS_DEFEND,
		AnimationID.GIANT_UPDATE_FIRE_SWORD_DEFEND, AnimationID.GIANT_UPDATE_ICE_DEFEND,
		AnimationID.GIANT_BLOCK
	);
	public static final Set<Integer> GIANT_MODERN_DEATHS = Set.of(
		AnimationID.GIANT_UPDATE_BASIC_DEATH, AnimationID.GIANT_UPDATE_MOSS_DEATH,
		AnimationID.GIANT_UPDATE_FIRE_DEATH, AnimationID.GIANT_UPDATE_ICE_DEATH,
		AnimationID.GIANT_DEATH
	);

	// Ernest's rooster is on the rooster sequences rather than the chicken ones, so those ids
	// belong here too - left out they would drive the 2005 mesh off a modern framemap. Its stand
	// and walk (ROOSTERREADY, ROOSTERWALK) are deliberately absent: applyRetroSwap replaces the
	// pose slots outright rather than intercepting them. ROOSTERMAGIC is absent too - no rooster
	// casts anything, and nothing that does is mapped to this category.
	public static final Set<Integer> CHICKEN_MODERN_ATTACKS = Set.of(
		AnimationID.LORE_CHICKEN_ATTACK, AnimationID.CHICKEN_ATTACK, AnimationID.ROOSTERATTACK
	);
	public static final Set<Integer> CHICKEN_MODERN_DEFENDS = Set.of(
		AnimationID.LORE_CHICKEN_DEFEND, AnimationID.CHICKEN_BLOCK, AnimationID.ROOSTERPARRY
	);
	public static final Set<Integer> CHICKEN_MODERN_DEATHS = Set.of(
		AnimationID.LORE_CHICKEN_DEATH, AnimationID.CHICKEN_DEATH, AnimationID.ROOSTERDEATH
	);

	public static final Set<Integer> COW_MODERN_ATTACKS = Set.of(
		AnimationID.COW_UPDATE_ATTACK, AnimationID.COW_ATTACK
	);
	public static final Set<Integer> COW_MODERN_DEFENDS = Set.of(
		AnimationID.COW_UPDATE_DEFEND, AnimationID.COW_BLOCK
	);
	public static final Set<Integer> COW_MODERN_DEATHS = Set.of(
		AnimationID.COW_UPDATE_DEATH, AnimationID.COW_DEATH
	);

	/**
	 * Modern cow animations with no 2005 counterpart, redirected to the retro idle.
	 *
	 * <p>Every one of these is keyed to framemap 1338, the modern cow rig, and reaches only 54-60%
	 * of the retro mesh's vertex groups - they visibly bend it. The legacy cow animations 2162,
	 * 2303 and 2312 are deliberately absent: they sit on framemap 282 like the 2005 sequences do
	 * and reach 85-100%, so they animate the retro mesh correctly and are left to play.
	 */
	public static final Set<Integer> COW_MODERN_MISC = Set.of(
		AnimationID.COW_GRAZE, AnimationID.COW_UPDATE_READY,
		AnimationID.COW_UPDATE_GRAZE, AnimationID.COW_UPDATE_DAIRY
	);

	// HELL_ATTACK/HELL_BLOCK/HELL_DEATH (158/159/161) are deliberately absent: they are the surviving
	// 2005 sequences themselves - the retro targets, not modern anims to intercept.
	//
	// Combat sequences are not part of a definition, so which of these a live hellhound plays cannot
	// be read from the cache. What was measured is that every live hellhound pose and every candidate
	// here resolves to framemap 1491, the modern dog rig, and any 1491 sequence bends mesh 2997. So
	// the whole DOG_UPDATE combat family is listed rather than a guessed subset; the sets are scoped
	// to this category, so the extra ids only ever apply to hellhounds.
	public static final Set<Integer> HELLHOUND_MODERN_ATTACKS = Set.of(
		AnimationID.DOG_UPDATE_SMALL_DOG_ATTACK, AnimationID.DOG_UPDATE_MEDIUM_DOG_ATTACK,
		AnimationID.DOG_UPDATE_WOLF_ATTACK, AnimationID.DOG_UPDATE_JACKAL_ATTACK,
		AnimationID.DOG_UPDATE_FIGHT_ARENA_ATTACK, AnimationID.DOG_UPDATE_HELLHOUND_GODWARS_ATTACK
	);
	public static final Set<Integer> HELLHOUND_MODERN_DEFENDS = Set.of(
		AnimationID.DOG_UPDATE_MEDIUM_DOG_DEFEND, AnimationID.DOG_UPDATE_WOLF_DEFEND,
		AnimationID.DOG_UPDATE_JACKAL_DEFEND, AnimationID.DOG_UPDATE_FOX_DEFEND,
		AnimationID.DOG_UPDATE_FIGHT_ARENA_DEFEND, AnimationID.DOG_UPDATE_GODWARS_DEFEND
	);
	public static final Set<Integer> HELLHOUND_MODERN_DEATHS = Set.of(
		AnimationID.DOG_UPDATE_MEDIUM_DOG_DEATH, AnimationID.DOG_UPDATE_WOLF_DEATH,
		AnimationID.DOG_UPDATE_JACKAL_DEATH, AnimationID.DOG_UPDATE_FOX_DEATH,
		AnimationID.DOG_UPDATE_HELLHOUND_GODWARS_DEATH
	);

	/**
	 * The God Wars hellhound's respawn, redirected to the retro idle. It is the death played in
	 * reverse, on framemap 1491 like the rest, and reaches only 65% of mesh 2997.
	 */
	public static final Set<Integer> HELLHOUND_MODERN_MISC = Set.of(
		AnimationID.DOG_UPDATE_HELLHOUND_GODWARS_DEATH_REVERSE
	);

	// The skeleton hellhound is on the same modern dog rig (framemap 1491, 36-45% reach on 4974),
	// with an attack and defend of its own on top of the family. SKELETON_HOUND_ATTACK/BLOCK/DEATH
	// (1495/1496/1497) are its surviving 2005 sequences and deliberately absent.
	public static final Set<Integer> SKELETON_HELLHOUND_MODERN_ATTACKS = Set.of(
		AnimationID.DOG_UPDATE_SKELETON_HELLHOUND_ATTACK,
		AnimationID.DOG_UPDATE_SMALL_DOG_ATTACK, AnimationID.DOG_UPDATE_MEDIUM_DOG_ATTACK,
		AnimationID.DOG_UPDATE_WOLF_ATTACK, AnimationID.DOG_UPDATE_JACKAL_ATTACK,
		AnimationID.DOG_UPDATE_FIGHT_ARENA_ATTACK, AnimationID.DOG_UPDATE_HELLHOUND_GODWARS_ATTACK
	);
	public static final Set<Integer> SKELETON_HELLHOUND_MODERN_DEFENDS = Set.of(
		AnimationID.DOG_UPDATE_SKELETON_HELLHOUND_DEFEND,
		AnimationID.DOG_UPDATE_MEDIUM_DOG_DEFEND, AnimationID.DOG_UPDATE_WOLF_DEFEND,
		AnimationID.DOG_UPDATE_JACKAL_DEFEND, AnimationID.DOG_UPDATE_FOX_DEFEND,
		AnimationID.DOG_UPDATE_FIGHT_ARENA_DEFEND, AnimationID.DOG_UPDATE_GODWARS_DEFEND
	);

	// Pre-instantiated immutable archetypes.
	//
	// The blockers are not all the same, and they no longer all stand:
	//   - adult dragons, demons: the 2005 meshes were replaced at their ids and exist nowhere in
	//     the live cache. This used to say nothing short of an asset-injection API could unblock
	//     them - that turned out to be the thing to build. They now render from injected geometry
	//     driven by the surviving 2005 sequences, gated behind the injection pipeline toggle.
	//   - imps: mesh 2887 is preserved exactly, but the animation frames behind the surviving
	//     sequence ids were re-authored for the modern rig. This used to say injection did not
	//     help, on the reasoning that a mesh needing no replacement gains nothing from it - which
	//     was wrong. Injection is what makes it possible to skin the model against the 2005 frames
	//     rather than the client's, so imps now ship the same way, behind their own toggle.
	//   - baby dragons: the same re-authored-frames problem, and the same fix applied. Bundled and
	//     shipping under the dragon toggle.
	//   - the giant family: never an animation problem at all - sequences 127-131 still resolve to
	//     framemap 302 and still fit the 2005 body, which survives. It is a geometry problem, and
	//     only for the heads: all five variants are body 2870 wearing a different head, and only
	//     the fire giant's survived. Hill giants render either way, wearing a Jogre head on the
	//     cache path and their real one when injected; the rest have no head in the live cache at
	//     all, so requiresInjectedGeometry keeps the cache path from drawing them wrong.
	//   - guards: this used to say an animation-only swap with no retro model at all. That was
	//     wrong twice over. The 2005 definition names nine parts and every one of them decodes;
	//     three of the nine (head 294, arms 151, hands 254) had their ids reused, which is what
	//     made the cache-backed path unable to assemble a whole guard. And they need the 2005
	//     clips for a reason no other category has: the surviving parts are byte-identical in both
	//     caches but their vertex groups were RENUMBERED, from a ~35 group 2005 human rig to
	//     framemap 0's 218. Same geometry, different bones.
	public static final RetroNpcData LESSER_DEMON_DEFAULT = RetroNpcData.builder()
		.category(RetroNpcCategory.LESSER_DEMONS)
		.retroModelIds(new int[]{2943})
		.idleAnimationId(AnimationID.DEMON_READY)
		.walkAnimationId(AnimationID.DEMON_WALK)
		.attackAnimationId(AnimationID.DEMON_ATTACK)
		.defendAnimationId(AnimationID.DEMON_BLOCK)
		.deathAnimationId(AnimationID.DEMON_DEATH)
		.modernAttackAnims(DEMON_MODERN_ATTACKS)
		.modernDefendAnims(DEMON_MODERN_DEFENDS)
		.modernDeathAnims(DEMON_MODERN_DEATHS)
		.build();

	public static final RetroNpcData GREATER_DEMON_DEFAULT = RetroNpcData.builder()
		.category(RetroNpcCategory.GREATER_DEMONS)
		.retroModelIds(new int[]{2942})
		.idleAnimationId(AnimationID.DEMON_READY)
		.walkAnimationId(AnimationID.DEMON_WALK)
		.attackAnimationId(AnimationID.DEMON_ATTACK)
		.defendAnimationId(AnimationID.DEMON_BLOCK)
		.deathAnimationId(AnimationID.DEMON_DEATH)
		.modernAttackAnims(DEMON_MODERN_ATTACKS)
		.modernDefendAnims(DEMON_MODERN_DEFENDS)
		.modernDeathAnims(DEMON_MODERN_DEATHS)
		.build();

	// Black demons share mesh 2942 with greater demons and differ only by the 2005 opcode 40 pairs
	// (918 -> 4, 929 -> 4, 0 -> 931), which applyCacheDefinitions grafts on from the JSON row.
	public static final RetroNpcData BLACK_DEMON_DEFAULT = RetroNpcData.builder()
		.category(RetroNpcCategory.BLACK_DEMONS)
		.retroModelIds(new int[]{2942})
		.idleAnimationId(AnimationID.DEMON_READY)
		.walkAnimationId(AnimationID.DEMON_WALK)
		.attackAnimationId(AnimationID.DEMON_ATTACK)
		.defendAnimationId(AnimationID.DEMON_BLOCK)
		.deathAnimationId(AnimationID.DEMON_DEATH)
		.modernAttackAnims(DEMON_MODERN_ATTACKS)
		.modernDefendAnims(DEMON_MODERN_DEFENDS)
		.modernDeathAnims(DEMON_MODERN_DEATHS)
		.build();

	// Retro mesh 2998 carries no color of its own - its whole palette (0, 41, 61, 127) is
	// saturation 0, a greyscale ramp - so the 2005 client gave each dragon its color by
	// recoloring one index. Index 61 is 57% of the mesh; the rest is black, shadow and highlight
	// detail that stayed gray in 2005 too.
	private static final int DRAGON_BODY_GREY = 61;

	// Colors as the 2005 cache specified them. Baby blue comes from the 2005 "Baby blue dragon"
	// (def 52) and is the same value the adult "Blue dragon" (def 55) uses - which is what makes
	// the other three trustworthy: every 2005 dragon recolors DRAGON_BODY_GREY to its own color,
	// so the adult defs supply the colors for the baby variants that postdate the cache.
	private static final int DRAGON_BLUE = -25049;
	private static final int DRAGON_RED = 687;     // 2005 "Red dragon", def 53
	private static final int DRAGON_GREEN = 22051; // 2005 "Green dragon", def 941
	private static final int DRAGON_BLACK = 16;    // 2005 "Black dragon", def 54

	private static RetroNpcData babyDragon(int bodyColor)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.BABY_DRAGONS)
			.retroModelIds(new int[]{2998})
			.idleAnimationId(AnimationID.BDRAG_READY)
			.walkAnimationId(AnimationID.BDRAG_WALK)
			.recolor(DRAGON_BODY_GREY, bodyColor)
			.build();
	}

	// Only the blue variant existed in February 2005, so the other three are registered here
	// rather than coming from the generated JSON. Combat slots stay -1 for the same reason as the
	// BABY_DRAGONS branch in createMappingData.
	public static final RetroNpcData BABY_BLUE_DRAGON = babyDragon(DRAGON_BLUE);
	public static final RetroNpcData BABY_RED_DRAGON = babyDragon(DRAGON_RED);
	public static final RetroNpcData BABY_GREEN_DRAGON = babyDragon(DRAGON_GREEN);
	public static final RetroNpcData BABY_BLACK_DRAGON = babyDragon(DRAGON_BLACK);

	public static final RetroNpcData IMP_DEFAULT = RetroNpcData.builder()
		.category(RetroNpcCategory.IMPS)
		.retroModelIds(new int[]{2887})
		.idleAnimationId(171)
		.walkAnimationId(168)
		.attackAnimationId(169)
		.defendAnimationId(170)
		.deathAnimationId(172)
		.modernAttackAnims(IMP_MODERN_ATTACKS)
		.modernDefendAnims(IMP_MODERN_DEFENDS)
		.modernDeathAnims(IMP_MODERN_DEATHS)
		.build();

	public static final RetroNpcData SKELETON_UNARMED = RetroNpcData.builder()
		.category(RetroNpcCategory.SKELETONS)
		.retroModelIds(new int[]{2944})
		.idleAnimationId(262)
		.walkAnimationId(259)
		.attackAnimationId(260)
		.defendAnimationId(261)
		.deathAnimationId(263)
		.modernAttackAnims(SKELETON_MODERN_ATTACKS)
		.modernDefendAnims(SKELETON_MODERN_DEFENDS)
		.modernDeathAnims(SKELETON_MODERN_DEATHS)
		.build();

	public static final RetroNpcData SKELETON_ARMED = RetroNpcData.builder()
		.category(RetroNpcCategory.SKELETONS)
		.retroModelIds(new int[]{2944, 2946})
		.idleAnimationId(262)
		.walkAnimationId(259)
		.attackAnimationId(260)
		.defendAnimationId(261)
		.deathAnimationId(263)
		.modernAttackAnims(SKELETON_MODERN_ATTACKS)
		.modernDefendAnims(SKELETON_MODERN_DEFENDS)
		.modernDeathAnims(SKELETON_MODERN_DEATHS)
		.build();

	// The 2005 giant skeleton is def 93: named plain "Skeleton", level 45, and nothing but the armed
	// kit scaled up - the same name and level live GIANTSKELETON still carries. The generator keeps
	// only the lowest id per name, which is why npc-mappings.json never showed it.
	private static final int GIANT_SKELETON_SCALE = 170;

	public static final RetroNpcData SKELETON_GIANT = RetroNpcData.builder()
		.category(RetroNpcCategory.SKELETONS)
		.retroModelIds(new int[]{2944, 2946})
		.idleAnimationId(262)
		.walkAnimationId(259)
		.attackAnimationId(260)
		.defendAnimationId(261)
		.deathAnimationId(263)
		.scaleXZ(GIANT_SKELETON_SCALE)
		.scaleY(GIANT_SKELETON_SCALE)
		.modernAttackAnims(SKELETON_MODERN_ATTACKS)
		.modernDefendAnims(SKELETON_MODERN_DEFENDS)
		.modernDeathAnims(SKELETON_MODERN_DEATHS)
		.build();

	public static final RetroNpcData ZOMBIE_UNARMED = RetroNpcData.builder()
		.category(RetroNpcCategory.ZOMBIES)
		.retroModelIds(new int[]{2931})
		.idleAnimationId(301)
		.walkAnimationId(298)
		.attackAnimationId(299)
		.defendAnimationId(300)
		.deathAnimationId(302)
		.modernAttackAnims(ZOMBIE_MODERN_ATTACKS)
		.modernDefendAnims(ZOMBIE_MODERN_DEFENDS)
		.modernDeathAnims(ZOMBIE_MODERN_DEATHS)
		.build();

	public static final RetroNpcData ZOMBIE_ARMED = RetroNpcData.builder()
		.category(RetroNpcCategory.ZOMBIES)
		.retroModelIds(new int[]{2931, 2932})
		.idleAnimationId(301)
		.walkAnimationId(298)
		.attackAnimationId(299)
		.defendAnimationId(300)
		.deathAnimationId(302)
		.modernAttackAnims(ZOMBIE_MODERN_ATTACKS)
		.modernDefendAnims(ZOMBIE_MODERN_DEFENDS)
		.modernDeathAnims(ZOMBIE_MODERN_DEATHS)
		.build();

	/**
	 * The nine 2005 parts of a guard's kit, in the order the definition lists them: torso, a strap,
	 * the head, arms, legs, hands, boots, and the two held items.
	 */
	private static final int[] GUARD_PARTS = {233, 246, 294, 151, 176, 254, 185, 519, 541};

	/**
	 * The same kit with the battleaxe in place of the sword, for Falador's axe guard.
	 */
	private static final int[] GUARD_AXE_PARTS = {233, 246, 294, 151, 176, 254, 185, 550, 541};

	/**
	 * The seven 2005 parts of an Ardougne guard, in the order the definition lists them. Six of
	 * body and kit plus one held weapon - no shield, and the live NPC carries none either.
	 */
	private static final int[] ARDOUGNE_GUARD_PARTS = {225, 301, 162, 179, 274, 185, 502};

	/**
	 * The same kit with no weapon, for the guards posted inside the Carnillean mansion.
	 */
	private static final int[] ARDOUGNE_CARNILLEAN_PARTS = {225, 301, 162, 179, 274, 185};

	/**
	 * The 2005 Ardougne guard's opcode 40 pairs, from definition 32.
	 */
	private static final short[] ARDOUGNE_RECOLOR_FIND = {25238, 8741};
	private static final short[] ARDOUGNE_RECOLOR_REPLACE = {811, -21597};

	/**
	 * The male bow guard from the live cache
	 */
	private static final int[] BOW_GUARD_LIVE_PARTS =
		{233, 250, 9458, 9450, 176, 28285, 185, 563, 215};

	/**
	 * NPCs in an injection-only category that are built from the live cache
	 */
	private static final Set<Integer> LIVE_GEOMETRY_IDS = Set.of(NpcID.FAI_FALADOR_GUARD4_F);

	/**
	 * Whether this NPC is built from the live cache even though its category is injection-only.
	 */
	public static boolean usesLiveGeometry(int npcId)
	{
		return LIVE_GEOMETRY_IDS.contains(npcId);
	}

	public static final RetroNpcData GUARD_DEFAULT = RetroNpcData.builder()
		.category(RetroNpcCategory.GUARDS)
		.retroModelIds(GUARD_PARTS)
		.idleAnimationId(808)
		.walkAnimationId(819)
		.attackAnimationId(422)
		.defendAnimationId(424)
		.deathAnimationId(836)
		.modernAttackAnims(GUARD_MODERN_ATTACKS)
		.modernDefendAnims(GUARD_MODERN_DEFENDS)
		.modernDeathAnims(GUARD_MODERN_DEATHS)
		.build();

	/**
	 * Builds an Ardougne guard. The town's own 2005 costume, not the Varrock kit in other colors:
	 * seven different meshes, of which only the boots (185) are shared with the town guard
	 */
	private static RetroNpcData ardougneGuard(int[] models)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.GUARDS)
			.retroModelIds(models)
			.idleAnimationId(808)
			.walkAnimationId(819)
			.attackAnimationId(422)
			.defendAnimationId(424)
			.deathAnimationId(836)
			.modernAttackAnims(GUARD_MODERN_ATTACKS)
			.modernDefendAnims(GUARD_MODERN_DEFENDS)
			.modernDeathAnims(GUARD_MODERN_DEATHS)
			.recolors(ARDOUGNE_RECOLOR_FIND, ARDOUGNE_RECOLOR_REPLACE)
			.build();
	}

	public static final RetroNpcData ARDOUGNE_GUARD_DEFAULT = ardougneGuard(ARDOUGNE_GUARD_PARTS);

	public static final RetroNpcData CARNILLEAN_GUARD_DEFAULT = ardougneGuard(ARDOUGNE_CARNILLEAN_PARTS);

	/**
	 * The 2005 body every giant and the cyclops is built on. Preserved in the live cache - 177
	 * vertices in both, 355 faces against 347 - so the cache-backed path can still load it.
	 */
	private static final int GIANT_BODY = 2870;

	/**
	 * The Jogre head the cache path wears. The real 2005 hill giant head is 2862, but that id now
	 * holds unrelated 466-vertex geometry, so only the bundle can supply the right one.
	 */
	private static final int JOGRE_HEAD = 2866;

	/**
	 * Builds one member of the giant family. They differ only in their parts and their 2005 recolor
	 * pairs; the animations are shared, which is why every variant resolves to framemap 302.
	 */
	private static RetroNpcData giant(RetroNpcCategory category, int[] models, int[] injectedModels)
	{
		return RetroNpcData.builder()
			.category(category)
			.retroModelIds(models)
			.injectedModelIds(injectedModels)
			.idleAnimationId(AnimationID.GIANT_READY)
			.walkAnimationId(AnimationID.GIANT_WALK)
			.attackAnimationId(AnimationID.GIANT_ATTACK)
			.defendAnimationId(AnimationID.GIANT_BLOCK)
			.deathAnimationId(AnimationID.GIANT_DEATH)
			.modernAttackAnims(GIANT_MODERN_ATTACKS)
			.modernDefendAnims(GIANT_MODERN_DEFENDS)
			.modernDeathAnims(GIANT_MODERN_DEATHS)
			.build();
	}

	/**
	 * The one member of the family that works without injection: its body survives, and the Jogre
	 * head stands in for the head that does not. The injected path takes the real 2862 head instead.
	 */
	public static final RetroNpcData HILL_GIANT_DEFAULT =
		giant(RetroNpcCategory.HILL_GIANTS, new int[]{GIANT_BODY, JOGRE_HEAD}, new int[]{GIANT_BODY, 2862});

	// The remaining variants have no live counterpart for their heads at all, so their model ids are
	// bundle ids on both fields - the cache path would load unrelated geometry and is gated off.
	public static final RetroNpcData FIRE_GIANT_DEFAULT =
		giant(RetroNpcCategory.FIRE_GIANTS, new int[]{GIANT_BODY, 2864, 4991, 4990}, null);

	public static final RetroNpcData ICE_GIANT_DEFAULT =
		giant(RetroNpcCategory.ICE_GIANTS, new int[]{GIANT_BODY, 2868}, null);

	public static final RetroNpcData MOSS_GIANT_DEFAULT =
		giant(RetroNpcCategory.MOSS_GIANTS, new int[]{GIANT_BODY, 2865, 4990}, null);

	public static final RetroNpcData CYCLOPS_DEFAULT =
		giant(RetroNpcCategory.CYCLOPS, new int[]{GIANT_BODY, 2867}, null);

	// Every 2005 bird in this family is mesh 2849 - the plain chicken, the brown one, the rooster
	// and the undead chicken all differ by opcode 40 alone, which allows us to use the live cache
	private static final int CHICKEN_BODY = 2849;

	// Hand-matched. The 2005 definition asked for no resize at all (128) and the modern Chicken
	// composition's own scale is 80, so neither source gets us to a bird the size of the live one.
	private static final int CHICKEN_SCALE = 204;

	/**
	 * A 2005 chicken variant: the shared mesh, the 2005 chicken sequences, and the opcode 40 pairs
	 * that are the only thing telling one bird from another.
	 *
	 * <p>Scale is a parameter rather than a constant because this family spans two sizes.
	 * {@link #CHICKEN_SCALE} is the only one that was measured against the live game; every other is
	 * that number times the ratio the definitions themselves ask for (and seem to be correct)
	 */
	private static RetroNpcData chicken(short[] find, short[] replace, int scale)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.CHICKENS)
			.retroModelIds(new int[]{CHICKEN_BODY})
			.idleAnimationId(AnimationID.CHICKEN_READY)
			.walkAnimationId(AnimationID.CHICKEN_WALK)
			.attackAnimationId(AnimationID.CHICKEN_ATTACK)
			.defendAnimationId(AnimationID.CHICKEN_BLOCK)
			.deathAnimationId(AnimationID.CHICKEN_DEATH)
			.scaleXZ(scale)
			.scaleY(scale)
			.recolors(find, replace)
			.modernAttackAnims(CHICKEN_MODERN_ATTACKS)
			.modernDefendAnims(CHICKEN_MODERN_DEFENDS)
			.modernDeathAnims(CHICKEN_MODERN_DEATHS)
			.build();
	}

	// 2005 def 1018 "Rooster" - mesh 2849 in a dark red-brown
	private static final short[] ROOSTER_FIND = {127, 11200, 8394, 61};
	private static final short[] ROOSTER_REPLACE = {3998, 6720, 1942, 1942};

	// Def 1018 asked for 172 against the chicken's 128, so 204 * 172/128 is the rooster.
	public static final RetroNpcData ROOSTER = chicken(ROOSTER_FIND, ROOSTER_REPLACE, 274);

	// Cows are the guard case: both 2005 meshes are still at their own ids and 98% intact, but their
	// vertex groups were renumbered onto a different rig, so the live copies animate off the wrong
	// joints. Mesh, rig and clips all come from the bundle
	private static final int COW_BODY = 3341;
	private static final int COW_UDDER = 3342;

	/**
	 * A 2005 cow variant: the shared mesh, the legacy cow sequences, and the opcode 40 pairs that
	 * are the only thing telling one cow from another.
	 */
	private static RetroNpcData cow(short[] find, short[] replace, int scale)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.COWS)
			.retroModelIds(new int[]{COW_BODY, COW_UDDER})
			.idleAnimationId(AnimationID.COW_READY)
			.walkAnimationId(AnimationID.COW_WALK)
			.attackAnimationId(AnimationID.COW_ATTACK)
			.defendAnimationId(AnimationID.COW_BLOCK)
			.deathAnimationId(AnimationID.COW_DEATH)
			.miscAnimationId(AnimationID.COW_READY)
			.scaleXZ(scale)
			.scaleY(scale)
			.recolors(find, replace)
			.modernAttackAnims(COW_MODERN_ATTACKS)
			.modernDefendAnims(COW_MODERN_DEFENDS)
			.modernDeathAnims(COW_MODERN_DEATHS)
			.modernMiscAnims(COW_MODERN_MISC)
			.build();
	}

	// The three 2005 cow definitions, by their def ids in the February 2005 cache. Modern OSRS
	// baked its cow variants into separate meshes instead, so which live cow wears which 2005
	// palette is a choice rather than a lookup; they are paired in id order.
	//
	// Def 81 - white hide with dark brown patches
	public static final RetroNpcData COW_DEFAULT = cow(
		new short[]{26, 10363, 30}, new short[]{10365, 5784, 10365}, 128);

	// Def 397 - brown all over
	public static final RetroNpcData COW_BROWN = cow(
		new short[]{10363, 26, 30}, new short[]{5784, 5784, 5784}, 115);

	// Def 955 - brown hide keeping its dark markings, with the beige patch turned grey-brown.
	public static final RetroNpcData COW_GREY = cow(
		new short[]{10363, 26, 4446}, new short[]{5784, 5784, 5289}, 128);

	// February 2005 has no calf: the modern one is a separate mesh Jagex added later. The honest
	// stand-in is the 2005 cow scaled down by what the modern calf's own composition asks for -
	// 68 against the cow's 128.
	public static final RetroNpcData COW_CALF = cow(
		new short[]{26, 10363, 30}, new short[]{10365, 5784, 10365}, 68);

	// The 2005 goblin is a four-part kit - 2951 body, 2953 torso, 2955 legs, 2956 arms - with two
	// substitutions on top, and that is the whole of the family. 2957 is the weapon an armed goblin
	// holds, and 2952 replaces the body on the one definition that describes its goblins as having
	// "grown strong".
	private static final int GOBLIN_BODY = 2951;
	private static final int GOBLIN_STRONG_BODY = 2952;
	private static final int[] GOBLIN_PARTS = {GOBLIN_BODY, 2953, 2955, 2956};
	private static final int[] GOBLIN_ARMED_PARTS = {GOBLIN_BODY, 2953, 2955, 2956, 2957};
	private static final int[] GOBLIN_STRONG_PARTS = {GOBLIN_STRONG_BODY, 2953, 2955, 2956};

	/**
	 * Restores the 2005 colors of the weapon mesh 2957, which the live cache repainted whole.
	 */
	private static final short[] GOBLIN_WEAPON_DRIFT_FIND = {70, 8084};
	private static final short[] GOBLIN_WEAPON_DRIFT_REPLACE = {-22417, 528};

	/**
	 * A 2005 goblin variant: a part list, and the opcode 40 pairs that are the only thing telling
	 * one colored goblin from another.
	 */
	private static RetroNpcData goblin(int[] models, short[] find, short[] replace)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.GOBLINS)
			.retroModelIds(models)
			.idleAnimationId(AnimationID.GOBLIN_READY)
			.walkAnimationId(AnimationID.GOBLIN_WALK)
			.attackAnimationId(AnimationID.GOBLIN_ATTACK_UNARMED)
			.defendAnimationId(AnimationID.GOBLIN_BLOCK)
			.deathAnimationId(AnimationID.GOBLIN_DEATH)
			.recolors(find, replace)
			.modernAttackAnims(GOBLIN_MODERN_ATTACKS)
			.modernDefendAnims(GOBLIN_MODERN_DEFENDS)
			.modernDeathAnims(GOBLIN_MODERN_DEATHS)
			.build();
	}

	// The six 2005 goblin definitions
	public static final RetroNpcData GOBLIN_DEFAULT = goblin(GOBLIN_PARTS, null, null);

	// Def 101 - the same goblin at level 5, carrying 2957, so it needs the weapon correction and
	// nothing else.
	public static final RetroNpcData GOBLIN_ARMED_DEFAULT = goblin(
		GOBLIN_ARMED_PARTS, GOBLIN_WEAPON_DRIFT_FIND, GOBLIN_WEAPON_DRIFT_REPLACE);

	// Def 102 - "These goblins have grown strong": level 13, and the only definition that swaps the
	// body mesh rather than a color. Unarmed, and the modern level 13 goblin is unarmed too.
	public static final RetroNpcData GOBLIN_STRONG = goblin(GOBLIN_STRONG_PARTS, null, null);

	// Def 298 - green armour, 916 -> 22443. Live GOBLIN_GREENARMOUR still carries this pair
	// verbatim over the 2005 meshes themselves, which is what pins green to this definition.
	public static final RetroNpcData GOBLIN_GREEN = goblin(GOBLIN_ARMED_PARTS,
		new short[]{916, 70, 8084}, new short[]{22443, -22417, 528});

	// Def 299 - red armour, 916 -> 933, and live GOBLIN_REDARMOUR carries that pair verbatim too.
	public static final RetroNpcData GOBLIN_RED = goblin(GOBLIN_ARMED_PARTS,
		new short[]{916, 70, 8084}, new short[]{933, -22417, 528});

	// Def 489 - the Goblin guard. The same armed kit as def 101 in the plain colors
	public static final RetroNpcData GOBLIN_GUARD_DEFAULT = goblin(
		GOBLIN_ARMED_PARTS, GOBLIN_WEAPON_DRIFT_FIND, GOBLIN_WEAPON_DRIFT_REPLACE);

	// The 2005 hellhound is def 49: mesh 2997, no opcode 40 data, no resize. Found in the live cache.
	private static final int HELLHOUND_BODY = 2997;

	// Both the 2005 definition and the live composition ask for 128, which says nothing about
	// whether the two meshes are the same size on screen - the chicken needed 204. Hand-tune here.
	private static final int HELLHOUND_SCALE = 128;

	private static RetroNpcData hellhound(int scale)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.HELLHOUNDS)
			.retroModelIds(new int[]{HELLHOUND_BODY})
			.idleAnimationId(AnimationID.HELL_READY)
			.walkAnimationId(AnimationID.HELL_WALK)
			.attackAnimationId(AnimationID.HELL_ATTACK)
			.defendAnimationId(AnimationID.HELL_BLOCK)
			.deathAnimationId(AnimationID.HELL_DEATH)
			.miscAnimationId(AnimationID.HELL_READY)
			.scaleXZ(scale)
			.scaleY(scale)
			.modernAttackAnims(HELLHOUND_MODERN_ATTACKS)
			.modernDefendAnims(HELLHOUND_MODERN_DEFENDS)
			.modernDeathAnims(HELLHOUND_MODERN_DEATHS)
			.modernMiscAnims(HELLHOUND_MODERN_MISC)
			.build();
	}

	public static final RetroNpcData HELLHOUND_DEFAULT = hellhound(HELLHOUND_SCALE);

	// February 2005 has no God Wars. The live ancient hellhound's composition asks for 90 against the
	// ordinary hound's 128, so it is the 2005 hound scaled by the same ratio - the calf's reasoning.
	public static final RetroNpcData HELLHOUND_GODWARS = hellhound(HELLHOUND_SCALE * 90 / 128);

	// The 2005 skeleton hellhound is def 1575: mesh 4974 on sequences 1493-1497, both preserved in
	// the live cache
	private static final int SKELETON_HELLHOUND_SCALE = 256;

	public static final RetroNpcData SKELETON_HELLHOUND = RetroNpcData.builder()
		.category(RetroNpcCategory.HELLHOUNDS)
		.retroModelIds(new int[]{4974})
		.idleAnimationId(AnimationID.SKELETON_HOUND_READY)
		.walkAnimationId(AnimationID.SKELETON_HOUND_WALK)
		.attackAnimationId(AnimationID.SKELETON_HOUND_ATTACK)
		.defendAnimationId(AnimationID.SKELETON_HOUND_BLOCK)
		.deathAnimationId(AnimationID.SKELETON_HOUND_DEATH)
		.scaleXZ(SKELETON_HELLHOUND_SCALE)
		.scaleY(SKELETON_HELLHOUND_SCALE)
		// Def 1575's opcode 40 pair. 10318 is still on the live copy of 4974, so it lands
		.recolors(new short[]{10318}, new short[]{11714})
		.modernAttackAnims(SKELETON_HELLHOUND_MODERN_ATTACKS)
		.modernDefendAnims(SKELETON_HELLHOUND_MODERN_DEFENDS)
		.modernDeathAnims(HELLHOUND_MODERN_DEATHS)
		.build();

	/**
	 * Populates mappings from the bundled npc-mappings.json entries (generated
	 * from the 2005 cache by the dev-only NpcMappingGenerator tool), while
	 * preserving explicit static archetype overrides and modern OSRS ID mappings.
	 */
	public static void load(List<RetroNpcMappingEntry> entries)
	{
		ID_MAPPINGS.clear();
		NAME_MAPPINGS.clear();

		// 1. Register base static archetype overrides and modern OSRS ID mappings
		registerStaticOverrides();

		if (entries == null)
		{
			return;
		}

		// 2. Populate mappings from the generated 2005 cache entries
		Map<String, RetroNpcMappingEntry> byName = new HashMap<>();
		for (RetroNpcMappingEntry entry : entries)
		{
			if (entry == null || entry.getName() == null || entry.getName().isEmpty()
				|| entry.getCategory() == null
				|| entry.getModelIds() == null || entry.getModelIds().length == 0)
			{
				continue;
			}

			String nameLower = entry.getName().toLowerCase(Locale.ROOT).trim();
			byName.put(nameLower, entry);
			NAME_MAPPINGS.putIfAbsent(nameLower, createMappingData(entry));
		}

		// 3. Hand the static archetypes the recolor pairs and resize from their JSON rows
		applyCacheDefinitions(byName);

		// 4. Derive the equipment variants, after the recolors so they inherit them
		applyWeaponVariants();
	}

	/**
	 * Re-points the guards who carry something other than the sword at a kit that shows it.
	 *
	 * <p>Runs last, and derives from whatever is registered rather than from the archetype
	 * constant, because {@link #applyCacheDefinitions} has by then replaced that instance with a
	 * recolored copy. Deriving from the constant instead would hand the axe guard the kit in
	 * a townsperson's colors.
	 */
	private static void applyWeaponVariants()
	{
		for (int axeGuard : new int[]{NpcID.FAI_FALADOR_GUARD3, NpcID.FAI_FALADOR_GUARD3_F})
		{
			RetroNpcData guard = ID_MAPPINGS.get(axeGuard);
			if (guard != null)
			{
				ID_MAPPINGS.put(axeGuard, guard.withModelIds(GUARD_AXE_PARTS));
			}
		}
		ID_MAPPINGS.put(NpcID.FAI_FALADOR_GUARD4_F,
			GUARD_DEFAULT.withModelIds(BOW_GUARD_LIVE_PARTS));
	}

	/**
	 * Grafts what only the 2005 definition knows - the recolor pairs and the resize - onto the
	 * static archetypes.
	 *
	 * <p>An archetype wins over the generated JSON row for a name, which is what keeps hand-checked
	 * combat animations and model ids in place. But the row is the only source of these two, and the
	 * archetypes are constructed before any cache is read, so the two are recombined here instead.
	 * Without the pairs a black demon renders in greater demon colors - both are mesh 2942, and the
	 * pairs are the only thing that separates them. Without the resize a greater demon renders at
	 * full size where 2005 asked for 110/128ths of it.
	 *
	 * <p>The recolors are scoped by {@link #categoryUsesRecolors}, for the same reason
	 * {@code createMappingData} scopes them: guards, goblins all carry opcode
	 * 40 data too, and the generator keeps only the lowest-id row per name, so forwarding wholesale
	 * would repaint a whole category in one arbitrary variant's colors. The resize needs no such
	 * scoping - it is one number per row rather than a palette - but it is only taken where the
	 * archetype asked for no resize at all, so a hand-corrected size is never overwritten.
	 */
	private static void applyCacheDefinitions(Map<String, RetroNpcMappingEntry> byName)
	{
		for (Map.Entry<String, RetroNpcData> mapping : new ArrayList<>(NAME_MAPPINGS.entrySet()))
		{
			RetroNpcData data = mapping.getValue();
			RetroNpcMappingEntry entry = data == null ? null : byName.get(mapping.getKey());
			if (entry == null)
			{
				continue;
			}

			RetroNpcData updated = data;

			if (categoryUsesRecolors(data.getCategory()) && !data.hasRecolors()
				&& entry.getOriginalColors() != null && entry.getReplacementColors() != null)
			{
				updated = updated.withRecolors(entry.getOriginalColors(), entry.getReplacementColors());
			}

			if (data.getScaleXZ() == 128 && data.getScaleY() == 128
				&& (entry.getScaleXZ() != 128 || entry.getScaleY() != 128))
			{
				updated = updated.withScale(entry.getScaleXZ(), entry.getScaleY());
			}

			if (updated == data)
			{
				continue;
			}

			NAME_MAPPINGS.put(mapping.getKey(), updated);

			// Both maps hold the same instance, so every id registered against the archetype has to
			// be pointed at the replacement too
			for (Map.Entry<Integer, RetroNpcData> idMapping : ID_MAPPINGS.entrySet())
			{
				if (idMapping.getValue() == data)
				{
					idMapping.setValue(updated);
				}
			}
		}
	}

	/**
	 * Whether a category can only be drawn from injected geometry.
	 *
	 * <p>These are the categories with no usable 2005 asset left at their model ids: the ids still
	 * resolve, but to unrelated geometry - statues and skulls for the dragons and demons, and for
	 * the giant family a head and two props that belong to something else entirely. So the
	 * cache-backed path must not run for them even as a fallback. Without this a bundle that failed
	 * to load would not disable them, it would draw them wrong.
	 *
	 * <p>Hill giants are deliberately absent: their body survives, and the Jogre head stands in for
	 * the head that does not, so they have a real cache-backed render to fall back to.
	 */
	public static boolean requiresInjectedGeometry(RetroNpcCategory category)
	{
		return category == RetroNpcCategory.ADULT_DRAGONS
			|| category == RetroNpcCategory.BABY_DRAGONS
			|| category == RetroNpcCategory.LESSER_DEMONS
			|| category == RetroNpcCategory.GREATER_DEMONS
			|| category == RetroNpcCategory.BLACK_DEMONS
			|| category == RetroNpcCategory.IMPS
			|| category == RetroNpcCategory.FIRE_GIANTS
			|| category == RetroNpcCategory.ICE_GIANTS
			|| category == RetroNpcCategory.MOSS_GIANTS
			|| category == RetroNpcCategory.CYCLOPS
			|| category == RetroNpcCategory.GUARDS
			|| category == RetroNpcCategory.COWS;
	}

	/**
	 * Whether a category is drawn from injected geometry when the pipeline is on.
	 *
	 * <p>Wider than {@link #requiresInjectedGeometry} by exactly one category: hill giants have a
	 * cache-backed render to fall back to, but the bundle carries their real 2005 head, so they are
	 * better injected when it is available.
	 *
	 * <p>The point of asking the category rather than asking the bundle what it holds is the
	 * skeleton. Mesh 2944 is bundled as the skinner's test subject, so "is this mesh in the bundle"
	 * would answer yes for it and route unarmed skeletons through the injected path - where the
	 * bundle carries only its idle and walk clips, leaving it standing in its idle while it attacks
	 * and while it dies. Nothing that is bundled to be measured should be drawn from the bundle.
	 */
	public static boolean usesInjectedGeometry(RetroNpcCategory category)
	{
		return requiresInjectedGeometry(category) || category == RetroNpcCategory.HILL_GIANTS;
	}

	/**
	 * Whether a category's retro mesh needs the 2005 recolor pairs to look right.
	 *
	 * <p>What these have in common is that one mesh has to serve several NPCs, so the palette is
	 * the only thing telling them apart - the dragons are a greyscale ramp, black and greater
	 * demons are the same mesh, and the chicken and the undead chicken are both 2849. Recoloring is
	 * structural for them rather than cosmetic. Every other category is left alone on purpose.
	 */
	private static boolean categoryUsesRecolors(RetroNpcCategory category)
	{
		return category == RetroNpcCategory.ADULT_DRAGONS
			|| category == RetroNpcCategory.BABY_DRAGONS
			|| category == RetroNpcCategory.LESSER_DEMONS
			|| category == RetroNpcCategory.GREATER_DEMONS
			|| category == RetroNpcCategory.BLACK_DEMONS
			// Fire, ice and moss giants are the same body mesh as the hill giant, told apart only by
			// their 2005 opcode 40 pairs.
			|| category == RetroNpcCategory.FIRE_GIANTS
			|| category == RetroNpcCategory.ICE_GIANTS
			|| category == RetroNpcCategory.MOSS_GIANTS
			// A guard's parts are generic 2005 human kit shared with everything else that wears it,
			// so the opcode 40 pairs are what make the kit a guard's colors rather than a
			// townsperson's. The pairs come from the definition the parts come from.
			|| category == RetroNpcCategory.GUARDS
			// The undead chicken is just a recolored regular chicken
			|| category == RetroNpcCategory.CHICKENS;
	}

	private static void registerMapping(RetroNpcData data, int... npcIds)
	{
		for (int id : npcIds)
		{
			ID_MAPPINGS.put(id, data);
		}
	}

	private static void registerStaticOverrides()
	{
		// Lesser Demons
		NAME_MAPPINGS.put("lesser demon", LESSER_DEMON_DEFAULT);
		registerMapping(LESSER_DEMON_DEFAULT,
			NpcID.LESSER_DEMON, NpcID.LESSER_DEMON2, NpcID.LESSER_DEMON3, NpcID.LESSER_DEMON4, NpcID.LESSER_DEMON5,
			NpcID.DRAGONSLAYER_DEMON, NpcID.KOUREND_LESSER_DEMON1, NpcID.KOUREND_LESSER_DEMON2,
			NpcID.LESSER_DEMON_SLAYERCAVE_1, NpcID.LESSER_DEMON_SLAYERCAVE_2, NpcID.LESSER_DEMON_SLAYERCAVE_3,
			NpcID.WILD_CAVE_LESSER_DEMON, NpcID.WILD_CAVE_LESSER_DEMON2, NpcID.WILD_CAVE_LESSER_DEMON3,
			NpcID.DT2_SCAR_MAZE_MAGE_DEMON_NORMAL, NpcID.DT2_SCAR_MAZE_MELEE_DEMON_NORMAL,
			NpcID.DT2_SCAR_MAZE_RANGED_DEMON_NORMAL, NpcID.DT2_SCAR_LESSER_DEMON_1
		);

		// Greater Demons
		NAME_MAPPINGS.put("greater demon", GREATER_DEMON_DEFAULT);
		registerMapping(GREATER_DEMON_DEFAULT,
			NpcID.GREATER_DEMON, NpcID.GREATER_DEMON2, NpcID.GREATER_DEMON3, NpcID.GREATER_DEMON4, NpcID.GREATER_DEMON5,
			NpcID.GREATER_DEMON_STRONGHOLDCAVE_1, NpcID.GREATER_DEMON_STRONGHOLDCAVE_2, NpcID.GREATER_DEMON_STRONGHOLDCAVE_3,
			NpcID.KOUREND_GREATER_DEMON1, NpcID.KOUREND_GREATER_DEMON2, NpcID.KOUREND_GREATER_DEMON3,
			NpcID.WILD_CAVE_GREATER_DEMON, NpcID.WILD_CAVE_GREATER_DEMON2, NpcID.WILD_CAVE_GREATER_DEMON3,
			NpcID.DT2_SCAR_GREATER_DEMON_1
		);

		// Black Demons
		NAME_MAPPINGS.put("black demon", BLACK_DEMON_DEFAULT);
		registerMapping(BLACK_DEMON_DEFAULT,
			NpcID.BLACK_DEMON, NpcID.BLACK_DEMON2, NpcID.BLACK_DEMON3, NpcID.BLACK_DEMON4, NpcID.BLACK_DEMON5,
			NpcID.BLACK_DEMON_STRONGHOLDCAVE_1, NpcID.BLACK_DEMON_STRONGHOLDCAVE_2, NpcID.BLACK_DEMON_STRONGHOLDCAVE_3,
			NpcID.BLACK_DEMON_STRONGHOLDCAVE_4, NpcID.BLACK_DEMON_STRONGHOLDCAVE_5,
			NpcID.GRANDTREE_BLACKDEMON, NpcID.NZONE_GRANDTREE_BLACKDEMON_HARD, NpcID.NZONE_GRANDTREE_BLACKDEMON_NORMAL,
			NpcID.KOUREND_BLACK_DEMON_1, NpcID.KOUREND_BLACK_DEMON_2,
			NpcID.WILD_CAVE_BLACK_DEMON, NpcID.WILD_CAVE_BLACK_DEMON2, NpcID.WILD_CAVE_BLACK_DEMON3,
			NpcID.DT2_SCAR_BLACK_DEMON_1
		);

		// Imps
		NAME_MAPPINGS.put("imp", IMP_DEFAULT);
		registerMapping(IMP_DEFAULT, NpcID.IMP, NpcID.GODWARS_ANCIENT_IMP, NpcID.CASTLEWARS_IMP);

		// Baby Dragons. All four modern colors share retro mesh 2998 and differ only by recolor.
		// Registered by id as well as name so a renamed or newly added variant still resolves.
		NAME_MAPPINGS.put("baby blue dragon", BABY_BLUE_DRAGON);
		registerMapping(BABY_BLUE_DRAGON,
			NpcID.BABYBLUEDRAGON, NpcID.BABYBLUEDRAGON2, NpcID.BABYBLUEDRAGON3,
			NpcID.BABY_BLUE_DRAGON_TAPOYAUIK_1, NpcID.BABY_BLUE_DRAGON_TAPOYAUIK_2
		);

		NAME_MAPPINGS.put("baby red dragon", BABY_RED_DRAGON);
		registerMapping(BABY_RED_DRAGON,
			NpcID.POH_BABYREDDRAGON,
			NpcID.BABYREDDRAGON, NpcID.BABYREDDRAGON2, NpcID.BABYREDDRAGON3
		);

		NAME_MAPPINGS.put("baby green dragon", BABY_GREEN_DRAGON);
		registerMapping(BABY_GREEN_DRAGON,
			NpcID.BABYGREENDRAGON1, NpcID.BABYGREENDRAGON2, NpcID.BABYGREENDRAGON3
		);

		NAME_MAPPINGS.put("baby black dragon", BABY_BLACK_DRAGON);
		registerMapping(BABY_BLACK_DRAGON,
			NpcID.CHICKENQUEST_BABY_BLACK_DRAGON, NpcID.BABY_BLACK_DRAGON_STRONGHOLDCAVE,
			NpcID.BABY_BLACK_DRAGON_NOHUNT
		);

		NAME_MAPPINGS.put("skeleton", SKELETON_UNARMED);
		registerMapping(SKELETON_UNARMED,
			NpcID.SKELETON_UNARMED, NpcID.SKELETON_UNARMED2, NpcID.SKELETON_UNARMED3,
				NpcID.SKELETON_UNARMED4, NpcID.SKELETON_UNAGRESSIVE
		);
		registerMapping(SKELETON_ARMED,
				NpcID.SKELETON_ARMED, NpcID.SKELETON_ARMED2, NpcID.SKELETON_ARMED3,
				NpcID.SKELETON_ARMED4, NpcID.SKELETON_ARMED5,
				NpcID.SKELETON_UNAGRESSIVE2, NpcID.SKELETON_UNAGRESSIVE3
		);

		// Giant skeletons. GIANTSKELETON and GIANTSKELETON2 are named plain "Skeleton", so without
		// their ids the name row handed them the normal-size unarmed kit. The ones actually named
		// "Giant skeleton" are later content with no 2005 definition, and take the same giant.
		NAME_MAPPINGS.put("giant skeleton", SKELETON_GIANT);
		registerMapping(SKELETON_GIANT,
			NpcID.GIANTSKELETON, NpcID.GIANTSKELETON2,
			NpcID.SWORD_SKELETON_3, NpcID.SWORD_SKELETON_3B,
			NpcID.LOTR_GIANT_SKELETON
		);

		// Zombies
		NAME_MAPPINGS.put("zombie", ZOMBIE_UNARMED);
		registerMapping(ZOMBIE_UNARMED,
			NpcID.ZOMBIE_UNARMED, NpcID.ZOMBIE_UNARMED2, NpcID.ZOMBIE_UNARMED3, NpcID.ZOMBIE_UNARMED4,
			NpcID.ZOMBIE_UNARMED5, NpcID.ZOMBIE_UNARMED6, NpcID.ZOMBIE_UNARMED_CITY1, NpcID.ZOMBIE_UNARMED_CITY3,
			NpcID.ZOMBIE_UNARMED_CITY6, NpcID.ZOMBIE_UNARMED_SEWER1, NpcID.ZOMBIE_UNARMED_SEWER2,
			NpcID.ZOMBIE_UNARMED_SEWER3, NpcID.ZOMBIE_UNARMED_SEWER4,
			NpcID.ZOMBIE2, NpcID.ZOMBIE2_B, NpcID.ZOMBIE2_C
		);
		registerMapping(ZOMBIE_ARMED,
			NpcID.ZOMBIE_ARMED, NpcID.ZOMBIE_ARMED2, NpcID.ZOMBIE_ARMED3, NpcID.ZOMBIE_ARMED_CITY1,
			NpcID.ZOMBIE_ARMED_CITY3, NpcID.ZOMBIE_ARMED_SEWER1, NpcID.ZOMBIE_ARMED_SEWER2,
			NpcID.ZOMBIE_ARMED_SEWER3, NpcID.ZOMBIE_ARMED_SEWER4
		);

		// Guards
		NAME_MAPPINGS.put("guard", GUARD_DEFAULT);
		registerMapping(GUARD_DEFAULT,
			// The base rows of each family. GUARDS is in ID_ONLY_CATEGORIES, so the name no longer
			// resolves and every guard has to be named here - including the _F and _VARIANT
			// derivatives below, which the name lookup used to cover.
			NpcID.GUARD1,
			// Only the melee half of the Falador family. GUARD2, GUARD4, GUARD5 and GUARD6 carry
			// a bow or crossbow - see EXCLUDED_IDS.
			NpcID.FAI_FALADOR_GUARD1, NpcID.FAI_FALADOR_GUARD3,
			NpcID.BIM_FAI_VARROCK_GUARD02, NpcID.BIM_FAI_VARROCK_GUARD02_F, NpcID.BIM_FAI_VARROCK_GUARD02_VARIANT02,
			NpcID.FAI_VARROCK_GUARD02, NpcID.FAI_VARROCK_GUARD02_VARIANT01, NpcID.FAI_VARROCK_GUARD02_VARIANT02,
			NpcID.FAI_VARROCK_GUARD02_F, NpcID.FAI_VARROCK_GUARD02_F_VARIANT01, NpcID.FAI_VARROCK_GUARD02_F_VARIANT02,
			NpcID.FAI_VARROCK_GUARD_CAPTAIN02,
			NpcID.GUARD1_VARIANT01, NpcID.GUARD1_F, NpcID.GUARD1_F_VARIANT01,
			NpcID.FAI_FALADOR_GUARD1_VARIANT01, NpcID.FAI_FALADOR_GUARD1_F, NpcID.FAI_FALADOR_GUARD1_VARIANT02,
			NpcID.FAI_FALADOR_GUARD3_F, NpcID.FAI_FALADOR_GUARD4_F,
			// Ratcatcher guards
			NpcID.RATCATCHER_STATICGUARD, NpcID.RATCATCHER_CHIEFGUARD,
			NpcID.RATCATCHER_GUARD_LEFT_FRONT, NpcID.RATCATCHER_GUARD_LEFT_MID,
			NpcID.RATCATCHER_GUARD_LEFT_BACK, NpcID.RATCATCHER_GUARD_LEFT_FULLBACK,
			NpcID.RATCATCHER_GUARD_RIGHT_FRONT, NpcID.RATCATCHER_GUARD_RIGHT_MID,
			NpcID.RATCATCHER_GUARD_RIGHT_BACK, NpcID.RATCATCHER_GUARD_RIGHT_FULLBACK,
			NpcID.RATCATCHER_GUARD_LEFT_INSIDE, NpcID.RATCATCHER_GUARD_RIGHT_INSIDE
		);

		// Register ardougne guards separately
		registerMapping(ARDOUGNE_GUARD_DEFAULT,
			NpcID.ARDOUGNE_GUARD, NpcID.ARDOUGNE_GUARD_VARIANT01,
			NpcID.ARDOUGNE_GUARD_F, NpcID.ARDOUGNE_GUARD_F_VARIANT01
		);

		// The Carnillean mansion guards are the town's guards posted indoors - same kit, no weapon,
		// matching 2005 definition 887 exactly. Reached by id like every other guard.
		registerMapping(CARNILLEAN_GUARD_DEFAULT,
			NpcID.SOTN_GUARD_CARNILLEAN_UPSTAIRS, NpcID.GUARD_CARNILLEAN_VIS,
			NpcID.GUARD_CARNILLEAN_CUTSCENE
		);

		// Deliberately left out: DEADMAN_GUARD_ARDOUGNE_VIS, DEADMAN_GUARD_YANILLE_VIS and their
		// _RANGE_VIS siblings

		// The giant family. All five are the same 2005 body with a variant head, so they share the
		// animations and differ only in their parts and their 2005 recolor pairs.
		NAME_MAPPINGS.put("hill giant", HILL_GIANT_DEFAULT);
		registerMapping(HILL_GIANT_DEFAULT,
			NpcID.GIANT, NpcID.GIANT2, NpcID.GIANT3, NpcID.GIANT4, NpcID.GIANT5, NpcID.GIANT6,
			NpcID.KOUREND_HILLGIANT
		);

		NAME_MAPPINGS.put("fire giant", FIRE_GIANT_DEFAULT);
		registerMapping(FIRE_GIANT_DEFAULT,
			NpcID.FIREGIANT, NpcID.FIREGIANT2, NpcID.FIREGIANT3,
			NpcID.FIREGIANT_BIG, NpcID.FIREGIANT_BIG2, NpcID.FIREGIANT_BIG3,
			NpcID.FIREGIANT_STRONGHOLDCAVE_1, NpcID.FIREGIANT_STRONGHOLDCAVE_2,
			NpcID.FIREGIANT_STRONGHOLDCAVE_3, NpcID.FIREGIANT_STRONGHOLDCAVE_4,
			NpcID.KOUREND_FIREGIANT1, NpcID.KOUREND_FIREGIANT2
		);

		NAME_MAPPINGS.put("ice giant", ICE_GIANT_DEFAULT);
		registerMapping(ICE_GIANT_DEFAULT,
			NpcID.ICEGIANT, NpcID.ICEGIANT2, NpcID.ICEGIANT3,
			NpcID.ICEGIANT_LOW_WANDERRANGE, NpcID.ICEGIANT_LOW_WANDERRANGE2,
			NpcID.WILD_CAVE_ICEGIANT, NpcID.WILD_CAVE_ICEGIANT2, NpcID.WILD_CAVE_ICEGIANT3
		);

		NAME_MAPPINGS.put("moss giant", MOSS_GIANT_DEFAULT);
		registerMapping(MOSS_GIANT_DEFAULT,
			NpcID.MOSSGIANT, NpcID.MOSSGIANT2, NpcID.MOSSGIANT3, NpcID.MOSSGIANT4,
			NpcID.ROVING_MOSSGIANT, NpcID.LUNAR_MOSSGIANT, NpcID.LUNAR_MOSSGIANT2,
			NpcID.KOUREND_MOSSGIANT, NpcID.PRIF_MOSSGIANT, NpcID.GB_MOSSGIANT
		);

		// WARGUILD_CYCLOPS_PET is left out - it is a pet, not the NPC
		NAME_MAPPINGS.put("cyclops", CYCLOPS_DEFAULT);
		registerMapping(CYCLOPS_DEFAULT,
			NpcID.CYCLOPS,
			NpcID.WARGUILD_CYCLOPS1, NpcID.WARGUILD_CYCLOPS2, NpcID.WARGUILD_CYCLOPS3,
			NpcID.WARGUILD_CYCLOPS4, NpcID.WARGUILD_CYCLOPS5, NpcID.WARGUILD_CYCLOPS6,
			NpcID.WARGUILD_CYCLOPS1_HIGH, NpcID.WARGUILD_CYCLOPS2_HIGH, NpcID.WARGUILD_CYCLOPS3_HIGH,
			NpcID.WARGUILD_CYCLOPS4_HIGH, NpcID.WARGUILD_CYCLOPS5_HIGH, NpcID.WARGUILD_CYCLOPS6_HIGH,
			NpcID.KOUREND_CYCLOPS1, NpcID.KOUREND_CYCLOPS2
		);

		// Cows resolve by id rather than by name alone, because the three 2005 variants are the
		// same mesh in different colors while the modern ones are separate meshes - one name row
		// cannot carry three palettes. The name row stays as the fallback for any cow not listed
		// here. "Cow (hard)" and the calves do not match the row by name at all.
		NAME_MAPPINGS.put("cow", COW_DEFAULT);
		registerMapping(COW_DEFAULT,
			NpcID.COW, NpcID.COW_BEEF, NpcID.FAIRY_COW,
			NpcID.NZONE_COW_NORMAL, NpcID.NZONE_COW_HARD
		);
		registerMapping(COW_BROWN, NpcID.COW2);
		registerMapping(COW_GREY, NpcID.COW3);
		registerMapping(COW_CALF, NpcID.COW2_CALF, NpcID.COW3_CALF, NpcID.CALF);

		// Roosters
		NAME_MAPPINGS.put("rooster", ROOSTER);
		registerMapping(ROOSTER,
			NpcID.ROOSTER,       // Fred's farm
			NpcID.FARM_ROOSTER,  // Ernest the Chicken - the live mesh the evil chicken shares
			NpcID.MISC_ROOSTER   // Miscellania
		);

		// Goblins resolve by id where the color matters and by name everywhere else. The 2005
		// family is six definitions over one name, so the generated row can only ever carry one of
		// them - the same problem the cows had.
		NAME_MAPPINGS.put("goblin", GOBLIN_DEFAULT);

		// The spear-armed goblin
		registerMapping(GOBLIN_ARMED_DEFAULT,
			// Lumbridge
			NpcID.GOBLIN_ARMED, NpcID.GOBLIN_ARMED_MELEE_1,
			NpcID.GOBLIN_UNARMED_MELEE_6, NpcID.GOBLIN_UNARMED_MELEE_7,
			NpcID.GOBLIN_UNARMED_MELEE_IN_6, NpcID.GOBLIN_UNARMED_MELEE_IN_7,
			// Barbarian village and the Dwarf Cannon mine
			NpcID.FAI_BARBARIAN_GOBLIN_ARMED_1, NpcID.FAI_BARBARIAN_GOBLIN_ARMED_2,
			NpcID.FAI_BARBARIAN_GOBLIN_ARMED_3, NpcID.FAI_BARBARIAN_GOBLIN_ARMED_4,
			NpcID.MCANNON_GOBLIN_GUARD,
			NpcID.GOBLIN_ARMED_MCANNON_1, NpcID.GOBLIN_ARMED_MCANNON_2, NpcID.GOBLIN_ARMED_MCANNON_3,
			NpcID.GOBLIN_ARMED_MCANNON_4, NpcID.GOBLIN_ARMED_MCANNON_5,
			// The Eye of Glouphrie soldiers, and the Shield of Arrav war goblins
			NpcID.EYEGLO_GOBLIN_SOLDIER_1, NpcID.EYEGLO_GOBLIN_SOLDIER_2, NpcID.EYEGLO_GOBLIN_SOLDIER_3,
			NpcID.EYEGLO_GOBLIN_SOLDIER_4, NpcID.EYEGLO_GOBLIN_SOLDIER_5,
			NpcID.SOS_WAR_GOBLIN_ARMED, NpcID.SOS_WAR_GOBLIN_ARMED2,
			NpcID.SOS_WAR_GOBLIN_HELMET, NpcID.SOS_WAR_GOBLIN_HELMET2,
			NpcID.SOS_WAR_GOBLIN_GREEN_SOLDIER_1, NpcID.SOS_WAR_GOBLIN_GREEN_SOLDIER_2,
			// God Wars
			NpcID.GODWARS_GOBLIN1, NpcID.GODWARS_GOBLIN2, NpcID.GODWARS_GOBLIN3,
			NpcID.GODWARS_GOBLIN4, NpcID.GODWARS_GOBLIN5
		);

		// Live GOBLIN_HELMET is the level 13 goblin, and 2005 def 102 is the only level 13 goblin
		// in that cache. Both are unarmed; only the body mesh changes.
		registerMapping(GOBLIN_STRONG, NpcID.GOBLIN_HELMET);

		// Goblin Village goblins
		registerMapping(GOBLIN_GREEN,
			NpcID.GOBLIN_GREEN_SOLDIER_1, NpcID.GOBLIN_GREEN_SOLDIER_2, NpcID.GOBLIN_GREEN_SOLDIER_3,
			NpcID.GOBLIN_GREEN_SOLDIER_4, NpcID.GOBLIN_GREEN_SOLDIER_5, NpcID.GOBLIN_GREEN_SOLDIER_6,
			NpcID.GOBLIN_GREEN_SOLDIER_7, NpcID.GOBLIN_GREEN_SOLDIER_8,
			NpcID.GOBLIN_GREENARMOUR, NpcID.XMAS20_GOBLIN_GREEN,
			NpcID.SLICE_CUTSCENE_FIREBOLT_GOBLIN, NpcID.SLICE_CUTSCENE_ARROW_GOBLIN
		);
		registerMapping(GOBLIN_RED,
			NpcID.GOBLIN_RED_SOLDIER_1, NpcID.GOBLIN_RED_SOLDIER_2, NpcID.GOBLIN_RED_SOLDIER_3,
			NpcID.GOBLIN_RED_SOLDIER_4, NpcID.GOBLIN_RED_SOLDIER_5, NpcID.GOBLIN_RED_SOLDIER_6,
			NpcID.GOBLIN_RED_SOLDIER_7, NpcID.GOBLIN_RED_SOLDIER_8,
			NpcID.GOBLIN_RED_SOLDIER_UNDERGROUND, NpcID.GOBLIN_REDARMOUR, NpcID.XMAS20_GOBLIN_RED,
			NpcID.SLICE_CUTSCENE_SCARED_GOBLIN_1
		);

		// "Goblin guard" is its own 2005 name, so the row would cover it; the archetype exists only
		// to hand it the same weapon correction every other armed goblin gets.
		NAME_MAPPINGS.put("goblin guard", GOBLIN_GUARD_DEFAULT);
		registerMapping(GOBLIN_GUARD_DEFAULT, NpcID.GOBLIN_GUARD);

		// Hellhounds.
		// Deliberately not registered:
		// revenant hellhounds, the reanimated hellhound, the scarred hellhounds (DT2, their own mesh),
		// and Cerberus/cerb pet.
		NAME_MAPPINGS.put("hellhound", HELLHOUND_DEFAULT);
		registerMapping(HELLHOUND_DEFAULT,
			NpcID.HELLHOUND, NpcID.HELLHOUND_STRONGHOLDCAVE, NpcID.POH_HELLHOUND,
			NpcID.KOUREND_HELLHOUND, NpcID.WILD_CAVE_HELL_HOUND
		);
		registerMapping(HELLHOUND_GODWARS, NpcID.GODWARS_ANCIENT_HELLHOUND);

		// Skeleton hellhounds
		NAME_MAPPINGS.put("skeleton hellhound", SKELETON_HELLHOUND);
		registerMapping(SKELETON_HELLHOUND,
			NpcID.SKELETON_HELLHOUND,
			// Nightmare Zone. The hard one is named "Skeleton Hellhound (hard)", so only its id reaches it
			NpcID.NZONE_SKELETON_HELLHOUND_NORMAL, NpcID.NZONE_SKELETON_HELLHOUND_HARD
		);
	}

	private static RetroNpcData createMappingData(RetroNpcMappingEntry entry)
	{
		RetroNpcCategory category = entry.getCategory();
		int attackAnim = -1;
		int defendAnim = -1;
		int deathAnim = -1;
		int miscAnim = -1;
		int[] models = entry.getModelIds();
		int stanceAnim = entry.getIdleAnim();
		int walkAnim = entry.getWalkAnim();
		// Seeded from the 2005 definition; categories below correct it where the retro and
		// modern meshes are different sizes
		int scaleXZ = entry.getScaleXZ();
		int scaleY = entry.getScaleY();

		// Scoped rather than seeded from every entry: plenty of 2005 definitions carry opcode 40
		// recolors - goblins and guards among them - and buildEntries keeps only the lowest-id row
		// per name, so forwarding wholesale would repaint a live category with one arbitrary
		// variant's colors. See categoryUsesRecolors for why these categories are the exception.
		short[] recolorFind = categoryUsesRecolors(category) ? entry.getOriginalColors() : null;
		short[] recolorReplace = categoryUsesRecolors(category) ? entry.getReplacementColors() : null;

		Set<Integer> modernAttacks = Collections.emptySet();
		Set<Integer> modernDefends = Collections.emptySet();
		Set<Integer> modernDeaths = Collections.emptySet();
		Set<Integer> modernMisc = Collections.emptySet();

		if (category == RetroNpcCategory.LESSER_DEMONS
			|| category == RetroNpcCategory.GREATER_DEMONS
			|| category == RetroNpcCategory.BLACK_DEMONS)
		{
			stanceAnim = stanceAnim != -1 ? stanceAnim : AnimationID.DEMON_READY;
			walkAnim = walkAnim != -1 ? walkAnim : AnimationID.DEMON_WALK;
			attackAnim = AnimationID.DEMON_ATTACK;
			defendAnim = AnimationID.DEMON_BLOCK;
			deathAnim = AnimationID.DEMON_DEATH;
			modernAttacks = DEMON_MODERN_ATTACKS;
			modernDefends = DEMON_MODERN_DEFENDS;
			modernDeaths = DEMON_MODERN_DEATHS;

		}
		else if (category == RetroNpcCategory.ADULT_DRAGONS)
		{
			attackAnim = AnimationID.DRAGON_ATTACK;
			// Filled for the King Black Dragon's sake - isDefendAnimation short-circuits while the
			// slot is -1, so this enables the 'defend' category
			defendAnim = AnimationID.DRAGON_BLOCK;
			modernAttacks = DRAGON_MODERN_ATTACKS;
			modernDefends = DRAGON_MODERN_DEFENDS;
			modernDeaths = DRAGON_MODERN_DEATHS;

		}
		else if (category == RetroNpcCategory.BABY_DRAGONS)
		{
			stanceAnim = stanceAnim != -1 ? stanceAnim : AnimationID.BDRAG_READY;
			walkAnim = walkAnim != -1 ? walkAnim : AnimationID.BDRAG_WALK;
		}
		else if (category == RetroNpcCategory.GOBLINS)
		{
			stanceAnim = stanceAnim != -1 ? stanceAnim : AnimationID.GOBLIN_READY;
			walkAnim = walkAnim != -1 ? walkAnim : AnimationID.GOBLIN_WALK;
			attackAnim = AnimationID.GOBLIN_ATTACK_UNARMED;
			defendAnim = AnimationID.GOBLIN_BLOCK;
			deathAnim = AnimationID.GOBLIN_DEATH;
			modernAttacks = GOBLIN_MODERN_ATTACKS;
			modernDefends = GOBLIN_MODERN_DEFENDS;
			modernDeaths = GOBLIN_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.ZOMBIES)
		{
			stanceAnim = stanceAnim != -1 ? stanceAnim : 301;
			walkAnim = walkAnim != -1 ? walkAnim : 298;
			attackAnim = 299;
			defendAnim = 300;
			deathAnim = 302;
			modernAttacks = ZOMBIE_MODERN_ATTACKS;
			modernDefends = ZOMBIE_MODERN_DEFENDS;
			modernDeaths = ZOMBIE_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.CHICKENS)
		{
			// The plain chicken and the undead chicken reach here; the rooster is an id-registered
			// archetype instead, because it needs a resize of its own and this branch would overwrite
			// it with the hen's. The two paths have to agree on every other slot, so both read the
			// same constants - see chicken(...).
			scaleXZ = CHICKEN_SCALE;
			scaleY = CHICKEN_SCALE;
			models = new int[]{CHICKEN_BODY};
			stanceAnim = AnimationID.CHICKEN_READY;
			walkAnim = AnimationID.CHICKEN_WALK;
			attackAnim = AnimationID.CHICKEN_ATTACK;
			defendAnim = AnimationID.CHICKEN_BLOCK;
			deathAnim = AnimationID.CHICKEN_DEATH;
			modernAttacks = CHICKEN_MODERN_ATTACKS;
			modernDefends = CHICKEN_MODERN_DEFENDS;
			modernDeaths = CHICKEN_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.COWS)
		{
			// Only the undead cow reaches here. Every other cow is an id-registered archetype that
			// declares all six slots itself, because the three 2005 variants need three different
			// palettes and the generator keeps only the lowest-id row per name.
			attackAnim = AnimationID.COW_ATTACK;
			defendAnim = AnimationID.COW_BLOCK;
			deathAnim = AnimationID.COW_DEATH;
			miscAnim = AnimationID.COW_READY;
			modernAttacks = COW_MODERN_ATTACKS;
			modernDefends = COW_MODERN_DEFENDS;
			modernDeaths = COW_MODERN_DEATHS;
			modernMisc = COW_MODERN_MISC;
		}
		else if (category == RetroNpcCategory.HILL_GIANTS
			|| category == RetroNpcCategory.FIRE_GIANTS
			|| category == RetroNpcCategory.ICE_GIANTS
			|| category == RetroNpcCategory.MOSS_GIANTS
			|| category == RetroNpcCategory.CYCLOPS)
		{
			// The generated row already carries this family's parts. Hill giants are the exception:
			// their 2005 head 2862 is gone from the live cache, so I set the head to a Jogre head
			// (it fits the 2005 body)
			if (category == RetroNpcCategory.HILL_GIANTS)
			{
				models = new int[]{GIANT_BODY, JOGRE_HEAD};
			}
			stanceAnim = stanceAnim != -1 ? stanceAnim : AnimationID.GIANT_READY;
			walkAnim = walkAnim != -1 ? walkAnim : AnimationID.GIANT_WALK;
			attackAnim = AnimationID.GIANT_ATTACK;
			defendAnim = AnimationID.GIANT_BLOCK;
			deathAnim = AnimationID.GIANT_DEATH;
			modernAttacks = GIANT_MODERN_ATTACKS;
			modernDefends = GIANT_MODERN_DEFENDS;
			modernDeaths = GIANT_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.GUARDS)
		{
			models = GUARD_PARTS;
			stanceAnim = 808;
			walkAnim = 819;
			attackAnim = 422;
			defendAnim = 424;
			deathAnim = 836;
			modernAttacks = GUARD_MODERN_ATTACKS;
			modernDefends = GUARD_MODERN_DEFENDS;
			modernDeaths = GUARD_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.IMPS)
		{
			// The 2005 sequences survive at these ids; it is the animation frames behind them that were
			// re-authored for the modern rig, which is why imps need the injected data
			stanceAnim = stanceAnim != -1 ? stanceAnim : 171;
			walkAnim = walkAnim != -1 ? walkAnim : 168;
			attackAnim = 169;
			defendAnim = 170;
			deathAnim = 172;
			modernAttacks = IMP_MODERN_ATTACKS;
			modernDefends = IMP_MODERN_DEFENDS;
			modernDeaths = IMP_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.SKELETONS)
		{
			stanceAnim = stanceAnim != -1 ? stanceAnim : 262;
			walkAnim = walkAnim != -1 ? walkAnim : 259;
			attackAnim = 260;
			defendAnim = 261;
			deathAnim = 263;
			modernAttacks = SKELETON_MODERN_ATTACKS;
			modernDefends = SKELETON_MODERN_DEFENDS;
			modernDeaths = SKELETON_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.GHOSTS)
		{
			// Avoid putting the kibosh on the restless ghost
			if (!"restless ghost".equalsIgnoreCase(entry.getName()))
			{
				stanceAnim = stanceAnim != -1 ? stanceAnim : AnimationID.GHOST_READY;
				walkAnim = walkAnim != -1 ? walkAnim : AnimationID.GHOST_WALK;
				attackAnim = AnimationID.GHOST_ATTACK;
				defendAnim = AnimationID.GHOST_BLOCK;
				deathAnim = AnimationID.GHOST_DEATH;
			}
			modernAttacks = GHOST_MODERN_ATTACKS;
			modernDefends = GHOST_MODERN_DEFENDS;
			modernDeaths = GHOST_MODERN_DEATHS;
		}

		return RetroNpcData.builder()
			.category(category)
			.retroModelIds(models)
			.idleAnimationId(stanceAnim)
			.walkAnimationId(walkAnim)
			.attackAnimationId(attackAnim)
			.defendAnimationId(defendAnim)
			.deathAnimationId(deathAnim)
			.miscAnimationId(miscAnim)
			.scaleXZ(scaleXZ)
			.scaleY(scaleY)
			.recolors(recolorFind, recolorReplace)
			.modernAttackAnims(modernAttacks)
			.modernDefendAnims(modernDefends)
			.modernDeathAnims(modernDeaths)
			.modernMiscAnims(modernMisc)
			.build();
	}

	public static RetroNpcData get(int npcId, String npcName)
	{
		// Ahead of both tables: an excluded NPC must not be reachable by name either
		if (EXCLUDED_IDS.contains(npcId))
		{
			return null;
		}

		if (npcName == null)
		{
			return ID_MAPPINGS.get(npcId);
		}

		String nameLower = npcName.toLowerCase(Locale.ROOT).trim();
		RetroNpcData byName = NAME_MAPPINGS.get(nameLower);
		RetroNpcData byId = ID_MAPPINGS.get(npcId);

		if (byName != null && ID_ONLY_CATEGORIES.contains(byName.getCategory()))
		{
			// Sharing a name with the town guard is not enough to be one
			byName = null;
		}

		if (byName == null)
		{
			// Modern NPCs whose names no longer match a 2005 name are still
			// swappable when their ID was explicitly registered.
			return byId;
		}

		if (byId != null && byId.getCategory() == byName.getCategory())
		{
			return byId;
		}

		return byName;
	}
}
