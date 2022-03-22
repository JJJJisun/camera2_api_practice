package com.jjjjisun.camera2_api

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.jjjjisun.camera2_api.databinding.ActivityLaunchBinding
import java.util.ArrayList

class LaunchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLaunchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaunchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TedPermission.with(this)
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    startActivity(Intent(this@LaunchActivity, MainActivity::class.java))
                    finish()
                }

                override fun onPermissionDenied(deniedPermissions: ArrayList<String>?) {
                    for (i in deniedPermissions!!)
                        i.showErrLog()
                }

            })
            .setDeniedMessage("앱을 실행하려면 권한을 허가하셔야합니다.")
            .setPermissions(Manifest.permission.CAMERA)
            .check()
    }
}

private fun String.showErrLog() {
    Log.d("tag", this)
}
