package io.nfls.android.provider

import android.content.Context
import android.util.Log
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.alibaba.sdk.android.oss.*
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.common.auth.OSSAuthCredentialsProvider
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider
import com.alibaba.sdk.android.oss.model.GetObjectRequest
import com.alibaba.sdk.android.oss.model.GetObjectResult
import com.alibaba.sdk.android.oss.model.ListObjectsRequest
import com.alibaba.sdk.android.oss.model.ListObjectsResult
import com.beust.klaxon.*
import com.github.scribejava.core.model.Verb
import io.nfls.android.BuildConfig
import io.nfls.android.Version
import io.nfls.android.adapter.File
import io.nfls.android.model.AbstractResponse
import io.nfls.android.model.PastPaper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.jetbrains.anko.doAsync
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import kotlin.io.*
import kotlin.io.inputStream
import java.lang.StringBuilder
import java.nio.charset.StandardCharsets
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by hqy on 21/03/2018.
 */

//import com.aliyun.oss.*
//import com.github.kittinunf.fuel.httpGet


public class SchoolProvider(private val context: Context) {

    public var list = ArrayList<File>()
    public var path = ArrayList<String>()
    public var all = ArrayList<File>()
    private var client: OSSClient? = null

    private val oauth = OAuth2Provider(context)

    public fun updateCheck(callback: (Boolean) -> Unit) {
        var request = Request.Builder().url("https://nfls.io/device/version?client_id=6At%2BKorhxljWM6LedHfyEg%3D%3D").build()
        doAsync {
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            val version = (Parser().parse(StringBuilder(response.body().string())) as JsonObject).string("data")

            if(Version(BuildConfig.VERSION_NAME).compareTo(Version(version)) == -1){
                callback(false)
            } else {
                callback(true)
            }
        }

    }
    public fun getToken(callback: (Boolean) -> Unit) {
        this.readCache(callback)
        val response = oauth.request("https://nfls.io/school/pastpaper/token", Verb.GET) {
            try {
                val token = (Parser().parse(StringBuilder(it)) as JsonObject).obj("data")
                val credentials = OSSStsTokenCredentialProvider(token!!.string("AccessKeyId"), token.string("AccessKeySecret"), token.string("SecurityToken"))
                val oss = OSSClient(context,"https://oss-cn-shanghai.aliyuncs.com", credentials)
                this.client = oss
                this.all.clear()
                getFileList("", callback)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    private fun getFileList(marker: String, completion: (Boolean) -> Unit) {
        Log.d("Loading", marker)
        val request = ListObjectsRequest("nfls-papers")
        request.maxKeys = 1000
        request.marker = marker
        val callback = object: OSSCompletedCallback<ListObjectsRequest, ListObjectsResult> {
            override fun onSuccess(request: ListObjectsRequest?, result: ListObjectsResult?) {
                val files = result?.objectSummaries
                files?.let {
                    for (i in 0..(it.size - 1)) {
                        val file = it.get(i)
                        all.add(File(file.key,file.lastModified,file.size.toDouble()))
                    }
                    if(it.size != 1000) {
                        writeCache()
                        completion(true)
                        return
                    } else {
                        getFileList(result.nextMarker, completion)
                        return
                    }
                }
                completion(true)
                return
            }

            override fun onFailure(request: ListObjectsRequest?, clientException: ClientException?, serviceException: ServiceException?) {
                completion(false)
            }
        }
        val task = this.client!!.asyncListObjects(request, callback)

    }

    private fun writeCache() {
        val json = JSON.toJSONBytes(all)
        val output = context.openFileOutput("list-cache.json", Context.MODE_PRIVATE)
        output.write(json)
        output.close()
    }

    private fun readCache(completion: (Boolean) -> Unit) {
        try {
            val json = Parser().parse(context.filesDir.path + "/list-cache.json") as JsonArray<JsonObject>
            for (file in json) {
                this.all.add(File(file.string("name")!!,Date(Timestamp(file.long("date")!!).time), file.double("size")!!))
            }
            //this.all = json!!
            completion(false)
        } catch (e:Exception) {
            Log.e("ERROR", e.toString())
        }

    }
    public fun getFileList() {
        if(path.size > 0) {
            this.list = ArrayList(this.all.filter {
                it.name.split("/").count() == path.count() + 1 && it.name.startsWith(path.reduce { acc, s -> acc + "/" + s })
            })
        } else {
            this.list = ArrayList(this.all.filter {
                it.name.split("/").count() == path.count() + 1
            })
        }
    }

    public fun download(file: File, progress: (Double) -> Unit, completion: (Boolean, java.io.File?) -> Unit) {
        val request = GetObjectRequest("nfls-papers", file.name)
        val path = context.filesDir.path + "/download/" + file.name
        val file = java.io.File(path)
        if(file.exists()){
            completion(true, file)
            return
        }
        if(client == null) {
            completion(false, null)
            return
        }


        val callback = object: OSSCompletedCallback<GetObjectRequest, GetObjectResult> {
            override fun onSuccess(request: GetObjectRequest?, result: GetObjectResult?) {
                val stream = result!!.objectContent

                FileUtils.copyInputStreamToFile(stream, file)
                completion(true, file)
            }
            override fun onFailure(request: GetObjectRequest?, clientException: ClientException?, serviceException: ServiceException?) {
                completion(false, null)
            }
        }
        request.setProgressListener { request, currentSize, totalSize ->
            progress(currentSize.toDouble() / totalSize)
        }

        val task = this.client!!.asyncGetObject(request, callback)

    }
}