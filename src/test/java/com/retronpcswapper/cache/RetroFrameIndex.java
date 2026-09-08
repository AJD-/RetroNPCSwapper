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
package com.retronpcswapper.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The whole of 2005 index 2, decoded: every frame group, plus the frame id to file id map the
 * sequences need. A 2005 sequence names frames by a flat id and says nothing about which file holds
 * them, so this map is the only way to resolve one.
 */
public class RetroFrameIndex
{
	private final Map<Integer, RetroFrameGroup> groups;
	private final Map<Integer, Integer> frameToFile;
	private final Map<Integer, RetroFrameDefinition> framesById;

	RetroFrameIndex(Map<Integer, RetroFrameGroup> groups, Map<Integer, Integer> frameToFile)
	{
		this.groups = groups;
		this.frameToFile = frameToFile;

		this.framesById = new HashMap<>();
		for (RetroFrameGroup group : groups.values())
		{
			for (RetroFrameDefinition frame : group.getFrames())
			{
				framesById.put(frame.getFrameId(), frame);
			}
		}
	}

	public Collection<RetroFrameGroup> getGroups()
	{
		return groups.values();
	}

	public RetroFrameGroup getGroup(int fileId)
	{
		return groups.get(fileId);
	}

	public int getFrameCount()
	{
		return framesById.size();
	}

	public RetroFrameDefinition getFrame(int frameId)
	{
		return framesById.get(frameId);
	}

	/** Which index 2 file holds a frame, or -1. The file is also what owns the rig. */
	public int getFileForFrame(int frameId)
	{
		Integer fileId = frameToFile.get(frameId);
		return fileId == null ? -1 : fileId;
	}

	/** The rig a frame is authored against, or null. */
	public RetroFramemapDefinition getFramemapForFrame(int frameId)
	{
		RetroFrameGroup group = groups.get(getFileForFrame(frameId));
		return group == null ? null : group.getFramemap();
	}
}
