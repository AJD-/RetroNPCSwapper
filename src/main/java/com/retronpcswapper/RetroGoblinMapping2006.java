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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The 2006 goblin rows. Every row is {@link RetroNpcCategory#GOBLINS}.
 */
final class RetroGoblinMapping2006
{
	private static final Map<Integer, RetroNpcData> ID_MAPPINGS = new HashMap<>();
	private static final Map<String, RetroNpcData> NAME_MAPPINGS = new HashMap<>();

	static
	{
	}

	private RetroGoblinMapping2006()
	{
	}

	/**
	 * @param nameLower lower-cased trimmed NPC name, or null
	 */
	static RetroNpcData get(int npcId, String nameLower)
	{
		RetroNpcData byId = ID_MAPPINGS.get(npcId);
		if (byId != null)
		{
			return byId;
		}
		return nameLower == null ? null : NAME_MAPPINGS.get(nameLower);
	}

	/**
	 * Every live model id a row references.
	 */
	static Set<Integer> modelIds()
	{
		Set<Integer> ids = new HashSet<>();
		for (RetroNpcData data : ID_MAPPINGS.values())
		{
			addModelIds(ids, data);
		}
		for (RetroNpcData data : NAME_MAPPINGS.values())
		{
			addModelIds(ids, data);
		}
		return ids;
	}

	private static void addModelIds(Set<Integer> ids, RetroNpcData data)
	{
		for (int modelId : data.getRetroModelIds())
		{
			ids.add(modelId);
		}
	}
}
