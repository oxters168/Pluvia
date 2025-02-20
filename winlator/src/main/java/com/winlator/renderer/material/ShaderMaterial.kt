package com.winlator.renderer.material

import android.opengl.GLES20
import androidx.collection.ArrayMap

open class ShaderMaterial {

    @JvmField
    var programId: Int = 0

    private val uniforms = ArrayMap<String, Int>()

    protected open val vertexShader: String
        get() = ""

    protected open val fragmentShader: String
        get() = ""

    fun setUniformNames(vararg names: String) {
        uniforms.clear()
        for (name in names) {
            uniforms[name] = -1
        }
    }

    fun use() {
        if (programId == 0) {
            programId = compileShaders(
                vertexShader,
                fragmentShader,
            )
        }

        GLES20.glUseProgram(programId)

        for (i in 0..<uniforms.size) {
            val location = uniforms.valueAt(i)
            if (location == -1) {
                val name = uniforms.keyAt(i)
                uniforms[name] = GLES20.glGetUniformLocation(programId, name)
            }
        }
    }

    fun getUniformLocation(name: String?): Int {
        val location = uniforms[name]
        return location ?: -1
    }

    fun destroy() {
        GLES20.glDeleteProgram(programId)
        programId = 0
    }

    companion object {
        protected fun compileShaders(vertexShader: String, fragmentShader: String?): Int {
            var vertexShader = vertexShader
            val beginIndex = vertexShader.indexOf("void main() {")

            vertexShader = """
                ${vertexShader.substring(0, beginIndex)}
                vec2 applyXForm(vec2 p, float xform[6]) {
                return vec2(xform[0] * p.x + xform[2] * p.y + xform[4], xform[1] * p.x + xform[3] * p.y + xform[5]);
                }
                ${vertexShader.substring(beginIndex)}
            """.trimIndent()

            val programId = GLES20.glCreateProgram()
            val compiled = IntArray(1)

            val vertexShaderId = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
            GLES20.glShaderSource(vertexShaderId, vertexShader)
            GLES20.glCompileShader(vertexShaderId)

            GLES20.glGetShaderiv(vertexShaderId, GLES20.GL_COMPILE_STATUS, compiled, 0)

            if (compiled[0] == 0) {
                throw RuntimeException("Could not compile vertex shader:  ${GLES20.glGetShaderInfoLog(vertexShaderId)}")
            }

            GLES20.glAttachShader(programId, vertexShaderId)

            val fragmentShaderId = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
            GLES20.glShaderSource(fragmentShaderId, fragmentShader)
            GLES20.glCompileShader(fragmentShaderId)

            GLES20.glGetShaderiv(fragmentShaderId, GLES20.GL_COMPILE_STATUS, compiled, 0)

            if (compiled[0] == 0) {
                throw RuntimeException("Could not compile fragment shader: ${GLES20.glGetShaderInfoLog(fragmentShaderId)}")
            }

            GLES20.glAttachShader(programId, fragmentShaderId)

            GLES20.glLinkProgram(programId)

            GLES20.glDeleteShader(vertexShaderId)
            GLES20.glDeleteShader(fragmentShaderId)

            return programId
        }
    }
}
