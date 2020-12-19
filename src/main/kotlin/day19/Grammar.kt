package day19

import java.lang.IllegalStateException


data class RuleMatch(val matched: List<Char>)

interface ProductionRule {
    fun match(input: List<Char>): List<RuleMatch>
}

data class RuleReference(val id: Int, val mapping: Map<Int, ProductionRule>) : ProductionRule {
    override fun match(input: List<Char>): List<RuleMatch> =
        mapping[id]!!.match(input)
}

data class TerminalRule(val symbol: Char) : ProductionRule {
    override fun match(input: List<Char>): List<RuleMatch> =
        if (input.isNotEmpty() && input.first() == symbol)
            listOf(RuleMatch(listOf(input.first())))
        else emptyList()
}

data class SequenceRule(val symbolSequence: List<ProductionRule>) : ProductionRule {
    override fun match(input: List<Char>): List<RuleMatch> = match(input, symbolSequence).distinct()

    private fun match(input: List<Char>, rules: List<ProductionRule>): List<RuleMatch> {
        val currentRule = rules.first()
        val remainingRules = rules.drop(1)
        val matchedResults = mutableListOf<RuleMatch>()
        for (possibleMatch in currentRule.match(input)) {
            if (remainingRules.isEmpty()) {
                matchedResults.add(possibleMatch)
            } else {
                val remainingInput = input.drop(possibleMatch.matched.size)
                val subMatches = match(remainingInput, remainingRules)
                for (subMatch in subMatches) {
                    matchedResults.add(RuleMatch(possibleMatch.matched + subMatch.matched))
                }
            }
        }
        return matchedResults
    }
}

data class AlternativeRule(val alternatives: List<List<ProductionRule>>) : ProductionRule {
    override fun match(input: List<Char>): List<RuleMatch> =
        alternatives.flatMap { SequenceRule(it).match(input) }.distinct()
}

fun parseRules(rules: List<String>): Map<Int, ProductionRule> {
    val registry = mutableMapOf<Int, ProductionRule>()
    for (line in rules) {
        val rule = parseRule(line.trim(), registry)
        registry[rule.first] = rule.second
    }
    return registry
}

private val terminalRulePattern = Regex("^(\\d+): \"(\\w)\"$")
private val nonTerminalRulePattern = Regex("^(\\d+): ((\\d+)( (\\d+))*( \\| (\\d+)( (\\d+))*)*)$")
fun parseRule(rule: String, mapping: Map<Int, ProductionRule>): Pair<Int, ProductionRule> {
    return when {
        terminalRulePattern.matches(rule) -> {
            val (ruleId, symbol) = terminalRulePattern.matchEntire(rule)!!.destructured
            Pair(ruleId.toInt(), TerminalRule(symbol.toCharArray().first()))
        }
        nonTerminalRulePattern.matches(rule) -> {
            val match = nonTerminalRulePattern.matchEntire(rule)
            val (ruleId, ruleContent) = match!!.destructured
            val alternatives = ruleContent.split("|")
                .asSequence()
                .filter { it.isNotEmpty() }
                .map { singleRule ->
                    singleRule.split(" ")
                        .asSequence()
                        .filter { it.isNotEmpty() }.map { RuleReference(it.toInt(), mapping) }
                        .toList()
                }
                .toList()
            val parsedRule = if (alternatives.size == 1) {
                SequenceRule(alternatives.first())
            } else {
                AlternativeRule(alternatives)
            }
            Pair(ruleId.toInt(), parsedRule)
        }
        else -> throw IllegalStateException("Failed to parse rule!")
    }
}

fun match(rules: Map<Int, ProductionRule>, input: String): Boolean {
    val rootRule = rules[0]!!
    val inputSequence = input.toCharArray().toList()
    return rootRule.match(inputSequence).any { it.matched.size == inputSequence.size }
}