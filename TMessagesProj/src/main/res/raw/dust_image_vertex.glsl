attribute vec4 aPosition;
attribute vec4 aTextureCoord;

uniform mat4 uMatrix;

varying highp vec2 vTextureCoord;
void main() {
    gl_Position = uMatrix * aPosition;
    vTextureCoord = aTextureCoord.xy;
}