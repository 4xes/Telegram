#version 300 es

precision highp float;

layout(location = 0) in vec2 inOffset;
layout(location = 1) in vec2 inVelocity;
layout(location = 2) in float inLifetime;
layout(location = 3) in float inDuration;

out vec2 outOffset;
out vec2 outVelocity;
out float outLifetime;
out float outDuration;

out float alpha;
out vec2 texCoord;

uniform float reset;
uniform float time;
uniform float deltaTime;
uniform vec2 dustOffset;
uniform vec2 dustSize;
uniform vec2 dustCount;
uniform float r;
uniform float seed;

float rand(vec2 n) {
    return fract(sin(dot(n,vec2(0.9898,1.1414-seed*.42)))*58.5453);
}

float rand() {
    return rand(vec2(gl_VertexID, gl_VertexID));
}

float modIdByX() {
    return mod(float(gl_VertexID), dustCount.x);
}

vec4 mapPosition(vec2 pos) {
    float x = pos.x * 2.0 - 1.0;
    float y = (1.0 - pos.y) * 2.0 - 1.0;
    return vec4(x, y, 0.0, 1.0);
}

vec2 texturePosition(int id) {
    float countX = dustCount.x;
    float countY = dustCount.y;
    float modIdX = modIdByX();
    float x = (modIdX / countX);
    float y = (float(id - int(modIdX)) / (countX * countY));
    return vec2(x, y);
}


vec2 startPosition(int id) {
    float countX = dustCount.x;
    float countY = dustCount.y;
    float modIdX = modIdByX();
    float x = dustOffset.x + (modIdX / countX) * dustSize.x;
    float y = dustOffset.y + (float(id - int(modIdX)) / (countX * countY)) * dustSize.y;
    return vec2(x, y);
}

float particleEaseInValueAt(float fraction, float t) {
    float windowSize = 0.8;

    float effectiveT = t;
    float windowStartOffset = -windowSize;
    float windowEndOffset = 1.0;

    float windowPosition = (1.0 - fraction) * windowStartOffset + fraction * windowEndOffset;
    float windowT = max(0.0, min(windowSize, effectiveT - windowPosition)) / windowSize;
    float localT = 1.0 - windowT;

    return localT;
}

vec2 rotate(vec2 v, float a) {
    float s = sin(a);
    float c = cos(a);
    mat2 m = mat2(c, s, -s, c);
    return m * v;
}

const float PI2 = 2.0 * 3.1415926535897932384626433832795;
const float multi = 0.001;
const float easeInDuration = 0.8;

vec2 randVelocity() {
    float direction = PI2 * rand();
    float velocity = (0.1 + rand() * 0.05) * (420.0 * multi);
    return rotate(vec2(velocity, velocity), direction);
}

void main() {
    vec2 offset = inOffset;
    vec2 velocity = inVelocity;
    float lifetime = inLifetime;
    vec2 startPosition = startPosition(gl_VertexID);
    vec2 texturePosition = texturePosition(gl_VertexID);


    if (time == 0.0 || reset > 0.) {
        offset = vec2(0.0, 0.0);
        velocity = randVelocity();
        lifetime = 0.7 + rand() * 0.8;
        alpha = 0.0;
    } else {
        float effectFraction = max(0.0, min(easeInDuration, time)) / easeInDuration;
        float modIdX = modIdByX();
        float particleXFraction = modIdX / dustCount.x;
        float particleFraction = particleEaseInValueAt(effectFraction, particleXFraction);

        offset += (velocity * deltaTime) * particleFraction;
        velocity += vec2(0.0, deltaTime * -120.0 * multi) * particleFraction;
        lifetime = max(0.0, lifetime - deltaTime * particleFraction);
        if (particleFraction == 0.0) {
            alpha = 0.0;
        } else {
            alpha = max(0.0, min(0.3, lifetime) / 0.3);
        }
    }

    outOffset = offset;
    outVelocity = velocity;
    outLifetime = lifetime;

    vec4 targetPosition = mapPosition(startPosition + offset);
    gl_PointSize = r;
    gl_Position = targetPosition;
    texCoord = texturePosition;
}