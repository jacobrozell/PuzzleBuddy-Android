package com.jacobrozell.puzzlebuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagIndex
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagSemantics

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PuzzleTagsField(
    tags: List<String>,
    catalog: List<String>,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    val canAddMore = tags.size < PuzzleTagSemantics.MAX_TAGS_PER_PUZZLE
    val trimmedDraft = draft.trim()
    val suggestions = PuzzleTagIndex.matchingTags(
        query = draft,
        excluding = tags,
        catalog = catalog,
    )

    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (tags.isEmpty()) {
                    "Up to ${PuzzleTagSemantics.MAX_TAGS_PER_PUZZLE} tags"
                } else {
                    "${tags.size} of ${PuzzleTagSemantics.MAX_TAGS_PER_PUZZLE} tags"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tags.isNotEmpty()) {
                TextButton(onClick = { onTagsChange(emptyList()) }) {
                    Text("Clear all")
                }
            }
        }

        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics {
                    contentDescription = "Tags, ${tags.joinToString()}"
                },
            ) {
                tags.forEach { tag ->
                    AssistChip(
                        onClick = {},
                        label = { Text(tag) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onTagsChange(tags.filterNot { PuzzleTagSemantics.matches(it, tag) })
                                },
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove tag $tag")
                            }
                        },
                    )
                }
            }
        }

        if (canAddMore) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Add tag") },
                placeholder = { Text("Search or add a tag…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        addDraft(draft, tags, onTagsChange)
                        draft = ""
                    },
                ),
            )
            if (trimmedDraft.isNotEmpty() && suggestions.isEmpty()) {
                Text(
                    "No matching tags. Press return to add \"$trimmedDraft\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (suggestions.isNotEmpty()) {
                Text(
                    if (trimmedDraft.isEmpty()) "Suggested tags" else "Matching tags",
                    style = MaterialTheme.typography.labelMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { suggestion ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                onTagsChange(PuzzleTagSemantics.sanitizedTags(tags + suggestion))
                                draft = ""
                            },
                            label = { Text(suggestion) },
                            modifier = Modifier.semantics {
                                contentDescription = "Add tag $suggestion"
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun addDraft(
    draft: String,
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
) {
    val parts = draft.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val toAdd = if (parts.isEmpty()) listOf(draft) else parts
    onTagsChange(PuzzleTagSemantics.sanitizedTags(tags + toAdd))
}
