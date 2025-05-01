package com.OxGames.Pluvia.ui.screen.settings.components

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.alorma.compose.settings.ui.SettingsSlider
import com.alorma.compose.settings.ui.SettingsSwitch
import com.alorma.compose.settings.ui.base.internal.SettingsTileDefaults
import com.alorma.compose.settings.ui.base.internal.SettingsTileScaffold

enum class SettingsItemType(val value: Int) {
    SWITCH(1),
    SPINNER(2),
    CHECKBOX(3),
    SEEKBAR(4)
}

@Composable
fun SettingsItemList(
    title: String,
    description: String,
    type: SettingsItemType,
    key: String,
    defaultValue: String,
    spinnerOptions: Array<String>? = null,
    seekBarMaxMinValues: Array<Int>? = null,
) {
    when (type) {
        SettingsItemType.SWITCH -> {
            var preference by remember {
                val value = PrefManager.getBoolean(key, defaultValue.toBoolean())
                mutableStateOf(value)
            }
            SettingsSwitch(
                state = preference,
                title = { Text(text = title) },
                subtitle = { Text(text = description) },
                onCheckedChange = {
                    preference = it
                    PrefManager.setBoolean(key, it)
                },
            )
        }

        SettingsItemType.SPINNER -> {
            var expanded by remember(key) { mutableStateOf(false) }
            var preference by remember(key) {
                val value = PrefManager.getString(key, defaultValue)!!
                mutableStateOf(value)
            }

            SettingsTileScaffold(
                title = { Text(text = title) },
                subtitle = { Text(text = description) },
                action = {
                    Column {
                        Row(
                            modifier = Modifier
                                .clickable { expanded = true }
                                .border(1.dp, SettingsTileDefaults.colors().titleColor, RoundedCornerShape(8))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = spinnerOptions!!.find { it == preference }!!, fontSize = 14.sp)
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null, // TODO
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            content = {
                                spinnerOptions!!.forEach {
                                    DropdownMenuItem(
                                        text = { Text(text = it) },
                                        onClick = {
                                            PrefManager.setString(key, it)
                                            preference = it
                                            expanded = false
                                        },
                                    )
                                }
                            },
                        )
                    }
                },
            )
        }

        SettingsItemType.CHECKBOX -> {
            var expanded by remember(key) { mutableStateOf(false) }
            var preference by remember(key) {
                val value = PrefManager.getString(key, defaultValue)!!
                mutableStateOf(value)
            }
            SettingsTileScaffold(
                title = { Text(text = title) },
                subtitle = { Text(text = description) },
                action = {
                    Column {
                        Row(
                            modifier = Modifier
                                .clickable { expanded = true }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(.40f),
                                text = preference.ifEmpty { "Nothing Selected" }, // TODO
                                fontSize = 14.sp,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null, // TODO
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            content = {
                                val onClick: (String) -> Unit = { str ->
                                    val checked = preference.split(",").filter { it.isNotEmpty() }.toMutableSet()
                                    if (checked.contains(str)) {
                                        checked.remove(str)
                                    } else {
                                        checked.add(str)
                                    }

                                    // Lazy way to sort the list.
                                    val builder = spinnerOptions!!
                                        .filter { checked.contains(it) }
                                        .joinToString(",")
                                    preference = builder
                                    PrefManager.setString(key, builder)
                                }

                                spinnerOptions!!.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(text = item) },
                                        leadingIcon = {
                                            Checkbox(
                                                checked = preference.split(",").contains(item),
                                                onCheckedChange = { onClick(item) },
                                            )
                                        },
                                        onClick = { onClick(item) },
                                    )
                                }
                            },
                        )
                    }
                },
            )
        }

        SettingsItemType.SEEKBAR -> {
            var preference by remember(key) {
                val value = PrefManager.getInt(key, defaultValue.toInt())!!
                mutableIntStateOf(value)
            }
            SettingsSlider(
                modifier = Modifier.height(96.dp),
                value = preference.toFloat(),
                valueRange = seekBarMaxMinValues!![0].toFloat()..seekBarMaxMinValues[1].toFloat(),
                title = { Text(text = title) },
                subtitle = {
                    val text = if (preference == 0) {
                        stringResource(R.string.unlimited)
                    } else {
                        "$preference FPS"
                    }
                    Text(text = "$description ($text)")
                },
                onValueChange = { newValue ->
                    preference = newValue.toInt()
                    PrefManager.setInt(key, preference)
                },
            )
        }
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsListSpinner() {
    val context = LocalContext.current
    PrefManager.init(context)
    PluviaTheme {
        Surface {
            Column {
                SettingsItemList(
                    title = "SWITCH",
                    description = "A Switch Item",
                    type = SettingsItemType.SWITCH,
                    key = "switch",
                    defaultValue = "false",
                )
                SettingsItemList(
                    title = "SPINNER",
                    description = "A Spinner Item",
                    type = SettingsItemType.SPINNER,
                    key = "spinner",
                    defaultValue = "CCC333",
                    spinnerOptions = arrayOf("AAA111", "BBB222", "CCC333", "DDD444"),
                )
                SettingsItemList(
                    title = "CHECKBOX",
                    description = "A Checkbox Item",
                    type = SettingsItemType.CHECKBOX,
                    key = "checkbox",
                    defaultValue = "AAA111,CCC333,DDD444,EEE555,",
                    spinnerOptions = arrayOf("AAA111", "BBB222", "CCC333", "DDD444", "EEE555"),
                )
                SettingsItemList(
                    title = "SEEKBAR",
                    description = "A Seekbar Item",
                    type = SettingsItemType.SEEKBAR,
                    key = "seekbar",
                    defaultValue = "40",
                    seekBarMaxMinValues = arrayOf(0, 100),
                )
            }
        }
    }
}
