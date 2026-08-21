package dev.bartuzen.qbitcontroller

import java.io.BufferedReader
import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLEncoder
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

private const val SingleInstancePort = 47832

class DesktopSingleInstance private constructor(
    private val lockFile: RandomAccessFile,
    private val lockChannel: FileChannel,
    private val lock: FileLock,
    private val serverSocket: ServerSocket?,
) : AutoCloseable {
    companion object {
        fun acquire(args: Array<String>, onArgsReceived: (CommandLineArguments) -> Unit): DesktopSingleInstance? {
            val lockFile = RandomAccessFile(singleInstanceLockFile(), "rw")
            val lockChannel = lockFile.channel
            val lock = try {
                lockChannel.tryLock()
            } catch (_: OverlappingFileLockException) {
                null
            }
            if (lock == null) {
                lockChannel.close()
                lockFile.close()
                sendArgsToPrimaryInstance(args)
                return null
            }

            val serverSocket = try {
                ServerSocket(SingleInstancePort, 16, InetAddress.getLoopbackAddress())
            } catch (_: Exception) {
                null
            }

            return DesktopSingleInstance(lockFile, lockChannel, lock, serverSocket).also {
                it.startServer(onArgsReceived)
            }
        }
    }

    private fun startServer(onArgsReceived: (CommandLineArguments) -> Unit) {
        val serverSocket = serverSocket ?: return
        thread(name = "qBitController-single-instance", isDaemon = true) {
            while (!serverSocket.isClosed) {
                try {
                    serverSocket.accept().use { socket ->
                        val args = socket.inputStream.bufferedReader().use(BufferedReader::readText)
                            .lineSequence()
                            .map { decodeArgument(it) }
                            .toList()
                            .toTypedArray()

                        onArgsReceived(CommandLineArguments.parse(args))
                    }
                } catch (_: Exception) {
                    if (!serverSocket.isClosed) {
                        Thread.sleep(100)
                    }
                }
            }
        }
    }

    override fun close() {
        serverSocket?.close()
        lock.release()
        lockChannel.close()
        lockFile.close()
    }
}

private fun sendArgsToPrimaryInstance(args: Array<String>) {
    try {
        Socket(InetAddress.getLoopbackAddress(), SingleInstancePort).use { socket ->
            socket.getOutputStream().bufferedWriter().use { writer ->
                args.forEach { arg ->
                    writer.appendLine(encodeArgument(arg))
                }
            }
        }
    } catch (_: Exception) {
    }
}

private fun singleInstanceLockFile(): File {
    val appData = System.getenv("LOCALAPPDATA")
        ?: System.getProperty("java.io.tmpdir")
    val directory = File(appData, "qBitController")
    directory.mkdirs()

    return File(directory, "qBitController.lock")
}

private fun encodeArgument(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

private fun decodeArgument(value: String): String = java.net.URLDecoder.decode(value, StandardCharsets.UTF_8)
