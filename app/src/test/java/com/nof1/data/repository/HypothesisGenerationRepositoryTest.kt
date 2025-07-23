package com.nof1.data.repository

import com.nof1.data.model.Project
import com.nof1.utils.SecureStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HypothesisGenerationRepository to ensure proper hypothesis generation
 */
class HypothesisGenerationRepositoryTest {

    @Test
    fun `generateHypothesesStrings should return mock data in test mode`() = runTest {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        every { mockSecureStorage.getApiBaseUrl() } returns "test"
        
        val repository = HypothesisGenerationRepository(mockSecureStorage)
        val project = Project(
            id = "test-project",
            name = "Test Project",
            goal = "Test goal"
        )
        
        // When
        val result = repository.generateHypothesesStrings(project, 3)
        
        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val hypotheses = result.getOrNull()
        assertNotNull("Hypotheses should not be null", hypotheses)
        assertEquals("Should return 2 mock hypotheses", 2, hypotheses!!.size)
        assertEquals("First hypothesis should be 'hyp1'", "hyp1", hypotheses[0])
        assertEquals("Second hypothesis should be 'hyp2'", "hyp2", hypotheses[1])
    }
    
    @Test
    fun `generateHypothesesStrings should fail when API key is missing`() = runTest {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        every { mockSecureStorage.getApiBaseUrl() } returns "https://api.openai.com/"
        every { mockSecureStorage.getOpenAIApiKey() } returns null
        
        val repository = HypothesisGenerationRepository(mockSecureStorage)
        val project = Project(
            id = "test-project",
            name = "Test Project", 
            goal = "Test goal"
        )
        
        // When
        val result = repository.generateHypothesesStrings(project, 3)
        
        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)
        assertTrue("Exception message should mention API key", 
            exception!!.message!!.contains("OpenAI API key not configured"))
    }
    
    @Test
    fun `generateHypothesesStrings should fail when API key is blank`() = runTest {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        every { mockSecureStorage.getApiBaseUrl() } returns "https://api.openai.com/"
        every { mockSecureStorage.getOpenAIApiKey() } returns ""
        
        val repository = HypothesisGenerationRepository(mockSecureStorage)
        val project = Project(
            id = "test-project",
            name = "Test Project",
            goal = "Test goal"
        )
        
        // When
        val result = repository.generateHypothesesStrings(project, 3)
        
        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)
        assertTrue("Exception message should mention API key", 
            exception!!.message!!.contains("OpenAI API key not configured"))
    }
    
    @Test
    fun `buildPrompt should create proper prompt from project`() {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        val repository = HypothesisGenerationRepository(mockSecureStorage)
        val project = Project(
            id = "test-project",
            name = "Test Project",
            goal = "Improve user engagement"
        )
        
        // When - using reflection to access private method for testing
        val buildPromptMethod = repository.javaClass.getDeclaredMethod("buildPrompt", Project::class.java, Int::class.java)
        buildPromptMethod.isAccessible = true
        val prompt = buildPromptMethod.invoke(repository, project, 3) as String
        
        // Then
        assertTrue("Prompt should contain project goal", prompt.contains("Improve user engagement"))
    }
    
    @Test
    fun `parseHypotheses should handle numbered list format`() {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        val repository = HypothesisGenerationRepository(mockSecureStorage)
        val content = """
            1. First hypothesis about user behavior
            2. Second hypothesis about engagement
            3. Third hypothesis about retention
        """.trimIndent()
        
        // When - using reflection to access private method for testing
        val parseHypothesesMethod = repository.javaClass.getDeclaredMethod("parseHypotheses", String::class.java)
        parseHypothesesMethod.isAccessible = true
        val hypotheses = parseHypothesesMethod.invoke(repository, content) as List<*>
        
        // Then
        assertEquals("Should parse 3 hypotheses", 3, hypotheses.size)
        assertEquals("First hypothesis should be parsed correctly", 
            "First hypothesis about user behavior", hypotheses[0])
        assertEquals("Second hypothesis should be parsed correctly", 
            "Second hypothesis about engagement", hypotheses[1])
        assertEquals("Third hypothesis should be parsed correctly", 
            "Third hypothesis about retention", hypotheses[2])
    }
    
    @Test
    fun `parseHypotheses should handle bullet point format`() {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        val repository = HypothesisGenerationRepository(mockSecureStorage)
        val content = """
            - First hypothesis with bullet point
            • Second hypothesis with bullet
            - Third hypothesis example
        """.trimIndent()
        
        // When - using reflection to access private method for testing
        val parseHypothesesMethod = repository.javaClass.getDeclaredMethod("parseHypotheses", String::class.java)
        parseHypothesesMethod.isAccessible = true
        val hypotheses = parseHypothesesMethod.invoke(repository, content) as List<*>
        
        // Then
        assertEquals("Should parse 3 hypotheses", 3, hypotheses.size)
        assertEquals("First hypothesis should be parsed correctly", 
            "First hypothesis with bullet point", hypotheses[0])
        assertEquals("Second hypothesis should be parsed correctly", 
            "Second hypothesis with bullet", hypotheses[1])
        assertEquals("Third hypothesis should be parsed correctly", 
            "Third hypothesis example", hypotheses[2])
    }
    
    @Test
    fun `parseHypotheses should filter out instructional text`() {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        val repository = HypothesisGenerationRepository(mockSecureStorage)
        val content = """
            Generate hypotheses for this project:
            1. Valid hypothesis about user behavior
            Here are some suggestions:
            2. Another valid hypothesis
            Each hypothesis should be testable
        """.trimIndent()
        
        // When - using reflection to access private method for testing
        val parseHypothesesMethod = repository.javaClass.getDeclaredMethod("parseHypotheses", String::class.java)
        parseHypothesesMethod.isAccessible = true
        val hypotheses = parseHypothesesMethod.invoke(repository, content) as List<*>
        
        // Then
        assertEquals("Should parse only valid hypotheses", 2, hypotheses.size)
        assertEquals("First hypothesis should be parsed correctly", 
            "Valid hypothesis about user behavior", hypotheses[0])
        assertEquals("Second hypothesis should be parsed correctly", 
            "Another valid hypothesis", hypotheses[1])
    }
    
    @Test
    fun `parseHypotheses should handle empty or invalid content gracefully`() {
        // Given
        val mockSecureStorage = mockk<SecureStorage>()
        val repository = HypothesisGenerationRepository(mockSecureStorage)
        
        // When & Then - empty content
        val parseHypothesesMethod = repository.javaClass.getDeclaredMethod("parseHypotheses", String::class.java)
        parseHypothesesMethod.isAccessible = true
        val emptyResult = parseHypothesesMethod.invoke(repository, "") as List<*>
        assertTrue("Empty content should return empty list", emptyResult.isEmpty())
        
        // When & Then - whitespace only
        val whitespaceResult = parseHypothesesMethod.invoke(repository, "   \n  \t  ") as List<*>
        assertTrue("Whitespace only should return empty list", whitespaceResult.isEmpty())
        
        // When & Then - short lines only
        val shortLinesResult = parseHypothesesMethod.invoke(repository, "short\nlines\nonly") as List<*>
        assertTrue("Short lines should return empty list", shortLinesResult.isEmpty())
    }
}