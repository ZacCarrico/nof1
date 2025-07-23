package com.nof1.viewmodel

import com.nof1.data.model.Experiment
import com.nof1.data.model.Hypothesis
import com.nof1.data.repository.ExperimentGenerationRepository
import com.nof1.data.repository.ExperimentRepository
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
 * Unit tests for ExperimentViewModel to ensure proper state management and generation
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExperimentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var mockRepository: ExperimentRepository
    private lateinit var mockGenerationRepository: ExperimentGenerationRepository
    private lateinit var viewModel: ExperimentViewModel

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
    fun `generateExperiments should update state flows correctly on success`() = runTest {
        // Given
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test description"
        )
        val mockExperiments = listOf("experiment1", "experiment2", "experiment3")
        
        coEvery { mockGenerationRepository.generateExperimentsStrings(hypothesis) } returns Result.success(mockExperiments)
        
        viewModel = ExperimentViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.generateExperiments(hypothesis)
        testScheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Generated experiments should be set", mockExperiments, viewModel.generatedExperiments.first())
        assertEquals("IsGenerating should be false after completion", false, viewModel.isGenerating.first())
        assertEquals("Generation error should be null on success", null, viewModel.generationError.first())
    }
    
    @Test
    fun `generateExperiments should update state flows correctly on failure`() = runTest {
        // Given
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test description"
        )
        val errorMessage = "API call failed"
        val exception = Exception(errorMessage)
        
        coEvery { mockGenerationRepository.generateExperimentsStrings(hypothesis) } returns Result.failure(exception)
        
        viewModel = ExperimentViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.generateExperiments(hypothesis)
        testScheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Generated experiments should be empty on failure", emptyList<String>(), viewModel.generatedExperiments.first())
        assertEquals("IsGenerating should be false after completion", false, viewModel.isGenerating.first())
        assertEquals("Generation error should contain error message", errorMessage, viewModel.generationError.first())
    }
    
    @Test
    fun `generateExperiments should set isGenerating to true during operation`() = runTest {
        // Given
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test description"
        )
        val mockExperiments = listOf("experiment1", "experiment2")
        
        coEvery { mockGenerationRepository.generateExperimentsStrings(hypothesis) } returns Result.success(mockExperiments)
        
        viewModel = ExperimentViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.generateExperiments(hypothesis)
        
        // Then - check isGenerating is true before advancing scheduler
        assertEquals("IsGenerating should be true during operation", true, viewModel.isGenerating.first())
        
        // Complete the operation
        testScheduler.advanceUntilIdle()
        assertEquals("IsGenerating should be false after completion", false, viewModel.isGenerating.first())
    }
    
    @Test
    fun `generateExperiments should handle exception during generation`() = runTest {
        // Given
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test description"
        )
        val exceptionMessage = "Unexpected error occurred"
        
        coEvery { mockGenerationRepository.generateExperimentsStrings(hypothesis) } throws RuntimeException(exceptionMessage)
        
        viewModel = ExperimentViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.generateExperiments(hypothesis)
        testScheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Generated experiments should be empty on exception", emptyList<String>(), viewModel.generatedExperiments.first())
        assertEquals("IsGenerating should be false after exception", false, viewModel.isGenerating.first())
        assertTrue("Generation error should contain exception message", 
            viewModel.generationError.first()!!.contains("Unexpected error: $exceptionMessage"))
    }
    
    @Test
    fun `generateExperiments should set error when generation repository is null`() = runTest {
        // Given
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test description"
        )
        viewModel = ExperimentViewModel(mockRepository, null)
        
        // When
        viewModel.generateExperiments(hypothesis)
        testScheduler.advanceUntilIdle()
        
        // Then
        assertEquals("Generated experiments should be empty when repository is null", 
            emptyList<String>(), viewModel.generatedExperiments.first())
        assertEquals("IsGenerating should be false when repository is null", 
            false, viewModel.isGenerating.first())
        assertEquals("Generation error should indicate configuration issue", 
            "Experiment generation not configured", viewModel.generationError.first())
    }
    
    @Test
    fun `clearGeneratedExperiments should reset state`() = runTest {
        // Given
        val hypothesis = Hypothesis(
            id = "test-hypothesis",
            projectId = "test-project",
            name = "Test Hypothesis",
            description = "Test description"
        )
        val mockExperiments = listOf("experiment1", "experiment2")
        
        coEvery { mockGenerationRepository.generateExperimentsStrings(hypothesis) } returns Result.success(mockExperiments)
        
        viewModel = ExperimentViewModel(mockRepository, mockGenerationRepository)
        
        // Generate some experiments first
        viewModel.generateExperiments(hypothesis)
        testScheduler.advanceUntilIdle()
        
        // Verify experiments were generated
        assertEquals("Generated experiments should be set", mockExperiments, viewModel.generatedExperiments.first())
        
        // When
        viewModel.clearGeneratedExperiments()
        
        // Then
        assertEquals("Generated experiments should be empty after clear", 
            emptyList<String>(), viewModel.generatedExperiments.first())
        assertEquals("Generation error should be null after clear", 
            null, viewModel.generationError.first())
    }
    
    @Test
    fun `insertExperiment should call repository insertExperiment`() = runTest {
        // Given
        val experiment = Experiment(
            hypothesisId = "test-hypothesis",
            projectId = "test-project",
            name = "Test Experiment",
            description = "Test description",
            question = "Test question"
        )
        
        viewModel = ExperimentViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.insertExperiment(experiment)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify { mockRepository.insertExperiment(experiment) }
    }
    
    @Test
    fun `updateExperiment should call repository updateExperiment`() = runTest {
        // Given
        val experiment = Experiment(
            id = "test-id",
            hypothesisId = "test-hypothesis",
            projectId = "test-project",
            name = "Updated Experiment",
            description = "Updated description",
            question = "Updated question"
        )
        
        viewModel = ExperimentViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.updateExperiment(experiment)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify { mockRepository.updateExperiment(experiment) }
    }
    
    @Test
    fun `deleteExperiment should call repository deleteExperiment`() = runTest {
        // Given
        val experiment = Experiment(
            id = "test-id",
            hypothesisId = "test-hypothesis",
            projectId = "test-project",
            name = "Test Experiment",
            description = "Test description",
            question = "Test question"
        )
        
        viewModel = ExperimentViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.deleteExperiment(experiment)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify { mockRepository.deleteExperiment(experiment) }
    }
    
    @Test
    fun `archiveExperiment should call repository archiveExperiment`() = runTest {
        // Given
        val experiment = Experiment(
            id = "test-id",
            hypothesisId = "test-hypothesis",
            projectId = "test-project",
            name = "Test Experiment",
            description = "Test description",
            question = "Test question"
        )
        
        viewModel = ExperimentViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.archiveExperiment(experiment)
        testScheduler.advanceUntilIdle()
        
        // Then
        verify { mockRepository.archiveExperiment(experiment) }
    }
    
    @Test
    fun `getExperimentsForHypothesis should call repository method`() {
        // Given
        val hypothesisId = "test-hypothesis"
        viewModel = ExperimentViewModel(mockRepository, mockGenerationRepository)
        
        // When
        viewModel.getExperimentsForHypothesis(hypothesisId)
        
        // Then
        verify { mockRepository.getActiveExperimentsForHypothesis(hypothesisId) }
    }
    
    @Test
    fun `initial state should be correct`() = runTest {
        // Given
        viewModel = ExperimentViewModel(mockRepository, mockGenerationRepository)
        
        // Then
        assertEquals("Initial generated experiments should be empty", 
            emptyList<String>(), viewModel.generatedExperiments.first())
        assertEquals("Initial isGenerating should be false", 
            false, viewModel.isGenerating.first())
        assertEquals("Initial generation error should be null", 
            null, viewModel.generationError.first())
    }
}