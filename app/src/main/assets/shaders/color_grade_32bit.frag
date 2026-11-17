#version 300 es
precision highp float;
precision highp sampler3D;

in vec2 v_texCoord;
out vec4 fragColor;

uniform sampler2D u_videoTexture;
uniform sampler3D u_lutTexture3D;
uniform bool u_useLut3D;
uniform float u_lutIntensity;

// 3-Way Color Wheels (RGB Offsets)
uniform vec3 u_lift;   // Shadows (-1.0 to 1.0)
uniform vec3 u_gamma;  // Midtones (-1.0 to 1.0)
uniform vec3 u_gain;   // Highlights (-1.0 to 1.0)
uniform vec3 u_offset; // Global (-1.0 to 1.0)

// Primary Adjustments
uniform float u_temperature; // Kelvin shift (-1.0 to 1.0)
uniform float u_tint;        // Green/Magenta shift (-1.0 to 1.0)
uniform float u_contrast;    // 0.0 to 2.0 (1.0 = neutral)
uniform float u_saturation;  // 0.0 to 2.0 (1.0 = neutral)
uniform float u_vignette;    // 0.0 to 1.0
uniform float u_grainAmount; // 0.0 to 0.1
uniform float u_time;

// Fast Procedural Film Grain Hash
float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// ACES Filmic Tone Mapping Curve (SDR Output Protection)
vec3 acesFilmic(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec4 src = texture(u_videoTexture, v_texCoord);
    vec3 rgb = src.rgb;

    // 1. Color Temperature & Tint (Bradford chromatic shift approximation)
    rgb.r += u_temperature * 0.12;
    rgb.b -= u_temperature * 0.12;
    rgb.g += u_tint * 0.08;

    // 2. 3-Way Lift / Gamma / Gain Processing (DaVinci Formula)
    // Lift: Affects shadows (1 - rgb)
    rgb += u_lift * (vec3(1.0) - rgb);

    // Gain: Multiplies highlights
    rgb *= (vec3(1.0) + u_gain);

    // Gamma: Power curve on midtones
    vec3 gammaCoeff = vec3(1.0) / max(vec3(1.0) + u_gamma, vec3(0.01));
    rgb = pow(max(rgb, vec3(0.0)), gammaCoeff);

    // Offset: Uniform global balance
    rgb += u_offset * 0.2;

    // 3. Contrast (Pivot at 0.18 Middle Gray)
    rgb = (rgb - vec3(0.18)) * u_contrast + vec3(0.18);

    // 4. Saturation (Rec.709 Luma Weights: 0.2126, 0.7152, 0.0722)
    float luma = dot(rgb, vec3(0.2126, 0.7152, 0.0722));
    rgb = mix(vec3(luma), rgb, u_saturation);

    // 5. Hardware-Accelerated 3D LUT Lookup
    if (u_useLut3D) {
        vec3 lutRgb = texture(u_lutTexture3D, clamp(rgb, 0.0, 1.0)).rgb;
        rgb = mix(rgb, lutRgb, u_lutIntensity);
    }

    // 6. Vignette Falloff
    if (u_vignette > 0.0) {
        vec2 uv = (v_texCoord - vec2(0.5)) * vec2(1.0, 0.5625); // 16:9 aspect correction
        float dist = length(uv);
        float vig = smoothstep(0.7, 0.25, dist * (1.0 + u_vignette * 1.5));
        rgb *= vig;
    }

    // 7. Subtle 35mm Analog Film Grain
    if (u_grainAmount > 0.0) {
        float noise = (hash(v_texCoord * 500.0 + fract(u_time)) - 0.5) * u_grainAmount;
        rgb += vec3(noise);
    }

    // 8. ACES Tone Mapping Curve
    rgb = acesFilmic(rgb);

    fragColor = vec4(clamp(rgb, 0.0, 1.0), src.a);
}
