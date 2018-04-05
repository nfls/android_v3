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
import org.jetbrains.anko.custom.async
import org.jetbrains.anko.doAsync
import java.sql.Timestamp
import java.util.*
import kotlin.coroutines.experimental.Continuation


/**
 * Created by hqy on 31/03/2018.
 */
class OAuth2Provider(private val context: Context) {
    private val oauth = ServiceBuilder("9J/xuPUoNBOmA1erNKlBqQ==").apiSecret("REGbItx41b4IYcK3PiPTXsWTh9KIA0vcHl/W4ediSEg=").build(NFLSApi())
    public var accessToken: OAuth2AccessToken? = null
    public fun recover(callback: (Boolean) -> Unit) {
        val token = this.context.getSharedPreferences("io.nfls.android", Context.MODE_PRIVATE).getString("refresh_token","")
        doAsync {
            try {
                val newToken = oauth.refreshAccessToken(token)
                writeToken(newToken)
                accessToken = newToken
                callback(true)
            } catch (e: Exception) {
                context.getSharedPreferences("io.nfls.android", Context.MODE_PRIVATE).edit().remove("refresh_token").apply()
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
            Log.d("OAUTH", accessToken!!.accessToken)
            oauth.signRequest(accessToken, request)
            val response = oauth.execute(request)
            Log.d("Response", response.body)
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