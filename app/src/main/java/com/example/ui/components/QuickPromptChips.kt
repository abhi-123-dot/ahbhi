package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class PromptCategory(
    val name: String,
    val icon: ImageVector,
    val prompts: List<String>
)

val Categories = listOf(
    PromptCategory(
        name = "Research",
        icon = Icons.Default.ManageSearch,
        prompts = listOf(
            "Compare Python vs Rust for beginners",
            "Explain Quantum Computing in simple English",
            "Latest AI tools and search trends 2026",
            "How does internet routing work step by step?"
        )
    ),
    PromptCategory(
        name = "YouTube",
        icon = Icons.Default.VideoLibrary,
        prompts = listOf(
            "10 Viral video titles & SEO description for tech",
            "Thumbnail text & AI image prompt for study tips",
            "YouTube tags & SEO for coding tutorials",
            "Content ideas for a new gaming channel"
        )
    ),
    PromptCategory(
        name = "Coding",
        icon = Icons.Default.Code,
        prompts = listOf(
            "How to fix NullPointerException in Kotlin?",
            "Build a REST API in Python step by step",
            "Explain loops and recursion with examples",
            "Common mistakes in async coroutines"
        )
    ),
    PromptCategory(
        name = "Study",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        prompts = listOf(
            "Solve quadratic equation with formula",
            "Explain Photosynthesis with key points",
            "Newton's laws of motion with real examples",
            "Fast revision method for exams"
        )
    ),
    PromptCategory(
        name = "Telugu-English",
        icon = Icons.Default.Translate,
        prompts = listOf(
            "AI ante enti simple ga cheppu broh?",
            "Python loops explain cheyyi broh",
            "YouTube channel grow cheyadaniki tips cheppu",
            "Best study routine in Telugu-English"
        )
    ),
    PromptCategory(
        name = "Gaming",
        icon = Icons.Default.SportsEsports,
        prompts = listOf(
            "Top tips for Minecraft beginners",
            "What is FPS and refresh rate in gaming?",
            "What makes RPG games fun?",
            "How to improve game reflexes"
        )
    )
)

@Composable
fun QuickPromptChips(
    selectedCategoryName: String,
    onCategorySelected: (String) -> Unit,
    onPromptClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Mode / Category Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Categories.forEach { category ->
                val isSelected = category.name == selectedCategoryName
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onCategorySelected(if (isSelected) "All" else category.name)
                    },
                    label = { Text(category.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.name,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("category_chip_${category.name}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Prompts corresponding to selected category or mixed
        val activePrompts = Categories.find { it.name == selectedCategoryName }?.prompts
            ?: Categories.map { it.prompts.first() }

        val activeCategory = Categories.find { it.name == selectedCategoryName }?.name ?: "Research"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            activePrompts.forEach { prompt ->
                SuggestionChip(
                    onClick = { onPromptClick(prompt, activeCategory) },
                    label = { Text(prompt, style = MaterialTheme.typography.bodySmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("suggestion_chip_${prompt.take(10)}")
                )
            }
        }
    }
}
