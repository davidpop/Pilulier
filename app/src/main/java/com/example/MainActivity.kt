package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AddEditMedicationScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MedicationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PilulierTheme
import com.example.ui.viewmodel.PilulierViewModel
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {

    private val viewModel: PilulierViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (appTheme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDark
            }

            PilulierTheme(darkTheme = isDarkTheme) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PilulierViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val editingMedication by viewModel.editingMedication.collectAsStateWithLifecycle()

    // Gestion du retour arrière Android
    if (currentScreen != ScreenTab.HOME) {
        BackHandler {
            viewModel.navigateBack()
        }
    }

    val showBottomBar = currentScreen == ScreenTab.HOME ||
            currentScreen == ScreenTab.TREATMENTS ||
            currentScreen == ScreenTab.HISTORY

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == ScreenTab.HOME,
                        onClick = { viewModel.navigateTo(ScreenTab.HOME) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == ScreenTab.HOME) Icons.Filled.Today else Icons.Outlined.Today,
                                contentDescription = "Aujourd'hui"
                            )
                        },
                        label = {
                            Text(
                                text = "Aujourd'hui",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (currentScreen == ScreenTab.HOME) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenTab.TREATMENTS,
                        onClick = { viewModel.navigateTo(ScreenTab.TREATMENTS) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == ScreenTab.TREATMENTS) Icons.Filled.Medication else Icons.Outlined.Medication,
                                contentDescription = "Mes traitements"
                            )
                        },
                        label = {
                            Text(
                                text = "Mes traitements",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (currentScreen == ScreenTab.TREATMENTS) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_treatments")
                    )

                    NavigationBarItem(
                        selected = currentScreen == ScreenTab.HISTORY,
                        onClick = { viewModel.navigateTo(ScreenTab.HISTORY) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == ScreenTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                contentDescription = "Historique"
                            )
                        },
                        label = {
                            Text(
                                text = "Historique",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (currentScreen == ScreenTab.HISTORY) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_history")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            ScreenTab.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenSettings = { viewModel.navigateTo(ScreenTab.SETTINGS) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            ScreenTab.TREATMENTS -> {
                MedicationsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            ScreenTab.HISTORY -> {
                HistoryScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            ScreenTab.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            ScreenTab.ADD_EDIT -> {
                AddEditMedicationScreen(
                    viewModel = viewModel,
                    editingMedication = editingMedication,
                    onBack = { viewModel.navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
