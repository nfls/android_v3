package io.nfls.android.view

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import io.nfls.android.R
import kotlinx.android.synthetic.main.activity_preview.*
import java.io.File

/**
 * Created by hqy on 05/04/2018.
 */
class Preview: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        val file = File(intent.getStringExtra("file"))
        //Log.d("FILE", file.path)
        pdfView.fromFile(file).load()
    }
}