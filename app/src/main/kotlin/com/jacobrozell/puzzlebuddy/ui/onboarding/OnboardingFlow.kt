package com.jacobrozell.puzzlebuddy.ui.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.domain.onboarding.OnboardingCopy
import com.jacobrozell.puzzlebuddy.ui.designsystem.AdaptiveLayout
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens
import kotlinx.coroutines.launch

private val pages = OnboardingCopy.pages()

private val pageIcons = listOf(
    Icons.Filled.Extension,
    Icons.Filled.QrCodeScanner,
    Icons.Filled.Style,
    Icons.Filled.CheckCircle,
)

@Composable
fun OnboardingFlow(
    onFinished: () -> Unit,
    onSkipped: (pageIndex: Int) -> Unit = { onFinished() },
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val brand = BrandTokens.colors
    val page = pagerState.currentPage

    // Full opaque screen. A transparent wash left the puzzle list showing through on
    // device, and Samsung's cutout plus gesture bar covered the controls.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .blockTouchesBehind()
            .semantics { contentDescription = "Onboarding" },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BrandTokens.gradientTop.copy(alpha = 0.18f),
                            BrandTokens.gradientMid.copy(alpha = 0.10f),
                            BrandTokens.gradientBottom.copy(alpha = 0.06f),
                        ),
                    ),
                ),
        )
        OnboardingColumn {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { index ->
                    OnboardingPage(index)
                }
                PageDots(
                    count = pages.size,
                    selected = page,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 12.dp)
                        .clearAndSetSemantics {
                            contentDescription = "Page ${page + 1} of ${pages.size}"
                        },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BrandTokens.accent.copy(alpha = 0.12f)),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (page > 0) {
                        OutlinedButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Back")
                        }
                    }
                    OutlinedButton(
                        onClick = { onSkipped(page) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Skip")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (page == pages.lastIndex) {
                                onFinished()
                            } else {
                                scope.launch { pagerState.animateScrollToPage(page + 1) }
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(if (page == pages.lastIndex) "Get Started" else "Next")
                    }
                }
                Text(
                    "Page ${page + 1} of ${pages.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = brand.textSecondary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                        .clearAndSetSemantics {},
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(index: Int) {
    val page = pages[index]
    val brand = BrandTokens.colors
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(CircleShape)
                    .background(BrandTokens.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = pageIcons[index],
                    contentDescription = null,
                    tint = BrandTokens.accent,
                    modifier = Modifier.size(64.dp),
                )
            }
            Text(
                page.title,
                style = MaterialTheme.typography.headlineMedium,
                color = brand.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp),
            )
            Text(
                page.message,
                style = MaterialTheme.typography.bodyLarge,
                color = brand.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 8.dp),
            )
        }
    }
}

@Composable
private fun PageDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (active) 22.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) BrandTokens.accent else BrandTokens.accent.copy(alpha = 0.28f),
                    ),
            )
        }
    }
}

/** Swallows taps the tutorial does not use, so the list underneath cannot be pressed through the screen. */
private fun Modifier.blockTouchesBehind(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            event.changes.forEach { change ->
                if (!change.isConsumed) change.consume()
            }
        }
    }
}

/** Full-height readable column. The shared width helper wraps its height, which collapsed this screen. */
@Composable
private fun OnboardingColumn(content: @Composable () -> Unit) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cap = if (maxWidth >= AdaptiveLayout.expandedWidthBreakpoint || isLandscape) {
            AdaptiveLayout.contentMaxWidth(maxWidth.value, isLandscape).dp
        } else {
            maxWidth
        }
        Box(
            modifier = Modifier
                .widthIn(max = cap)
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.TopCenter),
        ) {
            content()
        }
    }
}
