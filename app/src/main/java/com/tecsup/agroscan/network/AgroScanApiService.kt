package com.tecsup.agroscan.network

import com.tecsup.agroscan.data.Crop
import com.tecsup.agroscan.data.Field as AgroField
import com.tecsup.agroscan.data.WeatherRecord
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

/**
 * Interfaz de conexión con el Backend de AgroScan (FastAPI).
 * Ajustada estrictamente a la documentación de la API REST.
 */
interface AgroScanApiService {

    // 1. INICIAR SESIÓN
    @FormUrlEncoded
    @POST("api/auth/login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") clave: String
    ): LoginResponse

    // 6. LISTAR CAMPOS
    @GET("api/campos/")
    suspend fun listarCampos(): List<AgroField>

    //7. CREAR CAMPO (Usa Query Params según documentación)
    @POST("api/campos/")
    suspend fun crearCampo(
        @Query("nombre") nombre: String,
        @Query("latitud") lat: Double,
        @Query("longitud") lon: Double,
        @Query("hectareas") has: Double,
        @Query("region") region: String,
        @Query("zona") zona: String
    ): AgroField

    // 11. LISTAR CULTIVOS
    @GET("api/cultivos/")
    suspend fun listarCultivos(): List<Crop>

    // 12. CREAR CULTIVO (Usa Query Params)
    @POST("api/cultivos/")
    suspend fun crearCultivo(
        @Query("tipo_planta") tipo: String,
        @Query("variedad") variedad: String,
        @Query("fecha_siembra") fecha: String, // Formato YYYY-MM-DD
        @Query("campo_id") campoId: Int,
        @Query("temporada") temporada: String
    ): Crop

    // 16. CLIMA ACTUAL
    @GET("api/clima/{campo_id}/actual")
    suspend fun obtenerClimaActual(@Path("campo_id") campoId: Int): WeatherRecord

    // 24. ANALIZAR FOTOGRAFÍA (Usa Multipart/Form-Data)
    @Multipart
    @POST("api/diagnostico/{cultivo_id}/analizar")
    suspend fun analizarFoto(
        @Path("cultivo_id") cultivoId: Int,
        @Part foto: MultipartBody.Part,
        @Part("observacion") observacion: RequestBody?,
        @Part("latitud") lat: RequestBody?,
        @Part("longitud") lon: RequestBody?
    ): AnalysisResponse

    companion object {
        private const val BASE_URL = "https://reactive-sauna-uneaten.ngrok-free.dev/"

        fun create(token: String? = null): AgroScanApiService {
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                requestBuilder.addHeader("ngrok-skip-browser-warning", "true")
                token?.let {
                    requestBuilder.addHeader("Authorization", "Bearer $it")
                }
                chain.proceed(requestBuilder.build())
            }.build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AgroScanApiService::class.java)
        }
    }
}

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val rol: String,
    val nombre: String,
    val empresa_id: Int
)

data class AnalysisResponse(
    val resultado: String,
    val confianza: Double,
    val recomendaciones: List<String>
)
