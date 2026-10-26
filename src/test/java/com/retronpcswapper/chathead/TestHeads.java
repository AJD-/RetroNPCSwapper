package com.retronpcswapper.chathead;

import com.retronpcswapper.inject.RetroAssetBundle;
import com.retronpcswapper.inject.RetroLighter;
import com.retronpcswapper.inject.RetroMesh;
import net.runelite.api.ModelData;

/** Heads for the chathead tests, built and lit the way RetroModelCache builds a real one. */
final class TestHeads
{
	private TestHeads()
	{
	}

	/** One upright triangle, 128 wide and 128 tall, centered on the origin and facing the camera. */
	static RetroHead triangle()
	{
		RetroMesh mesh = new RetroMesh(1, 0,
			new float[]{-64f, 64f, 0f}, new float[]{64f, 64f, -64f}, new float[]{0f, 0f, 0f},
			new int[]{0}, new int[]{1}, new int[]{2},
			new short[]{(short) 0x3A05}, null, null, null, null,
			null, null, null, null,
			null);
		int[] lit1 = new int[1];
		int[] lit2 = new int[1];
		int[] lit3 = new int[1];
		RetroLighter.light(
			mesh.getVerticesCount(), mesh.getVerticesX(), mesh.getVerticesY(), mesh.getVerticesZ(),
			mesh.getFaceCount(), mesh.getFaceIndices1(), mesh.getFaceIndices2(), mesh.getFaceIndices3(),
			mesh.getFaceColors(), mesh.getFaceRenderTypes(), mesh.getFaceTextures(),
			ModelData.DEFAULT_AMBIENT, ModelData.DEFAULT_CONTRAST,
			ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z,
			lit1, lit2, lit3);
		return new RetroHead(mesh, 0, RetroAssetBundle.empty(), lit1, lit2, lit3);
	}
}
