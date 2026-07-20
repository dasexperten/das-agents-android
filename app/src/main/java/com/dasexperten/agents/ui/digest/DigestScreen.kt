package com.dasexperten.agents.ui.digest

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dasexperten.agents.viewmodel.DigestSlideUi
import com.dasexperten.agents.viewmodel.DigestViewModel

/* Colors match org.dasexperten.com digest modal (ui.html) */
private val DgBg = Color(0xFF141015)
private val DgPhotoBg = Color(0xFF1A1519)
private val DgGold = Color(0xD9FEF004) // brand gold #FEF004 ~85% alpha
private val DgGoldSolid = Color(0xFFFEF004)
private val DgWhite = Color(0xFFFFFFFF)
private val DgWhite96 = Color(0xF5FFFFFF)
private val DgWhite94 = Color(0xF0FFFFFF)
private val DgWhite82 = Color(0xD1FFFFFF)
private val DgCloseBg = Color(0x2EFFFFFF)

/**
 * Daily Digest — UI/UX identical to org.dasexperten.com mobile Digest
 * (full-bleed portrait · name on photo · sheet: Отчёт → Мой взгляд → question).
 * No A/B choices (Owner).
 */
@Composable
fun DigestScreen(
    viewModel: DigestViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val slide = state.slides.getOrNull(state.index)
    var dragAcc by remember { mutableFloatStateOf(0f) }
    val config = LocalConfiguration.current
    val padH = (config.screenHeightDp * 0.60f).dp

    Box(
        Modifier
            .fillMaxSize()
            .background(DgBg)
            .pointerInput(state.index, state.slides.size) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragAcc < -80f -> viewModel.next()
                            dragAcc > 80f -> viewModel.prev()
                        }
                        dragAcc = 0f
                    },
                    onHorizontalDrag = { _, dx -> dragAcc += dx },
                )
            },
    ) {
        when {
            state.loading && state.slides.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DgGoldSolid)
                }
            }
            state.error != null && state.slides.isEmpty() -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .statusBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(80.dp))
                    Text(state.error ?: "", color = DgWhite, textAlign = TextAlign.Center)
                    TextButton(onClick = viewModel::refresh) {
                        Text("Повторить", color = DgGoldSolid)
                    }
                }
            }
            slide == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Сегодня пусто — открытых слайдов нет", color = DgWhite82)
                }
                CloseFab(onBack)
            }
            else -> {
                // Fixed full-bleed portrait (dg-right / dg-photo)
                DigestPortrait(slide)

                // Scroll shell: 60% transparent pad + sheet (dg-scroll / dg-sheet)
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding(),
                ) {
                    Spacer(Modifier.height(padH))
                    DigestSheet(slide = slide, index = state.index, total = state.slides.size)
                }

                // Name fixed on photo top (dg-photo-head)
                DigestPhotoHead(slide = slide, index = state.index, total = state.slides.size)

                CloseFab(onBack)
            }
        }
    }
}

@Composable
private fun DigestPortrait(slide: DigestSlideUi) {
    Box(
        Modifier
            .fillMaxSize()
            .background(DgPhotoBg),
    ) {
        // fallback initials under photo (dg-photo-fallback)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF282229), Color(0xFF1A1519), Color(0xFF3A2F38)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = slide.initials,
                color = Color.White.copy(alpha = 0.18f),
                fontSize = 96.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
            )
        }
        AsyncImage(
            model = slide.fullPhotoUrl,
            contentDescription = slide.name,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        // Soft fade (dg-photo-fade mobile)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x2E141015),
                        0.14f to Color.Transparent,
                        0.58f to Color.Transparent,
                        1f to Color(0x1F141015),
                    ),
                ),
        )
    }
}

@Composable
private fun DigestPhotoHead(
    slide: DigestSlideUi,
    index: Int,
    total: Int,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xB80E0B0F),
                    0.42f to Color(0x610E0B0F),
                    0.72f to Color(0x140E0B0F),
                    1f to Color.Transparent,
                ),
            )
            .padding(start = 14.dp, end = 52.dp, top = 12.dp, bottom = 40.dp),
    ) {
        Column {
            Text(
                text = slide.name,
                style = TextStyle(
                    color = DgWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.4).sp,
                    shadow = Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 1f), 12f),
                ),
            )
            if (slide.role.isNotBlank()) {
                Text(
                    text = slide.role.uppercase(),
                    style = TextStyle(
                        color = DgWhite82,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 1f), 8f),
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                text = "${index + 1} / $total",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun DigestSheet(
    slide: DigestSlideUi,
    index: Int,
    total: Int,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.08f to Color(0x8C0E0B0F),
                    0.22f to Color(0xE00E0B0F),
                    0.42f to DgBg,
                    1f to DgBg,
                ),
            )
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
    ) {
        // Отчёт
        CapLabel("Отчёт")
        CapText(slide.report.ifBlank { "Краткий репорт по этой работе ещё не подгрузился." })
        Spacer(Modifier.height(12.dp))
        // Мой взгляд
        CapLabel("Мой взгляд")
        CapText(
            slide.view.ifBlank { "Пока без отдельной рекомендации." },
            slightlySmaller = true,
        )

        // Question under report/view (board mobile order)
        if (slide.question.isNotBlank()) {
            Text(
                text = annotatedBold(slide.question),
                style = TextStyle(
                    color = DgWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.3).sp,
                ),
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
            )
        }

        Text(
            text = "ОСТАЛОСЬ ${total - index - 1} · ГОТОВО $index",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun CapLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            color = DgGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 1f), 8f),
        ),
        modifier = Modifier.padding(bottom = 5.dp),
    )
}

@Composable
private fun CapText(text: String, slightlySmaller: Boolean = false) {
    Text(
        text = annotatedBold(text),
        style = TextStyle(
            color = if (slightlySmaller) DgWhite94 else DgWhite96,
            fontSize = if (slightlySmaller) 16.sp else 16.5.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = if (slightlySmaller) 24.sp else 25.sp,
            shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 1f), 8f),
        ),
    )
}

@Composable
private fun CloseFab(onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 10.dp, end = 10.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .background(DgCloseBg, CircleShape),
        ) {
            Text("×", color = DgWhite, fontSize = 26.sp, fontWeight = FontWeight.Normal)
        }
    }
}

/** Simple **bold** markers like board digestRich. */
private fun annotatedBold(raw: String) = buildAnnotatedString {
    val re = Regex("\\*\\*(.+?)\\*\\*")
    var last = 0
    for (m in re.findAll(raw)) {
        append(raw.substring(last, m.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = DgWhite)) {
            append(m.groupValues[1])
        }
        last = m.range.last + 1
    }
    if (last < raw.length) append(raw.substring(last))
}
