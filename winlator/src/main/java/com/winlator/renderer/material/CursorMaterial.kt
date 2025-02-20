package com.winlator.renderer.material

class CursorMaterial : ShaderMaterial() {

    init {
        setUniformNames("xform", "viewSize", "texture")
    }

    override val vertexShader: String
        get() = """
            uniform float xform[6];
            uniform vec2 viewSize;
            attribute vec2 position;
            varying vec2 vUV;
            void main() {
            vUV = position;
            vec2 transformedPos = applyXForm(position, xform);
            gl_Position = vec4(2.0 * transformedPos.x / viewSize.x - 1.0, 1.0 - 2.0 * transformedPos.y / viewSize.y, 0.0, 1.0);
            }
        """.trimIndent()

    override val fragmentShader: String
        get() = """
            precision mediump float;
            uniform sampler2D texture;
            varying vec2 vUV;
            void main() {
            gl_FragColor = texture2D(texture, vUV);
            }
        """.trimIndent()
}
