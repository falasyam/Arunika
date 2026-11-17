#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <GLES3/gl32.h>
#include <EGL/egl.h>

#define TAG "ArunikaNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Global OpenGL state
static GLuint g_program = 0;
static GLuint g_lutTexture3D = 0;
static GLint g_locLift = -1;
static GLint g_locGamma = -1;
static GLint g_locGain = -1;
static GLint g_locOffset = -1;
static GLint g_locTemp = -1;
static GLint g_locContrast = -1;
static GLint g_locSaturation = -1;
static GLint g_locVignette = -1;
static GLint g_locUseLut = -1;
static GLint g_locLutIntensity = -1;

static const char* VERTEX_SHADER_SRC =
    "#version 300 es\n"
    "layout(location = 0) in vec2 a_pos;\n"
    "layout(location = 1) in vec2 a_uv;\n"
    "out vec2 v_uv;\n"
    "void main() {\n"
    "    v_uv = a_uv;\n"
    "    gl_Position = vec4(a_pos, 0.0, 1.0);\n"
    "}\n";

static const char* FRAGMENT_SHADER_SRC =
    "#version 300 es\n"
    "precision highp float;\n"
    "precision highp sampler3D;\n"
    "in vec2 v_uv;\n"
    "out vec4 fragColor;\n"
    "uniform sampler2D u_videoTexture;\n"
    "uniform sampler3D u_lutTexture3D;\n"
    "uniform bool u_useLut3D;\n"
    "uniform float u_lutIntensity;\n"
    "uniform vec3 u_lift;\n"
    "uniform vec3 u_gamma;\n"
    "uniform vec3 u_gain;\n"
    "uniform vec3 u_offset;\n"
    "uniform float u_temperature;\n"
    "uniform float u_contrast;\n"
    "uniform float u_saturation;\n"
    "uniform float u_vignette;\n"
    "\n"
    "vec3 acesFilmic(vec3 x) {\n"
    "    float a = 2.51; float b = 0.03; float c = 2.43; float d = 0.59; float e = 0.14;\n"
    "    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);\n"
    "}\n"
    "\n"
    "void main() {\n"
    "    vec3 rgb = texture(u_videoTexture, v_uv).rgb;\n"
    "    rgb.r += u_temperature * 0.12;\n"
    "    rgb.b -= u_temperature * 0.12;\n"
    "    rgb += u_lift * (vec3(1.0) - rgb);\n"
    "    rgb *= (vec3(1.0) + u_gain);\n"
    "    vec3 gammaCoeff = vec3(1.0) / max(vec3(1.0) + u_gamma, vec3(0.01));\n"
    "    rgb = pow(max(rgb, vec3(0.0)), gammaCoeff);\n"
    "    rgb += u_offset * 0.2;\n"
    "    rgb = (rgb - vec3(0.18)) * u_contrast + vec3(0.18);\n"
    "    float luma = dot(rgb, vec3(0.2126, 0.7152, 0.0722));\n"
    "    rgb = mix(vec3(luma), rgb, u_saturation);\n"
    "    if (u_useLut3D) {\n"
    "        vec3 lutRgb = texture(u_lutTexture3D, clamp(rgb, 0.0, 1.0)).rgb;\n"
    "        rgb = mix(rgb, lutRgb, u_lutIntensity);\n"
    "    }\n"
    "    if (u_vignette > 0.0) {\n"
    "        vec2 uv = (v_uv - vec2(0.5)) * vec2(1.0, 0.5625);\n"
    "        float dist = length(uv);\n"
    "        rgb *= smoothstep(0.7, 0.25, dist * (1.0 + u_vignette * 1.5));\n"
    "    }\n"
    "    fragColor = vec4(acesFilmic(clamp(rgb, 0.0, 1.0)), 1.0);\n"
    "}\n";

static GLuint compileShader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);
    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        GLint infoLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 0) {
            std::vector<char> infoLog(infoLen);
            glGetShaderInfoLog(shader, infoLen, nullptr, infoLog.data());
            LOGE("Shader compilation failed: %s", infoLog.data());
        }
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

extern "C" JNIEXPORT void JNICALL
Java_com_falasyam_arunika_model_NativeGpuShaderEngine_nativeInit(
    JNIEnv* env, jobject thiz, jint width, jint height) {
    LOGI("Initializing Arunika 32-bit GLSL Engine: %dx%d", width, height);

    GLuint vs = compileShader(GL_VERTEX_SHADER, VERTEX_SHADER_SRC);
    GLuint fs = compileShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SRC);
    if (!vs || !fs) return;

    g_program = glCreateProgram();
    glAttachShader(g_program, vs);
    glAttachShader(g_program, fs);
    glLinkProgram(g_program);

    GLint linked = 0;
    glGetProgramiv(g_program, GL_LINK_STATUS, &linked);
    if (!linked) {
        LOGE("Program link failed!");
        return;
    }

    // Cache Uniform Locations
    g_locLift = glGetUniformLocation(g_program, "u_lift");
    g_locGamma = glGetUniformLocation(g_program, "u_gamma");
    g_locGain = glGetUniformLocation(g_program, "u_gain");
    g_locOffset = glGetUniformLocation(g_program, "u_offset");
    g_locTemp = glGetUniformLocation(g_program, "u_temperature");
    g_locContrast = glGetUniformLocation(g_program, "u_contrast");
    g_locSaturation = glGetUniformLocation(g_program, "u_saturation");
    g_locVignette = glGetUniformLocation(g_program, "u_vignette");
    g_locUseLut = glGetUniformLocation(g_program, "u_useLut3D");
    g_locLutIntensity = glGetUniformLocation(g_program, "u_lutIntensity");

    LOGI("Arunika Native Shader Engine linked successfully!");
}

extern "C" JNIEXPORT void JNICALL
Java_com_falasyam_arunika_model_NativeGpuShaderEngine_nativeUpload3dLut(
    JNIEnv* env, jobject thiz, jfloatArray dataArray, jint size) {
    if (size <= 0) return;

    jsize len = env->GetArrayLength(dataArray);
    jfloat* data = env->GetFloatArrayElements(dataArray, nullptr);

    if (g_lutTexture3D == 0) {
        glGenTextures(1, &g_lutTexture3D);
    }
    glBindTexture(GL_TEXTURE_3D, g_lutTexture3D);
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

    // Upload 3D RGB floating-point texture directly to GPU VRAM
    glTexImage3D(GL_TEXTURE_3D, 0, GL_RGB16F, size, size, size, 0, GL_RGB, GL_FLOAT, data);
    glBindTexture(GL_TEXTURE_3D, 0);

    env->ReleaseFloatArrayElements(dataArray, data, JNI_ABORT);
    LOGI("Hardware 3D Texture uploaded: %dx%dx%d", size, size, size);
}

extern "C" JNIEXPORT void JNICALL
Java_com_falasyam_arunika_model_NativeGpuShaderEngine_nativeSetUniforms(
    JNIEnv* env, jobject thiz,
    jfloat lr, jfloat lg, jfloat lb,
    jfloat gr, jfloat gg, jfloat gb,
    jfloat hr, jfloat hg, jfloat hb,
    jfloat or_, jfloat og, jfloat ob,
    jfloat temp, jfloat contrast, jfloat sat, jfloat vignette,
    jboolean useLut, jfloat lutInt) {
    if (!g_program) return;
    glUseProgram(g_program);

    glUniform3f(g_locLift, lr, lg, lb);
    glUniform3f(g_locGamma, gr, gg, gb);
    glUniform3f(g_locGain, hr, hg, hb);
    glUniform3f(g_locOffset, or_, og, ob);
    glUniform1f(g_locTemp, temp);
    glUniform1f(g_locContrast, contrast);
    glUniform1f(g_locSaturation, sat);
    glUniform1f(g_locVignette, vignette);
    glUniform1i(g_locUseLut, useLut ? 1 : 0);
    glUniform1f(g_locLutIntensity, lutInt);
}

extern "C" JNIEXPORT void JNICALL
Java_com_falasyam_arunika_model_NativeGpuShaderEngine_nativeRender(
    JNIEnv* env, jobject thiz, jint srcTex, jint dstFbo) {
    if (!g_program) return;
    glBindFramebuffer(GL_FRAMEBUFFER, dstFbo);
    glUseProgram(g_program);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, srcTex);
    GLint uTex = glGetUniformLocation(g_program, "u_videoTexture");
    if (uTex >= 0) glUniform1i(uTex, 0);

    if (g_lutTexture3D) {
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_3D, g_lutTexture3D);
        GLint uLut = glGetUniformLocation(g_program, "u_lutTexture3D");
        if (uLut >= 0) glUniform1i(uLut, 1);
    }

    // Fullscreen quad vertices [x, y, u, v]
    static const GLfloat quadVertices[] = {
        -1.0f, -1.0f,  0.0f, 0.0f,
         1.0f, -1.0f,  1.0f, 0.0f,
        -1.0f,  1.0f,  0.0f, 1.0f,
         1.0f,  1.0f,  1.0f, 1.0f,
    };
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), quadVertices);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), &quadVertices[2]);
    glEnableVertexAttribArray(1);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_falasyam_arunika_model_NativeGpuShaderEngine_nativeRelease(
    JNIEnv* env, jobject thiz) {
    if (g_lutTexture3D) {
        glDeleteTextures(1, &g_lutTexture3D);
        g_lutTexture3D = 0;
    }
    if (g_program) {
        glDeleteProgram(g_program);
        g_program = 0;
    }
    LOGI("Arunika Native Shader Engine released.");
}
