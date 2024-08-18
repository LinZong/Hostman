package moe.nemesiss.hostman.boost

import androidx.lifecycle.MutableLiveData


fun <T> MutableLiveData<T>.update(block: (T?) -> T?) {
    value = block(value)
}