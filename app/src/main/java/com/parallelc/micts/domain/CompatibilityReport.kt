package com.parallelc.micts.domain

data class CompatibilityReport(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val googleAppInstalled: Boolean,
    val googleAppVersion: String?,
    val activeAssistant: String?,
    val googleIsDefaultAssistant: Boolean,
    val contextualSearchActivityResolvable: Boolean,
    val contextualSearchFeatureDeclared: Boolean,
    val contextualSearchServiceAvailable: Boolean,
    val lensShareAvailable: Boolean,
    val selectedTriggerService: String,
    val captureMode: CaptureMode,
    val fastCaptureApiAvailable: Boolean,
    val fastCaptureEnabled: Boolean,
    val fastCaptureConnected: Boolean,
)
