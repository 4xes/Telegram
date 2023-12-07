#version 300 es

precision highp float;

in float alpha;
out vec4 fragColor;

uniform sampler2D oTexture;
in vec2 texCoord;

void main() {
    vec2 circCoord = 2.0 * gl_PointCoord - 1.0;
    vec4 textureColor = texture(oTexture, texCoord);
    if (textureColor.a == 0.0) {
        discard;
    }
    if (dot(circCoord, circCoord) > 1.0) {
        discard;
    }

    fragColor = vec4(textureColor.rgb, alpha);
}