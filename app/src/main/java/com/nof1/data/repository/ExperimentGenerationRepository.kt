package com.nof1.data.repository

import com.nof1.data.api.LlmApiService
import com.nof1.data.model.Experiment
import com.nof1.data.model.Hypothesis
import com.nof1.data.model.LlmRequest
import com.nof1.data.model.Message
import com.nof1.data.repository.ExperimentRepository
import com.nof1.utils.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ExperimentGenerationRepository(
    private val secureStorage: SecureStorage,
    private val experimentRepository: ExperimentRepository? = null
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
    
    suspend fun generateExperimentsStrings(hypothesis: Hypothesis, count: Int = 3): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                // Check for test mode
                val baseUrl = secureStorage.getApiBaseUrl()
                if (baseUrl.equals("test", ignoreCase = true)) {
                    // Simulate a small delay for realistic test experience
                    kotlinx.coroutines.delay(1000)
                    return@withContext Result.success(listOf("exp1", "exp2", "exp3"))
                }
                
                // Check if API service was created successfully
                val service = apiService
                if (service == null) {
                    return@withContext Result.failure(Exception("Failed to initialize API service"))
                }
                
                val apiKey = secureStorage.getOpenAIApiKey()
                if (apiKey.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("OpenAI API key not configured. Please add your API key in Settings."))
                }
                
                val prompt = buildPrompt(hypothesis, count)
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
                            val experiments = parseExperiments(content)
                            Result.success(experiments)
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
    
    suspend fun generateAndSaveExperiments(hypothesis: Hypothesis, count: Int = 3): Result<List<Experiment>> {
        return withContext(Dispatchers.IO) {
            try {
                val experimentsResult = generateExperimentsStrings(hypothesis, count)
                if (experimentsResult.isSuccess) {
                    val experimentsStrings = experimentsResult.getOrNull() ?: emptyList()
                    val savedExperiments = mutableListOf<Experiment>()
                    
                    for (experimentText in experimentsStrings) {
                        try {
                            val experiment = Experiment(
                                hypothesisId = hypothesis.id,
                                projectId = hypothesis.projectId,
                                name = if (experimentText.length > 50) {
                                    experimentText.take(47) + "..."
                                } else {
                                    experimentText
                                },
                                description = experimentText,
                                question = ""
                            )
                            
                            experimentRepository?.let { repo ->
                                val id = repo.insertExperiment(experiment)
                                savedExperiments.add(experiment)
                            } ?: run {
                                // If no repository, still add to list for return
                                savedExperiments.add(experiment)
                            }
                        } catch (e: Exception) {
                            // Skip this experiment but continue with others
                            continue
                        }
                    }
                    
                    Result.success(savedExperiments)
                } else {
                    Result.failure(experimentsResult.exceptionOrNull() ?: Exception("Failed to generate experiments"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Error saving experiments: ${e.message}", e))
            }
        }
    }
    
    suspend fun generateExperiments(hypothesis: Hypothesis, count: Int = 3): Result<List<Experiment>> {
        return withContext(Dispatchers.IO) {
            try {
                val experimentsResult = generateExperimentsStrings(hypothesis, count)
                if (experimentsResult.isSuccess) {
                    val experimentsStrings = experimentsResult.getOrNull() ?: emptyList()
                    val experiments = experimentsStrings.map { experimentText ->
                        Experiment(
                            hypothesisId = hypothesis.id,
                            projectId = hypothesis.projectId,
                            name = if (experimentText.length > 50) {
                                experimentText.take(47) + "..."
                            } else {
                                experimentText
                            },
                            description = experimentText,
                            question = ""
                        )
                    }
                    Result.success(experiments)
                } else {
                    Result.failure(experimentsResult.exceptionOrNull() ?: Exception("Failed to generate experiments"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Error generating experiments: ${e.message}", e))
            }
        }
    }
    
    suspend fun saveExperiments(experiments: List<Experiment>): Result<List<Experiment>> {
        return withContext(Dispatchers.IO) {
            try {
                val savedExperiments = mutableListOf<Experiment>()
                for (experiment in experiments) {
                    try {
                        experimentRepository?.let { repo ->
                            val id = repo.insertExperiment(experiment)
                            savedExperiments.add(experiment)
                        } ?: run {
                            savedExperiments.add(experiment)
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
                Result.success(savedExperiments)
            } catch (e: Exception) {
                Result.failure(Exception("Error saving experiments: ${e.message}", e))
            }
        }
    }
    
    private fun buildPrompt(hypothesis: Hypothesis, count: Int): String {
        return "Generate $count self-experiment ideas to test the hypothesis: \"${hypothesis.name}\". Description: ${hypothesis.description}. Each experiment should be a specific, actionable test for self-experimentation that could provide evidence for or against this hypothesis. The experiments should use only resources and methods that an average person can easily access without special equipment, expertise, or significant expense."
    }
    
    private fun parseExperiments(content: String): List<String> {
        return try {
            content.lines()
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { line ->
                    try {
                        when {
                            // Numbered list format: "1. Experiment text"
                            line.matches(Regex("^\\d+\\.\\s*.+")) -> {
                                line.replaceFirst(Regex("^\\d+\\.\\s*"), "").trim()
                            }
                            // Bullet point format: "- Experiment text" or "• Experiment text"
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
            // Fallback: return the original content as a single experiment
            listOf(content.trim()).filter { it.isNotBlank() }
        }
    }
}