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
	 *
	 * <p><b>The tell is the weapon model, not the stance.</b> Falador's bow guards 3272, 3273 and
	 * 3274 hold their bow in the ordinary 808 idle, so a stance test passes them straight through;
	 * only 3270 and 11945 use 4591/4226, the bow-at-rest pair. What they all share is a weapon slot
	 * holding a bow rather than sword 519 and shield 541:
	 *
	 * <ul>
	 * <li>563 on 3272, 3273 and 3274 - the bow every Archer and Ranger in the game carries</li>
	 * <li>16846 on 3270, the same bow the Ardougne archery trainers hold</li>
	 * <li>16846 again on 1112 and 1113, which are not registered</li>
	 * <li>42622 on 11945, the crossbow</li>
	 * </ul>
	 *
	 * <p>{@code FAI_VARROCK_GUARD} is here for a different reason: it stands on 6487 and is three
	 * models with none of the guard torso, sword or shield among them.
	 *
	 * <p>All of these sit in the middle of families that are otherwise registered, which is why
	 * they are named here rather than only left out of {@code registerMapping} - the omission on
	 * its own reads like an oversight and invites being tidied up. Their melee siblings still
	 * swap: 11943 and 11946 carry 23179, which no archer or ranger does.
	 *
	 * <p>The female bow guard 11947 is deliberately <b>not</b> here. Female guards are recent
	 * content with no 2005 counterpart, so she is replaced by the male archer rather than left
	 * alone - see {@link #BOW_GUARD_LIVE_PARTS}.
	 */
	private static final Set<Integer> EXCLUDED_IDS = Set.of(
		NpcID.FAI_FALADOR_GUARD2, NpcID.FAI_FALADOR_GUARD2_F,
		NpcID.FAI_FALADOR_GUARD4, NpcID.FAI_FALADOR_GUARD5, NpcID.FAI_FALADOR_GUARD6,
		NpcID.FAI_VARROCK_GUARD
	);

	/**
	 * Categories that resolve from the registered id list only, never from a name.
	 *
	 * <p>Matching by name is what lets one 2005 row cover every modern variant of an NPC, and
	 * for a monster it holds up: everything called "Lesser demon" is one. "Guard" is a job,
	 * not a costume. 184 NPCs carry that exact name and only about thirty are the town guard
	 * this kit belongs to - the rest are troll, dwarf, elf, goblin and cave goblin guards,
	 * archers, and the Deadman ranged variants, all of which were being handed a 2005 human
	 * swordsman.
	 *
	 * <p>Suppressed at lookup rather than by dropping the name row, because
	 * {@link #applyCacheRecolors} walks {@code NAME_MAPPINGS} to graft the opcode 40 pairs
	 * onto the archetypes. Removing the row would quietly cost the guard its 2005 colors.
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
	//
	// So almost nothing here needs intercepting - those sequences are the retro look already, and
	// an earlier revision rewrote every melee and firebreath attack into a head attack by listing
	// them. Only the post-2005 ranged attack has no retro counterpart.
	public static final Set<Integer> DRAGON_MODERN_ATTACKS = Set.of(
		AnimationID.DRAGON_RANGED_ATTACKS
	);
	public static final Set<Integer> DRAGON_MODERN_DEFENDS = Set.of();
	public static final Set<Integer> DRAGON_MODERN_DEATHS = Set.of();

	public static final Set<Integer> GOBLIN_MODERN_ATTACKS = Set.of(
		AnimationID.SLICE_SURFACE_GOBLIN_SQUAT_UNARMED_ATTACK, AnimationID.SLICE_SURFACE_GOBLIN_ARMED_ATTACK,
			AnimationID.SLICE_SURFACE_GOBLIN_SQUAT_ATTACK_SPEAR, AnimationID.SLICE_SURFACE_GOBLIN_SERGENT_ATTACK,
			AnimationID.GOBLIN_ATTACK_UNARMED, AnimationID.GOBLIN_ATTACK_ARMED
	);
	public static final Set<Integer> GOBLIN_MODERN_DEFENDS = Set.of(
		AnimationID.SLICE_SURFACE_GOBLIN_DEFEND, AnimationID.GOBLIN_BLOCK,
			AnimationID.SLICE_SURFACE_GOBLIN_DEFEND_SPEAR, AnimationID.SLICE_SURFACE_GOBLIN_SERGENT_DEFEND
	);
	public static final Set<Integer> GOBLIN_MODERN_DEATHS = Set.of(
		AnimationID.SLICE_SURFACE_GOBLIN_DEATH, AnimationID.SLICE_SURFACE_GOBLIN_DEATH_SPEAR,
			AnimationID.SLICE_ARROW_DEATH, AnimationID.GOBLIN_DEATH, AnimationID.SLICE_SURFACE_GOBLIN_SERGENT_DEATH
	);

	public static final Set<Integer> GUARD_MODERN_ATTACKS = Set.of(
		AnimationID.HUMAN_UNARMEDPUNCH, AnimationID.HUMAN_UNARMEDKICK
	);
	/**
	 * A guard fights with a sword and shield, so it blocks with {@code HUMAN_SHIELD_DEFENCE}
	 * rather than the unarmed block. That used to be rewritten onto the unarmed block 424,
	 * because 1156 would not decode from the 2005 cache - a block with no shield raise. The cause
	 * was a bzip2 decompressor that stopped after one block and truncated {@code seq.dat}, not
	 * anything about the sequence, so 1156 now ships as its own 2005 clip and passes straight
	 * through with no interception, exactly like the 386 sword stab.
	 *
	 * <p>What is left is the unarmed block on its own, which equals the guard's own
	 * {@code defendAnimationId}, so {@code onAnimationChanged} short-circuits before this set is
	 * ever consulted. It is kept rather than emptied so the archetype still answers
	 * {@code isDefendAnimation(424)} the way every other category does.
	 */
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
		AnimationID.SKELETON_UPDATE_GIANT_VARY3_ATTACK, AnimationID.SKELETON_UPDATE_CHAMPION_ATTACK
	);
	public static final Set<Integer> SKELETON_MODERN_DEFENDS = Set.of(
		AnimationID.SKELETON_BLOCK,
		AnimationID.HUMAN_UNARMEDBLOCK, AnimationID.HUMAN_UNARMED_DEF, AnimationID.HUMAN_SHIELD_DEFENCE,
		AnimationID.SKELETON_UPDATE_DEFEND, AnimationID.SKELETON_UPDATE_DEFEND_TRANSPARENT,
		AnimationID.SKELETON_UPDATE_GIANT_VARY3_DEFEND
	);
	public static final Set<Integer> SKELETON_MODERN_DEATHS = Set.of(
		 AnimationID.SKELETON_DEATH, AnimationID.HUMAN_DEATH,
		AnimationID.SKELETON_UPDATE_DEATH, AnimationID.SKELETON_UPDATE_DEATH_TRANSPARENT,
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

	public static final Set<Integer> CHICKEN_MODERN_ATTACKS = Set.of(
		AnimationID.LORE_CHICKEN_ATTACK, AnimationID.CHICKEN_ATTACK
	);
	public static final Set<Integer> CHICKEN_MODERN_DEFENDS = Set.of(
		AnimationID.LORE_CHICKEN_DEFEND, AnimationID.CHICKEN_BLOCK
	);
	public static final Set<Integer> CHICKEN_MODERN_DEATHS = Set.of(
		AnimationID.LORE_CHICKEN_DEATH, AnimationID.CHICKEN_DEATH
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
	//
	// The guard archetype stays inert: its mapping resolves but processNpc never activates it. It
	// is kept, along with its JSON entries, as staged data.
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

	// Known gap: black demons share mesh 2942 with greater demons and differ only by the 2005
	// opcode 40 pairs (918 -> 4, 929 -> 4, 0 -> 931). Those pairs are in the generated JSON, but a
	// static archetype takes precedence over the JSON row, so they never reach createMappingData
	// and a black demon currently renders in greater demon colors. Lesser and greater demons are
	// unaffected - they carry no recolor data in 2005, being the base color of their own meshes.
	// Fixing it means letting an archetype inherit the JSON row's recolors, which is a change to
	// how load() merges the two rather than a per-category tweak.
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
	 *
	 * <p>Six of the nine survive in the live cache byte for byte. Head 294, arms 151 and hands 254
	 * do not - their ids were reused - which is what "I couldn't find the correct head/arms" meant,
	 * and why the cache-backed path could never assemble a whole guard.
	 */
	private static final int[] GUARD_PARTS = {233, 246, 294, 151, 176, 254, 185, 519, 541};

	/**
	 * The same kit with the battleaxe in place of the sword, for Falador's axe guard.
	 *
	 * <p>2005 model 550 is byte-for-byte the mesh the live cache still holds at that id, so
	 * the axe is authentic rather than approximated. The shield (541) stays: the live NPC
	 * carries both, unlike the archers, who carry a bow and no shield at all.
	 */
	private static final int[] GUARD_AXE_PARTS = {233, 246, 294, 151, 176, 254, 185, 550, 541};

	/**
	 * The male bow guard, taken from the live cache rather than the bundle.
	 *
	 * <p>This is NPC 3272's own model list. The archer guard is 2006 content and has not changed
	 * since, so the live meshes are the period-correct ones - there is no 2005 archer to restore,
	 * because head 9458 and arms 9450 are not in that cache at all.
	 *
	 * <p>Live parts have to be drawn by the cache-backed path, not the bundle. They are bound to
	 * live framemap 0, a 218-group rig, while every bundled guard part is bound into [0..34] and
	 * animated by 2005 clips on rig 100083. Skinning these against those clips would drive the
	 * right geometry off the wrong joints, which is the whole reason guards are injection-only in
	 * the first place. Letting the client animate her instead sidesteps it entirely.
	 */
	private static final int[] BOW_GUARD_LIVE_PARTS =
		{233, 250, 9458, 9450, 176, 28285, 185, 563, 215};

	/**
	 * NPCs in an injection-only category that are nonetheless built from the live cache.
	 *
	 * <p>{@link #requiresInjectedGeometry} is a category-wide rule, and it holds for guards because
	 * their 2005 head, arms and hands were reused for other things - a cache-backed guard would
	 * assemble unrelated geometry. It does not hold for the one guard whose parts are all genuine
	 * live meshes of exactly the NPC being drawn.
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

		// 3. Hand the static archetypes the recolor pairs from their JSON rows
		applyCacheRecolors(byName);

		// 4. Derive the equipment variants, after the recolors so they inherit them
		applyWeaponVariants();
	}

	/**
	 * Re-points the guards who carry something other than the sword at a kit that shows it.
	 *
	 * <p>Runs last, and derives from whatever is registered rather than from the archetype
	 * constant, because {@link #applyCacheRecolors} has by then replaced that instance with a
	 * recolored copy. Deriving from the constant instead would hand the axe guard the kit in
	 * a townsperson's colors.
	 */
	private static void applyWeaponVariants()
	{
		// GUARD3_F is the female of the axe guard. Female guards are recent content with no 2005
		// counterpart of their own, so they take the male kit - which for this one means the axe,
		// the same as the NPC it is a variant of.
		for (int axeGuard : new int[]{NpcID.FAI_FALADOR_GUARD3, NpcID.FAI_FALADOR_GUARD3_F})
		{
			RetroNpcData guard = ID_MAPPINGS.get(axeGuard);
			if (guard != null)
			{
				ID_MAPPINGS.put(axeGuard, guard.withModelIds(GUARD_AXE_PARTS));
			}
		}

		// The female bow guard becomes the male one. Derived from the archetype rather than
		// from the registered copy, deliberately: the 2005 opcode 40 pairs belong to the 2005
		// meshes, and these are live ones that already carry the colors they should.
		ID_MAPPINGS.put(NpcID.FAI_FALADOR_GUARD4_F,
			GUARD_DEFAULT.withModelIds(BOW_GUARD_LIVE_PARTS));
	}

	/**
	 * Grafts 2005 recolor pairs onto the static archetypes.
	 *
	 * <p>An archetype wins over the generated JSON row for a name, which is what keeps hand-checked
	 * combat animations and model ids in place. But the row is the only source of the opcode 40
	 * pairs, and the archetypes are constructed before any cache is read, so the two are recombined
	 * here instead. Without this a black demon renders in greater demon colors - both are mesh
	 * 2942, and the pairs are the only thing that separates them.
	 *
	 * <p>Scoped by {@link #categoryUsesRecolors}, for the same reason {@code createMappingData} is:
	 * guards, goblins and the restless ghost all carry opcode 40 data too, and the generator keeps
	 * only the lowest-id row per name, so forwarding wholesale would repaint a whole category in one
	 * arbitrary variant's colors.
	 */
	private static void applyCacheRecolors(Map<String, RetroNpcMappingEntry> byName)
	{
		for (Map.Entry<String, RetroNpcData> mapping : new ArrayList<>(NAME_MAPPINGS.entrySet()))
		{
			RetroNpcData data = mapping.getValue();
			if (data == null || !categoryUsesRecolors(data.getCategory()) || data.hasRecolors())
			{
				continue;
			}

			RetroNpcMappingEntry entry = byName.get(mapping.getKey());
			if (entry == null || entry.getOriginalColors() == null || entry.getReplacementColors() == null)
			{
				continue;
			}

			RetroNpcData recolored = data.withRecolors(entry.getOriginalColors(), entry.getReplacementColors());
			NAME_MAPPINGS.put(mapping.getKey(), recolored);

			// Both maps hold the same instance, so every id registered against the archetype has to
			// be pointed at the replacement too
			for (Map.Entry<Integer, RetroNpcData> idMapping : ID_MAPPINGS.entrySet())
			{
				if (idMapping.getValue() == data)
				{
					idMapping.setValue(recolored);
				}
			}
		}
	}

	/**
	 * Whether a category's retro mesh needs the 2005 recolor pairs to look right.
	 *
	 * <p>These meshes carry no usable color of their own - the dragons are a greyscale ramp, and
	 * black and greater demons are the same mesh - so recoloring is structural rather than
	 * cosmetic. Every other category is left alone on purpose.
	 */
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
			|| category == RetroNpcCategory.GUARDS;
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

	private static boolean categoryUsesRecolors(RetroNpcCategory category)
	{
		return category == RetroNpcCategory.ADULT_DRAGONS
			|| category == RetroNpcCategory.BABY_DRAGONS
			|| category == RetroNpcCategory.LESSER_DEMONS
			|| category == RetroNpcCategory.GREATER_DEMONS
			|| category == RetroNpcCategory.BLACK_DEMONS
			// Fire, ice and moss giants are the same body mesh as the hill giant, told apart only by
			// their 2005 opcode 40 pairs. Hill giants and the cyclops carry none and are left alone.
			|| category == RetroNpcCategory.FIRE_GIANTS
			|| category == RetroNpcCategory.ICE_GIANTS
			|| category == RetroNpcCategory.MOSS_GIANTS
			// A guard's parts are generic 2005 human kit shared with everything else that wears it,
			// so the opcode 40 pairs are what make the kit a guard's colors rather than a
			// townsperson's. The pairs come from the definition the parts come from.
			|| category == RetroNpcCategory.GUARDS;
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
				NpcID.SKELETON_ARMED, NpcID.SKELETON_ARMED2, NpcID.SKELETON_UNAGRESSIVE2,
				NpcID.SKELETON_UNAGRESSIVE3
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
			// The base rows of each family. Anything still literally named "Guard" already resolves
			// by name, so these are belt and braces - but the list below enumerates the _F and
			// _VARIANT derivatives of exactly these NPCs and simply never included them.
			NpcID.GUARD1, NpcID.ARDOUGNE_GUARD,
			// Only the melee half of the Falador family. GUARD2, GUARD4, GUARD5 and GUARD6 carry
			// a bow or crossbow - see EXCLUDED_IDS. GUARD4_F is listed below: she is the female
			// of GUARD4, replaced by the male archer rather than excluded.
			NpcID.FAI_FALADOR_GUARD1, NpcID.FAI_FALADOR_GUARD3,
			NpcID.BIM_FAI_VARROCK_GUARD02, NpcID.BIM_FAI_VARROCK_GUARD02_F, NpcID.BIM_FAI_VARROCK_GUARD02_VARIANT02,
			NpcID.FAI_VARROCK_GUARD02, NpcID.FAI_VARROCK_GUARD02_VARIANT01, NpcID.FAI_VARROCK_GUARD02_VARIANT02,
			NpcID.FAI_VARROCK_GUARD02_F, NpcID.FAI_VARROCK_GUARD02_F_VARIANT01, NpcID.FAI_VARROCK_GUARD02_F_VARIANT02,
			NpcID.FAI_VARROCK_GUARD_CAPTAIN02,
			NpcID.GUARD1_VARIANT01, NpcID.GUARD1_F, NpcID.GUARD1_F_VARIANT01,
			NpcID.ARDOUGNE_GUARD_VARIANT01, NpcID.ARDOUGNE_GUARD_F, NpcID.ARDOUGNE_GUARD_F_VARIANT01,
			NpcID.FAI_FALADOR_GUARD1_VARIANT01, NpcID.FAI_FALADOR_GUARD1_F, NpcID.FAI_FALADOR_GUARD1_VARIANT02,
			NpcID.FAI_FALADOR_GUARD3_F, NpcID.FAI_FALADOR_GUARD4_F,
			// The Ratcatchers mansion guards wear the town guard kit exactly - 233, 246, 294,
			// 176, 185, 519, 541 with the live arms and hands - and stand on 808. They were only
			// ever reached by name, so they need listing now that the name no longer resolves.
			NpcID.RATCATCHER_STATICGUARD, NpcID.RATCATCHER_CHIEFGUARD,
			NpcID.RATCATCHER_GUARD_LEFT_FRONT, NpcID.RATCATCHER_GUARD_LEFT_MID,
			NpcID.RATCATCHER_GUARD_LEFT_BACK, NpcID.RATCATCHER_GUARD_LEFT_FULLBACK,
			NpcID.RATCATCHER_GUARD_RIGHT_FRONT, NpcID.RATCATCHER_GUARD_RIGHT_MID,
			NpcID.RATCATCHER_GUARD_RIGHT_BACK, NpcID.RATCATCHER_GUARD_RIGHT_FULLBACK,
			NpcID.RATCATCHER_GUARD_LEFT_INSIDE, NpcID.RATCATCHER_GUARD_RIGHT_INSIDE
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
	}

	private static RetroNpcData createMappingData(RetroNpcMappingEntry entry)
	{
		RetroNpcCategory category = entry.getCategory();
		int attackAnim = -1;
		int defendAnim = -1;
		int deathAnim = -1;
		int[] models = entry.getModelIds();
		int stanceAnim = entry.getIdleAnim();
		int walkAnim = entry.getWalkAnim();
		// Seeded from the 2005 definition; categories below correct it where the retro and
		// modern meshes are different sizes
		int scaleXZ = entry.getScaleXZ();
		int scaleY = entry.getScaleY();

		// Deliberately NOT seeded from the entry. Plenty of 2005 definitions carry opcode-40
		// recolors - goblins and guards among them - and buildEntries collapses rows by name with
		// the lowest def id winning, so forwarding them wholesale would repaint a live category
		// with one arbitrary variant's colors. Only a branch that needs them opts in.
		//
		// Scoped rather than seeded from every entry: plenty of 2005 definitions carry opcode 40
		// recolors - goblins and guards among them - and buildEntries keeps only the lowest-id row
		// per name, so forwarding wholesale would repaint a live category with one arbitrary
		// variant's colors. See categoryUsesRecolors for why these categories are the exception.
		short[] recolorFind = categoryUsesRecolors(category) ? entry.getOriginalColors() : null;
		short[] recolorReplace = categoryUsesRecolors(category) ? entry.getReplacementColors() : null;

		Set<Integer> modernAttacks = Collections.emptySet();
		Set<Integer> modernDefends = Collections.emptySet();
		Set<Integer> modernDeaths = Collections.emptySet();

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
			modernAttacks = DRAGON_MODERN_ATTACKS;
			modernDefends = DRAGON_MODERN_DEFENDS;
			modernDeaths = DRAGON_MODERN_DEATHS;

		}
		else if (category == RetroNpcCategory.BABY_DRAGONS)
		{
			// Modern baby dragons already stand and walk on the 2005 sequences - every live
			// definition has standingAnim 27 and walkingAnim 21 - so these only backstop a -1
			// in the JSON.
			stanceAnim = stanceAnim != -1 ? stanceAnim : AnimationID.BDRAG_READY;
			walkAnim = walkAnim != -1 ? walkAnim : AnimationID.BDRAG_WALK;

			// BDRAG_ATTACK/BLOCK/DEATH (25/26/28) survive too, and the bundle carries them, but
			// combat sequences are not part of an NPC definition, so the cache cannot say what a
			// modern baby dragon plays in a fight. Leaving the slots at -1 short-circuits
			// isAttack/Defend/DeathAnimation. The failure mode of guessing is on record in the
			// ADULT_DRAGONS branch, where listing retro-native sequences as things to intercept
			// rewrote every attack into a head butt.
		}
		else if (category == RetroNpcCategory.GOBLINS)
		{
			stanceAnim = stanceAnim != -1 ? stanceAnim : 311;
			walkAnim = walkAnim != -1 ? walkAnim : 308;
			attackAnim = 309;
			defendAnim = 312;
			deathAnim = 313;
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
			// These are really tiny compared to modern chickens. The 2005 definition asked for no
			// resize at all (128), and the modern composition's own scale is 80, so neither
			// source gets us there - this is a hand-matched value.
			scaleXZ = 204;
			scaleY = 204;
			models = new int[]{2849};
			stanceAnim = 54;
			walkAnim = 53;
			attackAnim = 55;
			defendAnim = 56;
			deathAnim = 57;
			modernAttacks = CHICKEN_MODERN_ATTACKS;
			modernDefends = CHICKEN_MODERN_DEFENDS;
			modernDeaths = CHICKEN_MODERN_DEATHS;
		}
		else if (category == RetroNpcCategory.HILL_GIANTS
			|| category == RetroNpcCategory.FIRE_GIANTS
			|| category == RetroNpcCategory.ICE_GIANTS
			|| category == RetroNpcCategory.MOSS_GIANTS
			|| category == RetroNpcCategory.CYCLOPS)
		{
			// The generated row already carries this family's parts. Hill giants are the exception:
			// their 2005 head 2862 is gone from the live cache, so the row's parts would leave the
			// cache-backed path loading unrelated geometry - it takes the Jogre head instead.
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
			// Animations no longer exist in the official game cache, so these are disabled
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
			.scaleXZ(scaleXZ)
			.scaleY(scaleY)
			.recolors(recolorFind, recolorReplace)
			.modernAttackAnims(modernAttacks)
			.modernDefendAnims(modernDefends)
			.modernDeathAnims(modernDeaths)
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
