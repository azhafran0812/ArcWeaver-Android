package com.storymaker.arcweaver

import com.storymaker.arcweaver.domain.evaluator.ConditionalEvaluator

fun main() {
    val evaluator = ConditionalEvaluator()

    val variables = mutableMapOf<String, Any>()
    variables["gold"] = 10

    val result = evaluator.evaluate("gold > 5", variables)

    println("Result: $result")
}