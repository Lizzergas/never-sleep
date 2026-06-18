package buildlogic

data class KotlinAssignmentWrappingViolation(
    val lineNumber: Int,
    val original: String,
    val replacement: String,
)

object KotlinAssignmentWrapping {
    fun formatText(
        text: String,
        maxLineLength: Int,
    ): String {
        val hadFinalNewline = text.endsWith("\n")
        val lines = text.removeSuffix("\n").split("\n")
        val formatted = mutableListOf<String>()
        val outdents = mutableListOf<Outdent>()
        var index = 0

        while (index < lines.size) {
            val current = lines[index]
            val next = lines.getOrNull(index + 1)
            outdents.dropCompletedFor(current)

            val adjustedCurrent = current.removeIndentation(outdents.totalAmount())
            val adjustedNext = next?.removeIndentation(outdents.totalAmount())
            val replacement = adjustedNext?.let { replacementFor(adjustedCurrent, it, maxLineLength) }

            if (replacement != null) {
                formatted += replacement
                val amount = adjustedNext.countLeadingSpaces() - adjustedCurrent.countLeadingSpaces()
                if (amount > 0) {
                    outdents += Outdent(
                        threshold = next.countLeadingSpaces(),
                        amount = amount,
                    )
                }
                index += 2
            } else {
                formatted += adjustedCurrent
                index += 1
            }
        }

        return formatted.joinToString("\n") + if (hadFinalNewline) "\n" else ""
    }

    fun findViolations(
        text: String,
        maxLineLength: Int,
    ): List<KotlinAssignmentWrappingViolation> {
        val lines = text.removeSuffix("\n").split("\n")

        return lines
            .windowed(size = 2, partialWindows = false)
            .mapIndexedNotNull { index, pair ->
                val replacement = replacementFor(pair[0], pair[1], maxLineLength) ?: return@mapIndexedNotNull null
                KotlinAssignmentWrappingViolation(
                    lineNumber = index + 1,
                    original = "${pair[0]}\n${pair[1]}",
                    replacement = replacement,
                )
            }
    }

    private fun replacementFor(
        currentLine: String,
        nextLine: String,
        maxLineLength: Int,
    ): String? {
        val left = currentLine.trimEnd()
        val right = nextLine.trimStart()
        if (!left.endsWithStandaloneEquals()) return null
        if (left.isFunctionExpressionBody()) return null
        if (!right.startsWithExpressionToken()) return null

        val replacement = "$left $right"
        return replacement.takeIf { it.length <= maxLineLength }
    }

    private fun String.endsWithStandaloneEquals(): Boolean =
        length >= 2 && endsWith("=") && this[length - 2].isWhitespace()

    private fun String.isFunctionExpressionBody(): Boolean =
        Regex("""(^|\s)fun\s+""").containsMatchIn(trimStart()) ||
            Regex("""^\)\s*:\s*""").containsMatchIn(trimStart())

    private fun String.startsWithExpressionToken(): Boolean {
        val first = firstOrNull() ?: return false
        return first == '`' || first == '_' || first.isLetter()
    }

    private data class Outdent(
        val threshold: Int,
        val amount: Int,
    )

    private fun MutableList<Outdent>.dropCompletedFor(line: String) {
        if (line.isBlank()) return
        val indent = line.countLeadingSpaces()
        while (isNotEmpty() && indent < last().threshold) {
            removeAt(lastIndex)
        }
    }

    private fun List<Outdent>.totalAmount(): Int = sumOf { it.amount }

    private fun String.removeIndentation(amount: Int): String {
        if (amount <= 0) return this
        val removable = countLeadingSpaces().coerceAtMost(amount)
        return drop(removable)
    }

    private fun String.countLeadingSpaces(): Int = takeWhile { it == ' ' }.length
}
