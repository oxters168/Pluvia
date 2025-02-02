package com.OxGames.Pluvia.ui.component.text

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.OxGames.Pluvia.ui.theme.PluviaTheme

@Composable
fun TypingIndicator(
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val animationState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "typing sequence",
    )

    val stateToVisibleDots = remember {
        mapOf(0 to 0, 1 to 1, 2 to 2, 3 to 3)
    }

    val currentState = animationState.value.toInt() % 4
    val visibleDots = stateToVisibleDots[currentState] ?: 1

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "is typing",
            color = color,
            fontSize = fontSize,
        )

        repeat(3) { index ->
            Text(
                text = ".",
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = if (index < visibleDots) 0f else .25f),
                fontSize = fontSize,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_TypingIndicator() {
    PluviaTheme {
        Surface {
            TypingIndicator()
        }
    }
}
