package silicon.px.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import silicon.px.mesh.Payload;

final class QuadEmitter {
	private QuadEmitter() {
	}

	/**
	 * Every vertex gets the sprite centre as UV and the shader does the tiling, see terrain.fsh.
	 */
	static void merged(VertexConsumer out, Appearance look, float[] corners, int[] colors, int[] lights) {
		TextureAtlasSprite sprite = look.sprite();
		float cu = (sprite.getU0() + sprite.getU1()) * 0.5f;
		float cv = (sprite.getV0() + sprite.getV1()) * 0.5f;

		int p = Payload.encode(look.face(), look.transform(), look.log2W(), look.log2H());
		float nx = Payload.normalFloat(Payload.x(p));
		float ny = Payload.normalFloat(Payload.y(p));
		float nz = Payload.normalFloat(Payload.z(p));

		for (int i = 0; i < 4; i++)
			out.addVertex(corners[i * 3], corners[i * 3 + 1], corners[i * 3 + 2], colors[i], cu, cv, OverlayTexture.NO_OVERLAY, lights[i], nx, ny, nz);
	}
}
