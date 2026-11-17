package com.falasyam.arunika.model

/**
 * Parameter passing untuk 32-bit Floating Point GPU Shader
 */
data class GpuGradeUniforms(
    val liftR: Float = 0f, val liftG: Float = 0f, val liftB: Float = 0f,
    val gammaR: Float = 0f, val gammaG: Float = 0f, val gammaB: Float = 0f,
    val gainR: Float = 0f, val gainG: Float = 0f, val gainB: Float = 0f,
    val offsetR: Float = 0f, val offsetG: Float = 0f, val offsetB: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val vignette: Float = 0f,
    val grain: Float = 0.02f,
    val useLut3D: Boolean = false,
    val lutIntensity: Float = 0.85f
)

/**
 * Interface Shader Engine untuk Arunika
 */
interface IShaderEngine {
    fun initialize(width: Int, height: Int)
    fun setUniforms(params: GpuGradeUniforms)
    fun load3dLutTexture(lutData: FloatArray, size: Int)
    fun renderFrame(sourceTextureId: Int, targetFboId: Int)
    fun release()
}

/**
 * JNI Native Bridge ke C++ NDK (MediaCodec + OpenGL ES 3.2 / Vulkan)
 */
class NativeGpuShaderEngine : IShaderEngine {
    companion object {
        init {
            System.loadLibrary("arunika_native_engine")
        }
    }

    private var isInitialized = false

    override fun initialize(width: Int, height: Int) {
        nativeInit(width, height)
        isInitialized = true
    }

    override fun setUniforms(params: GpuGradeUniforms) {
        if (!isInitialized) return
        nativeSetUniforms(
            params.liftR, params.liftG, params.liftB,
            params.gammaR, params.gammaG, params.gammaB,
            params.gainR, params.gainG, params.gainB,
            params.offsetR, params.offsetG, params.offsetB,
            params.temperature,
            params.contrast, params.saturation,
            params.vignette,
            params.useLut3D,
            params.lutIntensity
        )
    }

    override fun load3dLutTexture(lutData: FloatArray, size: Int) {
        if (!isInitialized) return
        nativeUpload3dLut(lutData, size)
    }

    override fun renderFrame(sourceTextureId: Int, targetFboId: Int) {
        if (!isInitialized) return
        nativeRender(sourceTextureId, targetFboId)
    }

    override fun release() {
        if (isInitialized) nativeRelease()
        isInitialized = false
    }

    // C++ JNI Declarations
    private external fun nativeInit(width: Int, height: Int)
    private external fun nativeSetUniforms(
        liftR: Float, liftG: Float, liftB: Float,
        gammaR: Float, gammaG: Float, gammaB: Float,
        gainR: Float, gainG: Float, gainB: Float,
        offsetR: Float, offsetG: Float, offsetB: Float,
        temperature: Float,
        contrast: Float, saturation: Float,
        vignette: Float,
        useLut3D: Boolean, lutIntensity: Float
    )
    private external fun nativeUpload3dLut(data: FloatArray, size: Int)
    private external fun nativeRender(srcTex: Int, dstFbo: Int)
    private external fun nativeRelease()
}
