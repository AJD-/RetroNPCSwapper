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

import lombok.Data;

/** One 2005 object (scenery) definition, as decoded from loc.dat. */
@Data
public class RetroLocDefinition
{
	private int id;
	private String name;
	private String description;

	/** The models, from opcode 1 (typed) or opcode 5 (untyped, for placement type 10 alone). */
	private int[] models;

	/** The placement type each opcode 1 model is for, or null when the models came from opcode 5. */
	private int[] modelTypes;

	private int sizeX = 1;
	private int sizeY = 1;
	private int animation = -1;

	/** Opcodes 29 and 39, as stored: offsets the client adds to its base lighting. */
	private int ambient;
	private int contrast;

	/** Model resize in 1/128ths (opcodes 65-67). */
	private int scaleX = 128;
	private int scaleY = 128;
	private int scaleZ = 128;

	/** Opcodes 70-72. */
	private int offsetX;
	private int offsetY;
	private int offsetZ;

	/** Opcode 21: whether the client bends the model to the ground under it. */
	private boolean contouredGround;

	/** Opcode 62: whether the model is mirrored. */
	private boolean mirrored;

	private String[] actions = new String[5];
	private short[] originalColors;
	private short[] replacementColors;

	/** Opcode 77: the varbit or varp choosing among {@link #children}, -1 when unused. */
	private int varbit = -1;
	private int varp = -1;
	private int[] children;
}
