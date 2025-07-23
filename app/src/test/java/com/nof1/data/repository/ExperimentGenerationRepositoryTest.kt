package com.nof1.data.repository

import com.nof1.data.model.Hypothesis
import com.nof1.utils.SecureStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ExperimentGenerationRepository to ensure proper experiment generation
 */
class ExperimentGenerationRepositoryTest {

    @Test
    fun `generateExperimentsStrings should return mock data in test mode`() = runTest {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        every { mockSecureStorage.getApiBaseUrl() } returns "test"
        
        val repository = ExperimentGenerationRepository(mockSecureStorage)
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test hypothesis description"
        )
        
        // When
        val result = repository.generateExperimentsStrings(hypothesis, 3)
        
        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val experiments = result.getOrNull()
        assertNotNull("Experiments should not be null", experiments)
        assertEquals("Should return 3 mock experiments", 3, experiments!!.size)
        assertEquals("First experiment should be 'exp1'", "exp1", experiments[0])
        assertEquals("Second experiment should be 'exp2'", "exp2", experiments[1])
        assertEquals("Third experiment should be 'exp3'", "exp3", experiments[2])
    }
    
    @Test
    fun `generateExperimentsStrings should fail when API key is missing`() = runTest {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        every { mockSecureStorage.getApiBaseUrl() } returns "https://api.openai.com/"
        every { mockSecureStorage.getOpenAIApiKey() } returns null
        
        val repository = ExperimentGenerationRepository(mockSecureStorage)
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test hypothesis description"
        )
        
        // When
        val result = repository.generateExperimentsStrings(hypothesis, 3)
        
        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)
        assertTrue("Exception message should mention API key", 
            exception!!.message!!.contains("OpenAI API key not configured"))
    }
    
    @Test
    fun `generateExperimentsStrings should fail when API key is blank`() = runTest {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        every { mockSecureStorage.getApiBaseUrl() } returns "https://api.openai.com/"
        every { mockSecureStorage.getOpenAIApiKey() } returns ""
        
        val repository = ExperimentGenerationRepository(mockSecureStorage)
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test hypothesis description"
        )
        
        // When
        val result = repository.generateExperimentsStrings(hypothesis, 3)
        
        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)
        assertTrue("Exception message should mention API key", 
            exception!!.message!!.contains("OpenAI API key not configured"))
    }
    
    @Test
    fun `buildPrompt should create proper prompt from hypothesis`() {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        val repository = ExperimentGenerationRepository(mockSecureStorage)
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project",
            name = "Users prefer dark mode",
            description = "Users will engage more with dark mode interface"
        )
        
        // When - using reflection to access private method for testing
        val buildPromptMethod = repository.javaClass.getDeclaredMethod("buildPrompt", Hypothesis::class.java, Int::class.java)
        buildPromptMethod.isAccessible = true
        val prompt = buildPromptMethod.invoke(repository, hypothesis, 3) as String
        
        // Then
        assertTrue("Prompt should contain hypothesis name", prompt.contains("Users prefer dark mode"))
        assertTrue("Prompt should contain hypothesis description", prompt.contains("Users will engage more with dark mode interface"))
        assertTrue("Prompt should mention experiment count", prompt.contains("3"))
        assertTrue("Prompt should mention testing", prompt.contains("test"))
    }
    
    @Test 
    fun `parseExperiments should handle numbered list format`() {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        val repository = ExperimentGenerationRepository(mockSecureStorage)
        val content = """
            1. A/B test dark mode vs light mode preferences
            2. Survey users about interface preferences
            3. Track engagement metrics with different themes
        """.trimIndent()
        
        // When - using reflection to access private method for testing
        val parseExperimentsMethod = repository.javaClass.getDeclaredMethod("parseExperiments", String::class.java)
        parseExperimentsMethod.isAccessible = true
        val experiments = parseExperimentsMethod.invoke(repository, content) as List<*>
        
        // Then
        assertEquals("Should parse 3 experiments", 3, experiments.size)
        assertEquals("First experiment should be parsed correctly", 
            "A/B test dark mode vs light mode preferences", experiments[0])
        assertEquals("Second experiment should be parsed correctly", 
            "Survey users about interface preferences", experiments[1])
        assertEquals("Third experiment should be parsed correctly", 
            "Track engagement metrics with different themes", experiments[2])
    }
    
    @Test
    fun `parseExperiments should handle bullet point format`() {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        val repository = ExperimentGenerationRepository(mockSecureStorage)
        val content = """
            - Conduct user interviews about preferences
            • Analyze usage data for interface patterns
            - Run controlled experiment with theme switching
        """.trimIndent()
        
        // When - using reflection to access private method for testing
        val parseExperimentsMethod = repository.javaClass.getDeclaredMethod("parseExperiments", String::class.java)
        parseExperimentsMethod.isAccessible = true
        val experiments = parseExperimentsMethod.invoke(repository, content) as List<*>
        
        // Then
        assertEquals("Should parse 3 experiments", 3, experiments.size)
        assertEquals("First experiment should be parsed correctly", 
            "Conduct user interviews about preferences", experiments[0])
        assertEquals("Second experiment should be parsed correctly", 
            "Analyze usage data for interface patterns", experiments[1])
        assertEquals("Third experiment should be parsed correctly", 
            "Run controlled experiment with theme switching", experiments[2])
    }
    
    @Test
    fun `parseExperiments should filter out instructional text`() {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        val repository = ExperimentGenerationRepository(mockSecureStorage)
        val content = """
            Generate experiment ideas:
            1. Valid experiment with user testing
            Here are some suggestions:
            2. Another valid experiment design
            Each experiment should be measurable
        """.trimIndent()
        
        // When - using reflection to access private method for testing
        val parseExperimentsMethod = repository.javaClass.getDeclaredMethod("parseExperiments", String::class.java)
        parseExperimentsMethod.isAccessible = true
        val experiments = parseExperimentsMethod.invoke(repository, content) as List<*>
        
        // Then
        assertEquals("Should parse only valid experiments", 2, experiments.size)
        assertEquals("First experiment should be parsed correctly", 
            "Valid experiment with user testing", experiments[0])
        assertEquals("Second experiment should be parsed correctly", 
            "Another valid experiment design", experiments[1])
    }
    
    @Test
    fun `parseExperiments should handle empty or invalid content gracefully`() {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        val repository = ExperimentGenerationRepository(mockSecureStorage)
        
        // When & Then - empty content
        val parseExperimentsMethod = repository.javaClass.getDeclaredMethod("parseExperiments", String::class.java)
        parseExperimentsMethod.isAccessible = true
        val emptyResult = parseExperimentsMethod.invoke(repository, "") as List<*>
        assertTrue("Empty content should return empty list", emptyResult.isEmpty())
        
        // When & Then - whitespace only
        val whitespaceResult = parseExperimentsMethod.invoke(repository, "   \n  \t  ") as List<*>
        assertTrue("Whitespace only should return empty list", whitespaceResult.isEmpty())
        
        // When & Then - short lines only
        val shortLinesResult = parseExperimentsMethod.invoke(repository, "short\nlines\nonly") as List<*>
        assertTrue("Short lines should return empty list", shortLinesResult.isEmpty())
    }
    
    @Test
    fun `generateExperiments should create proper Experiment objects`() = runTest {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        every { mockSecureStorage.getApiBaseUrl() } returns "test"
        
        val repository = ExperimentGenerationRepository(mockSecureStorage)
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project", 
            name = "Test Hypothesis",
            description = "Test hypothesis description"
        )
        
        // When
        val result = repository.generateExperiments(hypothesis, 3)
        
        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val experiments = result.getOrNull()
        assertNotNull("Experiments should not be null", experiments)
        assertEquals("Should return 3 experiments", 3, experiments!!.size)
        
        experiments.forEach { experiment ->
            assertEquals("Hypothesis ID should match", "test-hypothesis", experiment.hypothesisId)
            assertEquals("Project ID should match", "test-project", experiment.projectId)
            assertTrue("Name should not be empty", experiment.name.isNotEmpty())
            assertTrue("Description should not be empty", experiment.description.isNotEmpty())
            assertEquals("Question should be empty", "", experiment.question)
        }
    }
    
    @Test
    fun `generateExperiments should handle long experiment names properly`() = runTest {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        every { mockSecureStorage.getApiBaseUrl() } returns "test"
        
        // Override the mock response for this test to return long experiment text
        val repository = object : ExperimentGenerationRepository(mockSecureStorage) {
            override suspend fun generateExperimentsStrings(hypothesis: Hypothesis, count: Int): Result<List<String>> {
                val longExperiment = "This is a very long experiment description that exceeds fifty characters and should be truncated"
                return Result.success(listOf(longExperiment))
            }
        }
        
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project",
            name = "Test Hypothesis", 
            description = "Test hypothesis description"
        )
        
        // When
        val result = repository.generateExperiments(hypothesis, 1)
        
        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val experiments = result.getOrNull()
        assertNotNull("Experiments should not be null", experiments)
        assertEquals("Should return 1 experiment", 1, experiments!!.size)
        
        val experiment = experiments[0]
        assertTrue("Name should be truncated to 50 characters", experiment.name.length <= 50)
        assertTrue("Name should end with '...' when truncated", experiment.name.endsWith("..."))
    }
}