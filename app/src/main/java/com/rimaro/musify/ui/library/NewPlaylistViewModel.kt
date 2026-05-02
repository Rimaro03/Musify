package com.rimaro.musify.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.util.ImportPlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewPlaylistViewModel @Inject constructor(
    application: Application,
    private val importPlaylist: ImportPlaylist
) : AndroidViewModel(application) {
    val importState = MutableStateFlow<ImportResult?>(null)

    fun importFromCsv(uri: Uri) {
        viewModelScope.launch {
            importPlaylist.importFromCsv(uri)
                .collect { result ->
                    importState.value = result
                }
        }
    }
}
