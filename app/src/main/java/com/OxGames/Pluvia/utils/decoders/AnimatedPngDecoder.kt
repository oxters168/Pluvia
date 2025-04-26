package com.OxGames.Pluvia.utils.decoders

import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.github.penfeizhou.animation.apng.decode.APNGParser
import com.github.penfeizhou.animation.io.ByteBufferReader
import com.github.penfeizhou.animation.io.StreamReader
import java.nio.ByteBuffer
import okio.BufferedSource

// .png (Animated) PNG file decoder
// Reference: https://github.com/coil-kt/coil/issues/506#issuecomment-952526682
class AnimatedPngDecoder(private val source: ImageSource) : Decoder {
    override suspend fun decode(): DecodeResult {
        val buffer = source.source().squashToDirectByteBuffer()
        return DecodeResult(
            drawable = APNGDrawable { ByteBufferReader(buffer) },
            isSampled = false,
        )
    }

    private fun BufferedSource.squashToDirectByteBuffer(): ByteBuffer {
        request(Long.MAX_VALUE)

        val byteBuffer = ByteBuffer.allocateDirect(buffer.size.toInt())
        while (!buffer.exhausted()) {
            buffer.read(byteBuffer)
        }

        byteBuffer.flip()

        return byteBuffer
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            val stream = result.source.source().peek().inputStream()
            return if (APNGParser.isAPNG(StreamReader(stream))) {
                AnimatedPngDecoder(result.source)
            } else {
                null
            }
        }
    }
}
