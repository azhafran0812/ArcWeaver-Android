package com.storymaker.arcweaver.domain.evaluator

class ConditionalEvaluator {

    fun evaluate(condition: String, variables: Map<String, Any>): Boolean {
        println("Condition raw: '$condition'")

        val cleaned = condition.trim().replace(Regex("\\s+"), " ")
        println("Condition cleaned: '$cleaned'")

        val parts = cleaned.split(" ")
        println("Parts: $parts")

        if (parts.size != 3) return false

        val key = parts[0]
        val operator = parts[1]
        val value = parts[2].toIntOrNull() ?: return false

        val variableValue = (variables[key] as? Int) ?: return false

        println("Variable: $key = $variableValue")

        return when (operator) {
            "==" -> variableValue == value
            "!=" -> variableValue != value
            ">"  -> variableValue > value
            "<"  -> variableValue < value
            ">=" -> variableValue >= value
            "<=" -> variableValue <= value
            else -> false
        }
    }
}