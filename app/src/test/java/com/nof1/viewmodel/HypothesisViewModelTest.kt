package com.nof1.viewmodel

import com.nof1.data.model.Hypothesis
import com.nof1.data.model.Project
import com.nof1.data.repository.HypothesisGenerationRepository
import com.nof1.data.repository.HypothesisRepositoryInterface
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for HypothesisViewModel to ensure proper state management and generation
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HypothesisViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var mockRepository: HypothesisRepositoryInterface
    private lateinit var mockGenerationRepository: HypothesisGenerationRepository
    private lateinit var viewModel: HypothesisViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)
        mockGenerationRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generateHypotheses should update state flows correctly on success`() = runTest {
        // Given
        val project = Project(id = "test-project", name = "Test Project", goal = "Test goal")
        val mockHypotheses = listOf("hypothesis1", "hypothesis2", "hypothesis3")
        
        coEvery { mockGenerationRepository.generateHypothesesStrings(project) } returns Result.success(mockHypotheses)
        
        viewModel = HypothesisViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.generateHypotheses(project)
        testScheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Generated hypotheses should be set", mockHypotheses, viewModel.generatedHypotheses.first())
        assertEquals("IsGenerating should be false after completion", false, viewModel.isGenerating.first())
        assertEquals("Generation error should be null on success", null, viewModel.generationError.first())
    }
    
    @Test
    fun `generateHypotheses should update state flows correctly on failure`() = runTest {
        // Given
        val project = Project(id = "test-project", name = "Test Project", goal = "Test goal")
        val errorMessage = "API call failed"
        val exception = Exception(errorMessage)
        
        coEvery { mockGenerationRepository.generateHypothesesStrings(project) } returns Result.failure(exception)
        
        viewModel = HypothesisViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.generateHypotheses(project)
        testScheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Generated hypotheses should be empty on failure", emptyList<String>(), viewModel.generatedHypotheses.first())
        assertEquals("IsGenerating should be false after completion", false, viewModel.isGenerating.first())
        assertEquals("Generation error should contain error message", errorMessage, viewModel.generationError.first())
    }
    
    @Test
    fun `generateHypotheses should set isGenerating to true during operation`() = runTest {
        // Given
        val project = Project(id = "test-project", name = "Test Project", goal = "Test goal")
        val mockHypotheses = listOf("hypothesis1", "hypothesis2")
        
        coEvery { mockGenerationRepository.generateHypothesesStrings(project) } returns Result.success(mockHypotheses)
        
        viewModel = HypothesisViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.generateHypotheses(project)
        
        // Then - check isGenerating is true before advancing scheduler
        assertEquals("IsGenerating should be true during operation", true, viewModel.isGenerating.first())
        
        // Complete the operation
        testScheduler.advanceUntilIdle()
        assertEquals("IsGenerating should be false after completion", false, viewModel.isGenerating.first())
    }
    
    @Test
    fun `generateHypotheses should handle exception during generation`() = runTest {
        // Given
        val project = Project(id = "test-project", name = "Test Project", goal = "Test goal")
        val exceptionMessage = "Unexpected error occurred"
        
        coEvery { mockGenerationRepository.generateHypothesesStrings(project) } throws RuntimeException(exceptionMessage)
        
        viewModel = HypothesisViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.generateHypotheses(project)
        testScheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Generated hypotheses should be empty on exception", emptyList<String>(), viewModel.generatedHypotheses.first())
        assertEquals("IsGenerating should be false after exception", false, viewModel.isGenerating.first())
        assertTrue("Generation error should contain exception message", 
            viewModel.generationError.first()!!.contains("Unexpected error: $exceptionMessage"))
    }
    
    @Test
    fun `generateHypotheses should set error when generation repository is null`() = runTest {
        // Given
        val project = Project(id = "test-project", name = "Test Project", goal = "Test goal")
        viewModel = HypothesisViewModel(mockRepository, null)
        
        // When
        viewModel.generateHypotheses(project)
        testScheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Generated hypotheses should be empty when repository is null", 
            emptyList<String>(), viewModel.generatedHypotheses.first())
        assertEquals("IsGenerating should be false when repository is null", 
            false, viewModel.isGenerating.first())
        assertEquals("Generation error should indicate configuration issue", 
            "Hypothesis generation not configured", viewModel.generationError.first())
    }
    
    @Test
    fun `clearGeneratedHypotheses should reset state`() = runTest {
        // Given
        val project = Project(id = "test-project", name = "Test Project", goal = "Test goal")
        val mockHypotheses = listOf("hypothesis1", "hypothesis2")
        
        coEvery { mockGenerationRepository.generateHypothesesStrings(project) } returns Result.success(mockHypotheses)
        
        viewModel = HypothesisViewModel(mockRepository, mockGenerationRepository)
        
        // Generate some hypotheses first
        viewModel.generateHypotheses(project)
        testScheduler.advanceUntilIdle()
        
        // Verify hypotheses were generated
        assertEquals("Generated hypotheses should be set", mockHypotheses, viewModel.generatedHypotheses.first())
        
        // When
        viewModel.clearGeneratedHypotheses()
        
        // Then
        assertEquals("Generated hypotheses should be empty after clear", 
            emptyList<String>(), viewModel.generatedHypotheses.first())
        assertEquals("Generation error should be null after clear", 
            null, viewModel.generationError.first())
    }
    
    @Test
    fun `insertHypothesis should call repository insertHypothesis`() = runTest {
        // Given
        val hypothesis = Hypothesis(
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test description"
        )
        
        viewModel = HypothesisViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.insertHypothesis(hypothesis)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify { mockRepository.insertHypothesis(hypothesis) }
    }
    
    @Test
    fun `updateHypothesis should call repository updateHypothesis`() = runTest {
        // Given
        val hypothesis = Hypothesis(
            id = "test-id",
            projectId = "test-project",
            name = "Updated Hypothesis",
            description = "Updated description"
        )
        
        viewModel = HypothesisViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.updateHypothesis(hypothesis)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify { mockRepository.updateHypothesis(hypothesis) }
    }
    
    @Test
    fun `deleteHypothesis should call repository deleteHypothesis`() = runTest {
        // Given
        val hypothesis = Hypothesis(
            id = "test-id",
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test description"
        )
        
        viewModel = HypothesisViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.deleteHypothesis(hypothesis)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify { mockRepository.deleteHypothesis(hypothesis) }
    }
    
    @Test
    fun `archiveHypothesis should call repository archiveHypothesis`() = runTest {
        // Given
        val hypothesis = Hypothesis(
            id = "test-id",
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test description"
        )
        
        viewModel = HypothesisViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.archiveHypothesis(hypothesis)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify { mockRepository.archiveHypothesis(hypothesis) }
    }
    
    @Test
    fun `initial state should be correct`() = runTest {
        // Given
        viewModel = HypothesisViewModel(mockRepository, mockGenerationRepository)
        
        // Then
        assertEquals("Initial generated hypotheses should be empty", 
            emptyList<String>(), viewModel.generatedHypotheses.first())
        assertEquals("Initial isGenerating should be false", 
            false, viewModel.isGenerating.first())
        assertEquals("Initial generation error should be null", 
            null, viewModel.generationError.first())
    }
}