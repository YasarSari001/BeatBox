package com.blackstack.beatbox.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Aşağı çekerek yenileme (pull-to-refresh) sarmalayıcısı.
 *
 * Material3'ün [PullToRefreshBox] bileşenini kullanır; içerik kaydırılabilir
 * olduğunda (LazyColumn vb.) jest ve gösterge yönetimini kendisi yapar.
 * [isRefreshing] true olduğunda üstte aurora renginde ilerleme göstergesi
 * görünür.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary
            )
        },
        content = content
    )
}
