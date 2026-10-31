package com.ivy.settings.support

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.ivy.base.links.SupportDestinations
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Round-trips the on-device UPI QR code through ZXing's decoder, and writes the PNG to
 * build/support-qr/ so it can also be checked with an independent decoder.
 */
class UpiQrCodeTest {

    private val payload = SupportDestinations.upiPaymentUri()

    @Test
    fun `QR payload is the UPI payment link without an amount`() {
        payload shouldBe "upi://pay?pa=SimplyFahim@sbi&pn=SimplyFahim&cu=INR"
        payload shouldNotContain "am="
    }

    @Test
    fun `generated QR code decodes back to the UPI link`() {
        val matrix = UpiQrCode.encode(payload)

        decode(matrix, pixelsPerModule = 8) shouldBe payload
    }

    @Test
    fun `QR code has the four-module quiet zone`() {
        val matrix = UpiQrCode.encode(payload)
        val quiet = UpiQrCode.QUIET_ZONE_MODULES

        for (i in 0 until matrix.width) {
            for (q in 0 until quiet) {
                matrix[i, q] shouldBe false
                matrix[q, i] shouldBe false
                matrix[i, matrix.height - 1 - q] shouldBe false
                matrix[matrix.width - 1 - q, i] shouldBe false
            }
        }
        // The finder pattern starts right after the quiet zone.
        matrix[quiet, quiet] shouldBe true
    }

    @Test
    fun `writes the QR image for external verification`() {
        val matrix = UpiQrCode.encode(payload)
        val image = render(matrix, UpiQrCode.EXPORT_PIXELS_PER_MODULE)
        val out = File("build/support-qr/upi-qr.png").apply { parentFile?.mkdirs() }
        ImageIO.write(image, "png", out)

        decode(matrix, UpiQrCode.EXPORT_PIXELS_PER_MODULE) shouldBe payload
    }

    private fun render(matrix: BitMatrix, pixelsPerModule: Int): BufferedImage {
        val size = matrix.width * pixelsPerModule
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val dark = matrix[x / pixelsPerModule, y / pixelsPerModule]
                image.setRGB(x, y, if (dark) BLACK else WHITE)
            }
        }
        return image
    }

    private fun decode(matrix: BitMatrix, pixelsPerModule: Int): String {
        val image = render(matrix, pixelsPerModule)
        val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
        val source = RGBLuminanceSource(image.width, image.height, pixels)
        return QRCodeReader()
            .decode(BinaryBitmap(HybridBinarizer(source)), mapOf(DecodeHintType.PURE_BARCODE to false))
            .text
    }

    private companion object {
        const val BLACK = 0x000000
        const val WHITE = 0xFFFFFF
    }
}
