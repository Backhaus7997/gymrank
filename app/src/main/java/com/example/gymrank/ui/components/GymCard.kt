package com.example.gymrank.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gymrank.domain.model.GymUi
import com.example.gymrank.ui.theme.GymRankColors

@Composable
fun GymCard(
    gym: GymUi,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = GymRankColors.PrimaryAccent
    val cardShape = RoundedCornerShape(18.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
        shape = cardShape,
        color = Color(0xFF0F0F10),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ✅ Imagen de fondo (como Feed) por URL
            if (gym.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = gym.imageUrl,
                    contentDescription = "Gym image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ✅ Overlay oscuro para legibilidad
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )

            // ✅ Contenido
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {

                    if (gym.isHighCompetition) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.Black.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Text(
                                text = "🔥  Alta competencia",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    Text(
                        text = gym.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = gym.location,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onJoin,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color(0xFF000000)
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Unirme", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
