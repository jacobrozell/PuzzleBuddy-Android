package com.jacobrozell.puzzlebuddy.domain.catalog

import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

data class PaceBucket(
    val label: String,
    val count: Int,
) {
    val id: String get() = label
}

data class CollectionStats(
    val totalCount: Int,
    val completedCount: Int,
    val inProgressCount: Int,
    val wishlistCount: Int,
    val abandonedCount: Int,
    val totalPiecesCompleted: Int,
    val totalMinutesPuzzling: Int,
    val backlogCount: Int,
    val missingPiecesCount: Int,
    val onLoanCount: Int,
    val averageRating: Double?,
    val favoritePieceCount: Int?,
    val averageDaysToComplete: Double?,
    val favoritePuzzleType: PuzzleType?,
    val topPurchaseLocations: List<String>,
    val completionsThisMonth: Int,
    val completionsThisYear: Int,
    val biggestCompletedPieces: Int?,
    val smallestCompletedPieces: Int?,
    val topTags: List<PuzzleTagCount>,
    val totalSpend: Double?,
    val spendCurrencyCode: String?,
    val favoriteBrand: String?,
    val averageDifficulty: Double?,
    val averageInProgressPercent: Int?,
    val replayedPuzzleCount: Int,
    val averageHoursPer1000Pieces: Double?,
    val paceBuckets: List<PaceBucket>,
    val purchaseLocationCounts: List<PuzzleTagCount>,
    val completionsByMonthThisYear: List<Int>,
) {
    val formattedTotalHours: String get() = formatHours(totalMinutesPuzzling)

    val formattedAverageRating: String? get() = averageRating?.let { String.format(Locale.US, "%.1f", it) }

    val formattedAverageDaysToComplete: String? get() = formatAverageDays(averageDaysToComplete)

    val formattedTotalSpend: String?
        get() {
            val spend = totalSpend ?: return null
            if (spend <= 0) return null
            return PurchasePriceFormatting.displayLabel(spend, spendCurrencyCode)
        }

    val formattedAverageDifficulty: String?
        get() = averageDifficulty?.let { String.format(Locale.US, "%.1f / 5", it) }

    val averageDifficultyDescriptor: String?
        get() {
            val value = averageDifficulty ?: return null
            return when {
                value < 1.5 -> "Mostly easygoing"
                value < 2.5 -> "Light challenge"
                value < 3.5 -> "Solid challenge"
                value < 4.5 -> "Tough puzzles"
                else -> "Expert level"
            }
        }

    val formattedAverageSpeed: String?
        get() {
            val speed = averageHoursPer1000Pieces ?: return null
            return PuzzleDetailMetrics(null, speed).formattedHoursPer1000Pieces
        }

    val formattedAverageInProgress: String?
        get() = averageInProgressPercent?.let { "$it%" }

    val mostProductiveMonthThisYear: Pair<String, Int>?
        get() {
            val maxCount = completionsByMonthThisYear.maxOrNull() ?: return null
            if (maxCount <= 0) return null
            val index = completionsByMonthThisYear.indexOf(maxCount)
            if (index < 0) return null
            return monthSymbols[index] to maxCount
        }

    companion object {
        val monthSymbols = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December",
        )

        val monthAbbreviations = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )

        fun compute(
            puzzles: List<Puzzle>,
            zoneId: ZoneId = ZoneId.systemDefault(),
            now: Instant = Instant.now(),
        ): CollectionStats {
            val completed = puzzles.filter { it.status == PuzzleStatus.COMPLETED }
            val todo = puzzles.filter { it.status == PuzzleStatus.TODO }
            val inProgress = puzzles.filter { it.status == PuzzleStatus.IN_PROGRESS }
            val wishlist = puzzles.filter { it.status == PuzzleStatus.WISHLIST }
            val abandoned = puzzles.filter { it.status == PuzzleStatus.ABANDONED }
            val pieceCounts = completed.mapNotNull { it.pieces }
            val ratedCompleted = completed.filter { it.rating != PuzzleRating.NONE }
            val averageRating = if (ratedCompleted.isEmpty()) {
                null
            } else {
                ratedCompleted.sumOf { it.rating.value } / ratedCompleted.size
            }
            val totalMinutes = completed.sumOf { minutesSpent(it) }
            val dayCounts = PuzzleDateSemantics.completionDayCounts(completed, zoneId)
            val averageDays = if (dayCounts.isEmpty()) null else dayCounts.sum().toDouble() / dayCounts.size
            val spend = totalSpend(puzzles)
            val nowZoned = now.atZone(zoneId)
            return CollectionStats(
                totalCount = puzzles.size,
                completedCount = completed.size,
                inProgressCount = inProgress.size,
                wishlistCount = wishlist.size,
                abandonedCount = abandoned.size,
                totalPiecesCompleted = pieceCounts.sum(),
                totalMinutesPuzzling = totalMinutes,
                backlogCount = todo.size,
                missingPiecesCount = puzzles.count { it.hasMissingPieces },
                onLoanCount = puzzles.count { it.isOnLoan },
                averageRating = averageRating,
                favoritePieceCount = favoritePieceCount(pieceCounts),
                averageDaysToComplete = averageDays,
                favoritePuzzleType = favoritePuzzleType(completed),
                topPurchaseLocations = purchaseLocationCounts(puzzles, limit = 3).map { it.name },
                completionsThisMonth = completionDates(completed).count { date ->
                    val zoned = date.atZone(zoneId)
                    zoned.year == nowZoned.year && zoned.monthValue == nowZoned.monthValue
                },
                completionsThisYear = completionDates(completed).count { date ->
                    date.atZone(zoneId).year == nowZoned.year
                },
                biggestCompletedPieces = pieceCounts.maxOrNull(),
                smallestCompletedPieces = pieceCounts.minOrNull(),
                topTags = PuzzleTagIndex.counts(puzzles, limit = 5),
                totalSpend = spend?.first,
                spendCurrencyCode = spend?.second,
                favoriteBrand = favoriteBrand(puzzles),
                averageDifficulty = averageDifficulty(puzzles),
                averageInProgressPercent = averageInProgressPercent(inProgress),
                replayedPuzzleCount = puzzles.count { it.timesCompleted >= 2 },
                averageHoursPer1000Pieces = averageHoursPer1000Pieces(completed),
                paceBuckets = paceBuckets(completed),
                purchaseLocationCounts = purchaseLocationCounts(puzzles, limit = 3),
                completionsByMonthThisYear = monthlyCompletionCounts(completed, nowZoned.year, zoneId),
            )
        }

        fun completionDates(puzzles: List<Puzzle>): List<Instant> = puzzles.flatMap { puzzle ->
            if (puzzle.completions.isEmpty()) {
                if (puzzle.status == PuzzleStatus.COMPLETED) listOf(puzzle.completionDate) else emptyList()
            } else {
                puzzle.completions.map(PuzzleCompletion::completedAt)
            }
        }

        fun monthlyCompletionCounts(puzzles: List<Puzzle>, year: Int, zoneId: ZoneId): List<Int> {
            val months = IntArray(12)
            for (date in completionDates(puzzles)) {
                val zoned = date.atZone(zoneId)
                if (zoned.year == year && zoned.monthValue in 1..12) {
                    months[zoned.monthValue - 1] += 1
                }
            }
            return months.toList()
        }

        fun yearsWithCompletions(puzzles: List<Puzzle>, zoneId: ZoneId = ZoneId.systemDefault()): List<Int> =
            completionDates(puzzles).map { it.atZone(zoneId).year }.distinct().sortedDescending()

        fun formatAverageDays(value: Double?): String? {
            if (value == null) return null
            val rounded = value.roundToInt().toDouble()
            if (rounded >= 14) {
                val weeks = (rounded / 7).roundToInt()
                return if (weeks == 1) "About 1 week" else "About $weeks weeks"
            }
            if (rounded <= 1) return "About 1 day"
            return "About ${rounded.toInt()} days"
        }

        fun formatHours(minutes: Int): String {
            if (minutes <= 0) return "0 hours"
            val hours = minutes / 60.0
            if (hours >= 10) return "${hours.roundToInt()} hours"
            if (hours >= 1) {
                val rounded = (hours * 10).roundToInt() / 10.0
                return if (rounded == rounded.toInt().toDouble()) {
                    "${rounded.toInt()} hours"
                } else {
                    String.format(Locale.US, "%.1f hours", rounded)
                }
            }
            return "$minutes minutes"
        }

        fun formatPieceCount(count: Int): String =
            NumberFormat.getNumberInstance(Locale.getDefault()).format(count)

        fun favoritePieceCount(counts: List<Int>): Int? {
            if (counts.isEmpty()) return null
            val frequencies = counts.groupingBy { it }.eachCount()
            val maxFrequency = frequencies.values.maxOrNull() ?: return null
            val modes = frequencies.filterValues { it == maxFrequency }.keys.sorted()
            if (modes.size == 1) return modes.first()
            return median(counts)
        }

        private fun minutesSpent(puzzle: Puzzle): Int {
            val time = puzzle.estimatedTimeSpent ?: return 0
            val hours = maxOf(time.hours ?: 0, 0)
            val minutes = maxOf(time.minutes ?: 0, 0)
            return maxOf((hours * 60) + minutes, 0)
        }

        private fun median(values: List<Int>): Int {
            val sorted = values.sorted()
            val middle = sorted.size / 2
            return if (sorted.size % 2 == 0) {
                (sorted[middle - 1] + sorted[middle]) / 2
            } else {
                sorted[middle]
            }
        }

        private fun favoritePuzzleType(completed: List<Puzzle>): PuzzleType? {
            val types = completed.map { it.puzzleType }.filter { it != PuzzleType.NONE }
            if (types.isEmpty()) return null
            return types.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        }

        private fun purchaseLocationCounts(puzzles: List<Puzzle>, limit: Int): List<PuzzleTagCount> {
            val locations = puzzles.mapNotNull { it.purchaseLocation?.trim()?.ifEmpty { null } }
            if (locations.isEmpty()) return emptyList()
            val frequencies = linkedMapOf<String, Pair<String, Int>>()
            for (location in locations) {
                val key = location.lowercase()
                val existing = frequencies[key]
                frequencies[key] = if (existing == null) location to 1 else existing.first to existing.second + 1
            }
            return frequencies.values
                .map { PuzzleTagCount(it.first, it.second) }
                .sortedWith(compareByDescending<PuzzleTagCount> { it.count }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                .take(limit)
        }

        private fun favoriteBrand(puzzles: List<Puzzle>): String? {
            val brands = puzzles.mapNotNull { it.source?.trim()?.ifEmpty { null } }
            if (brands.isEmpty()) return null
            val frequencies = linkedMapOf<String, Pair<String, Int>>()
            for (brand in brands) {
                val key = brand.lowercase()
                val existing = frequencies[key]
                frequencies[key] = if (existing == null) brand to 1 else existing.first to existing.second + 1
            }
            return frequencies.values
                .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.first })
                .firstOrNull()?.first
        }

        private fun averageDifficulty(puzzles: List<Puzzle>): Double? {
            val values = puzzles.mapNotNull { puzzle ->
                if (puzzle.difficulty == PuzzleDifficulty.NONE) null else puzzle.difficulty.intValue.toDouble()
            }
            if (values.isEmpty()) return null
            return values.sum() / values.size
        }

        private fun averageInProgressPercent(inProgress: List<Puzzle>): Int? {
            if (inProgress.isEmpty()) return null
            val total = inProgress.sumOf { it.progressPercent }
            return (total.toDouble() / inProgress.size).roundToInt()
        }

        private fun averageHoursPer1000Pieces(completed: List<Puzzle>): Double? {
            val speeds = completed.mapNotNull { puzzle ->
                val pieces = puzzle.pieces ?: return@mapNotNull null
                if (pieces <= 0) return@mapNotNull null
                val minutes = minutesSpent(puzzle)
                if (minutes <= 0) return@mapNotNull null
                (minutes / 60.0) / (pieces / 1000.0)
            }
            if (speeds.isEmpty()) return null
            return speeds.sum() / speeds.size
        }

        private fun paceBuckets(completed: List<Puzzle>): List<PaceBucket> {
            val order = listOf("Quick finish", "Weekend puzzle", "Marathon project")
            val counts = mutableMapOf<String, Int>()
            for (puzzle in completed) {
                val minutes = minutesSpent(puzzle)
                if (minutes <= 0) continue
                val label = PuzzleDetailMetrics.timeBucketLabel(minutes)
                counts[label] = (counts[label] ?: 0) + 1
            }
            return order.mapNotNull { label ->
                val count = counts[label] ?: return@mapNotNull null
                if (count <= 0) null else PaceBucket(label, count)
            }
        }

        private fun totalSpend(puzzles: List<Puzzle>): Pair<Double, String>? {
            val priced = puzzles.mapNotNull { puzzle ->
                val price = puzzle.purchasePrice ?: return@mapNotNull null
                if (price <= 0) return@mapNotNull null
                val code = puzzle.purchaseCurrencyCode ?: runCatching {
                    Currency.getInstance(Locale.getDefault()).currencyCode
                }.getOrNull() ?: "USD"
                price to code
            }
            if (priced.isEmpty()) return null
            val dominant = priced.groupingBy { it.second }.eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .first().key
            val total = priced.filter { it.second == dominant }.sumOf { it.first }
            return total to dominant
        }
    }
}
