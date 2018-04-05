package io.nfls.android.model

import com.beust.klaxon.Json
import kotlin.reflect.KClass

/**
 * Created by hqy on 04/04/2018.
 */

class AbstractResponse<T>(val code: Int, val data: T) {
}

class PastPaper(
        @Json(name = "AccessKeyId")
        val accessKeyId: String,
        @Json(name = "AccessKeySecret")
        val accessKeySecret: String,
        @Json(name = "SecurityToken")
        val securityToken: String
) { }