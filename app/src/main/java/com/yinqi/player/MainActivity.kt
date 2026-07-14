package com.yinqi.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.core.content.ContextCompat
import com.yinqi.player.player.MusicViewModel
import com.yinqi.player.ui.MusicApp

class MainActivity : ComponentActivity() {
    private val viewModel: MusicViewModel by viewModels {
        MusicViewModel.factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
                viewModel::onPermissionResult,
            )
            val folderLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            IntentFlags.readOnly,
                        )
                    }
                    viewModel.useDocumentTree(uri)
                }
            }
            val lyricsFolderLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                if (uri != null) {
                    runCatching { contentResolver.takePersistableUriPermission(uri, IntentFlags.readOnly) }
                    viewModel.useLyricsFolder(uri)
                }
            }

            LaunchedEffect(Unit) {
                val granted = ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_GRANTED
                viewModel.initialize(granted)
            }

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_GRANTED
                if (granted && state.permissionRejected) viewModel.onPermissionResult(true)
            }

            MusicApp(
                state = state,
                viewModel = viewModel,
                onRequestPermission = { permissionLauncher.launch(permission) },
                onPickFolder = { folderLauncher.launch(null) },
                onPickLyricsFolder = { lyricsFolderLauncher.launch(null) },
            )
        }
    }
}

private object IntentFlags {
    const val readOnly = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
}
