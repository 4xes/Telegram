precision mediump float;
varying highp vec2 vTextureCoord;
uniform lowp sampler2D uTexture;
void main() {
    gl_FragColor = texture2D(uTexture, vTextureCoord);
}