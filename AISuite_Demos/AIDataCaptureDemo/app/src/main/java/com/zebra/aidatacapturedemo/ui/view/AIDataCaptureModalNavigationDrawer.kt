package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AIDataCaptureModalNavigationDrawer(
    drawerState: DrawerState,
    innerPadding: PaddingValues,
    activityInnerPadding: PaddingValues,
    selectedItem: String,
    onSelectedItemValuesChange: (String) -> Unit,
    scope: CoroutineScope,
    navController: NavHostController,
    viewModel: AIDataCaptureDemoViewModel,
    context: Context,
    activityLifecycle: Lifecycle
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.75f) // Set the width to 75%
                    .padding(
                        top = innerPadding.calculateTopPadding() - activityInnerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    ),
                drawerShape = RectangleShape,
                drawerContainerColor = Variables.surfaceTertiary,
            ) {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "Home",

                            // Standard/Title Small
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(500),
                                color = Variables.inverseDefault
                            )
                        )
                    },
                    selected = selectedItem == "Home",
                    onClick = {
                        onSelectedItemValuesChange("Home")
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home",
                            tint = Variables.inverseDefault
                        )
                    },
                    shape = RectangleShape,
                    modifier = Modifier
                        .height(40.dp)
                        .padding(horizontal = 0.dp, vertical = 0.dp),
                    colors = getNavigationDrawerItemColor()
                )
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = stringResource(R.string.about),

                            // Standard/Title Small
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(500),
                                color = Variables.inverseDefault
                            )
                        )
                    },
                    selected = selectedItem == stringResource(R.string.about),
                    onClick = {
                        onSelectedItemValuesChange(context.getString(R.string.about))
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Variables.inverseDefault
                        )
                    },
                    shape = RectangleShape,
                    modifier = Modifier.height(40.dp),
                    colors = getNavigationDrawerItemColor()
                )
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "Send Feedback",

                            // Standard/Title Small
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(500),
                                color = Variables.inverseDefault
                            )
                        )
                    },
                    selected = selectedItem == "Send Feedback",
                    onClick = {
                        onSelectedItemValuesChange("Send Feedback")
                        scope.launch { drawerState.close() }

                        openFeedbackUrl(context = context)
                    },
                    icon = {
                        Icon(
                            ImageVector.Companion.vectorResource(R.drawable.satisfied_icon),
                            contentDescription = "Send Feedback",
                            tint = Variables.inverseDefault
                        )
                    },
                    shape = RectangleShape,
                    modifier = Modifier.height(40.dp),
                    colors = getNavigationDrawerItemColor()
                )

                Spacer(Modifier.weight(1f))
                HorizontalDivider(thickness = 0.25.dp, color = Variables.textSubtle)
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = ImageVector.Companion.vectorResource(R.drawable.zebra_logo_icon),
                        contentDescription = "App Information",
                        tint = Variables.surfaceDefault
                    )
                    Text(
                        text = "Powered by Zebra Mobile Computing AI Suite",
                        style = TextStyle(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.textSubtle,
                            textAlign = TextAlign.Center,
                        )
                    )
//                    Spacer(modifier = Modifier.height(14.dp))

                }
            }
        },
        // This is the main content of the screen that the drawer will slide over.
        content = {
            if (selectedItem == "About") {
                AboutScreen(innerPadding = innerPadding)
            } else {
                NavigationStack(
                    navController,
                    viewModel,
                    activityInnerPadding = activityInnerPadding,
                    innerPadding,
                    context,
                    activityLifecycle
                )
            }
        }
    )
}

@Composable
private fun getNavigationDrawerItemColor(): NavigationDrawerItemColors {
    return NavigationDrawerItemDefaults.colors(
        selectedBadgeColor = Variables.mainInverse,
        unselectedBadgeColor = Variables.surfaceTertiary,
        selectedContainerColor = Variables.surfaceTertiarySelected,
        unselectedContainerColor = Variables.surfaceTertiary,
        selectedIconColor = Variables.inverseDefault,
        unselectedIconColor = Variables.mainLight,
        selectedTextColor = Variables.mainInverse,
        unselectedTextColor = Variables.mainLight
    )
}

private fun openFeedbackUrl(context: Context) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://app.smartsheet.com/b/form/da3b9fb25b88495cbca59a4470d7b186")
    )
    context.startActivity(intent)
}