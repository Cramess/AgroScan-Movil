package com.tecsup.agroscan.network

import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path

interface RoboflowApiService {
    /**
     * Envía una imagen para inferencia con el modelo Serverless de Roboflow
     */
    @POST("{project}/{version}")
    suspend fun detectDisease(
        @Path("project") project: String,
        @Path("version") version: String,
        @Query("api_key") apiKey: String,
        @Body imageBase64: RequestBody
    ): RoboflowResponse

    companion object {
        private const val BASE_URL = "https://serverless.roboflow.com/"

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
    val predictions: List<Prediction>
)

data class Prediction(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val confidence: Double,
    @com.google.gson.annotations.SerializedName("class") val className: String,
    val image_path: String? = null
)
