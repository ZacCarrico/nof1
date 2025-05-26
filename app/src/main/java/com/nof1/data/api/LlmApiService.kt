package com.nof1.data.api

import com.nof1.data.model.LlmRequest
import com.nof1.data.model.LlmResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface LlmApiService {
    @POST("v1/chat/completions")
    suspend fun generateCompletion(
        @Header("Authorization") authorization: String,
        @Body request: LlmRequest
    ): Response<LlmResponse>
}