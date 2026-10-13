#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 sectionPos;
flat in int siliconFace;
flat in int siliconTransform;
flat in ivec2 siliconSpriteLog2;

out vec4 fragColor;

vec4 sampleNearest(sampler2D sampler, vec2 uv, vec2 pixelSize, vec2 du, vec2 dv, vec2 texelScreenSize) {
    // Convert our UV back up to texel coordinates and find out how far over we are from the center of each pixel
    vec2 uvTexelCoords = uv / pixelSize;
    vec2 texelCenter = round(uvTexelCoords) - 0.5f;
    vec2 texelOffset = uvTexelCoords - texelCenter;

    // Move our offset closer to the texel center based on texel size on screen
    texelOffset = (texelOffset - 0.5f) * pixelSize / texelScreenSize + 0.5f;
    texelOffset = clamp(texelOffset, 0.0f, 1.0f);

    uv = (texelCenter + texelOffset) * pixelSize;
    return textureGrad(sampler, uv, du, dv);
}

// Rotated Grid Super-Sampling. lo/hi keep the samples inside one sprite.
vec4 sampleRGSS(sampler2D source, vec2 uv, vec2 pixelSize, vec2 du, vec2 dv, vec2 lo, vec2 hi) {
    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    float maxTexelSize = max(texelScreenSize.x, texelScreenSize.y);

    float minPixelSize = min(pixelSize.x, pixelSize.y);

    float transitionStart = minPixelSize * 1.0;
    float transitionEnd = minPixelSize * 2.0;
    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);

    float duLength = length(du);
    float dvLength = length(dv);
    float minDerivative = min(duLength, dvLength);
    float maxDerivative = max(duLength, dvLength);

    float effectiveDerivative = sqrt(minDerivative * maxDerivative);

    float mipLevelExact = max(0.0, log2(effectiveDerivative / minPixelSize));

    float mipLevelLow = floor(mipLevelExact);
    float mipLevelHigh = mipLevelLow + 1.0;
    float mipBlend = fract(mipLevelExact);

    const vec2 offsets[4] = vec2[](
    vec2(0.125, 0.375),
    vec2(-0.125, -0.375),
    vec2(0.375, -0.125),
    vec2(-0.375, 0.125)
    );

    vec4 rgssColorLow = vec4(0.0);
    vec4 rgssColorHigh = vec4(0.0);
    for (int i = 0; i < 4; ++i) {
        vec2 sampleUV = clamp(uv + offsets[i] * pixelSize, lo, hi);
        rgssColorLow += textureLod(source, sampleUV, mipLevelLow);
        rgssColorHigh += textureLod(source, sampleUV, mipLevelHigh);
    }
    rgssColorLow *= 0.25;
    rgssColorHigh *= 0.25;

    vec4 rgssColor = mix(rgssColorLow, rgssColorHigh, mipBlend);

    vec4 nearestColor = sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);

    return mix(nearestColor, rgssColor, blendFactor);
}

vec4 sampleTerrain(vec2 uv, vec2 pixelSize, vec2 du, vec2 dv, vec2 lo, vec2 hi) {
    if (UseRgss == 1) {
        return sampleRGSS(Sampler0, uv, pixelSize, du, dv, lo, hi);
    }
    return sampleNearest(Sampler0, uv, pixelSize, du, dv, sqrt(du * du + dv * dv));
}

// Same layout and transforms as UvTransform.java
vec2 standardUv(int face, vec3 p) {
    vec3 f = fract(p);
    if (face == 0) return vec2(f.x, 1.0 - f.z);
    if (face == 1) return f.xz;
    if (face == 2) return vec2(1.0 - f.x, 1.0 - f.y);
    if (face == 3) return vec2(f.x, 1.0 - f.y);
    if (face == 4) return vec2(f.z, 1.0 - f.y);
    return vec2(1.0 - f.z, 1.0 - f.y);
}

vec2 facePlane(int face, vec3 p) {
    if (face <= 1) return p.xz;
    if (face <= 3) return p.xy;
    return p.zy;
}

vec2 applyTransform(int t, vec2 uv) {
    for (int i = 0; i < (t & 3); i++) {
        uv = vec2(1.0 - uv.y, uv.x);
    }
    if (t >= 4) {
        uv.x = 1.0 - uv.x;
    }
    return uv;
}

vec4 sampleMerged(vec2 pixelSize) {
    vec2 spriteSize = vec2(1 << siliconSpriteLog2.x, 1 << siliconSpriteLog2.y) * pixelSize;
    vec2 origin = texCoord0 - spriteSize * 0.5;
    vec2 uv = origin + applyTransform(siliconTransform, standardUv(siliconFace, sectionPos)) * spriteSize;

    // The uv jumps back at every block edge, so derivatives come from the smooth position instead.
    vec2 dx = dFdx(facePlane(siliconFace, sectionPos));
    vec2 dy = dFdy(facePlane(siliconFace, sectionPos));
    if ((siliconTransform & 1) == 1) {
        dx = dx.yx;
        dy = dy.yx;
    }
    dx *= spriteSize;
    dy *= spriteSize;

    vec2 lo = origin + pixelSize * 0.5;
    vec2 hi = origin + spriteSize - pixelSize * 0.5;
    return sampleTerrain(clamp(uv, lo, hi), pixelSize, dx, dy, lo, hi);
}

void main() {
    vec2 pixelSize = 1.0f / TextureSize;
    vec4 color;
    if (siliconFace >= 0) {
        color = sampleMerged(pixelSize) * vertexColor;
    } else {
        color = sampleTerrain(texCoord0, pixelSize, dFdx(texCoord0), dFdy(texCoord0), vec2(0.0), vec2(1.0)) * vertexColor;
    }
    color = mix(FogColor * vec4(1, 1, 1, color.a), color, ChunkVisibility);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
