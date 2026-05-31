package com.tecsup.agroscan.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "es"
    ): WeatherResponse

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/"

        fun create(): WeatherApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherApiService::class.java)
        }
    }
}

data class WeatherResponse(
    val main: MainData,
    val wind: WindData,
    val weather: List<WeatherDescription>,
    val name: String
)

data class MainData(
    val temp: Double,
    val humidity: Int
)

data class WindData(
    val speed: Double
)

data class WeatherDescription(
    val description: String,
    val icon: String
)
