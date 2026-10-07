package com.dot.gallery.feature.viewer

import androidx.lifecycle.ViewModel
import com.dot.gallery.core.MediaDistributor
import me.tatarka.inject.annotations.Inject



class ActionsViewModel @Inject constructor(
    private val distributor: MediaDistributor
): ViewModel() {

}