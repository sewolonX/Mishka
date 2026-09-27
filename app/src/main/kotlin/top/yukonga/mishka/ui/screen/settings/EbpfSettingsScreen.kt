package top.yukonga.mishka.ui.screen.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.mishka.R
import top.yukonga.mishka.domain.model.EbpfOverride
import top.yukonga.mishka.platform.PlatformStorage
import top.yukonga.mishka.platform.StorageKeys
import top.yukonga.mishka.ui.component.AdaptiveTopAppBar
import top.yukonga.mishka.ui.component.CardItem
import top.yukonga.mishka.ui.component.RestartRequiredHint
import top.yukonga.mishka.ui.component.TetherInterfaceEditDialog
import top.yukonga.mishka.ui.component.blur.BlurredBar
import top.yukonga.mishka.ui.component.blur.rememberBlurBackdrop
import top.yukonga.mishka.ui.component.groupedCardItems
import top.yukonga.mishka.ui.util.horizontalCutoutPadding
import top.yukonga.mishka.viewmodel.EbpfSettingsViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun EbpfSettingsScreen(
    viewModel: EbpfSettingsViewModel,
    storage: PlatformStorage? = null,
    onBack: () -> Unit = {},
) {
    val uiState = viewModel.state.value
    val ebpf = uiState.ebpf ?: EbpfOverride()
    val scrollBehavior = MiuixScrollBehavior()

    // mode: 0=local, 1=shared, 2=hybrid
    var modeIndex by remember(ebpf.mode) {
        mutableIntStateOf(
            when (ebpf.mode) {
                "shared" -> 1
                "hybrid" -> 2
                else -> 0
            }
        )
    }
    val showLocal = modeIndex == 0 || modeIndex == 2
    val showShared = modeIndex == 1 || modeIndex == 2

    // local settings
    var localDataPlaneIndex by remember(ebpf.localDataPlane) {
        mutableIntStateOf(if (ebpf.localDataPlane == "tc") 1 else 0)
    }
    var localDnsModeIndex by remember(ebpf.localDnsMode) {
        mutableIntStateOf(dnsModeToIndex(ebpf.localDnsMode))
    }
    var localIpv6 by remember(ebpf.localIpv6) { mutableStateOf(ebpf.localIpv6) }
    var localBypassPrivate by remember(ebpf.localBypassPrivateAddress) { mutableStateOf(ebpf.localBypassPrivateAddress) }

    // shared settings
    var sharedDataPlaneIndex by remember(ebpf.sharedDataPlane) {
        mutableIntStateOf(if (ebpf.sharedDataPlane == "socket_assign") 1 else 0)
    }
    var sharedDnsModeIndex by remember(ebpf.sharedDnsMode) {
        mutableIntStateOf(dnsModeToIndex(ebpf.sharedDnsMode))
    }
    var sharedIpv6 by remember(ebpf.sharedIpv6) { mutableStateOf(ebpf.sharedIpv6) }
    var sharedBypassPrivate by remember(ebpf.sharedBypassPrivateAddress) { mutableStateOf(ebpf.sharedBypassPrivateAddress) }

    // 热点接口（从 ROOT_TETHER_IFACES 读写）
    val defaultTetherIfaces = "wlan2"
    var tetherIfaces by remember {
        mutableStateOf(storage?.getString(StorageKeys.ROOT_TETHER_IFACES, defaultTetherIfaces) ?: defaultTetherIfaces)
    }
    var showTetherDialog by remember { mutableStateOf(false) }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.ebpf_settings_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            val layoutDirection = LocalLayoutDirection.current
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.common_back),
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                                },
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalCutoutPadding()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
            ),
        ) {
            // === 重启提示 ===
            item { RestartRequiredHint() }

            // === 数据路径 ===
            item { SmallTitle(text = stringResource(R.string.ebpf_general)) }
            groupedCardItems(
                keyPrefix = "ebpf_general",
                outerBottomPadding = 0.dp,
                items = buildList {
                    add(CardItem("ebpfMode") {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.ebpf_mode),
                            summary = when (modeIndex) {
                                1 -> stringResource(R.string.ebpf_mode_shared_summary)
                                2 -> stringResource(R.string.ebpf_mode_hybrid_summary)
                                else -> stringResource(R.string.ebpf_mode_local_summary)
                            },
                            items = listOf(
                                stringResource(R.string.ebpf_mode_local),
                                stringResource(R.string.ebpf_mode_shared),
                                stringResource(R.string.ebpf_mode_hybrid),
                            ),
                            selectedIndex = modeIndex,
                            onSelectedIndexChange = { index ->
                                modeIndex = index
                                val mode = when (index) {
                                    1 -> "shared"
                                    2 -> "hybrid"
                                    else -> "local"
                                }
                                viewModel.updateEbpf { current -> current.copy(mode = mode) }
                            },
                        )
                    })
                },
            )

            // === 本机 ===
            if (showLocal) {
                item { SmallTitle(text = stringResource(R.string.ebpf_local)) }
                groupedCardItems(
                    keyPrefix = "ebpf_local",
                    outerBottomPadding = 0.dp,
                    items = buildList {
                        add(CardItem("localDataPlane") {
                            OverlayDropdownPreference(
                                title = stringResource(R.string.ebpf_data_plane),
                                summary = if (localDataPlaneIndex == 0) {
                                    stringResource(R.string.ebpf_data_plane_cgroup_summary)
                                } else {
                                    stringResource(R.string.ebpf_data_plane_tc_summary)
                                },
                                items = listOf("cgroup", "TC"),
                                selectedIndex = localDataPlaneIndex,
                                onSelectedIndexChange = { index ->
                                    localDataPlaneIndex = index
                                    viewModel.updateEbpf { current ->
                                        current.copy(localDataPlane = if (index == 0) "cgroup" else "tc")
                                    }
                                },
                            )
                        })
                        add(CardItem("localDnsMode") {
                            OverlayDropdownPreference(
                                title = stringResource(R.string.ebpf_dns_mode),
                                summary = dnsModeSummary(localDnsModeIndex),
                                items = dnsModeItems(),
                                selectedIndex = localDnsModeIndex,
                                onSelectedIndexChange = { index ->
                                    localDnsModeIndex = index
                                    viewModel.updateEbpf { current ->
                                        current.copy(localDnsMode = indexToDnsMode(index))
                                    }
                                },
                            )
                        })
                        add(CardItem("localIpv6") {
                            SwitchPreference(
                                title = stringResource(R.string.ebpf_ipv6),
                                summary = stringResource(R.string.ebpf_ipv6_summary),
                                checked = localIpv6,
                                onCheckedChange = {
                                    localIpv6 = it
                                    viewModel.updateEbpf { current -> current.copy(localIpv6 = it) }
                                },
                            )
                        })
                        add(CardItem("localBypassPrivate") {
                            SwitchPreference(
                                title = stringResource(R.string.ebpf_bypass_private),
                                summary = stringResource(R.string.ebpf_bypass_private_summary),
                                checked = localBypassPrivate,
                                onCheckedChange = {
                                    localBypassPrivate = it
                                    viewModel.updateEbpf { current ->
                                        current.copy(localBypassPrivateAddress = it)
                                    }
                                },
                            )
                        })
                    },
                )
            }

            // === 共享 ===
            if (showShared) {
                item { SmallTitle(text = stringResource(R.string.ebpf_shared)) }
                groupedCardItems(
                    keyPrefix = "ebpf_shared",
                    outerBottomPadding = 0.dp,
                    items = buildList {
                        add(CardItem("sharedDataPlane") {
                            OverlayDropdownPreference(
                                title = stringResource(R.string.ebpf_data_plane),
                                summary = if (sharedDataPlaneIndex == 0) {
                                    stringResource(R.string.ebpf_data_plane_packet_rewrite_summary)
                                } else {
                                    stringResource(R.string.ebpf_data_plane_socket_assign_summary)
                                },
                                items = listOf("packet_rewrite", "socket_assign"),
                                selectedIndex = sharedDataPlaneIndex,
                                onSelectedIndexChange = { index ->
                                    sharedDataPlaneIndex = index
                                    viewModel.updateEbpf { current ->
                                        current.copy(sharedDataPlane = if (index == 0) "packet_rewrite" else "socket_assign")
                                    }
                                },
                            )
                        })
                        add(CardItem("sharedDnsMode") {
                            OverlayDropdownPreference(
                                title = stringResource(R.string.ebpf_dns_mode),
                                summary = dnsModeSummary(sharedDnsModeIndex),
                                items = dnsModeItems(),
                                selectedIndex = sharedDnsModeIndex,
                                onSelectedIndexChange = { index ->
                                    sharedDnsModeIndex = index
                                    viewModel.updateEbpf { current ->
                                        current.copy(sharedDnsMode = indexToDnsMode(index))
                                    }
                                },
                            )
                        })
                        add(CardItem("sharedIpv6") {
                            SwitchPreference(
                                title = stringResource(R.string.ebpf_ipv6),
                                summary = stringResource(R.string.ebpf_ipv6_summary),
                                checked = sharedIpv6,
                                onCheckedChange = {
                                    sharedIpv6 = it
                                    viewModel.updateEbpf { current -> current.copy(sharedIpv6 = it) }
                                },
                            )
                        })
                        add(CardItem("sharedBypassPrivate") {
                            SwitchPreference(
                                title = stringResource(R.string.ebpf_bypass_private),
                                summary = stringResource(R.string.ebpf_bypass_private_summary),
                                checked = sharedBypassPrivate,
                                onCheckedChange = {
                                    sharedBypassPrivate = it
                                    viewModel.updateEbpf { current ->
                                        current.copy(sharedBypassPrivateAddress = it)
                                    }
                                },
                            )
                        })
                        add(CardItem("sharedIfaces") {
                            ArrowPreference(
                                title = stringResource(R.string.root_tether_ifaces_title),
                                summary = tetherIfaces.ifEmpty { defaultTetherIfaces },
                                onClick = { showTetherDialog = true },
                            )
                        })
                    },
                )
            }

            item {
                Spacer(
                    Modifier
                        .height(24.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }

    TetherInterfaceEditDialog(
        show = showTetherDialog,
        title = stringResource(R.string.root_tether_ifaces_title),
        initialValue = tetherIfaces,
        onDismiss = { showTetherDialog = false },
        onConfirm = { csv ->
            tetherIfaces = csv
            storage?.putString(StorageKeys.ROOT_TETHER_IFACES, csv)
        },
    )
}

// === DNS mode helpers ===

private fun dnsModeToIndex(mode: String) = when (mode) {
    "respect_policy" -> 1
    "off" -> 2
    else -> 0
}

private fun indexToDnsMode(index: Int) = when (index) {
    1 -> "respect_policy"
    2 -> "off"
    else -> "hijack"
}

@Composable
private fun dnsModeSummary(index: Int) = when (index) {
    1 -> stringResource(R.string.ebpf_dns_mode_respect_summary)
    2 -> stringResource(R.string.ebpf_dns_mode_off_summary)
    else -> stringResource(R.string.ebpf_dns_mode_hijack_summary)
}

@Composable
private fun dnsModeItems() = listOf(
    stringResource(R.string.ebpf_dns_mode_hijack),
    stringResource(R.string.ebpf_dns_mode_respect),
    stringResource(R.string.ebpf_dns_mode_off),
)