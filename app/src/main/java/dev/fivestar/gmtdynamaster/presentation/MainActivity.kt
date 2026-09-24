package dev.fivestar.gmtdynamaster.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private val request = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permission = Manifest.permission.ACCESS_COARSE_LOCATION
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            finish()
        } else {
            request.launch(permission)
        }
    }
}