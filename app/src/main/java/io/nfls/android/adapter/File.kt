package io.nfls.android.adapter

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import io.nfls.android.R
import io.nfls.android.view.Homepage
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by hqy on 25/03/2018.
 */
public class File {
    public val id: Long
    public val filename: String
    public val name: String
    public val date: Date
    public val size: Double

    constructor(name: String, date: Date, size: Double) {
        this.id = 0
        if(name.endsWith("/")){
            this.name = name.dropLast(1)
            this.filename = name.substring(name.dropLast(1).lastIndexOf("/")+1).dropLast(1)
        } else {
            this.name = name
            this.filename = name.substring(name.lastIndexOf("/")+1)
        }

        this.date = date
        this.size = size
    }
}


public class FileAdapter(var list: ArrayList<File>, var context: Context): BaseAdapter() {

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        var v: View
        var holder: FileViewHolder
        if (p1 == null) {
            v = View.inflate(context, R.layout.cell, null)
            holder = FileViewHolder(v)
            v.tag = holder
        } else {
            v = p1
            holder = v.tag as FileViewHolder
        }
        holder.button.text = list[p0].filename
        holder.button.setOnClickListener {
            if(list[p0].size == 0.0)
                (context as Homepage).addPath(list[p0].filename)
            else
                (context as Homepage).download(list[p0])
        }
        holder.text.text = promptCalc(list[p0])
        return v
    }

    override fun getItem(p0: Int): Any {
        return list.get(p0)
    }

    override fun getItemId(p0: Int): Long {
        return list.get(p0).id
    }

    override fun getCount(): Int {
        return list.size
    }

    private fun promptCalc(file: File): String {
        var string = ""
        if(file.size == 0.0) {
            string = "文件夹"
        } else {
            if(isCached(file))
                string = "已缓存 - "
            else
                string = sizeCalc(file.size) + " - "
            val df = SimpleDateFormat("yyyy/MM/DD HH:mm:ss")
            string += df.format(file.date)
        }
        return string
    }

    private fun sizeCalc(raw: Double): String {
        var size = raw / 1024
        var count = 0
        while (size > 1024) {
            size = size / 1024
            count ++
        }
        var quantity = ""
        when(count) {
            0 -> quantity = "KB"
            1 -> quantity = "MB"
            2 -> quantity = "GB"
        }
        return String.format("%.1f", size) + " " + quantity
    }

    private fun isCached(file: File): Boolean{
        val path = context.filesDir.path + "/download/" + file.name
        val file = java.io.File(path)
        return file.exists()
    }
}

class FileViewHolder(var viewItem: View) {
    var button: Button = viewItem.findViewById<TextView>(R.id.button) as Button
    var text: TextView = viewItem.findViewById<TextView>(R.id.text) as TextView
}