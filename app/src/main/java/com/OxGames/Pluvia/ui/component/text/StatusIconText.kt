package com.OxGames.Pluvia.ui.component.text

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.OxGames.Pluvia.data.SteamFriend
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import java.util.EnumSet

/**
 * A Composable for [SteamFriend] that displays their name or nick name,
 * and shows a status icon if they have a [EPersonaStateFlag] set.
 */
@Composable
fun StatusIconText(
    friend: SteamFriend,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        text = buildAnnotatedString {
            append(friend.nameOrNickname)
            if (friend.nickname.isNotEmpty()) {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                    ),
                ) {
                    append(" * ")
                }
            } else {
                append(" ")
            }
            appendInlineContent("icon", "[icon]")
        },
        inlineContent = mapOf(
            "icon" to InlineTextContent(
                Placeholder(
                    width = style.fontSize.value.sp,
                    height = style.fontSize.value.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
                children = {
                    friend.statusIcon?.let {
                        Icon(
                            imageVector = it,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = it.name,
                        )
                    }
                },
            ),
        ),
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_StatusIconText() {
    PluviaTheme {
        Surface {
            Column {
                StatusIconText(
                    friend = SteamFriend(
                        id = 0,
                        name = "Friend Name",
                        stateFlags = EnumSet.of(EPersonaStateFlag.ClientTypeVR),
                    ),
                )
                StatusIconText(
                    friend = SteamFriend(
                        id = 0,
                        name = "StatusIconText",
                        nickname = "Nickname",
                        stateFlags = EnumSet.of(EPersonaStateFlag.ClientTypeMobile),
                    ),
                )
                StatusIconText(
                    friend = SteamFriend(
                        id = 0,
                        name = "Friend Name",
                    ),
                )
            }
        }
    }
}
