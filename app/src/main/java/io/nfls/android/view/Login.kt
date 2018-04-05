package io.nfls.android.view

import android.content.Intent
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
        provider.recover {
            if (!it) {
                this.login.setOnClickListener {
                    provider.login(this.username.text.toString(), this.password.text.toString(), {
                        if (it) {
                            startActivity(Intent(this, Homepage::class.java))
                        } else {
                            runOnUiThread {
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle("登录失败").setMessage("用户名或密码不正确").setPositiveButton("OK", { dialog, value ->
                                    dialog.dismiss()
                                })
                                builder.create().show()
                            }
                        }
                    })
                }
            } else {
                startActivity(Intent(this, Homepage::class.java))
            }
        }

    }
}