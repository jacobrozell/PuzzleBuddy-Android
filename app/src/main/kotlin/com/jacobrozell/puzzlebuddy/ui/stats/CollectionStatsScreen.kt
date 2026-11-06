package com.jacobrozell.puzzlebuddy.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionMilestone
import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionStats
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagIndex
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.ui.designsystem.ReadableContentWidth
import com.jacobrozell.puzzlebuddy.ui.picknext.PickNextSheet
import com.jacobrozell.puzzlebuddy.ui.share.PuzzleShareMenu
import com.jacobrozell.puzzlebuddy.ui.puzzles.LARGE_FONT_SCALE
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionStatsScreen(
    onOpenPuzzle: (String) -> Unit,
    viewModel: CollectionStatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val puzzles = state.puzzles
    val stats = state.stats
    val brand = BrandTokens.colors
    val largeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val zone = remember { ZoneId.systemDefault() }
    val currentYear = remember(zone) { LocalDate.now(zone).year }
    val years = remember(puzzles, zone) {
        CollectionStats.yearsWithCompletions(puzzles, zone).ifEmpty { listOf(currentYear) }
    }
    var selectedYear by rememberSaveable { mutableIntStateOf(currentYear) }
    var showPickNext by remember { mutableStateOf(false) }
    var yearMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var bannerDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(years) {
        if (selectedYear !in years) selectedYear = years.first()
    }
    LaunchedEffect(puzzles.size) {
        bannerDismissed = false
    }
    val milestone = state.milestone.takeUnless { bannerDismissed }
    LaunchedEffect(milestone?.id) {
        milestone?.let { viewModel.onMilestoneShown(it.id) }
    }
    val monthlyCounts = remember(puzzles, selectedYear, zone) {
        CollectionStats.monthlyCompletionCounts(puzzles, selectedYear, zone)
    }
    val columns = if (largeFont) 1 else 2
    val tags = remember(puzzles) { PuzzleTagIndex.counts(puzzles, limit = Int.MAX_VALUE) }

    if (showPickNext) {
        PickNextSheet(
            puzzles = puzzles,
            tags = tags,
            onOpen = onOpenPuzzle,
            onPicked = viewModel::logPickNextSelected,
            onDismiss = { showPickNext = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                navigationIcon = {
                    IconButton(onClick = { showPickNext = true }) {
                        Icon(Icons.Filled.Casino, contentDescription = "Pick my next puzzle")
                    }
                },
                actions = {
                    PuzzleShareMenu(
                        entireCollection = puzzles,
                        visibleList = puzzles,
                        imageVector = Icons.Filled.Share,
                        onShare = { selected -> viewModel.share(context, selected) },
                    )
                },
            )
        },
    ) { padding ->
        ReadableContentWidth(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (puzzles.isEmpty()) {
                    SectionHeading("Your collection at a glance")
                    Column(
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = "No puzzles yet. Add puzzles on the Puzzles tab to see stats here."
                        },
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("No puzzles yet", style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                        Text(
                            "Add puzzles on the Puzzles tab to see stats here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = brand.textSecondary,
                        )
                    }
                } else {
                    milestone?.let { item ->
                        MilestoneBanner(
                            milestone = item,
                            onNice = {
                                bannerDismissed = true
                                viewModel.acknowledge(item.id)
                            },
                        )
                    }
                    HeroSection(stats, columns)
                    HighlightStrip(stats, monthlyCounts.sum(), selectedYear.toString())
                    if (stats.completedCount > 0) {
                        YearActivitySection(
                            year = selectedYear,
                            years = years,
                            yearMenu = yearMenu,
                            onYearMenu = { yearMenu = it },
                            onYear = { selectedYear = it },
                            monthlyCounts = monthlyCounts,
                            currentMonthIndex = if (selectedYear == currentYear) LocalDate.now(zone).monthValue - 1 else null,
                            showThisMonth = selectedYear == currentYear,
                            completionsThisMonth = stats.completionsThisMonth,
                            columns = columns,
                        )
                    }
                    StatSection("Collection", collectionCards(stats), columns)
                    val howYouPuzzle = howYouPuzzleCards(stats)
                    if (howYouPuzzle.isNotEmpty()) StatSection("How you puzzle", howYouPuzzle, columns)
                    if (stats.paceBuckets.isNotEmpty()) {
                        BreakdownSection(
                            title = "Your pace",
                            rows = stats.paceBuckets.map { it.label to it.count },
                        )
                    }
                    val pieceCards = pieceCountCards(stats)
                    if (pieceCards.isNotEmpty()) StatSection("Piece counts", pieceCards, columns)
                    if (stats.purchaseLocationCounts.isNotEmpty()) {
                        BreakdownSection(
                            title = "Where you shop",
                            rows = stats.purchaseLocationCounts.map { it.name to it.count },
                        )
                    }
                    if (stats.topTags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeading("Top tags")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = brand.card),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    stats.topTags.forEach { tag ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clearAndSetSemantics {
                                                    contentDescription = "${tag.name}, ${tag.count} puzzles"
                                                },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(tag.name, color = brand.textPrimary, fontWeight = FontWeight.Medium)
                                            Text(tag.count.toString(), color = brand.textSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneBanner(milestone: CollectionMilestone, onNice: () -> Unit) {
    val brand = BrandTokens.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = brand.cardElevated),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    milestoneIcon(milestone.icon),
                    contentDescription = null,
                    tint = BrandTokens.accentWarm,
                    modifier = Modifier.clearAndSetSemantics {},
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(milestone.title, style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
                    milestone.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary)
                    }
                }
            }
            OutlinedButton(onClick = onNice, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Nice!")
            }
        }
    }
}

@Composable
private fun HeroSection(stats: CollectionStats, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading("Your collection at a glance")
        StatGrid(
            cards = listOf(
                StatCardModel(stats.completedCount.toString(), "Puzzles completed"),
                StatCardModel(CollectionStats.formatPieceCount(stats.totalPiecesCompleted), "Pieces assembled"),
            ),
            columns = columns,
            hero = true,
        )
    }
}

@Composable
private fun HighlightStrip(stats: CollectionStats, yearTotal: Int, yearLabel: String) {
    val chips = buildList {
        add(yearLabel to yearTotal.toString())
        if (stats.totalMinutesPuzzling > 0) add("At the table" to stats.formattedTotalHours)
        stats.formattedAverageRating?.let { add("Avg. rating" to it) }
        if (stats.inProgressCount > 0) add("On the table" to stats.inProgressCount.toString())
        stats.formattedTotalSpend?.let { add("Collection value" to it) }
    }
    if (chips.isEmpty()) return
    val brand = BrandTokens.colors
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        chips.forEach { (caption, value) ->
            Card(
                modifier = Modifier
                    .widthIn(min = 104.dp)
                    .clearAndSetSemantics { contentDescription = "$caption, $value" },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = brand.card),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = brand.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(caption, style = MaterialTheme.typography.labelMedium, color = brand.textSecondary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun YearActivitySection(
    year: Int,
    years: List<Int>,
    yearMenu: Boolean,
    onYearMenu: (Boolean) -> Unit,
    onYear: (Int) -> Unit,
    monthlyCounts: List<Int>,
    currentMonthIndex: Int?,
    showThisMonth: Boolean,
    completionsThisMonth: Int,
    columns: Int,
) {
    val total = monthlyCounts.sum()
    val busiest = monthlyCounts.withIndex().filter { it.value > 0 }.maxByOrNull { it.value }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Activity in $year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandTokens.colors.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            Box {
                TextButton(
                    onClick = { onYearMenu(true) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clearAndSetSemantics { contentDescription = "Stats year" },
                ) {
                    Text(year.toString(), color = BrandTokens.accent)
                }
                DropdownMenu(expanded = yearMenu, onDismissRequest = { onYearMenu(false) }) {
                    years.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.toString()) },
                            onClick = {
                                onYear(option)
                                onYearMenu(false)
                            },
                        )
                    }
                }
            }
        }
        YearActivityChart(monthlyCounts, year, currentMonthIndex, total)
        val cards = buildList {
            if (showThisMonth) add(StatCardModel(completionsThisMonth.toString(), "Finished this month"))
            add(StatCardModel(total.toString(), "Finished in $year"))
            if (busiest != null) {
                add(
                    StatCardModel(
                        value = CollectionStats.monthSymbols[busiest.index],
                        label = "Busiest month",
                        subtitle = if (busiest.value == 1) "1 finished" else "${busiest.value} finished",
                    ),
                )
            }
        }
        StatGrid(cards, columns)
    }
}

@Composable
private fun YearActivityChart(
    monthlyCounts: List<Int>,
    year: Int,
    currentMonthIndex: Int?,
    total: Int,
) {
    val brand = BrandTokens.colors
    val description = yearActivityContentDescription(monthlyCounts, year, total)
    val maxCount = (monthlyCounts.maxOrNull() ?: 0).coerceAtLeast(1)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = brand.card),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (total == 0) "No finishes in $year" else "$total finished in $year",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = brand.textPrimary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                monthlyCounts.forEachIndexed { index, count ->
                    val fraction = count.toFloat() / maxCount
                    val barHeight = (64f * fraction).dp.coerceAtLeast(6.dp)
                    val color = when {
                        count == 0 -> brand.cardElevated
                        index == currentMonthIndex -> BrandTokens.accentWarm
                        else -> BrandTokens.accent
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        if (count > 0) {
                            Text(
                                count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = brand.textSecondary,
                            )
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight),
                        ) {
                            drawRoundRect(color = color, cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CollectionStats.monthAbbreviations.forEachIndexed { index, symbol ->
                    Text(
                        symbol,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == currentMonthIndex) BrandTokens.accent else brand.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

internal fun yearActivityContentDescription(monthlyCounts: List<Int>, year: Int, total: Int): String {
    if (total <= 0) return "No puzzles finished in $year."
    val parts = mutableListOf("$total puzzles finished in $year.")
    monthlyCounts.forEachIndexed { index, count ->
        if (count > 0) {
            val name = CollectionStats.monthSymbols.getOrElse(index) { "month ${index + 1}" }
            parts += "$name: $count."
        }
    }
    return parts.joinToString(" ")
}

@Composable
private fun StatSection(title: String, cards: List<StatCardModel>, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(title)
        StatGrid(cards, columns)
    }
}

@Composable
private fun BreakdownSection(title: String, rows: List<Pair<String, Int>>) {
    val brand = BrandTokens.colors
    val maxCount = rows.maxOf { it.second }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = brand.card),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { (name, count) ->
                    Column(
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = "$name, $count puzzles"
                        },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, color = brand.textPrimary, fontWeight = FontWeight.Medium)
                            Text(count.toString(), color = brand.textSecondary, fontWeight = FontWeight.SemiBold)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawRoundRect(
                                    color = brand.cardElevated,
                                    cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                                )
                            }
                            Canvas(
                                Modifier
                                    .fillMaxWidth(count.toFloat() / maxCount)
                                    .height(6.dp),
                            ) {
                                drawRoundRect(
                                    color = BrandTokens.accent,
                                    cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatGrid(cards: List<StatCardModel>, columns: Int, hero: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { card ->
                    StatCard(card, Modifier.weight(1f), hero)
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(card: StatCardModel, modifier: Modifier, hero: Boolean) {
    val brand = BrandTokens.colors
    val description = if (card.subtitle == null) {
        "${card.label}, ${card.value}"
    } else {
        "${card.label}, ${card.value}. ${card.subtitle}"
    }
    Card(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = brand.card),
    ) {
        Column(modifier = Modifier.padding(if (hero) 16.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                card.value,
                style = if (hero) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = brand.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(card.label, color = brand.textPrimary, fontWeight = if (hero) FontWeight.Normal else FontWeight.Medium)
            card.subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = BrandTokens.colors.textPrimary,
        modifier = Modifier.semantics { heading() },
    )
}

private data class StatCardModel(
    val value: String,
    val label: String,
    val subtitle: String? = null,
)

private fun collectionCards(stats: CollectionStats): List<StatCardModel> = buildList {
    add(StatCardModel(stats.totalCount.toString(), "Total collection", "Puzzles you own"))
    add(StatCardModel(stats.inProgressCount.toString(), "On the table", "In progress now"))
    add(StatCardModel(stats.backlogCount.toString(), "On your shelf", "To-Do puzzles waiting"))
    if (stats.wishlistCount > 0) add(StatCardModel(stats.wishlistCount.toString(), "On your wishlist", "Want to buy"))
    if (stats.abandonedCount > 0) add(StatCardModel(stats.abandonedCount.toString(), "Abandoned", "Will not finish"))
    stats.formattedAverageDaysToComplete?.let { add(StatCardModel(it, "Avg. finish time", "Start to complete")) }
    if (stats.missingPiecesCount > 0) add(StatCardModel(stats.missingPiecesCount.toString(), "Missing pieces", "Flagged incomplete"))
    if (stats.onLoanCount > 0) add(StatCardModel(stats.onLoanCount.toString(), "On loan", "Out with friends"))
    stats.formattedAverageRating?.let { add(StatCardModel(it, "Average rating", "Completed puzzles with stars")) }
    stats.favoritePieceCount?.let {
        add(StatCardModel(CollectionStats.formatPieceCount(it), "Go-to piece count", "Most common size you finish"))
    }
    if (stats.totalMinutesPuzzling > 0) add(StatCardModel(stats.formattedTotalHours, "Time at the table", "Across completed puzzles"))
    stats.formattedAverageInProgress?.let { add(StatCardModel(it, "Avg. progress", "Puzzles on the table")) }
    if (stats.replayedPuzzleCount > 0) add(StatCardModel(stats.replayedPuzzleCount.toString(), "Replayed", "Finished more than once"))
    stats.formattedTotalSpend?.let { add(StatCardModel(it, "Collection value", "Total you've spent")) }
}

private fun howYouPuzzleCards(stats: CollectionStats): List<StatCardModel> = buildList {
    stats.favoritePuzzleType?.let { add(StatCardModel(it.displayLabel, "Favorite type", "Among completed puzzles")) }
    stats.favoriteBrand?.let { add(StatCardModel(it, "Favorite brand", "Most in your collection")) }
    stats.formattedAverageDifficulty?.let {
        add(StatCardModel(it, "Avg. difficulty", stats.averageDifficultyDescriptor))
    }
    stats.formattedAverageSpeed?.let { add(StatCardModel(it, "Typical pace", "Across timed finishes")) }
}

private fun pieceCountCards(stats: CollectionStats): List<StatCardModel> = buildList {
    stats.biggestCompletedPieces?.let { add(StatCardModel(CollectionStats.formatPieceCount(it), "Biggest completed")) }
    stats.smallestCompletedPieces?.let { add(StatCardModel(CollectionStats.formatPieceCount(it), "Smallest completed")) }
}

private fun milestoneIcon(name: String): ImageVector = when (name) {
    "checkmark.seal.fill" -> Icons.Filled.Verified
    "star.circle.fill" -> Icons.Filled.Star
    "crown.fill" -> Icons.Filled.WorkspacePremium
    "puzzlepiece.extension.fill" -> Icons.Filled.Extension
    "sparkles" -> Icons.Filled.AutoAwesome
    "clock.fill" -> Icons.Filled.Schedule
    "calendar" -> Icons.Filled.CalendarMonth
    else -> Icons.Filled.CheckCircle
}
