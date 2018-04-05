package io.nfls.android.view

import android.content.Intent
import android.os.Bundle
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
import java.util.*

class Homepage : AppCompatActivity() {

    private var list = ArrayList<File>()
    private var adapter: FileAdapter? = null
    public var provider: SchoolProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)
        val provider = SchoolProvider(applicationContext)
        this.provider = provider
        provider.getToken {
            refresh()
        }
        this.pop.setOnClickListener {
            removePath()
        }
        this.logout.setOnClickListener {

        }
        this.verify.setOnClickListener {

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
        provider!!.download(file, { progress ->
            Log.d("Progress", progress.toString())
        }, { success, downloaded ->
            if (success) {
                val intent = Intent(this, Preview::class.java)
                Log.d("PATH", downloaded!!.path)
                intent.putExtra("file", downloaded!!.path)
                startActivity(intent)
            }
        })
    }

    public fun removePath () {
        provider!!.path.removeAt(provider!!.path.size - 1)
        refresh()
    }

}
