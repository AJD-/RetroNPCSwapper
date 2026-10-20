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

/**
 * The clothing a Prifddinas elf wears on live: three shades of one color, lightest first, which
 * live writes into the citizens' three clothing recolor slots. The clan is the clothing, so each
 * clan is one set, copied from the live definitions ({@code ./gradlew dumpNpcDefinitions}).
 * <p>
 * Colors are packed HSL as the client stores them - six bits of hue, three of saturation, seven
 * of lightness - in signed shorts, so every hue from 32 up reads as negative.
 */
enum PrifClothing
{
	/** Navy */
	AMLODD(-27116, -26864, -26740),
	/** Olive */
	CADARN(13854, 13978, 14102),
	/** Yellow */
	CRWYS(9127, 9125, 9121),
	/** Crimson */
	HEFIN(796, 920, 918),
	/** Near-black */
	IORWERTH(-32750, -32626, -32630),
	/** White */
	ITHELL(-27558, -32686, -32700),
	/** Teal */
	MEILYR(32185, 32307, 32429),
	/** Pink */
	TRAHAEARN(-14014, -14019, -14023),
	/** Gray */
	BANK_GREY(-22473, -22477, -22481);

	final short cloth;
	final short clothShade;
	final short clothDark;

	PrifClothing(int cloth, int clothShade, int clothDark)
	{
		this.cloth = (short) cloth;
		this.clothShade = (short) clothShade;
		this.clothDark = (short) clothDark;
	}
}
