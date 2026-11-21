package com.mindlink.journal.ui.history

enum class DiffOperation {
    UNCHANGED,
    ADDED,
    DELETED
}

data class DiffItem(
    val operation: DiffOperation,
    val text: String,
    val oldLineNumber: Int? = null,
    val newLineNumber: Int? = null
)

data class DiffSummary(
    val additions: Int,
    val deletions: Int,
    val unchanged: Int,
    val lines: List<DiffItem>
)

object DiffUtils {

    /**
     * Computes line-by-line diff between oldText and newText using Longest Common Subsequence (LCS).
     */
    fun computeLineDiff(oldText: String, newText: String): DiffSummary {
        val oldLines = if (oldText.isEmpty()) emptyList() else oldText.lines()
        val newLines = if (newText.isEmpty()) emptyList() else newText.lines()

        val n = oldLines.size
        val m = newLines.size

        val dp = Array(n + 1) { IntArray(m + 1) }

        for (i in 1..n) {
            for (j in 1..m) {
                if (oldLines[i - 1] == newLines[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        val result = mutableListOf<DiffItem>()
        var i = n
        var j = m

        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1] -> {
                    result.add(DiffItem(DiffOperation.UNCHANGED, oldLines[i - 1], i, j))
                    i--
                    j--
                }
                j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j]) -> {
                    result.add(DiffItem(DiffOperation.ADDED, newLines[j - 1], null, j))
                    j--
                }
                i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j]) -> {
                    result.add(DiffItem(DiffOperation.DELETED, oldLines[i - 1], i, null))
                    i--
                }
            }
        }

        result.reverse()

        var additions = 0
        var deletions = 0
        var unchanged = 0

        for (item in result) {
            when (item.operation) {
                DiffOperation.ADDED -> additions++
                DiffOperation.DELETED -> deletions++
                DiffOperation.UNCHANGED -> unchanged++
            }
        }

        return DiffSummary(additions, deletions, unchanged, result)
    }
}
