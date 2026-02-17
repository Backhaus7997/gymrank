package com.example.gymrank.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymrank.ui.theme.GymRankColors
import com.example.gymrank.ui.components.PrimaryButton
import com.example.gymrank.ui.components.SecondaryButton

@Composable
fun OnboardingQuestionScaffold(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    nextEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GymRankColors.Background,
                        GymRankColors.Surface.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        // Hero visual watermark in the free space
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            // Minimal watermark: big brand text (subtle)
            Text(
                text = "GYM RANK",
                color = GymRankColors.PrimaryAccent.copy(alpha = 0.12f),
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top title (optional)
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = GymRankColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Question card area
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }

            // Bottom actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onBack != null) {
                    SecondaryButton(
                        text = "Volver",
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (onNext != null) {
                    PrimaryButton(
                        text = "Continuar",
                        onClick = onNext,
                        enabled = nextEnabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
