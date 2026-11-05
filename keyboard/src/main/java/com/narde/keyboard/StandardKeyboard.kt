package com.narde.keyboard

import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Публичный API библиотеки стандартной клавиатуры Android (портретная раскладка)
// ---------------------------------------------------------------------------

/**
 * Клавиши стандартной экранной клавиатуры Android.
 *
 * Потребитель библиотеки сам решает, как отреагировать на каждую клавишу —
 * в том числе на [StdKey.Hide] (свернуть/скрыть клавиатуру), который
 * библиотека не обрабатывает сама.
 */
sealed interface StdKey {
    data class CharKey(val text: String) : StdKey
    object Space : StdKey
    object Backspace : StdKey
    object Enter : StdKey
    object Shift : StdKey
    object LangToggle : StdKey   // RU <-> EN
    object SymbolsToggle : StdKey // ?123 <-> ABC
    object Hide : StdKey          // свернуть клавиатуру (обработка на стороне хоста)
}

private sealed interface StdRowItem {
    data class Char(val text: String) : StdRowItem
    data class Special(val key: StdKey) : StdRowItem
}

/**
 * Стандартная экранная клавиатура Android как отдельный Compose-компонент.
 *
 * Точная копия раскладки системной клавиатуры Android (портрет):
 *   ряд 1: q w e r t y u i o p + ⌫   (равные клавиши)
 *   ряд 2: a s d f g h j k l + ⏎     (равные клавиши)
 *   ряд 3: ⇧ + z x c v b n m + , . + ?123   (равные клавиши)
 *   ряд 4: RU/EN + широкая Пробел + ▼ свернуть
 *
 * По сравнению с матричной версией убрано:
 *   - матричный дождь и любая анимация (Canvas, LaunchedEffect, delay, tick)
 *   - индикатор состояния (точка + надпись "CAPS ACTIVE / caps off")
 *   - панель "SECURE CLIENT INPUT // RU-CYRILLIC / EN-LATIN / SYMBOLS"
 *   - неоновая подсветка, зелёный терминальный стиль, моноширинный шрифт
 *
 * Оставлено:
 *   - кнопка сворачивания ▼ (StdKey.Hide) — хост сам скрывает клавиатуру
 *   - звук нажатия клавиши (SoundEffectConstants.CLICK)
 *   - переключение RU/EN, режим символов ?123/ABC, shift со сбросом после буквы
 *
 * @param onKey обработчик нажатий; хост отвечает за логику ввода и скрытие
 *   клавиатуры по [StdKey.Hide]
 * @param lang начальная раскладка: "RU" или "EN"
 * @param modifier внешний модификатор (размещение, padding и т.п.)
 */
@Composable
fun StandardKeyboard(
    onKey: (StdKey) -> Unit,
    lang: String = "RU",
    modifier: Modifier = Modifier
) {
    val isRussian = remember { mutableStateOf(lang == "RU") }
    val isShiftActive = remember { mutableStateOf(false) }
    val isSymbolsMode = remember { mutableStateOf(false) }

    val localView = LocalView.current
    val playClick = {
        try {
            localView.playSoundEffect(SoundEffectConstants.CLICK)
        } catch (_: Exception) {}
    }
    // Fix (NeXX fork): SymbolsToggle переключается внутри библиотеки, а не уходит хосту —
    // в оригинале isSymbolsMode никогда не менялся и ряд ?123 был недостижим.
    val emitKey: (StdKey) -> Unit = { key ->
        if (key is StdKey.SymbolsToggle) isSymbolsMode.value = !isSymbolsMode.value else onKey(key)
    }

    val enRow1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val enRow2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val enRow3 = listOf("z", "x", "c", "v", "b", "n", "m")

    val ruRow1 = listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ")
    val ruRow2 = listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э")
    val ruRow3 = listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю")

    val symRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val symRow2 = listOf("@", "#", "$", "%", "&", "*", "-", "+", "=", "(")
    val symRow3 = listOf(")", "!", "?", "\"", "'", ":", ";", "/", "\\")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(top = 4.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Ряд 1: буквы + backspace
            StdKeyRow(
                items = (if (isSymbolsMode.value) symRow1 else if (isRussian.value) ruRow1 else enRow1)
                    .map { StdRowItem.Char(it) } + StdRowItem.Special(StdKey.Backspace),
                shift = false,
                symbols = false,
                onChar = { playClick(); emitKey(StdKey.CharKey(it)) },
                onSpecial = { playClick(); emitKey(it) }
            )
            // Ряд 2: буквы + enter
            StdKeyRow(
                items = (if (isSymbolsMode.value) symRow2 else if (isRussian.value) ruRow2 else enRow2)
                    .map { StdRowItem.Char(it) } + StdRowItem.Special(StdKey.Enter),
                shift = isShiftActive.value,
                symbols = false,
                onChar = { text ->
                    playClick()
                    emitKey(StdKey.CharKey(text))
                    if (isShiftActive.value) isShiftActive.value = false
                },
                onSpecial = { playClick(); emitKey(it) }
            )
            // Ряд 3: shift + буквы + , . + ?123
            if (isSymbolsMode.value) {
                StdKeyRow(
                    items = symRow3.map { StdRowItem.Char(it) } + StdRowItem.Special(StdKey.SymbolsToggle),
                    shift = false,
                    symbols = true,
                    onChar = { playClick(); emitKey(StdKey.CharKey(it)) },
                    onSpecial = { playClick(); emitKey(it) }
                )
            } else {
                StdKeyRow(
                    items = listOf(StdRowItem.Special(StdKey.Shift))
                        + (if (isRussian.value) ruRow3 else enRow3).map { StdRowItem.Char(it) }
                        + listOf(
                            StdRowItem.Char(","),
                            StdRowItem.Char("."),
                            StdRowItem.Special(StdKey.SymbolsToggle)
                        ),
                    shift = isShiftActive.value,
                    symbols = false,
                    onChar = { text ->
                        playClick()
                        emitKey(StdKey.CharKey(text))
                        if (isShiftActive.value) isShiftActive.value = false
                    },
                    onSpecial = { key ->
                        playClick()
                        if (key is StdKey.Shift) {
                            isShiftActive.value = !isShiftActive.value
                        } else {
                            emitKey(key)
                        }
                    }
                )
            }

            // Ряд 4: RU/EN + Пробел + ▼ свернуть
            StdBottomRow(
                isRussian = isRussian.value,
                isSymbolsMode = isSymbolsMode.value,
                onLangToggle = { playClick(); isRussian.value = !isRussian.value },
                onSpace = { playClick(); emitKey(StdKey.Space) },
                onHide = { playClick(); emitKey(StdKey.Hide) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Внутренние compose-примитивы библиотеки
// ---------------------------------------------------------------------------

/**
 * Один ряд клавиш: каждая клавиша занимает равную долю ширины ряда
 * (как в системной клавиатуре Android). Размер через Layout — Modifier.weight()
 * internal в этой версии Compose.
 */
@Composable
private fun StdKeyRow(
    items: List<StdRowItem>,
    shift: Boolean,
    symbols: Boolean,
    onChar: (String) -> Unit,
    onSpecial: (StdKey) -> Unit
) {
    val spacingPx = 8f
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            items.forEach { item ->
                when (item) {
                    is StdRowItem.Char -> {
                        val disp = if (shift) item.text.uppercase() else item.text.lowercase()
                        StdKeyButton(text = disp, onClick = { onChar(disp) })
                    }
                    is StdRowItem.Special -> {
                        val (text, active) = when (item.key) {
                            StdKey.Shift -> "⇧" to shift
                            StdKey.Backspace -> "⌫" to false
                            StdKey.Enter -> "⏎" to false
                            StdKey.SymbolsToggle -> (if (symbols) "ABC" else "?123") to false
                            else -> "" to false
                        }
                        StdKeyButton(text = text, onClick = { onSpecial(item.key) }, isActive = active)
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val spacing = spacingPx.toInt()
        // Fix (NeXX fork): высота ряда ограничена — в Column под полем ввода maxHeight = Infinity,
        // Constraints.fixed/layout с Infinity ломают измерение. Клавиша фиксирована 44.dp.
        // MeasureScope : Density, поэтому 44.dp.roundToPx() доступен прямо здесь.
        val rowHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else 44.dp.roundToPx()
        val availableWidth = constraints.maxWidth - spacing * (measurables.size - 1)
        val keyWidth = availableWidth / measurables.size
        val placeables = measurables.map { it.measure(Constraints.fixed(keyWidth, rowHeight)) }
        layout(constraints.maxWidth, rowHeight) {
            var xPosition = 0
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(xPosition, 0)
                xPosition += placeable.width + spacing
            }
        }
    }
}

/**
 * Нижний ряд: переключатель языка + широкая пробел + ▼ свернуть.
 */
@Composable
private fun StdBottomRow(
    isRussian: Boolean,
    isSymbolsMode: Boolean,
    onLangToggle: () -> Unit,
    onSpace: () -> Unit,
    onHide: () -> Unit
) {
    val spacingPx = 8f
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            if (!isSymbolsMode) {
                StdKeyButton(
                    text = if (isRussian) "RU" else "EN",
                    onClick = onLangToggle
                )
            }
            StdKeyButton(
                text = if (isRussian) "Пробел" else "SPACE",
                onClick = onSpace
            )
            StdKeyButton(
                text = "▼",
                onClick = onHide
            )
        }
    ) { measurables, constraints ->
        val spacing = spacingPx.toInt()
        // Fix (NeXX fork): bounded height, см. StdKeyRow (MeasureScope : Density)
        val rowHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else 44.dp.roundToPx()
        val keyCount = measurables.size
        val placeables = measurables.mapIndexed { index, measurable ->
            val width = when (index) {
                0 -> if (isSymbolsMode) 0 else (constraints.maxWidth - spacing * 2) / 5
                keyCount - 1 -> if (isSymbolsMode) (constraints.maxWidth - spacing) / 2 else (constraints.maxWidth - spacing * 2) / 5
                else -> (constraints.maxWidth - spacing * 2) * 3 / 5
            }.coerceAtLeast(1)
            measurable.measure(Constraints.fixed(width, rowHeight))
        }
        layout(constraints.maxWidth, rowHeight) {
            var xPosition = 0
            placeables.forEachIndexed { index, placeable ->
                if (placeable.width > 0) {
                    placeable.placeRelative(xPosition, 0)
                    xPosition += placeable.width + spacing
                }
            }
        }
    }
}

@Composable
private fun StdKeyButton(
    text: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bg = when {
        isActive -> Color(0xFFE8EAED) // зафиксированная клавиша (shift)
        else -> Color(0xFFFFFFFF)
    }
    val borderColor = when {
        isActive -> Color(0xFF3C4043)
        else -> Color(0xFFDADCE0)
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color(0xFF3C4043),
                fontSize = 20.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Default
            )
        )
    }
}
