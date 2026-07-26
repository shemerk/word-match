package com.wordmatch.network

import com.wordmatch.model.JsonBinResponse
import retrofit2.http.GET
import retrofit2.http.Path

/** Reads a PUBLIC JSONBin.io bin — no auth header required for public bins. */
interface JsonBinApi {
    @GET("v3/b/{binId}/latest")
    suspend fun getWords(@Path("binId") binId: String): JsonBinResponse
}
