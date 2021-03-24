precision highp float;

uniform vec3 color1;
uniform vec3 color2;
uniform vec3 color3;
uniform vec3 color4;
uniform vec2 p1;
uniform vec2 p2;
uniform vec2 p3;
uniform vec2 p4;
uniform vec2 resolution;

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;
    uv.y = 1.0 - uv.y;

    float dp1 = distance(uv, p1);
    float dp2 = distance(uv, p2);
    float dp3 = distance(uv, p3);
    float dp4 = distance(uv, p4);
    float minD = min(dp1, min(dp2, min(dp3, dp4)));
    float p = 5.0;
    dp1 = pow(1.0 - (dp1 - minD), p);
    dp2 = pow(1.0 - (dp2 - minD), p);
    dp3 = pow(1.0 - (dp3 - minD), p);
    dp4 = pow(1.0 - (dp4 - minD), p);
    float sumDp = dp1 + dp2 + dp3 + dp4;

    vec3 color = (color1 * dp1 + color2 * dp2 + color3 * dp3 + color4 * dp4) / sumDp;
    gl_FragColor = vec4(color, 1.0);
}