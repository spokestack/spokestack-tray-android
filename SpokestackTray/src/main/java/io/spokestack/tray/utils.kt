package io.spokestack.tray

import android.os.StrictMode
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * General utility methods used by the tray.
 */

/**
 * Prefix a key with a spokestack designator to avoid collisions.
 */
fun namespaced_key(key: String): String {
    return "spsk_$key"
}

/**
 * Download a file from the specified URL. Should be called from a background thread/coroutine.
 *
 * @param fileURL The URL specifying the file to be downloaded.
 * @param downloadDir The directory to which the downloaded file should be saved.
 * @return The filesystem path to the downloaded file.
 */
fun downloadFile(fileURL: String, downloadDir: String): String {
    val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
    StrictMode.setThreadPolicy(policy)

    val fileName = fileURL.let {
        val lastPathIndex = it.lastIndexOf('/')
        val path = it.substring(lastPathIndex + 1)
        "$downloadDir/${namespaced_key(path)}"
    }

    val u = URL(fileURL)
    val c: HttpsURLConnection = u.openConnection() as HttpsURLConnection
    var fileSize = doGet(c)
    var counter = 0
    while (fileSize == -1 && counter <= 3) {
        c.disconnect()
        fileSize = doGet(c)
        counter++
    }
    val fOutput = File(fileName)
    if (fOutput.exists()) fOutput.delete()
    val f = BufferedOutputStream(FileOutputStream(fOutput))
    val inStream = c.inputStream
    val buffer = ByteArray(8192)
    var read: Int
    var downloadedData = 0
    while (inStream.read(buffer).also { read = it } > 0) {
        downloadedData += read
        f.write(buffer, 0, read)
    }
    f.close()
    return fileName
}

private fun doGet(conn: HttpsURLConnection): Int {
    conn.requestMethod = "GET"
    conn.readTimeout = 30000
    conn.connect()
    return conn.contentLength
}
