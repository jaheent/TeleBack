package com.teleback

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.documentfile.provider.DocumentFile
import com.teleback.presentation.terminal.TerminalScreen
import com.teleback.presentation.terminal.TerminalViewModel
import com.teleback.presentation.theme.TeleBackTheme
import com.teleback.presentation.theme.TeleBackThemeId

class MainActivity : ComponentActivity() {

    private val viewModel: TerminalViewModel by viewModels()

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            val docFile = DocumentFile.fromTreeUri(this, uri)
            val folderName = docFile?.name ?: uri.lastPathSegment ?: "TargetFolder"
            viewModel.saveFolder(uri.toString(), folderName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val selectedThemeId by viewModel.selectedThemeId.collectAsState()
            TeleBackTheme(themeId = TeleBackThemeId.fromId(selectedThemeId)) {
                TerminalScreen(
                    onPickFolderClick = {
                        folderPickerLauncher.launch(null)
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}
