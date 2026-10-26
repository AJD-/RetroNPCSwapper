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

import com.retronpcswapper.inject.RetroAssetBundle;
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
	 * <p>
	 * Matching is mostly by name, which is what lets a 2005 name keep working across every
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
		NpcID.VETION_HELLHOUND_JNR, NpcID.VETION_HELLHOUND_JNR_SINGLES,
		// Exclude Colosseum scorpion
		NpcID.WANDERING_DOOMSCORPION,
		// Exclude the gorilla skeleton
		NpcID.MM_SKELETON,
		// Arianwyn's model in NMZ
		NpcID.NZONE_SOTE_ARIANWYN_HARD,
		// Exclude the Hallowed Sepulchre spiders
		NpcID.HALLOWED_SPIDER_01, NpcID.HALLOWED_SPIDER_02
	);

	/**
	 * Categories that resolve from the registered id list only, never from a name.
	 *
	 * <p>Suppressed at lookup rather than by dropping the name row, because
	 * {@link #applyCacheDefinitions} walks {@code NAME_MAPPINGS} to graft the opcode 40 pairs
	 * onto the archetypes.
	 */
	private static final Set<RetroNpcCategory> ID_ONLY_CATEGORIES =
		Set.of(RetroNpcCategory.GUARDS, RetroNpcCategory.ELVES);

	/**
	 * Ids whose registered row beats the name row even when the two disagree about the category.
	 *
	 * <p>For NPCs whose display name lies about what they are. {@link #get} otherwise lets the name
	 * win a cross-category tie, because an id registered under a different category is almost always
	 * a mistake - but the Tarn's Lair and Dragon Slayer II mages are named plain "Skeleton" and the
	 * tiny scorpion plain "Scorpion", so without this their registrations would be dead code.
	 *
	 * <p>The tell in both families is the mesh: 21193 is the mage kit rather than skeleton kit, and
	 * 24612 is the small scorpion rather than the large one.
	 */
	private static final Set<Integer> NAME_OVERRIDDEN_IDS = Set.of(
		NpcID.LOTR_MAGE_SKELETON, NpcID.DS2_SKELETON_MAGIC, NpcID.TINYSCORPION);

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

	// Empty on purpose. Everything an elf is expected to play - bow 426, the spear and scythe
	// families, the blocks and death 836 - predates Feb 2005 and ships as a clip under its own id, so
	// it should pass straight through.
	public static final Set<Integer> ELF_MODERN_ATTACKS = Set.of();
	public static final Set<Integer> ELF_MODERN_DEFENDS = Set.of();
	public static final Set<Integer> ELF_MODERN_DEATHS = Set.of();

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
	 * 2303 and 2312 animate the retro mesh correctly and are left to play.
	 */
	public static final Set<Integer> COW_MODERN_MISC = Set.of(
		AnimationID.COW_GRAZE, AnimationID.COW_UPDATE_READY,
		AnimationID.COW_UPDATE_GRAZE, AnimationID.COW_UPDATE_DAIRY
	);

	// HELL_ATTACK/HELL_BLOCK/HELL_DEATH (158/159/161) are the surviving
	// 2005 sequences - the retro targets, not modern anims to intercept.
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

	// The large and small scorpions were reworked together (SCORPION_UPDATE_* and
	// SMALL_SCORPION_UPDATE_*), and the jungle scorpion already mixes the large idle with the small
	// walk, so each set lists both families. Scoped to the scorpion categories, so the extra ids only
	// ever apply to scorpions.
	public static final Set<Integer> SCORPION_MODERN_ATTACKS = Set.of(
			AnimationID.SCORPION_UPDATE_ATTACK_TAIL, AnimationID.SMALL_SCORPION_UPDATE_ATTACK
	);
	public static final Set<Integer> SCORPION_MODERN_DEFENDS = Set.of(
			AnimationID.SCORPION_UPDATE_DEFEND, AnimationID.SMALL_SCORPION_UPDATE_DEFEND
	);
	public static final Set<Integer> SCORPION_MODERN_DEATHS = Set.of(
			AnimationID.SCORPION_UPDATE_DEATH, AnimationID.SMALL_SCORPION_UPDATE_DEATH
	);

	// GIANTRAT_ATTACK/BLOCK/DEATH (138/139/141) are the surviving 2005 seqs
	public static final Set<Integer> GIANT_RAT_MODERN_ATTACKS = Set.of(
		AnimationID.GIANT_RAT_UPDATE_ATTACK
	);
	public static final Set<Integer> GIANT_RAT_MODERN_DEFENDS = Set.of(
		AnimationID.GIANT_RAT_UPDATE_DEFEND
	);
	public static final Set<Integer> GIANT_RAT_MODERN_DEATHS = Set.of(
		AnimationID.GIANT_RAT_UPDATE_DEATH, AnimationID.GIANT_RAT_UPDATE_DEATH_FAST
	);

	// GIANTSPIDER_* (142-149) and SPIDER_* (279-282) are the surviving 2005 seqs
	public static final Set<Integer> SPIDER_MODERN_ATTACKS = Set.of(
		AnimationID.SPIDER_UPDATE_ATTACK, AnimationID.SPIDER_UPDATE_ATTACK_LARGE,
		AnimationID.SPIDER_UPDATE_TBW_BUSH_ATTACK,
		AnimationID.SMALL_SPIDER_UPDATE_ATTACK, AnimationID.SMALL_SPIDER_UPDATE_ATTACK_SOUND
	);
	public static final Set<Integer> SPIDER_MODERN_DEFENDS = Set.of(
		AnimationID.SPIDER_UPDATE_DEFEND, AnimationID.SPIDER_UPDATE_DEFEND_LARGE,
		AnimationID.SMALL_SPIDER_UPDATE_DEFEND, AnimationID.SMALL_SPIDER_UPDATE_DEFEND_SOUND
	);
	public static final Set<Integer> SPIDER_MODERN_DEATHS = Set.of(
		AnimationID.SPIDER_UPDATE_DEATH, AnimationID.SPIDER_UPDATE_DEATH_LARGE,
		AnimationID.SMALL_SPIDER_UPDATE_DEATH
	);
	// GIANTSPIDER_CASTING/TELEPORT (147-149) survive too, but the misc slot is many-to-one, so
	// these fall back to standing still like the hellhound's
	public static final Set<Integer> SPIDER_MODERN_MISC = Set.of(
		AnimationID.SPIDER_UPDATE_CASTING, AnimationID.SPIDER_UPDATE_TELEPORT,
		AnimationID.SPIDER_UPDATE_TELEPORT_REVERSE
	);

	// BEAR_* (37-44) are the surviving 2005 seqs
	public static final Set<Integer> BEAR_MODERN_ATTACKS = Set.of(
		AnimationID.BEAR_REWORK_ATTACK, AnimationID.BEAR_REWORK_CUB_ATTACK
	);
	public static final Set<Integer> BEAR_MODERN_DEFENDS = Set.of(
		AnimationID.BEAR_REWORK_DEFEND, AnimationID.BEAR_REWORK_CUB_DEFEND
	);
	public static final Set<Integer> BEAR_MODERN_DEATHS = Set.of(
		AnimationID.BEAR_REWORK_DEATH, AnimationID.BEAR_REWORK_CUB_DEATH
	);
	public static final Set<Integer> BEAR_MODERN_MISC = Set.of(
		AnimationID.BEAR_REWORK_READY_GRIZZLY, AnimationID.BEAR_REWORK_READY_CUB_GRIZZLY
	);

	// Kalphites play their modern ids, which the bundle fills with 2005 frames. The ground family
	// (workers, soldiers, guardians, larvae, crawling queen) and the airborne queen ride different
	// 2005 rigs, so each set only names ids whose clips fit that family's meshes. That includes the
	// old ids live re-rigged onto each family's modern framemap rather than retiring
	public static final Set<Integer> KALPHITE_MODERN_ATTACKS = Set.of(
		AnimationID.KALPHITE_UPDATE_JAWS_ATTACK, AnimationID.KALPHITE_UPDATE_LARVAE_JAWS_ATTACK,
		AnimationID.KALPHITE_UPDATE_CLAW_ATTACK, AnimationID.KALPHITE_UPDATE_CLAW_LORD_ATTACK,
		AnimationID.KALPHITE_UPDATE_RANGED_ATTACK,
		AnimationID.KALPHITE_UPDATE_QUEEN_JAWS_ATTACK, AnimationID.KALPHITE_UPDATE_QUEEN_RANGED_ATTACK,
		AnimationID.KALPHITE_ATTACK_CLAWS, AnimationID.KALPHITE_LIGHTNING, AnimationID.KALPHITE_LIGHTNING_DUPE
	);
	public static final Set<Integer> KALPHITE_MODERN_DEFENDS = Set.of(
		AnimationID.KALPHITE_UPDATE_DEFEND, AnimationID.KALPHITE_UPDATE_QUEEN_DEFEND
	);
	public static final Set<Integer> KALPHITE_MODERN_DEATHS = Set.of(
		AnimationID.KALPHITE_UPDATE_DEATH, AnimationID.KALPHITE_UPDATE_LARVAE_DEATH,
		AnimationID.KALPHITE_UPDATE_LORD_DEATH, AnimationID.KALPHITE_UPDATE_QUEEN_DEATH
	);
	public static final Set<Integer> KALPHITE_FLYING_QUEEN_MODERN_ATTACKS = Set.of(
		AnimationID.KALPHITE_UPDATE_FLYING_QUEEN_STINGER_ATTACK,
		AnimationID.KALPHITE_UPDATE_FLYING_QUEEN_RANGED_ATTACK,
		AnimationID.KALPHITE_QUEEN_ATTACK_CLAWS, AnimationID.KALPHITE_QUEEN_LIGHTNING,
		AnimationID.KALPHITE_QUEEN_RANGED_ATTACK, AnimationID.KALPHITE_QUEEN_RANGED_ATTACK_DUPE
	);
	public static final Set<Integer> KALPHITE_FLYING_QUEEN_MODERN_DEFENDS = Set.of(
		AnimationID.KALPHITE_UPDATE_FLYING_QUEEN_DEFEND
	);
	public static final Set<Integer> KALPHITE_FLYING_QUEEN_MODERN_DEATHS = Set.of(
		AnimationID.KALPHITE_UPDATE_FLYING_QUEEN_DEATH
	);

	// Pre-instantiated archetypes for 2005 cache-backed assets
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

	// Mithril, adamant and rune dragons postdate the 2005 cache, so they have no definition and no
	// generated row. They wear the bronze/iron/steel mesh set, which is one greyscale ramp told apart
	// by three opcode 40 pairs on these slots - the same slots every 2005 metal dragon recolors.
	private static final short[] METAL_DRAGON_COLOR_SLOTS = {61, 33, 41};

	// The colors follow the live dragons, since 2005 has none to give. Each takes the face-weighted
	// dominant hue and saturation of its live mesh with its recolors applied, and the brightness
	// ladder of the 2005 metal whose live palette is nearest in brightness:
	//   mithril  live hue 42 sat 2, brightness 16-53 -> iron's 43/30/35
	//   adamant  live hue 24 sat 1, brightness 16-76 -> bronze's 43/39/45
	//   rune     live hue 38 sat 1, brightness 30-80 -> steel's 51/39/43
	private static final short[] MITHRIL_DRAGON_COLORS = {-22229, -22242, -22237};
	private static final short[] ADAMANT_DRAGON_COLORS = {24747, 24743, 24749};
	private static final short[] RUNE_DRAGON_COLORS = {-26445, -26457, -26453};

	// Mirrors the ADULT_DRAGONS branch of createMappingData, which is what the bronze, iron and steel
	// rows go through, so all six metals play the same sequences
	private static RetroNpcData metalDragon(short[] colors, int scale)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.ADULT_DRAGONS)
			.retroModelIds(new int[]{4986, 5022, 4987})
			.idleAnimationId(AnimationID.DRAGON_READY)
			.walkAnimationId(AnimationID.DRAGON_WALK)
			.attackAnimationId(AnimationID.DRAGON_ATTACK)
			.defendAnimationId(AnimationID.DRAGON_BLOCK)
			.scaleXZ(scale)
			.scaleY(scale)
			.recolors(METAL_DRAGON_COLOR_SLOTS, colors)
			.modernAttackAnims(DRAGON_MODERN_ATTACKS)
			.modernDefendAnims(DRAGON_MODERN_DEFENDS)
			.modernDeathAnims(DRAGON_MODERN_DEATHS)
			.attackAnimationOverride(AnimationID.DRAGON_FIREBREATH_ALL_ATTACK, AnimationID.BRUT_DRAGON_CASTING)
			.build();
	}

	// Mith dragons are a bit smaller than Addy/Rune Dragons
	public static final RetroNpcData MITHRIL_DRAGON = metalDragon(MITHRIL_DRAGON_COLORS, 110);
	public static final RetroNpcData ADAMANT_DRAGON = metalDragon(ADAMANT_DRAGON_COLORS, 128);
	public static final RetroNpcData RUNE_DRAGON = metalDragon(RUNE_DRAGON_COLORS, 128);

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

	/**
	 * The six parts of 2005 definition 94, the only Skeleton Mage in that cache. It is not the
	 * skeleton mesh at all but generic human kit, tinted bone by its opcode 40 pairs - the same failure
	 * the guards had. 292, 170 and 256 had their ids reused (the missing torso), and 209, 251 and 325
	 * survive but were re-bound to the live human rig, so both geometry and clips come from the bundle.
	 */
	private static final int[] SKELETON_MAGE_PARTS = {209, 251, 292, 170, 256, 325};

	// The live mage plays the skeleton rig's SKELETON_UPDATE_* family, so the skeleton modern sets are
	// what detect its combat actions; the 2005 human sequences below are what gets played. It fights
	// in two styles: its melee swings are the skeleton's and take the 2005 unarmed punch, while its
	// spells have casts of their own and take the 2005 cast.
	public static final RetroNpcData SKELETON_MAGE = RetroNpcData.builder()
		.category(RetroNpcCategory.SKELETON_MAGES)
		.retroModelIds(SKELETON_MAGE_PARTS)
		.idleAnimationId(AnimationID.HUMAN_READY)
		.walkAnimationId(AnimationID.HUMAN_WALK_F)
		.attackAnimationId(AnimationID.HUMAN_UNARMEDPUNCH)
		.attackAnimationOverride(AnimationID.HUMAN_CASTSTRIKE,
			AnimationID.SKELETON_UPDATE_MAGE_CASTING, AnimationID.SKELETON_STRIKE_CASTING,
			AnimationID.SKELETON_UPDATE_MAGE_CASTING_SWANSONG)
		.defendAnimationId(AnimationID.HUMAN_UNARMEDBLOCK)
		.deathAnimationId(AnimationID.HUMAN_DEATH)
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

	// 2005 def 887 is the one guard of the costume who talks, with head 60
	public static final RetroNpcData CARNILLEAN_GUARD_DEFAULT = ardougneGuard(ARDOUGNE_CARNILLEAN_PARTS)
		.withChathead(new int[]{60});

	/**
	 * 2005 definition 1183, 4044 is the bow.
	 */
	private static final int[] ELF_ARCHER_PARTS = {4014, 4018, 4022, 4024, 4044};

	/**
	 * 2005 definition 1184, Elf with the pike
	 */
	private static final int[] ELF_WARRIOR_PARTS = {4014, 4018, 4022, 4024, 4033};

	private static final int[] ARIANWYN_PARTS = {4015, 4021, 4023, 4025, 4027};

	// 4027 is Arianwyn's cloak, not a held item, so her bow is added rather than swapped in
	private static final int[] ARIANWYN_BOW_PARTS = {4015, 4021, 4023, 4025, 4027, 4044};

	private static final int[] ISLWYN_PARTS = {4015, 4020, 4023, 4025};

	private static final int[] ISLWYN_BOW_PARTS = {4015, 4020, 4023, 4025, 4044};

	/**
	 * 2005 definition 1679.
	 */
	private static final int[] ELUNED_PARTS = {4030, 4032, 4028, 4031, 4029};

	private static final short[] ISLWYN_RECOLOR_FIND = {16701, 9152};
	private static final short[] ISLWYN_RECOLOR_REPLACE = {16945, 111};

	private static final short[] ELUNED_RECOLOR_FIND = {16701, 9137, 4550};
	private static final short[] ELUNED_RECOLOR_REPLACE = {25145, 8, 7062};

	/**
	 * An elf in 2005 human kit. Only the pose is set here: combat ids pass straight through to the
	 * bundle's 2005 clips, the same way the guards' weapon families do.
	 * <p>
	 * Each named elf carries its definition's opcode 60 head: 4011 for Islwyn, 4012 for Arianwyn,
	 * 4013 for Eluned. The warriors 1183 and 1184 had none - they could not be talked to - so they
	 * keep the modern chathead.
	 */
	private static RetroNpcData.Builder elf(int[] parts, int idle)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.ELVES)
			.retroModelIds(parts)
			.idleAnimationId(idle)
			.walkAnimationId(AnimationID.HUMAN_WALK_F)
			.deathAnimationId(AnimationID.HUMAN_DEATH)
			.modernAttackAnims(ELF_MODERN_ATTACKS)
			.modernDefendAnims(ELF_MODERN_DEFENDS)
			.modernDeathAnims(ELF_MODERN_DEATHS);
	}

	private static RetroNpcData.Builder bowElf(int[] parts, int idle)
	{
		return elf(parts, idle)
			.attackAnimationId(AnimationID.HUMAN_BOW)
			.defendAnimationId(AnimationID.HUMAN_UNARMEDBLOCK);
	}

	public static final RetroNpcData ELF_ARCHER = bowElf(ELF_ARCHER_PARTS, AnimationID.HUMAN_STAFFREADY).build();

	public static final RetroNpcData ELF_WARRIOR = elf(ELF_WARRIOR_PARTS, AnimationID.HUMAN_STAFFREADY)
		.attackAnimationId(AnimationID.HUMAN_SCYTHE_SWEEP)
		.defendAnimationId(AnimationID.HUMAN_SPEAR_BLOCK)
		.build();

	public static final RetroNpcData ARIANWYN_DEFAULT = elf(ARIANWYN_PARTS, AnimationID.HUMAN_READY)
		.chathead(4012)
		.build();

	public static final RetroNpcData ARIANWYN_BOW = bowElf(ARIANWYN_BOW_PARTS, AnimationID.HUMAN_READY)
		.chathead(4012)
		.build();

	public static final RetroNpcData ISLWYN_DEFAULT = elf(ISLWYN_PARTS, AnimationID.HUMAN_READY)
		.recolors(ISLWYN_RECOLOR_FIND, ISLWYN_RECOLOR_REPLACE)
		.chathead(4011)
		.build();

	public static final RetroNpcData ISLWYN_BOW = bowElf(ISLWYN_BOW_PARTS, AnimationID.HUMAN_READY)
		.recolors(ISLWYN_RECOLOR_FIND, ISLWYN_RECOLOR_REPLACE)
		.chathead(4011)
		.build();

	public static final RetroNpcData ELUNED_DEFAULT = elf(ELUNED_PARTS, AnimationID.HUMAN_READY)
		.recolors(ELUNED_RECOLOR_FIND, ELUNED_RECOLOR_REPLACE)
		.chathead(4013)
		.build();

	/**
	 * A Lletya elf from the Nov-2005 cache. That build re-authored the human rig, so these meshes and
	 * the clips that animate them are stored apart from the Feb-2005 kit above, at
	 * {@link RetroAssetBundle#NOV_HUMAN_KEY_BASE}. Every Nov elf stands in 808 and walks in 819 - the
	 * warriors included, where their Feb counterparts stood in 813 - and like the Feb elves, combat
	 * ids pass straight through to the clips the bundle carries under that base.
	 * <p>
	 * Parts, recolor pairs and resize are each definition's own, in npc.dat order, and so is the
	 * dialogue head, on the Nov head rig in the same namespace. 2360 and 2362 had no head.
	 */
	private static RetroNpcData.Builder novElf(int[] parts, short[] find, short[] replace, int scaleXZ,
		int scaleY)
	{
		return elf(parts, AnimationID.HUMAN_READY)
			.assetKeyBase(RetroAssetBundle.NOV_HUMAN_KEY_BASE)
			.recolors(find, replace)
			.scaleXZ(scaleXZ)
			.scaleY(scaleY);
	}

	/** Nov-2005 definition 2359, the Elf warrior with the pike and the old Iorwerth head 4020. */
	public static final RetroNpcData NOV_ELF_WARRIOR = novElf(new int[]{4020, 4025, 4016, 4033, 4023},
		new short[]{9152, 6449, 25886}, new short[]{8912, 7646, 7646}, 127, 120)
		.attackAnimationId(AnimationID.HUMAN_SCYTHE_SWEEP)
		.defendAnimationId(AnimationID.HUMAN_SPEAR_BLOCK)
		.chathead(4011)
		.build();

	/** Nov-2005 definition 2360, the second pike warrior, on the Lletya head 8266. */
	public static final RetroNpcData NOV_ELF_WARRIOR2 = novElf(new int[]{8266, 4025, 4016, 4033, 4023},
		new short[]{9137, 6449, 4550}, new short[]{9112, 7646, 6829}, 127, 120)
		.attackAnimationId(AnimationID.HUMAN_SCYTHE_SWEEP)
		.defendAnimationId(AnimationID.HUMAN_SPEAR_BLOCK)
		.build();

	/** Nov-2005 definition 2361, the Elf warrior with the bow. */
	public static final RetroNpcData NOV_ELF_ARCHER = novElf(new int[]{8266, 4025, 4016, 4044, 4023},
		new short[]{9152, 9137, 25886}, new short[]{8910, 5429, 7646}, 127, 120)
		.attackAnimationId(AnimationID.HUMAN_BOW)
		.defendAnimationId(AnimationID.HUMAN_UNARMEDBLOCK)
		.chathead(8261)
		.build();

	/** Nov-2005 definition 2362, the second bow warrior, on the head 8263. */
	public static final RetroNpcData NOV_ELF_ARCHER2 = novElf(new int[]{8263, 4025, 4016, 4044, 4023},
		new short[]{9152, 9137, 4550}, new short[]{8910, 5429, 6699}, 128, 125)
		.attackAnimationId(AnimationID.HUMAN_BOW)
		.defendAnimationId(AnimationID.HUMAN_UNARMEDBLOCK)
		.build();

	/** Nov-2005 definition 1777. */
	public static final RetroNpcData ILFEEN = novElf(new int[]{4029, 4030, 4032, 3194, 4028, 4031},
		new short[]{17864, 14517, 9327}, new short[]{7845, 8753, 6478}, 128, 128).chathead(4013).build();

	/** Nov-2005 definition 2352, the general store. */
	public static final RetroNpcData EUDAV = novElf(new int[]{8264, 8268, 6975, 8267},
		new short[]{7595, 8792}, new short[]{6587, 7634}, 127, 120).chathead(8258).build();

	/** Nov-2005 definition 2353, the seamstress. */
	public static final RetroNpcData ORONWEN = novElf(new int[]{4030, 6981, 480, 6978, 8270},
		new short[]{7595, 8792}, new short[]{6587, 16706}, 128, 129).chathead(4013).build();

	/** Nov-2005 definition 2354. */
	public static final RetroNpcData LLETYA_BANKER_MALE = novElf(new int[]{8266, 8269, 6975, 6976},
		new short[]{107, 7595, 8792, 8652}, new short[]{7390, 7376, 7479, 5400}, 128, 128).chathead(8261).build();

	/** Nov-2005 definition 2355. */
	public static final RetroNpcData LLETYA_BANKER_FEMALE = novElf(new int[]{4030, 6980, 480, 6978, 6979},
		new short[]{107, 7595, 8792, 9137, 14517, 8652}, new short[]{7390, 7376, 7479, 4502, 7479, 6439},
		135, 130).chathead(4013).build();

	/** Nov-2005 definition 2356, the bow seller. */
	public static final RetroNpcData DALLDAV = novElf(new int[]{4021, 6977, 6975, 8267},
		new short[]{8792, 7595, 8741}, new short[]{7634, 13867, 13867}, 127, 120).chathead(4012).build();

	/** Nov-2005 definition 2357, the cook. */
	public static final RetroNpcData GETHIN = novElf(new int[]{8357, 8263, 8268, 6975, 6976},
		new short[]{107, 7595, 8792}, new short[]{7390, 6480, 6583}, 128, 128).chathead(8257, 41).build();

	/** Nov-2005 definition 2363. */
	public static final RetroNpcData GOREU = novElf(new int[]{8266, 6977, 6975, 4023},
		new short[]{16701, 9137, 25886}, new short[]{7613, 5429, 7646}, 127, 113).chathead(8261).build();

	/** Nov-2005 definition 2364. */
	public static final RetroNpcData YSGAWYN = novElf(new int[]{4020, 8269, 6975, 8267},
		new short[]{16701, 9137, 25886, 8792, 9152}, new short[]{7613, 5429, 7646, 13762, 8101},
		132, 131).chathead(4011).build();

	/** Nov-2005 definition 2365. */
	public static final RetroNpcData ARVEL = novElf(new int[]{4030, 6981, 480, 6978, 6979},
		new short[]{16701, 9137, 25886, 8792, 8652, 14517}, new short[]{7613, 8109, 7646, 13762, 7595, 7595},
		128, 128).chathead(4013).build();

	/** Nov-2005 definition 2366. */
	public static final RetroNpcData MAWRTH = novElf(new int[]{4030, 6980, 480, 6978, 6979},
		new short[]{14517}, new short[]{7595}, 123, 110).chathead(4013).build();

	/** Nov-2005 definition 2367. */
	public static final RetroNpcData KELYN = novElf(new int[]{4030, 6981, 6978, 6979},
		new short[]{16701, 9137, 25886}, new short[]{7613, 5429, 7646}, 128, 128).chathead(4013).build();

	/** Nov-2005 definition 2368, an elven child. */
	public static final RetroNpcData EOIN = novElf(new int[]{8264, 8269, 6975, 6976},
		new short[]{9137, 8792}, new short[]{8117, 11592}, 100, 79).chathead(8259).build();

	/** Nov-2005 definition 2369, an elven child. */
	public static final RetroNpcData IONA = novElf(new int[]{4021, 6977, 6975, 8267},
		new short[]{9137, 8792}, new short[]{8117, 11592}, 106, 87).chathead(8260).build();

	/*
	 * The colors painted on the Nov Lletya kits' meshes, by what they cover - measured on the meshes
	 * themselves, so these are the slots a recolor aims at, not colors anyone sees. Packed HSL, like
	 * every color here.
	 */
	/** Hair on the Lletya heads. */
	private static final short KIT_HAIR = 9137;
	/** Hair on the old Iorwerth head 4020. */
	private static final short KIT_HAIR_OLD_HEAD = 9152;
	/** The main garment. */
	private static final short KIT_GARMENT = 8792;
	/** The tunic on the older parts, the warriors' included. */
	private static final short KIT_TUNIC = 16701;
	/** The secondary cloth. */
	private static final short KIT_SECONDARY = 8652;
	/** Trim, belts and boots. */
	private static final short KIT_TRIM = 7595;
	/** The low band on the warrior's body 4016. */
	private static final short KIT_WARRIOR_BAND = 12469;
	/** The two colors of the pike 4033 that definition 2359 paints wood. */
	private static final short KIT_PIKE_SHAFT = 6449;
	private static final short KIT_PIKE_BAND = 25886;
	/** The wood definition 2359 paints the pike. */
	private static final short PIKE_WOOD = 7646;

	/** The hair and cloth slots of the Nov Lletya kits, which a Prifddinas citizen's own colors replace. */
	private static final short[] CITIZEN_RECOLOR_FIND = {
		KIT_HAIR, KIT_HAIR_OLD_HEAD, KIT_GARMENT, KIT_TUNIC, KIT_SECONDARY, KIT_TRIM};

	/**
	 * The Lletya outfits a citizen can wear, by body. Gethin's chef's hat and the children are left
	 * out, and the female banker is Mawrth's outfit at another size. Each keeps its own resize.
	 */
	private static final RetroNpcData[] CITIZEN_MALE_KITS = {GOREU, YSGAWYN, EUDAV, DALLDAV, LLETYA_BANKER_MALE};

	private static final RetroNpcData[] CITIZEN_FEMALE_KITS = {ARVEL, MAWRTH, KELYN, ORONWEN};

	/**
	 * A Prifddinas citizen, who has no 2005 definition: a Lletya outfit for the same body, chosen by
	 * id so a crowd is not all one elf, worn in the citizen's own live colors. Every citizen recolors
	 * the same six live indices - three shades of hair, then three of clothing - and the clothing is
	 * what tells the eight clans apart, so carrying the live values over is what makes a Hefin
	 * citizen crimson and an Iorwerth one near-black, the odd mixed outfit included.
	 * <p>
	 * The lightest hair shade goes to the head, and the clothing shades go lightest to darkest onto
	 * the main garment, the secondary cloth and the trim.
	 */
	static RetroNpcData citizen(int npcId, boolean female, int hair, int cloth, int clothShade, int clothDark)
	{
		RetroNpcData[] kits = female ? CITIZEN_FEMALE_KITS : CITIZEN_MALE_KITS;
		return dressed(kits[npcId % kits.length], hair, cloth, clothShade, clothDark);
	}

	/** A Lletya outfit chosen by hand, in the given hair and clothing colors. */
	static RetroNpcData dressed(RetroNpcData kit, int hair, int cloth, int clothShade, int clothDark)
	{
		return kit.withRecolors(CITIZEN_RECOLOR_FIND, new short[]{
			(short) hair, (short) hair, (short) cloth, (short) cloth, (short) clothShade, (short) clothDark});
	}

	/**
	 * Registers a citizen under its own id and any others that are the same character, which share
	 * the outfit the first id picks rather than each picking their own.
	 */
	private static void registerCitizen(int npcId, boolean female, int hair, PrifClothing clothing,
		int... sameNpcIds)
	{
		registerCitizen(npcId, female, hair, clothing.cloth, clothing.clothShade, clothing.clothDark, sameNpcIds);
	}

	/** A citizen whose live clothing mixes shades from more than one set. */
	private static void registerCitizen(int npcId, boolean female, int hair, int cloth, int clothShade,
		int clothDark, int... sameNpcIds)
	{
		RetroNpcData data = citizen(npcId, female, hair, cloth, clothShade, clothDark);
		registerMapping(data, npcId);
		registerMapping(data, sameNpcIds);
	}

	private static void registerDressed(RetroNpcData kit, int hair, PrifClothing clothing, int... npcIds)
	{
		registerMapping(dressed(kit, hair, clothing.cloth, clothing.clothShade, clothing.clothDark), npcIds);
	}

	/**
	 * Prifddinas's halberd guards, who are the Mourning's End warriors again: the pike warrior's
	 * kit from definition 2359, keeping its pike colors.
	 */
	private static final int[] PRIF_GUARD_PARTS = {4020, 4025, 4016, 4033, 4023};

	private static final int[] PRIF_GUARD_FEMALE_PARTS = {4030, 4025, 4016, 4033, 4023};

	private static final int[] PRIF_GUARD_FEMALE_CHATHEAD = {4013};

	private static final short[] PRIF_GUARD_RECOLOR_FIND = {
		KIT_HAIR, KIT_HAIR_OLD_HEAD, KIT_TUNIC, KIT_TRIM, KIT_WARRIOR_BAND, KIT_PIKE_SHAFT, KIT_PIKE_BAND};

	/**
	 * Prif Crystal armor & undersuit/boot colors
	 */
	private static final short PRIF_CRYSTAL = 32200;

	private static final short PRIF_UNDERSUIT = -30054;

	private static final short PRIF_BOOTS = 3604;

	static RetroNpcData prifGuard(boolean female, int hair)
	{
		// The women's head 4030 is the one the Lletya women wear, and every one of them talks with
		// head 4013
		return NOV_ELF_WARRIOR.withModelIds(female ? PRIF_GUARD_FEMALE_PARTS : PRIF_GUARD_PARTS)
		    .withChathead(female ? PRIF_GUARD_FEMALE_CHATHEAD : NOV_ELF_WARRIOR.getChatheadModelIds())
			.withRecolors(PRIF_GUARD_RECOLOR_FIND, new short[]{(short) hair, (short) hair, PRIF_CRYSTAL,
				PRIF_BOOTS, PRIF_UNDERSUIT, PIKE_WOOD, PIKE_WOOD});
	}

	private static void registerPrifGuard(int npcId, boolean female, int hair)
	{
		registerMapping(prifGuard(female, hair), npcId);
	}

	/**
	 * Elena is a human, but her story runs through the elves' - Plague City to Lletya to
	 * Prifddinas - so she rides the Elves toggle. Plain Feb-2005 human kit on the guards' rig and
	 * clips, so no key base. Several of her colors are past 32767 and are stored as the signed shorts
	 * the palette uses.
	 */
	private static RetroNpcData.Builder elena(int[] parts, short[] find, short[] replace)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.ELVES)
			.retroModelIds(parts)
			.idleAnimationId(AnimationID.HUMAN_READY)
			.walkAnimationId(AnimationID.HUMAN_WALK_F)
			.deathAnimationId(AnimationID.HUMAN_DEATH)
			.recolors(find, replace);
	}

	/**
	 * Feb-2005 definition 335, "She looks concerned.", in the Plague City block of definitions -
	 * the prisoner.
	 */
	public static final RetroNpcData ELENA_PRISONER = elena(new int[]{390, 456, 332, 353, 440, 445},
		new short[]{8741, 25238, 6798, (short) 43072, 41},
		new short[]{(short) 32962, 21838, 4502, 4550, (short) 59350}).chathead(113).build();

	/**
	 * Feb-2005 definition 715, "She doesn't look too happy.", in the Biohazard block - Elena once
	 * she is free, which is every later appearance.
	 */
	public static final RetroNpcData ELENA_DEFAULT = elena(new int[]{390, 474, 347, 353, 438, 358},
		new short[]{8741, 25238, 6798, (short) 43072},
		new short[]{(short) 32962, 21838, 4502, 4550}).chathead(113).build();

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

	// What the 2005 chicken definition itself asked for
	private static final int CHICKEN_2005_SCALE = 128;

	/**
	 * The chicken family at the size it was in 2005.
	 * <p>
	 * Every scale in the family is the definition's own times {@link #CHICKEN_SCALE}/128 - see
	 * chicken(...) - so dividing that factor back out recovers the definition: 128 for the hen and
	 * the undead chicken, 172 for the rooster. Rounded, because 274 does not divide back evenly.
	 */
	public static RetroNpcData at2005Size(RetroNpcData data)
	{
		if (data == null || data.getCategory() != RetroNpcCategory.CHICKENS)
		{
			return data;
		}

		return data.withScale(
			Math.round(data.getScaleXZ() * (float) CHICKEN_2005_SCALE / CHICKEN_SCALE),
			Math.round(data.getScaleY() * (float) CHICKEN_2005_SCALE / CHICKEN_SCALE));
	}

	/**
	 * A 2005 chicken variant: the shared mesh, the 2005 chicken sequences, and the opcode 40 pairs
	 * that are the only thing telling one bird from another.
	 * <p>
	 * Scale is a parameter rather than a constant because this family spans two sizes.
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
	private static RetroNpcData goblin(int[] models, short[] find, short[] replace, int... chathead)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.GOBLINS)
			.retroModelIds(models)
			.chathead(chathead)
			.chatheadKeyBase(RetroAssetBundle.GOBLIN_CHATHEAD_KEY_BASE)
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

	// Def 298 - green armor, 916 -> 22443. Live GOBLIN_GREENARMOUR still carries this pair
	// verbatim over the 2005 meshes themselves, which is what pins green to this definition.
	public static final RetroNpcData GOBLIN_GREEN = goblin(GOBLIN_ARMED_PARTS,
		new short[]{916, 70, 8084}, new short[]{22443, -22417, 528}, 142);

	// Def 299 - red armor, 916 -> 933, and live GOBLIN_REDARMOUR carries that pair verbatim too.
	public static final RetroNpcData GOBLIN_RED = goblin(GOBLIN_ARMED_PARTS,
		new short[]{916, 70, 8084}, new short[]{933, -22417, 528}, 144);

	// Def 489 - the Goblin guard. The same armed kit as def 101 in the plain colors, and the green
	// goblin's head 142
	public static final RetroNpcData GOBLIN_GUARD_DEFAULT = goblin(
		GOBLIN_ARMED_PARTS, GOBLIN_WEAPON_DRIFT_FIND, GOBLIN_WEAPON_DRIFT_REPLACE, 142);

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

	// The large 2005 scorpion is mesh 2967 in the 2005 cache (re-assigned in modern OSRS)
	// Its animation ids are the modern scorpion sequences, not the 2005 ones.
	private static final int SCORPION_BODY = 2967;

	private static RetroNpcData scorpion(short[] find, short[] replace, int scale)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.SCORPIONS)
			.retroModelIds(new int[]{SCORPION_BODY})
			.idleAnimationId(AnimationID.SCORPION_UPDATE_READY)
			.walkAnimationId(AnimationID.SCORPION_UPDATE_WALK)
			.attackAnimationId(AnimationID.SCORPION_UPDATE_ATTACK_TAIL)
			.defendAnimationId(AnimationID.SCORPION_UPDATE_DEFEND)
			.deathAnimationId(AnimationID.SCORPION_UPDATE_DEATH)
			.miscAnimationId(AnimationID.SCORPION_UPDATE_READY)
			.scaleXZ(scale)
			.scaleY(scale)
			.recolors(find, replace)
			.modernAttackAnims(SCORPION_MODERN_ATTACKS)
			.modernDefendAnims(SCORPION_MODERN_DEFENDS)
			.modernDeathAnims(SCORPION_MODERN_DEATHS)
			.build();
	}

	// Defs 107 "Scorpion" and 108 "Poison Scorpion": the same mesh, with no opcode 40 data and no resize
	public static final RetroNpcData SCORPION_DEFAULT = scorpion(null, null, 128);

	// Def 144 asks for 180
	public static final RetroNpcData KING_SCORPION = scorpion(null, null, 180);

	// The opcode 40 pairs below live only here. npc-mappings.json has no scorpion row, so
	// applyCacheDefinitions has nothing to graft, and SCORPIONS stays out of categoryUsesRecolors.
	//
	// Def 271, the Fight Arena scorpion
	public static final RetroNpcData KHAZARD_SCORPION = scorpion(
		new short[]{3627, 3738}, new short[]{41, 24}, 128);

	// Def 1477, the Monkey Madness jungle scorpion. Its scale of 32 matches the 32 the live
	// composition still asks for
	public static final RetroNpcData JUNGLE_SCORPION = scorpion(
		new short[]{3627, 3738}, new short[]{268, 272}, 32);

	// The small 2005 scorpions (defs 109 Pit, 385-387 Kharid, 493 Grave) survive in the modern cache
	public static final RetroNpcData SMALL_SCORPION = RetroNpcData.builder()
		.category(RetroNpcCategory.SMALL_SCORPIONS)
		.retroModelIds(new int[]{2968})
		.idleAnimationId(AnimationID.SMALLSCORPION_READY)
		.walkAnimationId(AnimationID.SMALLSCORPION_WALK)
		.attackAnimationId(AnimationID.SMALLSCORPION_ATTACK)
		.defendAnimationId(AnimationID.SMALLSCORPION_BLOCK)
		.deathAnimationId(AnimationID.SMALLSCORPION_DEATH)
		.miscAnimationId(AnimationID.SMALLSCORPION_READY)
		.scaleXZ(128)
		.scaleY(128)
		.modernAttackAnims(SCORPION_MODERN_ATTACKS)
		.modernDefendAnims(SCORPION_MODERN_DEFENDS)
		.modernDeathAnims(SCORPION_MODERN_DEATHS)
		.build();

	// The 2005 giant rat is mesh 2959 on sequences 137-141, and every Feb-2005 "Giant rat" def (86,
	// 87, 748, 950, and "Blessed Giant rat" 978) is that pair. Both survive in the live cache
	private static final int GIANT_RAT_BODY = 2959;

	// Set default mesh scale
	private static final int GIANT_RAT_SCALE = 128;

	private static RetroNpcData.Builder giantRat()
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.GIANT_RATS)
			.retroModelIds(new int[]{GIANT_RAT_BODY})
			.idleAnimationId(AnimationID.GIANTRAT_READY)
			.walkAnimationId(AnimationID.GIANTRAT_WALK)
			.attackAnimationId(AnimationID.GIANTRAT_ATTACK)
			.defendAnimationId(AnimationID.GIANTRAT_BLOCK)
			.deathAnimationId(AnimationID.GIANTRAT_DEATH)
			.scaleXZ(GIANT_RAT_SCALE)
			.scaleY(GIANT_RAT_SCALE)
			.modernAttackAnims(GIANT_RAT_MODERN_ATTACKS)
			.modernDefendAnims(GIANT_RAT_MODERN_DEFENDS)
			.modernDeathAnims(GIANT_RAT_MODERN_DEATHS);
	}

	// 2959's own palette: the dark blue rat
	public static final RetroNpcData GIANT_RAT_DEFAULT = giantRat().build();

	// Light gray Lumbridge rat - just a recolor of the base rat
	public static final RetroNpcData GIANT_RAT_GRAY = giantRat()
		.recolors(new short[]{-22237}, new short[]{70})
		.build();

	// The 2005 spiders are four meshes on two rigs, all of which exist in the live cache.
	// The opcode 40 pairs below live only here. npc-mappings.json has no spider row, so
	// applyCacheDefinitions has nothing to graft, and SPIDERS stays out of categoryUsesRecolors.
	private static final int POISON_SPIDER_BODY = 2883;
	private static final int GIANT_SPIDER_BODY = 2884;
	private static final int ICE_SPIDER_BODY = 2885;
	private static final int SMALL_SPIDER_BODY = 2996;
	private static final int SMALL_SPIDER_LIVE_BASE = 7591;

	private static RetroNpcData.Builder largeSpider(int mesh)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.SPIDERS)
			.retroModelIds(new int[]{mesh})
			.idleAnimationId(AnimationID.GIANTSPIDER_READY)
			.walkAnimationId(AnimationID.GIANTSPIDER_WALK)
			.attackAnimationId(AnimationID.GIANTSPIDER_ATTACK)
			.defendAnimationId(AnimationID.GIANTSPIDER_BLOCK)
			.deathAnimationId(AnimationID.GIANTSPIDER_DEATH)
			.miscAnimationId(AnimationID.GIANTSPIDER_READY)
			.modernAttackAnims(SPIDER_MODERN_ATTACKS)
			.modernDefendAnims(SPIDER_MODERN_DEFENDS)
			.modernDeathAnims(SPIDER_MODERN_DEATHS)
			.modernMiscAnims(SPIDER_MODERN_MISC);
	}

	// Every 2005 small spider has standingAnim -1, and there is no SPIDER_READY animation
	private static RetroNpcData.Builder smallSpider(int bodyColor)
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.SPIDERS)
			.retroModelIds(new int[]{SMALL_SPIDER_BODY})
			.staticIdle()
			.walkAnimationId(AnimationID.SPIDER_WALK)
			.attackAnimationId(AnimationID.SPIDER_ATTACK)
			.defendAnimationId(AnimationID.SPIDER_BLOCK)
			.deathAnimationId(AnimationID.SPIDER_DEATH)
			.recolor(SMALL_SPIDER_LIVE_BASE, bodyColor)
			.modernAttackAnims(SPIDER_MODERN_ATTACKS)
			.modernDefendAnims(SPIDER_MODERN_DEFENDS)
			.modernDeathAnims(SPIDER_MODERN_DEATHS);
	}

	// Def 61, the Feb-2005 grey. It has no pair of its own; this one undoes the repaint
	public static final RetroNpcData SPIDER_DEFAULT = smallSpider(61).build();

	// Def 1004, the Underground Pass spider (61 -> 10258)
	public static final RetroNpcData SPIDER_UPASS = smallSpider(10258).build();

	// Defs 1473 and 1474, the Monkey Madness warehouse spiders (61 -> 20, resized to 64)
	public static final RetroNpcData SPIDER_MM = smallSpider(20)
		.scaleXZ(64)
		.scaleY(64)
		.build();

	// Def 1221 (61 -> 900), a non-combat spider
	public static final RetroNpcData SPIDER_RED = smallSpider(900).build();

	// Def 2034 exists only in the Nov-2005 cache, since Barrows postdates the February one. Its pair
	// already names 7591, so no correction is needed
	public static final RetroNpcData CRYPT_SPIDER = smallSpider(12574).build();

	// Defs 59 and 60
	public static final RetroNpcData GIANT_SPIDER = largeSpider(GIANT_SPIDER_BODY)
		.recolors(new short[]{10258}, new short[]{8084})
		.build();

	// Def 58: 2884's own palette
	public static final RetroNpcData SHADOW_SPIDER = largeSpider(GIANT_SPIDER_BODY).build();

	// Def 977: 2884's own palette
	public static final RetroNpcData BLESSED_SPIDER = largeSpider(GIANT_SPIDER_BODY).build();

	// Def 63
	public static final RetroNpcData DEADLY_RED_SPIDER = largeSpider(GIANT_SPIDER_BODY)
		.recolors(new short[]{10258}, new short[]{-1122})
		.build();

	// Def 62 (and the Nov-2005 Tai Bwo Wannai def 2491)
	public static final RetroNpcData JUNGLE_SPIDER = largeSpider(GIANT_SPIDER_BODY)
		.recolors(new short[]{10258}, new short[]{16945})
		.build();

	// Nov-2005 def 2492, the Tai Bwo Wannai spider that springs out of a bush.
	public static final RetroNpcData JUNGLE_SPIDER_BUSH = largeSpider(GIANT_SPIDER_BODY)
		.walkAnimationId(AnimationID.TBW_CLEANUP_SPIDER_SURPRISE_ATTACK)
		.attackAnimationOverride(AnimationID.TBW_CLEANUP_SPIDER_SURPRISE_ATTACK,
			AnimationID.SPIDER_UPDATE_TBW_BUSH_ATTACK)
		.recolors(new short[]{10258}, new short[]{16945})
		.build();

	// Def 1478, the Monkey Madness jungle spider
	public static final RetroNpcData MM_JUNGLE_SPIDER = largeSpider(GIANT_SPIDER_BODY)
		.recolors(new short[]{8128, 10258}, new short[]{1835, 12589})
		.scaleXZ(100)
		.scaleY(100)
		.build();

	// Def 2035, Nov-2005 only, like the crypt spider. 2884 kept every face color
	public static final RetroNpcData GIANT_CRYPT_SPIDER = largeSpider(GIANT_SPIDER_BODY)
		.recolors(new short[]{10258, 8128, 127}, new short[]{17930, 13074, 16846})
		.build();

	// Def 64: 2885's own palette
	public static final RetroNpcData ICE_SPIDER = largeSpider(ICE_SPIDER_BODY).build();

	// Defs 134 and 1009: 2883's own palette
	public static final RetroNpcData POISON_SPIDER = largeSpider(POISON_SPIDER_BODY).build();

	// Every 2005 bear is mesh 2966, and it survives in the live cache with every face color.
	// The live framemap 275 and the frames behind 37-44 are identical to 2005's. The opcode 40 pairs
	// are defined here rather than in npc-mappings.json
	private static final int BEAR_BODY = 2966;
	private static final short BEAR_BASE = 3491;

	private static RetroNpcData.Builder bear()
	{
		return RetroNpcData.builder()
			.category(RetroNpcCategory.BEARS)
			.retroModelIds(new int[]{BEAR_BODY})
			.idleAnimationId(AnimationID.BEAR_READY)
			.walkAnimationId(AnimationID.BEAR_WALK)
			.attackAnimationId(AnimationID.BEAR_ATTACK)
			.defendAnimationId(AnimationID.BEAR_BLOCK)
			.deathAnimationId(AnimationID.BEAR_DEATH)
			.miscAnimationId(AnimationID.BEAR_READY)
			.modernAttackAnims(BEAR_MODERN_ATTACKS)
			.modernDefendAnims(BEAR_MODERN_DEFENDS)
			.modernDeathAnims(BEAR_MODERN_DEATHS)
			.modernMiscAnims(BEAR_MODERN_MISC);
	}

	// Def 105, the plain brown "Bear". Live BROWNBEAR is its descendant, renamed Grizzly bear
	public static final RetroNpcData BEAR_BROWN = bear().build();

	// Def 106
	public static final RetroNpcData BEAR_BLACK = bear()
		.recolors(new short[]{BEAR_BASE}, new short[]{12})
		.build();

	// Def 1195, the Regicide grizzly
	public static final RetroNpcData GRIZZLY_BEAR = bear()
		.recolors(new short[]{BEAR_BASE}, new short[]{520})
		.build();

	// Defs 1196 and 1197, paired with the two live cubs by id order
	public static final RetroNpcData GRIZZLY_BEAR_CUB_1 = bear()
		.recolors(new short[]{BEAR_BASE}, new short[]{520})
		.scaleXZ(70)
		.scaleY(70)
		.build();
	public static final RetroNpcData GRIZZLY_BEAR_CUB_2 = bear()
		.recolors(new short[]{BEAR_BASE}, new short[]{520})
		.scaleXZ(80)
		.scaleY(80)
		.build();

	// Defs 1326 and 1327
	public static final RetroNpcData BEAR_CUB_1 = bear()
		.recolors(new short[]{BEAR_BASE, 3377}, new short[]{3486, 3373})
		.scaleXZ(96)
		.scaleY(96)
		.build();
	public static final RetroNpcData BEAR_CUB_2 = bear()
		.recolors(new short[]{BEAR_BASE, 3377}, new short[]{3486, 3373})
		.scaleXZ(64)
		.scaleY(64)
		.build();

	// Every 2005 Kalphite mesh is gone from the live cache, so the whole family is bundle-only. The
	// opcode 40 pairs and the scales are the 2005 definitions; npc-mappings.json has no Kalphite
	// row to graft from
	private static final short KALPHITE_SHELL = 10659;
	private static final short KALPHITE_UNDERSIDE = 22412;

	private static RetroNpcData.Builder kalphite(int mesh, int scale, int idle, int walk,
		int attack, int defend, int death, Set<Integer> attacks, Set<Integer> defends, Set<Integer> deaths)
	{
		RetroNpcData.Builder builder = RetroNpcData.builder()
			.category(RetroNpcCategory.KALPHITES)
			.retroModelIds(new int[]{mesh})
			.idleAnimationId(idle)
			.walkAnimationId(walk)
			.attackAnimationId(attack)
			.defendAnimationId(defend)
			.deathAnimationId(death)
			.miscAnimationId(idle)
			.scaleXZ(scale)
			.scaleY(scale)
			.modernDefendAnims(defends)
			.modernDeathAnims(deaths);

		// Each attack keeps its own clip rather than collapsing onto attackAnimationId. The Queen is
		// a boss, so the swap stays one to one and tells apart exactly the attacks live does
		for (int modern : attacks)
		{
			builder.attackAnimationOverride(modern, modern);
		}
		return builder;
	}

	private static RetroNpcData.Builder groundKalphite(int mesh, int scale, int idle, int walk,
		int attack, int defend, int death)
	{
		return kalphite(mesh, scale, idle, walk, attack, defend, death,
			KALPHITE_MODERN_ATTACKS, KALPHITE_MODERN_DEFENDS, KALPHITE_MODERN_DEATHS);
	}

	// Def 1153
	public static final RetroNpcData KALPHITE_WORKER = groundKalphite(3852, 128,
		AnimationID.KALPHITE_UPDATE_READY, AnimationID.KALPHITE_UPDATE_WALK,
		AnimationID.KALPHITE_UPDATE_JAWS_ATTACK, AnimationID.KALPHITE_UPDATE_DEFEND,
		AnimationID.KALPHITE_UPDATE_DEATH)
		.recolors(new short[]{KALPHITE_SHELL, KALPHITE_UNDERSIDE}, new short[]{20364, 11160})
		.build();

	// Def 1154. The live lair Soldier's model 24595 is the POH Soldier's 24593 moved 71 back, so
	// it stands half a tile off its footprint's centre; the 2005 mesh is centred and follows it
	public static final RetroNpcData KALPHITE_SOLDIER = groundKalphite(3851, 172,
		AnimationID.KALPHITE_UPDATE_READY, AnimationID.KALPHITE_UPDATE_WALK,
		AnimationID.KALPHITE_UPDATE_JAWS_ATTACK, AnimationID.KALPHITE_UPDATE_DEFEND,
		AnimationID.KALPHITE_UPDATE_DEATH)
		.recolors(new short[]{KALPHITE_SHELL, KALPHITE_UNDERSIDE}, new short[]{26506, 10894})
		.offsetZ(-71)
		.build();

	// The POH composition draws the centred 24593 at 172, a third bigger than the lair Soldier, so
	// the 2005 mesh takes that 172 on top of its own: 172 * 172 / 128
	public static final RetroNpcData KALPHITE_SOLDIER_POH = groundKalphite(3851, 231,
		AnimationID.KALPHITE_UPDATE_READY, AnimationID.KALPHITE_UPDATE_WALK,
		AnimationID.KALPHITE_UPDATE_JAWS_ATTACK, AnimationID.KALPHITE_UPDATE_DEFEND,
		AnimationID.KALPHITE_UPDATE_DEATH)
		.recolors(new short[]{KALPHITE_SHELL, KALPHITE_UNDERSIDE}, new short[]{26506, 10894})
		.build();

	// Def 1155. The live gamevals call it a lord
	public static final RetroNpcData KALPHITE_GUARDIAN = groundKalphite(3849, 256,
		AnimationID.KALPHITE_UPDATE_LORD_READY, AnimationID.KALPHITE_UPDATE_LORD_WALK,
		AnimationID.KALPHITE_UPDATE_CLAW_LORD_ATTACK, AnimationID.KALPHITE_UPDATE_DEFEND,
		AnimationID.KALPHITE_UPDATE_LORD_DEATH)
		.recolors(new short[]{KALPHITE_SHELL}, new short[]{27528})
		.build();

	// Def 1161: the worker's mesh at a quarter size
	public static final RetroNpcData KALPHITE_LARVA = groundKalphite(3852, 32,
		AnimationID.KALPHITE_UPDATE_READY, AnimationID.KALPHITE_UPDATE_LARVAE_WALK,
		AnimationID.KALPHITE_UPDATE_LARVAE_JAWS_ATTACK, AnimationID.KALPHITE_UPDATE_DEFEND,
		AnimationID.KALPHITE_UPDATE_LARVAE_DEATH)
		.recolors(new short[]{KALPHITE_SHELL, KALPHITE_UNDERSIDE}, new short[]{11596, 17102})
		.build();

	// Def 1158. Mesh 3850 rides the same 2005 rig as the rest of the ground family
	public static final RetroNpcData KALPHITE_QUEEN_CRAWLING = groundKalphite(3850, 128,
		AnimationID.KALPHITE_UPDATE_QUEEN_READY, AnimationID.KALPHITE_UPDATE_QUEEN_WALK,
		AnimationID.KALPHITE_UPDATE_QUEEN_JAWS_ATTACK, AnimationID.KALPHITE_UPDATE_QUEEN_DEFEND,
		AnimationID.KALPHITE_UPDATE_QUEEN_DEATH)
		.build();

	// Def 1160, on a 2005 rig of its own. The transform from the crawling form arrives as an
	// NpcChanged, which processNpc rebuilds onto this
	public static final RetroNpcData KALPHITE_QUEEN_AIRBORNE = kalphite(3854, 256,
		AnimationID.KALPHITE_UPDATE_FLYING_QUEEN_READY_WALK, AnimationID.KALPHITE_UPDATE_FLYING_QUEEN_READY_WALK,
		AnimationID.KALPHITE_UPDATE_FLYING_QUEEN_STINGER_ATTACK, AnimationID.KALPHITE_UPDATE_FLYING_QUEEN_DEFEND,
		AnimationID.KALPHITE_UPDATE_FLYING_QUEEN_DEATH,
		KALPHITE_FLYING_QUEEN_MODERN_ATTACKS, KALPHITE_FLYING_QUEEN_MODERN_DEFENDS,
		KALPHITE_FLYING_QUEEN_MODERN_DEATHS)
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
	 * Grafts what only the 2005 definition knows - the recolor pairs, the resize and the dialogue
	 * head - onto the static archetypes.
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

			// The head is the definition's own, like the resize, and only fills a gap: an archetype
			// that names a head keeps it
			if (!data.hasChathead() && entry.getChatheadModelIds() != null)
			{
				updated = updated.withChathead(entry.getChatheadModelIds());
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
			|| category == RetroNpcCategory.SKELETON_MAGES
			|| category == RetroNpcCategory.COWS
			|| category == RetroNpcCategory.SCORPIONS
			|| category == RetroNpcCategory.ELVES
			|| category == RetroNpcCategory.KALPHITES;
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
			// The skeleton mage is the same generic human kit, and its pairs are what paint it bone.
			// There is only one 2005 definition, so no variant's colors can win by accident.
			|| category == RetroNpcCategory.SKELETON_MAGES
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

		// The metals past steel, by name the way bronze, iron and steel resolve
		NAME_MAPPINGS.put("mithril dragon", MITHRIL_DRAGON);
		NAME_MAPPINGS.put("adamant dragon", ADAMANT_DRAGON);
		NAME_MAPPINGS.put("rune dragon", RUNE_DRAGON);

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

		// Skeleton mages. LOTR_MAGE_SKELETON (Tarn's Lair) and DS2_SKELETON_MAGIC are both named plain
		// "Skeleton" while carrying mesh 21193, the mage kit, so their ids only reach this row through
		// NAME_OVERRIDDEN_IDS - see get(). The DS2 melee and ranged skeletons beside 8072 wear ordinary
		// skeleton kit and stay on the name row. Left out: WGS_UNDEAD_MAGE ("Undead Mage") has no 2005
		// counterpart.
		NAME_MAPPINGS.put("skeleton mage", SKELETON_MAGE);
		registerMapping(SKELETON_MAGE,
			NpcID.SKELETONMAGE, NpcID.UNATTACKABLE_SKELETON_MAGE,
			NpcID.SWAN_SKELETON_BATTLE, NpcID.SWAN_SKELETON_UNATTACKABLE, NpcID.SWAN_SKELETON_TRAINING,
			NpcID.LOTR_MAGE_SKELETON, NpcID.DS2_SKELETON_MAGIC
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

		// Elves, by id only. Every live id carrying one of the names, including the Song of the
		// Elves and Prifddinas copies. Only named multiloc children are registered - the client
		// reports the transformed id, so a parent never reaches a lookup.
		registerMapping(ELF_ARCHER,
			NpcID.REGICIDE_DARKELF, NpcID.REGICIDE_DARKELF4,
			NpcID.SOTE_CAVE_WARRIOR_2, NpcID.SOTE_IORWERTH_WARRIOR_1, NpcID.SOTE_IORWERTH_WARRIOR_2,
			NpcID.SOTE_UPASS_IORWERTH_WARRIOR_1, NpcID.SOTE_UPASS_IORWERTH_WARRIOR_2,
			NpcID.SOTE_IORWERTH_WARRIOR_CUTSCENE_2, NpcID.SOTE_IORWERTH_WARRIOR_CUTSCENE_4,
			NpcID.SOTE_IORWERTH_WARRIOR_CUTSCENE_9, NpcID.SOTE_IORWERTH_WARRIOR_CUTSCENE_10
		);
		registerMapping(ELF_WARRIOR,
			NpcID.REGICIDE_DARKELF2, NpcID.REGICIDE_DARKELF3,
			NpcID.SOTE_CAVE_WARRIOR_1, NpcID.SOTE_IORWERTH_WARRIOR_3, NpcID.SOTE_IORWERTH_WARRIOR_4,
			NpcID.SOTE_UPASS_IORWERTH_WARRIOR_3, NpcID.SOTE_UPASS_IORWERTH_WARRIOR_4,
			NpcID.PRIF_DARKELF2, NpcID.PRIF_DARKELF3,
			NpcID.SOTE_IORWERTH_WARRIOR_CUTSCENE_1, NpcID.SOTE_IORWERTH_WARRIOR_CUTSCENE_3,
			NpcID.SOTE_IORWERTH_WARRIOR_CUTSCENE_5, NpcID.SOTE_IORWERTH_WARRIOR_CUTSCENE_6,
			NpcID.SOTE_IORWERTH_WARRIOR_CUTSCENE_7, NpcID.SOTE_IORWERTH_WARRIOR_CUTSCENE_8
		);
		registerMapping(ARIANWYN_DEFAULT,
			NpcID.REGICIDE_GOOD_ELF3, NpcID.SOTE_ARIANWYN_CUTSCENE, NpcID.SOTE_ARIANWYN_CUTSCENE_NOMOVE,
			NpcID.MOURNING_ARIANWYN_VIS
		);
		registerMapping(ARIANWYN_BOW,
			NpcID.SOTE_ARIANWYN_COMBAT, NpcID.NZONE_SOTE_ARIANWYN_NORMAL, NpcID.SOTE_ARIANWYN_CUTSCENE_BOW
		);
		registerMapping(ISLWYN_DEFAULT,
			NpcID.ROVING_ISLWYN_1OP, NpcID.ROVING_ISLWYN_2OPS, NpcID.SOTE_ISLWYN_VIS,
			NpcID.SOTE_ISLWYN_CUTSCENE, NpcID.ISLWYN_PRIF
		);
		registerMapping(ISLWYN_BOW,
			NpcID.SOTE_ISLWYN_UPASS, NpcID.SOTE_ISLWYN_CUTSCENE_BOW, NpcID.SOTE_ISLWYN_BATTLE
		);
		registerMapping(ELUNED_DEFAULT,
			NpcID.ROVING_FEMALE_WOODELF_TEMP_1, NpcID.ROVING_FEMALE_WOODELF_1OP,
			NpcID.ROVING_FEMALE_WOODELF_2OP, NpcID.SOTE_ELUNED_UPASS, NpcID.SOTE_ELUNED_CUTSCENE,
			NpcID.ELUNED_PRIF
		);

		// Lletya, from the Nov-2005 cache, which has every one of them as a definition of its own.
		//
		// The Mourning's End warriors are two live pairs, since remodelled as a man and a woman, and
		// two Nov pairs that differ by head and color. Neither side carries anything that ties one
		// to the other, so they pair in id order.
		registerMapping(NOV_ELF_WARRIOR, NpcID.MOURNING_GUARD_MELEE);
		registerMapping(NOV_ELF_WARRIOR2, NpcID.MOURNING_GUARD_MELEE2);
		registerMapping(NOV_ELF_ARCHER, NpcID.MOURNING_GUARD_RANGE);
		registerMapping(NOV_ELF_ARCHER2, NpcID.MOURNING_GUARD_RANGE2);
		registerMapping(ILFEEN,
			NpcID.ROVING_ILFEEN_1OP, NpcID.ROVING_ILFEEN_2OPS, NpcID.SOTE_ILFEEN_VIS,
			NpcID.SOTE_ILFEEN_CUTSCENE
		);
		registerMapping(EUDAV, NpcID.MOURNING_STORES_MAN);
		registerMapping(ORONWEN, NpcID.MOURNING_SEAMSTRESS);
		registerMapping(LLETYA_BANKER_MALE, NpcID.MOURNING_ELF_BANKERM);
		registerMapping(LLETYA_BANKER_FEMALE, NpcID.MOURNING_ELF_BANKERF);
		registerMapping(DALLDAV, NpcID.MOURNING_BOW_SELLER);
		registerMapping(GETHIN, NpcID.MOURNING_ELVEN_COOK);
		registerMapping(GOREU, NpcID.MOURNING_TOWN_ELF_1);
		registerMapping(ARVEL, NpcID.MOURNING_TOWN_ELF_3);
		registerMapping(MAWRTH, NpcID.MOURNING_TOWN_ELF_4, NpcID.SOTE_MAWRTH, NpcID.SOTE_MAWRTH_CUTSCENE);
		// Kelyn; as Lady Kelyn Ithell she is registered with the other clan lords and ladies below
		registerMapping(KELYN, NpcID.MOURNING_TOWN_ELF_5_VIS, NpcID.SOTE_LADY_ITHELL_NORMAL);
		registerMapping(EOIN, NpcID.MOURNING_ELF_KID_1, NpcID.SOTE_EOIN, NpcID.SOTE_EOIN_CUTSCENE);
		registerMapping(IONA, NpcID.MOURNING_ELF_KID_2, NpcID.SOTE_IONA);
		// SOTE_YSGAWYN_CUTSCENE and _CUTSCENE_RUN carry a bow the Nov definition has no part for
		registerMapping(YSGAWYN, NpcID.SOTE_YSGAWYN_VIS);

		// Song of the Elves rebels are the Mourning's End warriors again - the same live models and
		// the same colors, pair for pair - so they wear the same Nov definitions. The one exception
		// is SOTE_ELF_REBEL_CUTSCENE_3, the second warrior's body carrying a bow, which takes the bow
		// warrior's whole kit rather than an outfit no 2005 definition had
		registerMapping(NOV_ELF_ARCHER,
			NpcID.SOTE_ELF_REBEL_CUTSCENE_1, NpcID.SOTE_ELF_REBEL_1, NpcID.SOTE_ELF_REBEL_1_UPASS,
			NpcID.SOTE_UPASS_REBEL_WARRIOR_1, NpcID.SOTE_ELF_REBEL_CUTSCENE_3
		);
		registerMapping(NOV_ELF_ARCHER2,
			NpcID.SOTE_ELF_REBEL_CUTSCENE_2, NpcID.SOTE_ELF_REBEL_2, NpcID.SOTE_ELF_REBEL_2_UPASS,
			NpcID.SOTE_UPASS_REBEL_WARRIOR_2
		);
		registerMapping(NOV_ELF_WARRIOR,
			NpcID.SOTE_ELF_REBEL_CUTSCENE_4, NpcID.SOTE_ELF_REBEL_3, NpcID.SOTE_ELF_REBEL_3_UPASS,
			NpcID.SOTE_UPASS_REBEL_WARRIOR_3
		);
		registerMapping(NOV_ELF_WARRIOR2,
			NpcID.SOTE_ELF_REBEL_CUTSCENE_5, NpcID.SOTE_ELF_REBEL_4, NpcID.SOTE_ELF_REBEL_4_UPASS,
			NpcID.SOTE_UPASS_REBEL_WARRIOR_4
		);

		// Prifddinas citizens and the arena spectators, generated from their live definitions: body,
		// then the lightest hair shade and the clan's clothing. The odd citizen whose live clothing
		// mixes sets keeps its three shades as they are
		registerCitizen(NpcID.PRIF_CITIZEN_AMROD, false, 3738, PrifClothing.HEFIN); // Amrod
		registerCitizen(NpcID.PRIF_CITIZEN_MIRIEL, true, 8394, PrifClothing.HEFIN); // Miriel
		registerCitizen(NpcID.PRIF_CITIZEN_CURUFIN, false, 28, PrifClothing.HEFIN); // Curufin
		registerCitizen(NpcID.PRIF_CITIZEN_ENERDHIL, false, 12, PrifClothing.AMLODD); // Enerdhil
		registerCitizen(NpcID.PRIF_CITIZEN_TATIE, true, 12, PrifClothing.AMLODD); // Tatie
		registerCitizen(NpcID.PRIF_CITIZEN_FINDUILAS, true, 10341, PrifClothing.AMLODD); // Finduilas
		registerCitizen(NpcID.PRIF_CITIZEN_GELMIR, false, 3881, PrifClothing.ITHELL); // Gelmir
		registerCitizen(NpcID.PRIF_CITIZEN_MITHRELLAS, true, -22250, PrifClothing.ITHELL); // Mithrellas
		registerCitizen(NpcID.PRIF_CITIZEN_ERESTOR, false, 7467, PrifClothing.ITHELL); // Erestor
		registerCitizen(NpcID.PRIF_CITIZEN_LINDIR, false, -26428, PrifClothing.ITHELL); // Lindir
		registerCitizen(NpcID.PRIF_CITIZEN_IDRIL, true, 4750, PrifClothing.ITHELL); // Idril
		registerCitizen(NpcID.PRIF_CITIZEN_INGWION, false, 28, PrifClothing.IORWERTH); // Ingwion
		registerCitizen(NpcID.PRIF_CITIZEN_THINGOL, false, 8394, PrifClothing.IORWERTH); // Thingol
		registerCitizen(NpcID.PRIF_CITIZEN_ELENWE, true, 138, PrifClothing.IORWERTH); // Elenwe
		registerCitizen(NpcID.PRIF_CITIZEN_OROPHIN, false, 5667, PrifClothing.IORWERTH); // Orophin
		registerCitizen(NpcID.PRIF_CITIZEN_VAIRE, true, 8394, PrifClothing.TRAHAEARN); // Vaire
		registerCitizen(NpcID.PRIF_CITIZEN_ELLADAN, false, 8394, PrifClothing.TRAHAEARN); // Elladan
		registerCitizen(NpcID.PRIF_CITIZEN_GUILIN, false, 12, PrifClothing.TRAHAEARN); // Guilin
		registerCitizen(NpcID.PRIF_CITIZEN_INGWE, false, 536, PrifClothing.TRAHAEARN); // Ingwe
		registerCitizen(NpcID.PRIF_CITIZEN_CIRDAN, false, -22250, PrifClothing.TRAHAEARN); // Cirdan
		registerCitizen(NpcID.PRIF_CITIZEN_GLORFINDEL, false, 536, PrifClothing.TRAHAEARN); // Glorfindel
		registerCitizen(NpcID.PRIF_CITIZEN_AREDHEL, true, 3881, -14014, -24384, -14023); // Aredhel, Trahaearn
		registerCitizen(NpcID.PRIF_CITIZEN_CELEGORM, false, 3881, PrifClothing.CADARN); // Celegorm
		registerCitizen(NpcID.PRIF_CITIZEN_ANAIRE, true, 3738, PrifClothing.CADARN); // Anaire
		registerCitizen(NpcID.PRIF_CITIZEN_MAEGLIN, false, 5667, PrifClothing.CADARN); // Maeglin
		registerCitizen(NpcID.PRIF_CITIZEN_EDRAHIL, false, 536, PrifClothing.CRWYS); // Edrahil
		registerCitizen(NpcID.PRIF_CITIZEN_FINGON, false, 536, PrifClothing.CRWYS); // Fingon
		registerCitizen(NpcID.PRIF_CITIZEN_SALGANT, false, -26428, PrifClothing.MEILYR); // Salgant
		registerCitizen(NpcID.PRIF_CITIZEN_CELEBRIAN, true, 7467, PrifClothing.MEILYR); // Celebrian
		registerCitizen(NpcID.PRIF_CITIZEN_IMIN, false, 3881, PrifClothing.MEILYR); // Imin
		registerCitizen(NpcID.PRIF_CITIZEN_OROPHER, false, -22250, PrifClothing.MEILYR); // Oropher
		registerCitizen(NpcID.PRIF_CITIZEN_FINGOLFIN, false, 12, PrifClothing.HEFIN); // Fingolfin
		registerCitizen(NpcID.PRIF_CITIZEN_MAHTAN, false, 8394, -27558, -32686, 914); // Mahtan, Ithell
		registerCitizen(NpcID.PRIF_CITIZEN_INDIS, true, 4750, PrifClothing.CADARN); // Indis
		registerCitizen(NpcID.PRIF_CITIZEN_IMINYE, true, 138, PrifClothing.ITHELL); // Iminye
		registerCitizen(NpcID.PRIF_CITIZEN_FEANOR, false, 3738, PrifClothing.AMLODD); // Feanor
		registerCitizen(NpcID.PRIF_CITIZEN_SAEROS, false, 138, PrifClothing.HEFIN); // Saeros
		registerCitizen(NpcID.PRIF_CITIZEN_NELLAS, true, 138, PrifClothing.TRAHAEARN); // Nellas
		registerCitizen(NpcID.PRIF_SPECTATOR1, false, 7467, PrifClothing.CADARN); // Rhyfel
		registerCitizen(NpcID.PRIF_SPECTATOR2, false, 138, PrifClothing.ITHELL); // Gwyl
		registerCitizen(NpcID.PRIF_SPECTATOR3, true, 138, PrifClothing.AMLODD); // Enilly
		registerCitizen(NpcID.PRIF_SPECTATOR4, true, 28, PrifClothing.HEFIN); // Ffoni
		registerCitizen(NpcID.PRIF_SPECTATOR5, false, 7467, PrifClothing.IORWERTH); // Ymladd
		registerCitizen(NpcID.PRIF_SPECTATOR6, false, -26428, PrifClothing.IORWERTH); // Sadwrn
		registerCitizen(NpcID.PRIF_SPECTATOR7, true, 5667, PrifClothing.IORWERTH); // Diol
		registerCitizen(NpcID.PRIF_SPECTATOR8, true, 3881, PrifClothing.IORWERTH); // Ysbeid
		registerCitizen(NpcID.PRIF_SPECTATOR9, false, 138, PrifClothing.IORWERTH); // Cleddyf
		registerCitizen(NpcID.PRIF_SPECTATOR10, true, 28, PrifClothing.IORWERTH); // Saeth
		registerCitizen(NpcID.PRIF_CITIZEN_NIMRODEL, true, 28, PrifClothing.CRWYS); // Nimrodel
		registerCitizen(NpcID.PRIF_CITIZEN_MAEDHROS, false, -22493, PrifClothing.AMLODD); // Maedhros
		registerCitizen(NpcID.PRIF_CITIZEN_FINARFIN, false, -26428, PrifClothing.HEFIN); // Finarfin
		registerCitizen(NpcID.PRIF_CITIZEN_GWINDOR, false, 4750, PrifClothing.HEFIN); // Gwindor
		registerCitizen(NpcID.PRIF_CITIZEN_ELDALOTE, true, 5667, PrifClothing.ITHELL); // Eldalote
		registerCitizen(NpcID.PRIF_CITIZEN_ENELYE, true, 3738, PrifClothing.ITHELL); // Enelye
		registerCitizen(NpcID.PRIF_CITIZEN_NERDANEL, true, 138, PrifClothing.IORWERTH); // Nerdanel
		registerCitizen(NpcID.PRIF_CITIZEN_NIMLOTH, true, 4750, PrifClothing.MEILYR); // Nimloth
		registerCitizen(NpcID.PRIF_CITIZEN_FINDIS, true, 5667, PrifClothing.AMLODD); // Findis
		registerCitizen(NpcID.PRIF_CITIZEN_EARWEN, true, 3881, -24380, -24384, -24389); // Earwen, Trahaearn (slate)
		registerCitizen(NpcID.PRIF_CITIZEN_CARANTHIR, false, 28, PrifClothing.CADARN); // Caranthir
		registerCitizen(NpcID.PRIF_CITIZEN_ENEL, false, 7467, PrifClothing.CRWYS); // Enel
		registerCitizen(NpcID.PRIF_CITIZEN_HENDOR, false, 10341, PrifClothing.HEFIN); // Hendor
		registerCitizen(NpcID.PRIF_CITIZEN_GALATHIL, false, -26428, PrifClothing.HEFIN); // Galathil
		registerCitizen(NpcID.PRIF_CITIZEN_TURGON, false, 10341, PrifClothing.ITHELL); // Turgon
		registerCitizen(NpcID.PRIF_CITIZEN_LENWE, false, 7467, PrifClothing.AMLODD); // Lenwe
		registerCitizen(NpcID.PRIF_CITIZEN_ARANWE, false, 4750, PrifClothing.CADARN); // Aranwe

		// The city's shopkeepers and instructors, the same way - every NPC in the cache wearing the
		// citizens' six live recolors, bar the Gwenith villagers and the Deadman guards. Oswallt's
		// trowel, Elgan's staff and Iwan's mace have no part in any Lletya outfit and are left behind
		registerCitizen(NpcID.PRIF_ARMOURSTORE, false, 3881, PrifClothing.IORWERTH); // Aneirin
		registerCitizen(NpcID.PRIF_WEAPONSTORE, false, 28, PrifClothing.IORWERTH); // Celyn
		registerCitizen(NpcID.PRIF_FARMINGSTORE, true, 12, PrifClothing.CRWYS); // Branwen
		registerCitizen(NpcID.PRIF_GARDENER, false, 4750, PrifClothing.CRWYS); // Oswallt
		registerCitizen(NpcID.PRIF_HERBSTORE, true, 138, PrifClothing.MEILYR); // Llio
		registerCitizen(NpcID.PRIF_ACORN_TRADER_NOT_SPOKEN, false, 536, PrifClothing.CRWYS, NpcID.PRIF_ACORN_TRADER_SPOKEN); // Pennant
		registerCitizen(NpcID.PRIF_AGILITY, true, 3881, PrifClothing.HEFIN); // Ystwyth
		registerCitizen(NpcID.PRIF_BARTENDER, true, 28, PrifClothing.HEFIN); // Eira
		registerCitizen(NpcID.PRIF_RUNESTORE, true, 7467, PrifClothing.AMLODD); // Efa
		registerCitizen(NpcID.GAUNTLET_INSTRUCTOR, false, 5667, PrifClothing.AMLODD); // Bryn
		registerCitizen(NpcID.PRIF_STAFFSTORE, false, 10341, PrifClothing.AMLODD); // Elgan
		registerCitizen(NpcID.PRIF_SEAMSTRESS, true, 3738, PrifClothing.ITHELL); // Nia
		registerCitizen(NpcID.PRIF_ESTATE_AGENT, false, 7467, PrifClothing.ITHELL); // Alwyn
		registerCitizen(NpcID.PRIF_DYESTORE, true, 3738, PrifClothing.ITHELL); // Guinevere
		registerCitizen(NpcID.PRIF_MININGSTORE, false, -22250, PrifClothing.TRAHAEARN); // Gwyn
		registerCitizen(NpcID.PRIF_SINGER1, true, 138, PrifClothing.TRAHAEARN, NpcID.PRIF_SINGER1_LEARNT); // Reese
		registerCitizen(NpcID.ZALCANO_INSTRUCTOR, true, 3881, PrifClothing.TRAHAEARN); // Rhiannon
		registerCitizen(NpcID.PRIF_LLIANN, true, 3738, PrifClothing.ITHELL); // Lliann
		registerCitizen(NpcID.PRIF_SINGER2, true, 5667, PrifClothing.ITHELL, NpcID.PRIF_SINGER2_LEARNT); // Conwenna
		registerCitizen(NpcID.PRIF_GENERALSTORE, false, 7467, PrifClothing.CADARN); // Gwallter
		registerCitizen(NpcID.PRIF_RANGESTORE, true, 8394, PrifClothing.CADARN); // Sian
		registerCitizen(NpcID.PRIF_SAWMILL_OPERATOR, false, 7467, PrifClothing.CRWYS); // Sawmill Operator
		registerCitizen(NpcID.PRIF_MACESTORE, false, 28, PrifClothing.HEFIN); // Iwan
		registerCitizen(NpcID.PRIF_LIBRARIAN, true, 5667, PrifClothing.BANK_GREY); // Lilifang
		registerCitizen(NpcID.PRIF_PRIEST, false, 28, PrifClothing.HEFIN); // Crefydd
		registerCitizen(NpcID.PRIF_FOODSTORE, false, 3738, PrifClothing.MEILYR); // Derwen
		registerCitizen(NpcID.PRIF_SILVER, false, 10341, PrifClothing.MEILYR); // Osian
		registerCitizen(NpcID.PRIF_SPICE, false, 10341, PrifClothing.MEILYR); // Caerwyn
		registerCitizen(NpcID.PRIF_SILK, true, 8394, PrifClothing.MEILYR); // Anwen
		registerCitizen(NpcID.PRIF_GEM, true, 8394, PrifClothing.MEILYR); // Glenda

		// The bankers wear the Lletya bankers' own outfits, in the Prifddinas bank's grey
		registerDressed(LLETYA_BANKER_MALE, 5667, PrifClothing.BANK_GREY,
			NpcID.PRIF_BANKERM1, NpcID.PRIF_BANKERM2, NpcID.PRIF_BANKERM3, NpcID.PRIF_BANKERM4);
		registerDressed(LLETYA_BANKER_FEMALE, 5667, PrifClothing.BANK_GREY,
			NpcID.PRIF_BANKERF1, NpcID.PRIF_BANKERF2);

		// The clan lords and ladies: one outfit each, chosen by hand so every form of a character
		// matches, in the clan's colors and the hair measured off the top of their live model. Lady
		// Kelyn Ithell is Kelyn - live draws both from the same model - so she wears Kelyn's own
		// definition. Lord Baxtorian Cadarn is Baxtorian of the Waterfall Quest and is not here
		registerDressed(EUDAV, 6348, PrifClothing.AMLODD,
			NpcID.SOTE_LORD_AMLODD_VIS, NpcID.SOTE_LORD_AMLODD_UPASS, NpcID.LORD_AMLODD);
		registerDressed(DALLDAV, 156, PrifClothing.CRWYS,
			NpcID.SOTE_LORD_CRWYS_VIS, NpcID.SOTE_LORD_CRWYS_UPASS, NpcID.LORD_CRWYS);
		registerDressed(YSGAWYN, -22248, PrifClothing.IORWERTH, NpcID.LORD_IORWERTH_NEW);
		registerDressed(ARVEL, 103, PrifClothing.MEILYR,
			NpcID.SOTE_LADY_MEILYR_VIS, NpcID.SOTE_LADY_MEILYR_UPASS, NpcID.LADY_MEILYR);
		registerDressed(MAWRTH, 2357, PrifClothing.HEFIN,
			NpcID.SOTE_LADY_HEFIN_VIS, NpcID.SOTE_LADY_HEFIN_UPASS, NpcID.LADY_HEFIN);
		registerDressed(ORONWEN, 92, PrifClothing.TRAHAEARN,
			NpcID.SOTE_LADY_TRAHAEARN_VIS, NpcID.SOTE_LADY_TRAHAEARN_UPASS, NpcID.LADY_TRAHAEARN);
		registerMapping(KELYN, NpcID.SOTE_LADY_ITHELL_VIS, NpcID.SOTE_LADY_ITHELL_UPASS, NpcID.LADY_ITHELL);

		// The city guards, in crystal armor, brown boots, and unique hair colors.
		registerPrifGuard(NpcID.PRIF_GUARD0, false, -22250);
		registerPrifGuard(NpcID.PRIF_GUARD1, false, -26428);
		registerPrifGuard(NpcID.PRIF_GUARD2, false, 12);
		registerPrifGuard(NpcID.PRIF_GUARD3, false, 7467);
		registerPrifGuard(NpcID.PRIF_GUARD4, true, -26428);
		registerPrifGuard(NpcID.PRIF_GUARD5, true, 3881);
		registerPrifGuard(NpcID.PRIF_GUARD6, true, 12);
		registerPrifGuard(NpcID.PRIF_GUARD7, true, -26428);
		registerPrifGuard(NpcID.PRIF_CITY_GUARD, true, -26428);
		registerMapping(NOV_ELF_WARRIOR, NpcID.PRIF_CITY_GUARD_EVIL);
		// The arena's gladiators are the city guard in the arena: the same model, armor and halberd
		registerPrifGuard(NpcID.PRIF_GLADIATOR1, false, -22493);
		registerPrifGuard(NpcID.PRIF_GLADIATOR2, false, -22493);
		// Deliberately left out: DEADMAN_GUARD_PRIF_VIS and DEADMAN_GUARD_PRIF_RANGE_VIS, like the other
		// Deadman guards

		// Elena. The prisoner model live keeps for her captivity in both Plague City and Song of the
		// Elves is SOTE_ELENA_PRISON, which the ELENA_PRISON parent resolves to as well. Deliberately
		// left out: SOTE_ELENA_HURT and SOTE_ELENA_CUTSCENE_HURT, which stand in a modern wounded pose
		// 808 would overwrite
		registerMapping(ELENA_PRISONER, NpcID.SOTE_ELENA_PRISON);
		registerMapping(ELENA_DEFAULT,
			NpcID.ELENA2_VIS, NpcID.ELENAP_VIS, NpcID.SOTE_ELENA, NpcID.SOTE_ELENA_CUTSCENE,
			NpcID.SOTE_ELENA_CUTSCENE_NOMOVE, NpcID.SOTE_ELENA_CUTSCENE_RUN, NpcID.SOTE_RESISTANCE_ELENA,
			NpcID.ELENA_PRIF
		);

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

		// Hellhounds
		// Deliberately not registered:
		// revenant hellhounds, the reanimated hellhound, the scarred hellhounds (DT2, their own mesh),
		// and Cerberus/cerb pet
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

		// Large scorpions
		NAME_MAPPINGS.put("scorpion", SCORPION_DEFAULT);
		registerMapping(SCORPION_DEFAULT,
			NpcID.SCORPION, NpcID.SOS_PEST_SCORPION, NpcID.SOS_PEST_SCORPION2
		);
		NAME_MAPPINGS.put("poison scorpion", SCORPION_DEFAULT);
		registerMapping(SCORPION_DEFAULT, NpcID.POISON_SCORPION);
		NAME_MAPPINGS.put("king scorpion", KING_SCORPION);
		registerMapping(KING_SCORPION, NpcID.KINGSCORPION);
		NAME_MAPPINGS.put("khazard scorpion", KHAZARD_SCORPION);
		registerMapping(KHAZARD_SCORPION,
			NpcID.ARENA_SCORPION, NpcID.ARENA_SCORPION_VIS, NpcID.ARENA_SCORPION_CUTSCENE
		);
		// Named plain "Scorpion", so only its id reaches the jungle variant
		registerMapping(JUNGLE_SCORPION, NpcID.MM_JUNGLE_SCORPION);

		// Small scorpions
		NAME_MAPPINGS.put("pit scorpion", SMALL_SCORPION);
		NAME_MAPPINGS.put("kharid scorpion", SMALL_SCORPION);
		NAME_MAPPINGS.put("grave scorpion", SMALL_SCORPION);
		registerMapping(SMALL_SCORPION,
			NpcID.SMALLSCORPION, NpcID.GRAVE_SCORPION,
			NpcID.QUESTSCORPIONA, NpcID.QUESTSCORPIONB, NpcID.QUESTSCORPIONC,
			NpcID.TINYSCORPION
		);

		// Giant rats. The three Lumbridge gray variants need the ids registered for the recolor.
		// Deliberately not registered: the angry giant rats (Soul's Bane), which were introduced after
		// the cutoff for the cache the plugin is based off of
		// TODO: Update the 2005 cache to December of 2005 to address this?
		NAME_MAPPINGS.put("giant rat", GIANT_RAT_DEFAULT);
		registerMapping(GIANT_RAT_DEFAULT,
			NpcID.GIANTRAT, NpcID.GIANTRAT2, NpcID.GIANTRAT3,
			NpcID.GIANTRAT1, NpcID.GIANTRAT1_2, NpcID.GIANTRAT1_3,
			NpcID.NEWBIEGIANTRAT, NpcID.NEWBIEGIANTRAT2, NpcID.NEWBIEGIANTRAT3,
			NpcID.SOS_FAM_GIANTRAT, NpcID.SOS_FAM_GIANTRAT2, NpcID.SOS_FAM_GIANTRAT3,
			NpcID.TUT2_GIANTRAT, NpcID.RAT_BOSS_GIANT_RAT
		);
		registerMapping(GIANT_RAT_GRAY, NpcID.GIANTRAT_GREY, NpcID.GIANTRAT_GREY2, NpcID.GIANTRAT_GREY3);

		// Underground Pass "Blessed Giant rat" NPCs
		NAME_MAPPINGS.put("blessed giant rat", GIANT_RAT_DEFAULT);
		registerMapping(GIANT_RAT_DEFAULT, NpcID.BLESSED_GIANTRAT, NpcID.BLESSED_GIANTRAT2);

		// Small spiders. The post-2005 NPCs named plain "Spider" swap too, apart from the Hallowed
		// Sepulchre ones in EXCLUDED_IDS. The variants are all named plain "Spider", so only their
		// ids reach them
		NAME_MAPPINGS.put("spider", SPIDER_DEFAULT);
		registerMapping(SPIDER_DEFAULT,
			NpcID.SPIDER, NpcID.SOS_PEST_SPIDER,
			NpcID.SPIDER_NONCOMBAT, NpcID.SPIDER_THAT_SPAWNS_VAMPYRES, NpcID.SPIDER_NOCOMBAT2,
			NpcID.AKD_SPIDER_NOOP
		);
		registerMapping(SPIDER_UPASS, NpcID.UPASS_LITTLE_SPIDER);
		registerMapping(SPIDER_MM, NpcID.MM_WAREHOUSE_SPIDER, NpcID.MM_CUTSCENE_SPIDER);
		registerMapping(SPIDER_RED, NpcID.LABORATORY_SPIDER, NpcID.LABORATORY_SPIDER_AGGRESSIVE);
		NAME_MAPPINGS.put("crypt spider", CRYPT_SPIDER);
		registerMapping(CRYPT_SPIDER, NpcID.BARROWS_SPIDER);

		// Large spiders
		NAME_MAPPINGS.put("giant spider", GIANT_SPIDER);
		registerMapping(GIANT_SPIDER,
			NpcID.GIANTSPIDER1, NpcID.GIANTSPIDER2, NpcID.SOS_PEST_GIANTSPIDER1
		);
		NAME_MAPPINGS.put("shadow spider", SHADOW_SPIDER);
		registerMapping(SHADOW_SPIDER, NpcID.SHADOW_SPIDER);
		NAME_MAPPINGS.put("blessed spider", BLESSED_SPIDER);
		registerMapping(BLESSED_SPIDER, NpcID.BLESSED_SPIDER, NpcID.BLESSED_SPIDER_KALRAG);
		NAME_MAPPINGS.put("deadly red spider", DEADLY_RED_SPIDER);
		registerMapping(DEADLY_RED_SPIDER, NpcID.DEADLY_RED_SPIDER);
		NAME_MAPPINGS.put("jungle spider", JUNGLE_SPIDER);
		registerMapping(JUNGLE_SPIDER, NpcID.JUNGLE_SPIDER, NpcID.TBWCU_JUNGLE_SPIDER);
		registerMapping(JUNGLE_SPIDER_BUSH, NpcID.TBWCU_SPIDER_APPEAR);
		registerMapping(MM_JUNGLE_SPIDER, NpcID.MM_JUNGLE_SPIDER);
		NAME_MAPPINGS.put("giant crypt spider", GIANT_CRYPT_SPIDER);
		registerMapping(GIANT_CRYPT_SPIDER, NpcID.BARROWS_GIANTSPIDER);
		NAME_MAPPINGS.put("ice spider", ICE_SPIDER);
		registerMapping(ICE_SPIDER,
			NpcID.ICE_SPIDER, NpcID.ICE_SPIDER_SHORTRANGE, NpcID.TAPO_ICE_SPIDER_SHORTRANGE
		);
		NAME_MAPPINGS.put("poison spider", POISON_SPIDER);
		registerMapping(POISON_SPIDER,
			NpcID.POISONSPIDER, NpcID.DUNGEONSPIDER, NpcID.WBR_CAVE_POISONSPIDER, NpcID.WBR_POISONSPIDER
		);

		// Bears
		NAME_MAPPINGS.put("grizzly bear", BEAR_BROWN);
		registerMapping(BEAR_BROWN, NpcID.BROWNBEAR, NpcID.WBR_BEAR);
		NAME_MAPPINGS.put("black bear", BEAR_BLACK);
		registerMapping(BEAR_BLACK, NpcID.DARKBEAR);
		registerMapping(GRIZZLY_BEAR, NpcID.REGICIDE_DARKBEAR);
		NAME_MAPPINGS.put("grizzly bear cub", GRIZZLY_BEAR_CUB_1);
		registerMapping(GRIZZLY_BEAR_CUB_1, NpcID.REGICIDE_DARKBEAR_CUB1);
		registerMapping(GRIZZLY_BEAR_CUB_2, NpcID.REGICIDE_DARKBEAR_CUB2);
		NAME_MAPPINGS.put("bear cub", BEAR_CUB_1);
		registerMapping(BEAR_CUB_1, NpcID.BROWNBEAR_CUB_1, NpcID.PRIF_GEE);
		registerMapping(BEAR_CUB_2, NpcID.BROWNBEAR_CUB_2);

		// Kalphites, by id only. A name row would also take the Clan Cup queens and anything else
		// that shares the names
		registerMapping(KALPHITE_WORKER,
			NpcID.KALPHITE_WORKER, NpcID.KALPHITE_WORKER_STRONGHOLDCAVE, NpcID.KALPHITE_WORKER_CHAMBER);
		registerMapping(KALPHITE_SOLDIER, NpcID.KALPHITE_SOLDIER, NpcID.KALPHITE_SOLDIER_STRONGHOLDCAVE);
		registerMapping(KALPHITE_SOLDIER_POH, NpcID.POH_KALPHITE_SOLDIER);
		registerMapping(KALPHITE_GUARDIAN,
			NpcID.KALPHITE_LORD, NpcID.KALPHITE_LORD_STRONGHOLDCAVE, NpcID.KALPHITE_LORD_CHAMBER);
		registerMapping(KALPHITE_LARVA, NpcID.KALPHITE_LARVA);
		registerMapping(KALPHITE_QUEEN_CRAWLING, NpcID.KALPHITE_QUEEN, NpcID.SWAN_KALPHITE_1);
		registerMapping(KALPHITE_QUEEN_AIRBORNE, NpcID.KALPHITE_FLYINGQUEEN, NpcID.SWAN_KALPHITE_2);
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
			.chathead(entry.getChatheadModelIds())
			.chatheadKeyBase(category == RetroNpcCategory.GOBLINS ? RetroAssetBundle.GOBLIN_CHATHEAD_KEY_BASE : 0)
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

		if (byId != null && NAME_OVERRIDDEN_IDS.contains(npcId))
		{
			// The name alone doesn't correctly map the NPC, so the id it was registered under wins
			// outright - including over a name row in another category
			return byId;
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
