package com.lizz.myapptemplate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.lizz.myapptemplate.designsystem.Theme

@Composable
internal fun AppRouteContentContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.md)
            .clipToBounds(),
    ) {
        content()
    }
}
