package io.nfls.android.view

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.MimeTypeMap
import io.nfls.android.R
import kotlinx.android.synthetic.main.activity_preview.*
import java.io.File
import android.os.StrictMode



/**
 * Created by hqy on 05/04/2018.
 */
class Preview: AppCompatActivity() {
    private var file: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        file = File(intent.getStringExtra("file"))
        if(file!!.path.endsWith("pdf")) {
            pdfView.fromFile(file).load()
        } else {
            share()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_share, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        share()
        return super.onOptionsItemSelected(item)
    }

    fun share(){
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        val intent = Intent(Intent.ACTION_SEND)
        val uri = Uri.fromFile(file!!)
        var type = ""
        if(uri.scheme.equals(ContentResolver.SCHEME_CONTENT)){
            type = this.contentResolver.getType(uri)
        } else {
            val ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase())
        }
        intent.type = type
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file!!.absolutePath))
        startActivity(Intent.createChooser(intent,"Open with"))
    }
}