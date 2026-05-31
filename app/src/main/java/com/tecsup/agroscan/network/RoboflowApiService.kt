package com.tecsup.agroscan.network

import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface RoboflowApiService {
    /**
     * Envía una imagen en base64 para inferencia con YOLOv8 en Roboflow
     */
    @POST("{project}/{version}")
    suspend fun detectDisease(
        @retrofit2.http.Path("project") project: String,
        @retrofit2.http.Path("version") version: Int,
        @Query("api_key") apiKey: String,
        @Body imageBase64: RequestBody
    ): RoboflowResponse

    companion object {
        private const val BASE_URL = "https://detect.roboflow.com/"

        fun create(): RoboflowApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RoboflowApiService::class.java)
        }
    }
}

data class RoboflowResponse(
    val predictions: List<Prediction>,
    val image: ImageInfo
)

data class Prediction(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val confidence: Double,
    @com.google.gson.annotations.SerializedName("class") val className: String
)

data class ImageInfo(
    val width: Int,
    val height: Int
)
