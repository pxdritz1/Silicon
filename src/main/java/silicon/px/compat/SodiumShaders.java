package silicon.px.compat;

import silicon.px.Silicon;

/**
 * Patches Sodium's terrain shader source as Sodium loads it, so merged quads repeat their texture once per block.
 * If Sodium's shader ever changes so that an anchor below is missing, nothing is patched and Silicon stays off
 * under Sodium instead of drawing stretched textures.
 */
public final class SodiumShaders {
	private static final String VERTEX = "blocks/block_layer_opaque.vsh";
	private static final String FRAGMENT = "blocks/block_layer_opaque.fsh";

	private static volatile boolean vertexDone, fragmentDone;

	private SodiumShaders() {
	}

	public static boolean patched() {
		return vertexDone && fragmentDone;
	}

	public static String patch(String path, String source) {
		if (path.endsWith(VERTEX)) {
			String out = vertex(source);
			vertexDone = out != null;
			return out != null ? out : source;
		}
		if (path.endsWith(FRAGMENT)) {
			String out = fragment(source);
			fragmentDone = out != null;
			return out != null ? out : source;
		}
		return source;
	}

	private static String vertex(String src) {
		String decl = "flat out uint v_Material;";
		String assign = "v_Material = _material_params;";
		if (!src.contains(decl) || !src.contains(assign) || !src.contains("vec3 position = ")) {
			Silicon.LOGGER.warn("Sodium's terrain vertex shader looks different than expected, Silicon stays off with Sodium");
			return null;
		}
		return src
				.replace(decl, decl + """

						out vec3 v_SiliconLocal;
						out vec3 v_SiliconView;
						flat out vec2 v_SiliconUv;
						""")
				.replace(assign, assign + """

						    v_SiliconLocal = _vert_position;
						    v_SiliconView = position;
						    v_SiliconUv = _vert_tex_diffuse_coord;
						""");
	}

	private static String fragment(String src) {
		String decl = "flat in uint v_Material;";
		String main = "void main() {";
		String sample = "vec4 color = u_UseRGSS ? sampleRGSS(u_BlockTex, v_TexCoord, u_TexelSize) : sampleNearest(u_BlockTex, v_TexCoord, u_TexelSize);";
		if (!src.contains(decl) || !src.contains(main) || !src.contains(sample)) {
			Silicon.LOGGER.warn("Sodium's terrain fragment shader looks different than expected, Silicon stays off with Sodium");
			return null;
		}
		String vanillaSample = sample.substring("vec4 color = ".length(), sample.length() - 1);
		return src
				.replace(decl, decl + """

						in vec3 v_SiliconLocal;
						in vec3 v_SiliconView;
						flat in vec2 v_SiliconUv;
						""")
				.replace(main, FUNCTIONS + main)
				.replace(sample, "uint siliconCode = (v_Material >> 3u) & 31u;\n    vec4 color = siliconCode != 0u ? siliconSample(siliconCode) : "
						+ vanillaSample + ";");
	}

	// Mirrors the vanilla terrain.fsh in this mod. Sodium has no free per-vertex normal, so the face comes from
	// the triangle's own plane (screen space derivatives of its position) and which side the camera is on.
	private static final String FUNCTIONS = """
			vec2 siliconStandardUv(int face, vec3 p) {
			    vec3 f = fract(p);
			    if (face == 0) return vec2(f.x, 1.0 - f.z);
			    if (face == 1) return f.xz;
			    if (face == 2) return vec2(1.0 - f.x, 1.0 - f.y);
			    if (face == 3) return vec2(f.x, 1.0 - f.y);
			    if (face == 4) return vec2(f.z, 1.0 - f.y);
			    return vec2(1.0 - f.z, 1.0 - f.y);
			}

			vec2 siliconPlane(int face, vec3 p) {
			    if (face <= 1) return p.xz;
			    if (face <= 3) return p.xy;
			    return p.zy;
			}

			vec2 siliconTransform(int t, vec2 uv) {
			    for (int i = 0; i < (t & 3); i++) {
			        uv = vec2(1.0 - uv.y, uv.x);
			    }
			    if (t >= 4) {
			        uv.x = 1.0 - uv.x;
			    }
			    return uv;
			}

			int siliconFace() {
			    vec3 n = abs(cross(dFdx(v_SiliconLocal), dFdy(v_SiliconLocal)));
			    // a face is only ever seen from outside, so the camera's side of the plane gives the direction
			    if (n.y >= n.x && n.y >= n.z) return v_SiliconView.y < 0.0 ? 1 : 0;
			    if (n.z >= n.x) return v_SiliconView.z < 0.0 ? 3 : 2;
			    return v_SiliconView.x < 0.0 ? 5 : 4;
			}

			vec4 siliconSample(uint code) {
			    int t = int((code - 1u) & 7u);
			    float pixels = float(16 << int((code - 1u) >> 3u));
			    int face = siliconFace();

			    vec2 spriteSize = pixels * u_TexelSize;
			    // the centre went through Sodium's 15 bit texture coordinates, snap the corner back onto the texel grid
			    vec2 origin = round((v_SiliconUv - spriteSize * 0.5) / u_TexelSize) * u_TexelSize;
			    vec2 uv = origin + siliconTransform(t, siliconStandardUv(face, v_SiliconLocal)) * spriteSize;

			    vec2 dx = dFdx(siliconPlane(face, v_SiliconLocal));
			    vec2 dy = dFdy(siliconPlane(face, v_SiliconLocal));
			    if ((t & 1) == 1) {
			        dx = dx.yx;
			        dy = dy.yx;
			    }
			    dx *= spriteSize;
			    dy *= spriteSize;

			    vec2 lo = origin + u_TexelSize * 0.5;
			    vec2 hi = origin + spriteSize - u_TexelSize * 0.5;
			    return sampleNearest(u_BlockTex, clamp(uv, lo, hi), u_TexelSize, dx, dy, sqrt(dx * dx + dy * dy));
			}

			""";
}
