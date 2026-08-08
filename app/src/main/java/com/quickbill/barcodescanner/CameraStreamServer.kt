package com.quickbill.barcodescanner

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import fi.iki.elonen.NanoHTTPD

class CameraStreamServer(
    port: Int = DEFAULT_PORT
) : NanoHTTPD(port) {

    companion object {
        const val DEFAULT_PORT = 8080
        private const val BOUNDARY = "quickbillframe"
    }

    private val latestFrame =
        AtomicReference<ByteArray?>(null)

    private val runningClients =
        CopyOnWriteArrayList<InputStream>()

    fun updateFrame(bitmap: Bitmap) {

        val output =
            ByteArrayOutputStream()

        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            72,
            output
        )

        latestFrame.set(
            output.toByteArray()
        )
    }

    fun clearFrame() {
        latestFrame.set(null)
    }

    fun hasFrame(): Boolean {
        return latestFrame.get() != null
    }

    override fun serve(
        session: IHTTPSession
    ): Response {

        return when (session.uri) {

            "/" -> {

                val html =
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width">
                        <title>QuickBill Barcode Scanner</title>
                        <style>
                            body {
                                background: #0B1220;
                                color: white;
                                font-family: Arial, sans-serif;
                                text-align: center;
                                margin: 0;
                                padding: 30px;
                            }

                            h1 {
                                margin-bottom: 8px;
                            }

                            p {
                                color: #94A3B8;
                            }

                            img {
                                width: 100%;
                                max-width: 900px;
                                border-radius: 16px;
                                margin-top: 20px;
                            }
                        </style>
                    </head>

                    <body>

                        <h1>QuickBill Barcode Scanner</h1>

                        <p>Camera server is running</p>

                        <img src="/video">

                    </body>
                    </html>
                    """.trimIndent()

                newFixedLengthResponse(
                    Response.Status.OK,
                    "text/html",
                    html
                )
            }

            "/video" -> {

                createStreamResponse()
            }

            "/status" -> {

                val json =
                    """
                    {
                        "application": "QuickBill Barcode Scanner",
                        "status": "running",
                        "stream": "/video",
                        "port": $DEFAULT_PORT
                    }
                    """.trimIndent()

                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    json
                )
            }

            else -> {

                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "text/plain",
                    "Not Found"
                )
            }
        }
    }

    private fun createStreamResponse(): Response {

        val pipeOutput =
            java.io.PipedOutputStream()

        val pipeInput =
            java.io.PipedInputStream(
                pipeOutput,
                64 * 1024
            )

        runningClients.add(pipeInput)

        Thread {

            try {

                while (!Thread.currentThread().isInterrupted) {

                    val frame =
                        latestFrame.get()

                    if (frame != null) {

                        pipeOutput.write(
                            "--$BOUNDARY\r\n".toByteArray()
                        )

                        pipeOutput.write(
                            "Content-Type: image/jpeg\r\n".toByteArray()
                        )

                        pipeOutput.write(
                            "Content-Length: ${frame.size}\r\n".toByteArray()
                        )

                        pipeOutput.write(
                            "Cache-Control: no-cache\r\n\r\n"
                                .toByteArray()
                        )

                        pipeOutput.write(frame)

                        pipeOutput.write(
                            "\r\n".toByteArray()
                        )

                        pipeOutput.flush()
                    }

                    Thread.sleep(80)
                }

            } catch (_: Exception) {

                // Client disconnected.
            } finally {

                runningClients.remove(
                    pipeInput
                )

                try {
                    pipeOutput.close()
                } catch (_: Exception) {
                }

                try {
                    pipeInput.close()
                } catch (_: Exception) {
                }
            }

        }.apply {
            name = "QuickBill-MJPEG-Client"
            isDaemon = true
            start()
        }

        return newChunkedResponse(
            Response.Status.OK,
            "multipart/x-mixed-replace; boundary=$BOUNDARY",
            pipeInput
        )
    }

    override fun stop() {

        latestFrame.set(null)

        for (client in runningClients) {
            try {
                client.close()
            } catch (_: Exception) {
            }
        }

        runningClients.clear()

        super.stop()
    }
}