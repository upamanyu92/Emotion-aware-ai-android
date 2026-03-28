package com.example.emotionawareai.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceProcessorLogicTest {

    @Test
    fun `no match is silently recovered in continuous mode`() {
        assertTrue(VoiceError.NO_MATCH.shouldSilentlyRecoverInContinuousMode(isRestarting = false))
    }

    @Test
    fun `speech timeout is silently recovered in continuous mode`() {
        assertTrue(VoiceError.SPEECH_TIMEOUT.shouldSilentlyRecoverInContinuousMode(isRestarting = false))
    }

    @Test
    fun `client error is silently recovered only during recognizer restart`() {
        assertTrue(VoiceError.CLIENT_ERROR.shouldSilentlyRecoverInContinuousMode(isRestarting = true))
        assertFalse(VoiceError.CLIENT_ERROR.shouldSilentlyRecoverInContinuousMode(isRestarting = false))
    }

    @Test
    fun `other voice errors are not silently recovered`() {
        assertFalse(VoiceError.AUDIO_ERROR.shouldSilentlyRecoverInContinuousMode(isRestarting = true))
        assertFalse(VoiceError.NETWORK_ERROR.shouldSilentlyRecoverInContinuousMode(isRestarting = true))
        assertFalse(VoiceError.NOT_AVAILABLE.shouldSilentlyRecoverInContinuousMode(isRestarting = true))
    }
}
