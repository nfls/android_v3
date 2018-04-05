package io.nfls.android.view

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import io.nfls.android.R

import kotlinx.android.synthetic.main.content_homepage.*

//import com.aliyun.oss.*
import io.nfls.android.adapter.FileAdapter
import io.nfls.android.adapter.File
import io.nfls.android.provider.OAuth2Provider
import io.nfls.android.provider.SchoolProvider
import kotlinx.android.synthetic.main.activity_homepage.*
import org.apache.commons.io.FileUtils
import java.util.*

class Homepage : AppCompatActivity() {

    private var list = ArrayList<File>()
    private var adapter: FileAdapter? = null
    public var provider: SchoolProvider? = null
    private var isOffline = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)
        val provider = SchoolProvider(applicationContext)
        this.provider = provider
        val dialog = ProgressDialog(this)
        dialog.setTitle("加载中")
        dialog.setMessage("列表加载中，请稍后")
        dialog.setCancelable(false)
        dialog.show()
        provider.getToken {
            refresh()
            dialog.dismiss()
            if (!it) {
                if (provider.all.isEmpty()) {
                    runOnUiThread {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("获取列表失败").setMessage("您尚未完成实名认证，或者未绑定手机，请先在首页的主站上完成相关认证操作！").setPositiveButton("OK", { dialog, value ->
                            dialog.dismiss()
                        })
                        builder.create().show()
                    }
                }
            } else {
                isOffline = false
            }
        }
        provider.updateCheck {
            if (!it) {
                runOnUiThread {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("检测到新版本").setMessage("新版本已发布，请及时更新以使用最新功能！").setPositiveButton("OK", { dialog, value ->
                        dialog.dismiss()
                    })
                    builder.create().show()
                }
            }

        }
        this.pop.isEnabled = false
        this.pop.setOnClickListener {
            removePath()
        }
        this.logout.setOnClickListener {
            getSharedPreferences("io.nfls.android", Context.MODE_PRIVATE).edit().clear().apply()
            FileUtils.deleteDirectory(java.io.File(filesDir.path + "/download"))
            FileUtils.deleteQuietly(java.io.File(filesDir.path + "/list-cache.json"))
            finish()
        }
        this.site.setOnClickListener {
            openInBrowser("https://nfls.io")
        }
        this.flush.setOnClickListener {
            FileUtils.deleteDirectory(java.io.File(filesDir.path + "/download"))
            refresh()
        }
    }


    private fun refresh () {
        provider!!.getFileList()
        runOnUiThread {
            pop.isEnabled = provider!!.path.size != 0
            this.adapter = FileAdapter(provider!!.list, this)
            this.listView.adapter = this.adapter
        }
    }


    public fun addPath (path: String) {
        provider!!.path.add(path)
        refresh()
    }

    public fun download (file: File) {
        val dialog = ProgressDialog(this)
        dialog.setTitle("下载中，请稍后")
        dialog.setMessage(file.filename)
        dialog.isIndeterminate = false
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog.setCancelable(false)
        dialog.show()
        provider!!.download(file, { progress ->
            dialog.progress = (progress * 100).toInt()
        }, { success, downloaded ->
            dialog.dismiss()
            if (success) {
                val intent = Intent(this, Preview::class.java)
                Log.d("PATH", downloaded!!.path)
                intent.putExtra("file", downloaded.path)
                startActivity(intent)
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("下载失败").setMessage("当前可能为离线模式，请在网络正常后关闭后台并重启App再试").setPositiveButton("OK", { dialog, value ->
                    dialog.dismiss()
                })
                builder.create().show()
            }
        })
    }

    public fun removePath () {
        provider!!.path.removeAt(provider!!.path.size - 1)
        refresh()
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

    override fun onBackPressed() {
        if(provider!!.path.size > 0)
            removePath()
    }
}
