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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HypothesisGenerationRepository(
    private val secureStorage: SecureStorage,
    private val hypothesisRepository: HypothesisRepository? = null
) {
    
    private val apiService = Retrofit.Builder()
        .baseUrl(secureStorage.getApiBaseUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LlmApiService::class.java)
    
    suspend fun generateHypotheses(project: Project, count: Int = 3): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
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
                
                val response = apiService.generateCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
                
                if (response.isSuccessful) {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    if (content != null) {
                        val hypotheses = parseHypotheses(content)
                        Result.success(hypotheses)
                    } else {
                        Result.failure(Exception("No response content"))
                    }
                } else {
                    Result.failure(Exception("API request failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
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
                        val hypothesis = Hypothesis(
                            projectId = project.id,
                            name = hypothesisText.take(50) + if (hypothesisText.length > 50) "..." else "",
                            description = hypothesisText
                        )
                        
                        hypothesisRepository?.let { repo ->
                            repo.insertHypothesis(hypothesis)
                            savedHypotheses.add(hypothesis)
                        }
                    }
                    
                    Result.success(savedHypotheses)
                } else {
                    Result.failure(hypothesesResult.exceptionOrNull() ?: Exception("Failed to generate hypotheses"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun buildPrompt(project: Project, count: Int): String {
        return "Generate hypotheses for achieving ${project.goal}, described as ${project.description}"
    }
    
    private fun parseHypotheses(content: String): List<String> {
        return content.lines()
            .filter { it.trim().isNotEmpty() }
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.matches(Regex("^\\d+\\.\\s*.*"))) {
                    trimmed.replaceFirst(Regex("^\\d+\\.\\s*"), "")
                } else if (trimmed.isNotBlank() && !trimmed.startsWith("Generate") && !trimmed.startsWith("Each")) {
                    trimmed
                } else {
                    null
                }
            }
            .take(10)
    }
}