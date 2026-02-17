package im.ilyk.ccoptimizer.core.billing

/**
 * Factory for creating configured cycle inference engines
 */
object CycleInferenceEngineFactory {
    
    /**
     * Create a cycle inference engine with default configuration
     */
    fun createDefault(): CycleInferenceEngine {
        return CycleInferenceEngineImpl(CycleConfig.DEFAULT)
    }
    
    /**
     * Create a cycle inference engine with conservative settings
     * Higher confidence thresholds, more data required
     */
    fun createConservative(): CycleInferenceEngine {
        return CycleInferenceEngineImpl(CycleConfig.CONSERVATIVE)
    }
    
    /**
     * Create a cycle inference engine with aggressive settings
     * Lower confidence thresholds, works with less data
     */
    fun createAggressive(): CycleInferenceEngine {
        return CycleInferenceEngineImpl(CycleConfig.AGGRESSIVE)
    }
    
    /**
     * Create a cycle inference engine with custom configuration
     */
    fun createWithConfig(config: CycleConfig): CycleInferenceEngine {
        return CycleInferenceEngineImpl(config)
    }
}
