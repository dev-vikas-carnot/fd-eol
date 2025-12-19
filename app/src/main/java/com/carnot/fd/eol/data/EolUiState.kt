package com.carnot.fd.eol.data

/**
 * Sealed class representing the strict state machine for the EOL workflow.
 * Each state determines what UI elements are visible and what actions are allowed.
 */
sealed class EolUiState {
    /**
     * Initial state: User can input/scan IMEI
     */
    object ImeiInput : EolUiState()

    /**
     * Device status validation in progress
     * - IMEI is locked (read-only)
     * - Polling API every 2 seconds
     * - Timer running (max 2 minutes)
     * - VIN section is HIDDEN
     * - Submit button is DISABLED
     */
    object DeviceStatusPolling : EolUiState()

    /**
     * All device flags passed - VIN input is now enabled
     * - IMEI is locked
     * - Device status indicators show PASS
     * - VIN section becomes VISIBLE
     * - Submit button remains DISABLED until VIN is valid
     */
    object VinInputEnabled : EolUiState()

    /**
     * All conditions met for submission
     * - IMEI locked
     * - All device flags PASS
     * - VIN is valid (Globals.isVinValidString)
     * - Submit button is ENABLED
     */
    object SubmitReady : EolUiState()

    /**
     * Submission in progress or completed
     * - All UI locked
     * - No further input allowed
     */
    object Submitted : EolUiState()
}
