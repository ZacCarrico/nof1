package com.nof1.data.repository

import com.nof1.data.api.LlmApiService
import com.nof1.data.model.Hypothesis
import com.nof1.data.model.LlmRequest
import com.nof1.data.model.Message
import com.nof1.data.model.Project
import com.nof1.data.repository.HypothesisRepository
import com.nof1.utils.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class HypothesisGenerationRepository(
    private val secureStorage: SecureStorage,
    private val hypothesisRepository: HypothesisRepository? = null
) {
    
    private val apiService by lazy {
        try {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
            
            Retrofit.Builder()
                .baseUrl(secureStorage.getApiBaseUrl())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LlmApiService::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun generateHypotheses(project: Project, count: Int = 3): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if API service was created successfully
                val service = apiService
                if (service == null) {
                    return@withContext Result.failure(Exception("Failed to initialize API service"))
                }
                
                val apiKey = secureStorage.getOpenAIApiKey()
                if (apiKey.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("OpenAI API key not configured. Please add your API key in Settings."))
                }
                
                val prompt = buildPrompt(project, count)
                val request = LlmRequest(
                    messages = listOf(
                        Message("system", prompt)
                    )
                )
                
                val response = service.generateCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val content = responseBody.choices?.firstOrNull()?.message?.content
                        if (!content.isNullOrBlank()) {
                            val hypotheses = parseHypotheses(content)
                            Result.success(hypotheses)
                        } else {
                            Result.failure(Exception("Empty response content from API"))
                        }
                    } else {
                        Result.failure(Exception("No response body from API"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Result.failure(Exception("API request failed (${response.code()}): $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network error: ${e.message}", e))
            }
        }
    }
    
    suspend fun generateAndSaveHypotheses(project: Project, count: Int = 3): Result<List<Hypothesis>> {
        return withContext(Dispatchers.IO) {
            try {
                val hypothesesResult = generateHypotheses(project, count)
                if (hypothesesResult.isSuccess) {
                    val hypothesesStrings = hypothesesResult.getOrNull() ?: emptyList()
                    val savedHypotheses = mutableListOf<Hypothesis>()
                    
                    for (hypothesisText in hypothesesStrings) {
                        try {
                            val hypothesis = Hypothesis(
                                projectId = project.id,
                                name = if (hypothesisText.length > 50) {
                                    hypothesisText.take(47) + "..."
                                } else {
                                    hypothesisText
                                },
                                description = hypothesisText
                            )
                            
                            hypothesisRepository?.let { repo ->
                                repo.insertHypothesis(hypothesis)
                                savedHypotheses.add(hypothesis)
                            } ?: run {
                                // If no repository, still add to list for return
                                savedHypotheses.add(hypothesis)
                            }
                        } catch (e: Exception) {
                            // Skip this hypothesis but continue with others
                            continue
                        }
                    }
                    
                    Result.success(savedHypotheses)
                } else {
                    Result.failure(hypothesesResult.exceptionOrNull() ?: Exception("Failed to generate hypotheses"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Error saving hypotheses: ${e.message}", e))
            }
        }
    }
    
    private fun buildPrompt(project: Project, count: Int): String {
        return "Generate hypotheses for achieving ${project.goal}, described as ${project.description}"
    }
    
    private fun parseHypotheses(content: String): List<String> {
        return try {
            content.lines()
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { line ->
                    try {
                        when {
                            // Numbered list format: "1. Hypothesis text"
                            line.matches(Regex("^\\d+\\.\\s*.+")) -> {
                                line.replaceFirst(Regex("^\\d+\\.\\s*"), "").trim()
                            }
                            // Bullet point format: "- Hypothesis text" or "• Hypothesis text"
                            line.matches(Regex("^[-•]\\s*.+")) -> {
                                line.replaceFirst(Regex("^[-•]\\s*"), "").trim()
                            }
                            // Plain text but not empty and not instructional
                            line.isNotBlank() && 
                            !line.startsWith("Generate", ignoreCase = true) && 
                            !line.startsWith("Each", ignoreCase = true) &&
                            !line.startsWith("Here", ignoreCase = true) &&
                            !line.startsWith("The following", ignoreCase = true) &&
                            line.length > 10 -> {
                                line
                            }
                            else -> null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                .filter { it.isNotBlank() && it.length > 10 }
                .take(10)
                .toList()
        } catch (e: Exception) {
            // Fallback: return the original content as a single hypothesis
            listOf(content.trim()).filter { it.isNotBlank() }
        }
    }
}