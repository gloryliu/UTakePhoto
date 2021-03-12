package com.lzr.utakephoto

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.lzr.takephoto.manager.TakePhotoResult
import com.lzr.takephoto.manager.UTakePhoto

class MainActivity : AppCompatActivity() {

    private lateinit var button1:Button;
    private lateinit var button2:Button;
    private lateinit var image:ImageView;
    private lateinit var imageCamera:ImageView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button1 = findViewById(R.id.button1)
        button2 = findViewById(R.id.button2)
        image = findViewById(R.id.image)
        imageCamera = findViewById(R.id.imageCamera)

        button1.setOnClickListener {
            UTakePhoto.with(this).openCamera().request(object : TakePhotoResult{
                override fun takeSuccess(filePath: String) {
                    var bitmap = BitmapFactory.decodeFile(filePath)
                    imageCamera.setImageBitmap(bitmap)
                }

                override fun takeFailure(ex: Exception) {
                    Toast.makeText(applicationContext,ex.message,Toast.LENGTH_LONG)
                }

                override fun takeCancel() {
                    Toast.makeText(applicationContext,"取消",Toast.LENGTH_LONG)
                }

            })
        }

        button2.setOnClickListener {
            UTakePhoto.with(this).openAlbum().request(object : TakePhotoResult{
                override fun takeSuccess(filePath: String) {
                    var bitmap = BitmapFactory.decodeFile(filePath)
                    image.setImageBitmap(bitmap)
                }

                override fun takeFailure(ex: Exception) {
                    Toast.makeText(applicationContext,ex.message,Toast.LENGTH_LONG)
                }

                override fun takeCancel() {
                    Toast.makeText(applicationContext,"取消",Toast.LENGTH_LONG)
                }

            })
        }
    }
}