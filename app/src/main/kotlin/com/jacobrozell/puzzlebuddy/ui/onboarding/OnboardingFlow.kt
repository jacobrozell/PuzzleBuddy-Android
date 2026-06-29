package com.jacobrozell.puzzlebuddy.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.ui.designsystem.BrandBackground
import com.jacobrozell.puzzlebuddy.ui.designsystem.ReadableContentWidth
import kotlinx.coroutines.launch

private data class OnboardingPage(val title: String, val message: String)

private val pages = listOf(
    OnboardingPage(
        title = "Welcome to Puzzle Buddy",
        message = "Your personal jigsaw puzzle catalog — track every box on your shelf, offline and private.",
    ),
    OnboardingPage(
        title = "Shop With Confidence",
        message = "Scan a barcode at the thrift store to check duplicates instantly, or look up product details when online lookup is enabled.",
    ),
    OnboardingPage(
        title = "Build Your Collection",
        message = "Log brands, piece counts, progress, and ratings. Import from IPDb CSV in Settings.",
    ),
    OnboardingPage(
        title = "Ready to Puzzle?",
        message = "Everything stays on your device. Add your first puzzle to get started.",
    ),
)

@Composable
fun OnboardingFlow(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    BrandBackground {
        ReadableContentWidth(
            modifier = Modifier.fillMaxSize(),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .semantics { contentDescription = "Onboarding" },
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(page.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(
                        page.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            Text(
                "Page ${pagerState.currentPage + 1} of ${pages.size}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pagerState.currentPage == 0) {
                    OutlinedButton(
                        onClick = onFinished,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Skip")
                    }
                }
                Button(
                    onClick = {
                        if (pagerState.currentPage == pages.lastIndex) {
                            onFinished()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (pagerState.currentPage == pages.lastIndex) "Get Started" else "Next")
                }
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Back")
                    }
                }
            }
        }
        }
    }
}
