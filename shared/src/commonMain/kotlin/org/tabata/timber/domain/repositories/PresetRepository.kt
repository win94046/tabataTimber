package org.tabata.timber.domain.repositories

import org.tabata.timber.domain.models.TimerConfiguration

/**
 * Repository interface for managing timer presets
 */
interface PresetRepository {
    /**
     * Get all saved presets
     */
    suspend fun getAllPresets(): List<TimerPreset>
    
    /**
     * Get a specific preset by ID
     * @param id Preset identifier
     */
    suspend fun getPreset(id: Long): TimerPreset?
    
    /**
     * Save a new preset or update existing one
     * @param preset Preset to save
     * @return ID of the saved preset
     */
    suspend fun savePreset(preset: TimerPreset): Long
    
    /**
     * Delete a preset
     * @param id Preset identifier to delete
     */
    suspend fun deletePreset(id: Long)
    
    /**
     * Export preset as JSON string
     * @param id Preset identifier to export
     */
    suspend fun exportPreset(id: Long): String
    
    /**
     * Import preset from JSON string
     * @param json JSON representation of preset
     */
    suspend fun importPreset(json: String): TimerPreset
    
    /**
     * Validate import JSON before processing
     * @param json JSON string to validate
     */
    suspend fun validateImportJson(json: String): ValidationResult
}

/**
 * Timer preset data model
 */
data class TimerPreset(
    val id: Long = 0,
    val name: String,
    val configuration: TimerConfiguration,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * JSON validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)