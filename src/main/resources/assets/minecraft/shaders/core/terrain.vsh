#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 sectionPos;
flat out int siliconFace;
flat out int siliconTransform;
flat out ivec2 siliconSpriteLog2;

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp((uv / 256.0) + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

void main() {
    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
    sectionPos = Position;

    // Silicon marks merged quads with x == 127 in the normal, see Payload.java
    ivec3 n = ivec3(round(Normal * 127.0));
    if (n.x == 127 && n.y > 0) {
        siliconFace = (n.y - 1) % 6;
        siliconTransform = (n.y - 1) / 6;
        siliconSpriteLog2 = ivec2((n.z - 1) & 7, (n.z - 1) >> 3);
    } else {
        siliconFace = -1;
        siliconTransform = 0;
        siliconSpriteLog2 = ivec2(0);
    }
}
