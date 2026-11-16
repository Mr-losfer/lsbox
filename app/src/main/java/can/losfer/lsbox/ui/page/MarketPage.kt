package can.losfer.lsbox.ui.page

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import can.losfer.lsbox.R
import can.losfer.lsbox.market.MarketRepository
import can.losfer.lsbox.market.MarketSite
import can.losfer.lsbox.market.MarketSiteType
import can.losfer.lsbox.ui.components.AppTopBarScaffold
import can.losfer.lsbox.ui.components.SettingsCard
import can.losfer.lsbox.ui.components.SettingsCardPosition
import can.losfer.lsbox.ui.components.SettingsGroup
import can.losfer.lsbox.ui.components.SettingsRow
import can.losfer.lsbox.ui.components.TopBarActionBox

/**
 * 配置站点市场页：从仓库 JSON 拉取订阅站点列表，按点播/直播分组展示。
 * 点击条目即自动添加为订阅并设为当前订阅。
 */
@Composable
fun MarketPage(
    onNavigateBack: () -> Unit,
    onPick: (MarketSite) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var sites by remember { mutableStateOf<List<MarketSite>>(emptyList()) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = false
        val result = MarketRepository.loadSites()
        loading = false
        if (result.isSuccess) {
            sites = result.getOrDefault(emptyList())
        } else {
            error = true
        }
    }

    BackHandler { onNavigateBack() }

    AppTopBarScaffold(
        titleContent = {
            Text(
                text = stringResource(R.string.market_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            TopBarActionBox(
                R.drawable.ic_arrow_left,
                stringResource(R.string.common_back),
                onClick = onNavigateBack,
            )
        },
    ) { topPad, navBottom ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> LoadingState(topPad)
                error -> ErrorState(topPad, onRetry = { reloadKey++ })
                else -> MarketList(
                    sites = sites,
                    topPad = topPad,
                    navBottom = navBottom,
                    onPick = onPick,
                )
            }
        }
    }
}

@Composable
private fun LoadingState(topPad: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.market_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorState(topPad: androidx.compose.ui.unit.Dp, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.market_load_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.market_retry))
        }
    }
}

@Composable
private fun MarketList(
    sites: List<MarketSite>,
    topPad: androidx.compose.ui.unit.Dp,
    navBottom: androidx.compose.ui.unit.Dp,
    onPick: (MarketSite) -> Unit,
) {
    val vodSites = sites.filter { it.type == MarketSiteType.Vod }
    val liveSites = sites.filter { it.type == MarketSiteType.Live }
    val vodTitle = stringResource(R.string.market_group_vod)
    val liveTitle = stringResource(R.string.market_group_live)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPad + 4.dp,
            bottom = navBottom + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        listOf(
            vodTitle to vodSites,
            liveTitle to liveSites,
        ).forEach { (title, group) ->
            if (group.isNotEmpty()) {
                item(key = "header_$title") {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
                itemsIndexed(group, key = { _, s -> "$title-${s.url}" }) { index, site ->
                    SettingsCard(position = cardPosition(index, group.size)) {
                        SiteRow(site = site, onClick = { onPick(site) })
                    }
                }
            }
        }
    }
}

private fun cardPosition(index: Int, size: Int): SettingsCardPosition = when {
    size <= 1 -> SettingsCardPosition.SINGLE
    index == 0 -> SettingsCardPosition.FIRST
    index == size - 1 -> SettingsCardPosition.LAST
    else -> SettingsCardPosition.MIDDLE
}

@Composable
private fun SiteRow(site: MarketSite, onClick: () -> Unit) {
    SettingsRow(
        title = site.name,
        subtitle = site.description.ifBlank {
            if (site.type == MarketSiteType.Vod) {
                stringResource(R.string.market_type_vod)
            } else {
                stringResource(R.string.market_type_live)
            }
        },
        leadingIconRes = if (site.type == MarketSiteType.Vod) {
            R.drawable.ic_settings_play
        } else {
            R.drawable.ic_settings_start
        },
        onClick = onClick,
    )
}
