package silicon.px.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Everything that decides what a face looks like. Faces only merge when these are equal.
 * target is where the face gets written (a vanilla layer or a Sodium buffer), compared by identity.
 * shade and material are only used by the Sodium path and stay 0 for vanilla.
 */
record Appearance(Object target, TextureAtlasSprite sprite, int face, int transform, int log2W, int log2H,
		int color, int light, int shade, int material) {
}
