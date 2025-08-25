package com.zebra.aidatacapturedemo.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getString
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel


@Composable
fun AIDataCaptureStartScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    innerPadding: PaddingValues
) {
    AnimateExpandableList(viewModel, navController, innerPadding)
}

data class ExpandableItem(
    val iconId: Int,
    val title: String,
    var isExpanded: Boolean = false
)

@Composable
fun AnimateExpandableList(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    innerPadding: PaddingValues
) {
    val itemsTitle = arrayOf("Use Case Demos", "Technology Demos")
    val itemsIcon = arrayOf(R.drawable.usecase_icon, R.drawable.technology_icon)

    val items =
        remember { List(2) { index -> ExpandableItem(itemsIcon[index], itemsTitle[index]) } }

    val expandedStates =
        remember { mutableStateListOf(*BooleanArray(items.size) { true }.toTypedArray()) }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .border(width = 1.dp, color = Variables.borderDefault)
            .fillMaxWidth()
            .background(color = Variables.surfaceDefault)
            .height(48.dp),
        horizontalAlignment = Alignment.Start,
        state = listState
    ) {
        itemsIndexed(items, key = { index, _ -> index }) { index, item ->
            ExpandableListItem(
                item = item,
                index = index,
                isExpanded = expandedStates[index],
                onExpandedChange = {
                    for (i in items.indices) {
                        expandedStates[i] = false
                    }
                    expandedStates[index] = it
                },
                viewModel, navController
            )
        }
    }
}

@Composable
fun ExpandableListItem(
    item: ExpandableItem,
    index: Int,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Variables.surfaceDefault)
            .clickable(interactionSource = interactionSource, indication = null) {
                onExpandedChange(!isExpanded)
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .border(width = 1.dp, color = Variables.borderDefault)
                .fillMaxWidth()
                .wrapContentHeight()
                .background(color = Variables.mainInverse)
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = ImageVector.Companion.vectorResource(item.iconId),
                contentDescription = null,
                modifier = Modifier
                    .padding(1.dp)
                    .width(24.dp)
                    .height(24.dp),
                tint = Variables.mainSubtle
            )
            Text(
                text = item.title,
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                    fontWeight = FontWeight(500),
                    color = Variables.mainDefault,
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = ImageVector.Companion.vectorResource(id = R.drawable.down_arrow_icon),
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .graphicsLayer(rotationZ = rotationAngle)
                    .padding(1.dp)
                    .width(20.dp)
                    .height(20.dp),
                tint = Variables.mainSubtle
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),

            ) {
            if (item.title.equals("Use Case Demos")) {
                AIDataCaptureUsecaseList(viewModel, navController)
            } else if (item.title.equals("Technology Demos")) {
                AIDataCaptureTechnologyList(viewModel, navController)
            }
        }
    }
}

@Composable
fun AIDataCaptureUsecaseList(viewModel: AIDataCaptureDemoViewModel, navController: NavController) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)
    ) {
        AIDataCaptureListItem(
            R.drawable.ocr_finder_icon,
            stringResource(id = R.string.ocr_find_demo),
            stringResource(id = R.string.ocr_find_desc),
            Variables.mainIcon1,
            Variables.secondaryIcon1,
            onItemClick = { selectedUsecase ->
                viewModel.updateAppBarTitle(getString(context, R.string.ocr_find_demo))
                viewModel.updateSelectedUsecase(selectedUsecase)
                viewModel.initModel()
                navController.navigate(route = Screen.DemoStart.route)
            })
        AIDataCaptureListItem(
            R.drawable.product_enrollment_recognition_icon,
            stringResource(id = R.string.product_enrollment_recognition_demo),
            stringResource(id = R.string.product_enrollment_recognition_desc),
            Variables.mainIcon1,
            Variables.secondaryIcon1,
            onItemClick = { selectedUsecase ->
                viewModel.updateAppBarTitle(
                    getString(
                        context,
                        R.string.product_enrollment_recognition_demo
                    )
                )
                viewModel.updateSelectedUsecase(selectedUsecase)
                viewModel.initModel()
                navController.navigate(route = Screen.DemoStart.route)
            })
    }
}

@Composable
fun AIDataCaptureTechnologyList(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)
    ) {
        AIDataCaptureListItem(
            R.drawable.ocr_icon,
            stringResource(id = R.string.ocr_demo),
            stringResource(id = R.string.ocr_desc),
            Variables.mainIcon2,
            Variables.secondaryIcon2,
            onItemClick = { selectedUsecase ->
                viewModel.updateAppBarTitle(getString(context, R.string.ocr_demo))
                viewModel.updateSelectedUsecase(selectedUsecase)
                viewModel.initModel()
                navController.navigate(route = Screen.DemoStart.route)
            })
        AIDataCaptureListItem(
            R.drawable.barcode_icon,
            stringResource(id = R.string.barcode_demo),
            stringResource(id = R.string.barcode_desc),
            Variables.mainIcon2,
            Variables.secondaryIcon2,
            onItemClick = { selectedUsecase ->
                viewModel.updateAppBarTitle(getString(context, R.string.barcode_demo))
                viewModel.updateSelectedUsecase(selectedUsecase)
                viewModel.initModel()
                navController.navigate(route = Screen.DemoStart.route)
            })
        AIDataCaptureListItem(
            R.drawable.retail_shelf_icon,
            stringResource(id = R.string.retail_shelf_demo),
            stringResource(id = R.string.retail_shelf_desc),
            Variables.mainIcon2,
            Variables.secondaryIcon2,
            onItemClick = { selectedUsecase ->
                viewModel.updateAppBarTitle(getString(context, R.string.retail_shelf_demo))
                viewModel.updateSelectedUsecase(selectedUsecase)
                viewModel.initModel()
                navController.navigate(route = Screen.DemoStart.route)
            })
    }
}

@Composable
fun AIDataCaptureListItem(
    resId: Int,
    title: String,
    description: String,
    mainColor: Color,
    secondaryColor: Color,
    onItemClick: (text: String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .shadow(4.dp, shape = RoundedCornerShape(12.dp))
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Variables.surfaceDefault, shape = RoundedCornerShape(size = 16.dp))
            .padding(start = 8.dp, end = 8.dp)
            .clickable {
                onItemClick(title)
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(40.dp)
                .height(40.dp)
                .background(
                    shape = RoundedCornerShape(4.dp),
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            mainColor,
                            secondaryColor
                        )
                    )
                )
        ) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = "image description",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(1.dp)
                    .width(24.dp)
                    .height(24.dp)
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .padding(top = 12.dp, bottom = 12.dp)
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                    fontWeight = FontWeight(500),
                    color = Variables.mainDefault,
                )
            )
            Text(
                text = description,
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans_regular)),
                    fontWeight = FontWeight(400),
                    color = Variables.mainSubtle,
                )
            )
        }
    }
}

fun CustomRoundedCornerShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp
) = RoundedCornerShape(
    topStart = CornerSize(topStart),
    topEnd = CornerSize(topEnd),
    bottomEnd = CornerSize(bottomEnd),
    bottomStart = CornerSize(bottomStart)
)