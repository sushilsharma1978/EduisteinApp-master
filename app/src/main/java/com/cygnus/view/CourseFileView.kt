package com.cygnus.view

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import co.aspirasoft.view.BaseView
import com.cygnus.R
import com.cygnus.model.CourseFile
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import java.lang.Exception

class CourseFileView : BaseView<CourseFile> {



    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private val mFileView: MaterialButton
    private val postDatehome: MaterialTextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_course_file, this)
        mFileView = findViewById(R.id.fileView)
        postDatehome = findViewById(R.id.postDatehome)
    }

    override fun updateView(model: CourseFile) {

try{
    val extracteddatetime = model.name.substring(model.name.length - 24, model.name.length);
    mFileView.text = model.name.substring(0,model.name.length-24)
    postDatehome.text = extracteddatetime
    Log.e("msg","DATETIMEEE::"+extracteddatetime+","+model.name.substring(model.name.length - 24));



}
catch (e:Exception){
    Log.e("msg","DATETIMEEE11::"+e.toString());

}

    }

    override fun setOnClickListener(l: OnClickListener?) {
        mFileView.setOnClickListener(l)
    }

    fun setStatus(status: FileStatus) {
        mFileView.setIconResource(when (status) {
            FileStatus.Cloud -> R.drawable.ic_cloud
            FileStatus.Local -> R.drawable.ic_downloaded
            FileStatus.Downloading -> R.drawable.ic_download
        })
    }

    enum class FileStatus {
        Cloud,
        Local,
        Downloading
    }

}