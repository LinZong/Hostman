package moe.nemesiss.hostman.model.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PostmortemViewModel : ViewModel() {




    val stackTrace = MutableLiveData<String>()

    val actionPanelMode = MutableLiveData(ActionPanelMode.SIMPLE)

    enum class ActionPanelMode {
        SIMPLE,
        EXPERT
    }
}