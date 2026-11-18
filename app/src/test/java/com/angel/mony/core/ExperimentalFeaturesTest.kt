package com.angel.mony.core

import org.junit.Assert.assertFalse
import org.junit.Test

class ExperimentalFeaturesTest {
    @Test fun publicDemonstrationBuildDoesNotExposeExperimentalAi() {
        assertFalse(ExperimentalFeatures.AI_ENABLED)
    }
}
