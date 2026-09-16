package com.metmc.os.office

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class MetmcOfficeActivity : Activity() {
    private lateinit var office: MetmcOfficeView

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        office = MetmcOfficeView(this)
        setContentView(office)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 9020 || requestCode == 9021) && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }
                office.openUri(uri, requestCode == 9021)
            }
        }
    }
}
