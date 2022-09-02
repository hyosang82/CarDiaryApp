package kr.hyosang.drivediary.client.util

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.json.JSONObject
import java.io.CharArrayReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext

class HttpTask(val url: String, val method: String) {
    private val TAG = "HttpTask"

    var formData = HashMap<String, Object>()
    var rawBody: ByteArray? = null

    var responseCode: Int = -1
    var responseMessage: String? = null
    var responseBody: String? = null

    fun execute(callback: (task: HttpTask) -> Unit) {
        CoroutineScope(IO).async {
            _execute(callback)
        }
    }

    private suspend fun _execute(callback: (task: HttpTask) -> Unit) {
        val conn = URL(url).openConnection() as HttpURLConnection
        val reqBody = getRequestBody()
        val bOutput = (reqBody != null)

        Log.i(TAG, "REQ => [$method] $url")

        conn.doInput = true
        conn.doOutput = bOutput
        conn.requestMethod = method

        if(bOutput) {
            conn.outputStream.write(reqBody!!)
        }

        this.responseCode = conn.responseCode
        this.responseMessage = conn.responseMessage

        Log.i(TAG, "RES <= $responseCode $responseMessage")

        val reader = InputStreamReader(
            try {
                conn.inputStream
            }catch(e: IOException) {
                conn.errorStream
            }, "UTF-8")
        var buf = CharArray(1024)
        var len: Int = 0
        val respBody = StringBuffer()

        while(true) {
            len = reader.read(buf)
            if(len > 0) {
                respBody.append(String(buf, 0, len))
            }else {
                break
            }
        }

        responseBody = respBody.toString()

        //done
        withContext(Main) {
            callback.invoke(this@HttpTask)
        }
    }

    private fun getRequestBody(): ByteArray? {
        if((rawBody != null) && (rawBody!!.isNotEmpty())) {
            return rawBody!!
        }else if(formData.size > 0) {
            return formData.entries
                .map {
                    String.format(
                        "%s=%s",
                        URLEncoder.encode(it.key, "UTF-8"),
                        it.value
                    )
                }
                .joinToString("&")
                .toByteArray(Charset.forName("UTF-8"))

        }else {
            return null
        }
    }

    fun getJsonBody(): JSONObject? {
        if(responseCode == 200) {
            if(responseBody?.isNotEmpty() == true) {
                return JSONObject(responseBody)
            }else {
                Log.w(TAG, "Response body is empty")
            }
        }else {
            Log.w(TAG, "Response code not 200: $responseCode")
        }

        return null
    }


}