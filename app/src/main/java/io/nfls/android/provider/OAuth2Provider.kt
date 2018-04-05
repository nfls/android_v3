package io.nfls.android.provider

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.telecom.Call
import android.util.Log
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.builder.api.DefaultApi20
import com.github.scribejava.core.model.*
import com.github.scribejava.core.oauth.OAuth20Service
import io.nfls.android.view.Login
import org.apache.commons.io.FileUtils
import org.jetbrains.anko.custom.async
import org.jetbrains.anko.doAsync
import java.io.IOException
import java.net.ConnectException
import java.sql.Timestamp
import java.util.*
import kotlin.coroutines.experimental.Continuation


/**
 * Created by hqy on 31/03/2018.
 */
class OAuth2Provider(private val context: Context) {
    private val oauth = ServiceBuilder("6At+KorhxljWM6LedHfyEg==").apiSecret("e9kzxypxX6paJceobbxo/DDhEFnKmtwUr9gY8R2jA9I=").build(NFLSApi())
    public var accessToken: OAuth2AccessToken? = null
    public fun recover(callback: (Boolean) -> Unit) {
        val token = this.context.getSharedPreferences("io.nfls.android", Context.MODE_PRIVATE).getString("refresh_token","")
        //callback(false)
        doAsync {
            try {
                val newToken = oauth.refreshAccessToken(token)
                writeToken(newToken)
                accessToken = newToken
                callback(true)
            } catch (e: IOException) {
                callback(true)
            } catch (e: Exception) {
                context.getSharedPreferences("io.nfls.android", Context.MODE_PRIVATE).edit().remove("refresh_token").apply()
                FileUtils.deleteDirectory(java.io.File(context.filesDir.path + "/download"))
                FileUtils.deleteQuietly(java.io.File(context.filesDir.path + "/list-cache.json"))
                callback(false)
            }

        }
    }
    public fun login(username:String, password:String, callback: (Boolean) -> Unit) {
        doAsync {
            try {
                val token = oauth.getAccessTokenPasswordGrant(username,password)
                writeToken(token)
                accessToken = token
                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    private fun writeToken(token: OAuth2AccessToken) {
        val editor = context.getSharedPreferences("io.nfls.android", Context.MODE_PRIVATE).edit()
        editor.putString("refresh_token", token.refreshToken)
        editor.putString("access_token", token.accessToken)
        editor.putLong("expiration", System.currentTimeMillis() + token.expiresIn * 1000)
        editor.apply()
    }

    private fun getToken(): OAuth2AccessToken {
        return OAuth2AccessToken(context.getSharedPreferences("io.nfls.android", Context.MODE_PRIVATE).getString("access_token",""))
    }

    public fun request(url: String, method: Verb, callback: (String) -> Unit) {
        if (accessToken == null) {
            accessToken = this.getToken()
        }
        doAsync {
            val request = OAuthRequest(method, url)
            oauth.signRequest(accessToken, request)
            val response = oauth.execute(request)
            callback(response.body)
        }

    }
}


class NFLSApi: DefaultApi20() {
    override fun getAuthorizationBaseUrl(): String {
        return "https://nfls.io/oauth/authorize"
    }

    override fun getAccessTokenEndpoint(): String {
        return "https://nfls.io/oauth/accessToken"
    }
}