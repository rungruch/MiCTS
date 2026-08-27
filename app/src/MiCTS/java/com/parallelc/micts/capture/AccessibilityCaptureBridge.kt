package com.parallelc.micts.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object AccessibilityCaptureBridge {
    private val mutableService = MutableStateFlow<MiCTSAccessibilityCaptureService?>(null)
    val service = mutableService.asStateFlow()

    fun attach(value: MiCTSAccessibilityCaptureService) {
        mutableService.value = value
    }

    fun detach(value: MiCTSAccessibilityCaptureService) {
        if (mutableService.value === value) mutableService.value = null
    }
}
