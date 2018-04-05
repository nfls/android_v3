package io.nfls.android.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import io.nfls.android.R
import io.nfls.android.provider.OAuth2Provider
import kotlinx.android.synthetic.main.activity_login.*

/**
 * Created by hqy on 03/04/2018.
 */

class Login: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val provider = OAuth2Provider(this)
        runOnUiThread {
            login.text = "正在尝试自动登录"
        }

        this.login.setOnClickListener {
            login.isEnabled = false
            provider.login(this.username.text.toString(), this.password.text.toString(), {
                if (it) {
                    startActivity(Intent(this, Homepage::class.java))
                    login.isEnabled = true
                } else {
                    runOnUiThread {
                        login.isEnabled = true
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("登录失败").setMessage("用户名或密码不正确").setPositiveButton("OK", { dialog, value ->
                            dialog.dismiss()
                        })
                        builder.create().show()
                    }
                }
            })
        }
        this.register.setOnClickListener {
            this.openInBrowser("https://nfls.io/#/user/register")
        }
        this.reset.setOnClickListener {
            this.openInBrowser("https://nfls.io/#/user/reset")
        }
        provider.recover {
            runOnUiThread{
                login.text = "登录"
                if (it) {
                    login.isEnabled = false
                    startActivity(Intent(this, Homepage::class.java))
                    login.isEnabled = true
                }
            }
        }
    }

    public fun openInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setPackage("com.android.chrome")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            runOnUiThread {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("未安装Chrome").setMessage("您的系统未安装Google Chrome浏览器，网页版可能会不兼容。").setPositiveButton("OK", { dialog, value ->
                    dialog.dismiss()
                    intent.setPackage(null)
                    startActivity(intent)
                })
                builder.create().show()
            }
        }
    }
}