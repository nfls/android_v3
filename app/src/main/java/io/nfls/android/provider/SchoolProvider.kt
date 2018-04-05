package io.nfls.android.provider

import android.content.Context
import android.util.Log
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
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.github.scribejava.core.model.Verb
import io.nfls.android.adapter.File
import io.nfls.android.model.AbstractResponse
import io.nfls.android.model.PastPaper
import org.apache.commons.io.FileUtils
import java.io.InputStream
import kotlin.io.*
import kotlin.io.inputStream
import java.lang.StringBuilder
import java.util.*

/**
 * Created by hqy on 21/03/2018.
 */

//import com.aliyun.oss.*
//import com.github.kittinunf.fuel.httpGet


public class SchoolProvider(private val context: Context): OSSCompletedCallback<ListObjectsRequest, ListObjectsResult> {

    public var list = ArrayList<File>()
    public var path = ArrayList<String>()
    private var all = ArrayList<File>()
    private var client: OSSClient? = null

    private val oauth = OAuth2Provider(context)

    public fun getToken(callback: () -> Unit) {
        val response = oauth.request("https://nfls.io/school/pastpaper/token", Verb.GET) {
            val token = (Parser().parse(StringBuilder(it)) as JsonObject).obj("data")
            val credentials = OSSStsTokenCredentialProvider(token!!.string("AccessKeyId"), token.string("AccessKeySecret"), token.string("SecurityToken"))
            getClient(credentials, callback)
        }

    }

    public fun getClient(credentials: OSSCredentialProvider, callback: () -> Unit) {
        val oss = OSSClient(context,"https://oss-cn-shanghai.aliyuncs.com", credentials)
        this.client = oss
        val request = ListObjectsRequest("nfls-papers")
        request.maxKeys = 1000
        val task = oss.asyncListObjects(request, this)
        task.waitUntilFinished()
        callback()
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

    override fun onSuccess(request: ListObjectsRequest?, result: ListObjectsResult?) {
        val files = result?.objectSummaries

        files?.let {
            for (i in 0..(files.size - 1)) {

                val file = files.get(i)
                Log.d("File",file.key)
                this.all.add(File(file.key,file.lastModified,file.size.toDouble()))
            }
        }
    }

    override fun onFailure(request: ListObjectsRequest?, clientException: ClientException?, serviceException: ServiceException?) {
        Log.d("Test","Test")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}