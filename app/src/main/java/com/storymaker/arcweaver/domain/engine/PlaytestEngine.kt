package com.storymaker.arcweaver.domain.engine

import com.storymaker.arcweaver.domain.evaluator.ConditionalEvaluator
import com.storymaker.arcweaver.domain.parser.VariableParser

data class EngineResult(
    val isSuccess: Boolean,
    val message: String,
    val nextNodeId: Int? = null,
    val updatedVariablesJson: String? = null
)

class PlaytestEngine(
    private val evaluator: ConditionalEvaluator = ConditionalEvaluator(),
    private val parser: VariableParser = VariableParser()
) {

    fun processChoice(
        currentVariablesJson: String?,
        requiredCondition: String?,
        targetNodeId: Int
    ): EngineResult {

        // 1. Parse variables
        val variables = parser.parse(currentVariablesJson)

        // 2. Evaluasi kondisi
        val isAllowed = if (requiredCondition.isNullOrBlank()) {
            true //
        } else {
            evaluator.evaluate(requiredCondition, variables)
        }

        if (!isAllowed) {
            return EngineResult(
                isSuccess = false,
                message = "Pilihan terkunci",
                nextNodeId = null
            )
        }

        // 3.Update state
        val updatedJson = parser.toJson(variables)

        // 4. Return sukses + node tujuan
        return EngineResult(
            isSuccess = true,
            message = "Berhasil pindah node",
            nextNodeId = targetNodeId,
            updatedVariablesJson = updatedJson
        )
    }
}