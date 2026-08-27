package com.parallelc.micts.data

object TextRecognitionGatewayFactory {
    fun create(): TextRecognitionGateway = MlKitTextRecognitionGateway()
}
