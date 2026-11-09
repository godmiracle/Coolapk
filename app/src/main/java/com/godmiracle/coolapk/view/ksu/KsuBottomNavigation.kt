package com.godmiracle.coolapk.view.ksu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godmiracle.coolapk.R
import com.godmiracle.coolapk.view.ksu.liquid.lens
import com.godmiracle.coolapk.view.ksu.liquid.vibrancy
import com.google.android.material.color.MaterialColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import com.google.android.material.R as MaterialR

private data class NavigationTab(@StringRes val label: Int, @DrawableRes val icon: Int)

private val navigationTabs = listOf(
    NavigationTab(R.string.tab_home, R.drawable.ic_home_baseline),
    NavigationTab(R.string.tab_follow, R.drawable.ic_star),
    NavigationTab(R.string.tab_discover, R.drawable.ic_explorer),
    NavigationTab(R.string.tab_me, R.drawable.ic_account),
)

@Composable
internal fun KsuBottomNavigation(
    selectedIndex: Int,
    backdrop: Backdrop,
    onSelected: (Int) -> Unit,
    onReselected: (Int) -> Unit,
    onSearch: () -> Unit,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val primary = Color(MaterialColors.getColor(context, MaterialR.attr.colorPrimary, android.graphics.Color.BLUE))
    val colors = remember(isDark, primary) {
        if (isDark) darkColorScheme(primary = primary) else lightColorScheme(primary = primary)
    }

    MiuixTheme(colors = colors) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
            val tabWidth = minOf(76.dp, ((maxWidth - 72.dp).coerceAtLeast(0.dp) / 4))
            val barWidth = tabWidth * navigationTabs.size + 8.dp
            Row(
                Modifier.width(barWidth + 64.dp).align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingBottomBar(
                    modifier = Modifier.width(barWidth).pointerInput(Unit) { detectTapGestures { } },
                    selectedIndex = selectedIndex,
                    onSelected = onSelected,
                    onReselected = onReselected,
                    backdrop = backdrop,
                    tabsCount = navigationTabs.size,
                ) { activateTab ->
                    navigationTabs.forEachIndexed { index, item ->
                        FloatingBottomBarItem(
                            selected = selectedIndex == index,
                            onClick = { activateTab(index) },
                            modifier = Modifier.defaultMinSize(minWidth = tabWidth),
                        ) {
                            Icon(
                                painter = painterResource(item.icon),
                                contentDescription = null,
                            )
                            Text(
                                text = stringResource(item.label),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible,
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))
                GlassSearchButton(backdrop, isDark, onSearch)
            }
        }
    }
}

@Composable
private fun GlassSearchButton(backdrop: Backdrop, isDark: Boolean, onSearch: () -> Unit) {
    val containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    val highlight = rememberGravityRotatedHighlight(iosIndicatorSpecular, extraDegrees = -45f)
    val searchLabel = stringResource(R.string.search)
    Box(
        modifier = Modifier
            .size(56.dp)
            .dropShadow(CircleShape, Shadow(radius = 10.dp, color = Color.Black, alpha = if (isDark) 0.2f else 0.1f))
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    padding = maxOf(padding, 40.dp.toPx())
                    vibrancy()
                    blur(4.dp.toPx(), 4.dp.toPx())
                    lens(refractionHeight = 24.dp.toPx(), refractionAmount = 24.dp.toPx())
                },
                highlight = { highlight.value.copy(alpha = 0.75f) },
                onDrawSurface = { drawRect(containerColor) },
            )
            .clip(CircleShape)
            .semantics { contentDescription = searchLabel }
            .clickable(onClickLabel = searchLabel, role = Role.Button, onClick = onSearch),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MiuixTheme.colorScheme.onSurface,
        )
    }
}
