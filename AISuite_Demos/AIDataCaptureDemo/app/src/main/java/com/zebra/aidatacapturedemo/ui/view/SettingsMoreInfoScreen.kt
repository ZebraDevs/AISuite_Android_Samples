package com.zebra.aidatacapturedemo.ui.view

import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.util.TypedValue
import android.widget.Space
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.ui.view.Variables.mainDefault
import com.zebra.aidatacapturedemo.ui.view.Variables.mainInverse
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

val itemsTitle: MutableList<String> = mutableStateListOf()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMoreInfoScreen(
    viewModel: AIDataCaptureDemoViewModel,
    document: Document,
    showSheet: Boolean
): Boolean {
    itemsTitle.clear()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet: MutableState<Boolean> = remember { mutableStateOf(showSheet) }
    rememberCoroutineScope()
    if (showBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet.value = false
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            containerColor = Variables.surfaceDefault,
            tonalElevation = 16.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp, bottom = 20.dp)
                        .width(44.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Variables.mainDisabled)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SettingMoreInfoMain(viewModel, document)
            }
        }
    }
    return showBottomSheet.value
}

@Composable
fun SettingMoreInfoMain(viewModel: AIDataCaptureDemoViewModel, document: Document) {
    Column(
    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
        .fillMaxSize()
        .background(color = Variables.surfaceDefault, shape = RoundedCornerShape(size = 36.dp))
        .padding(top = 24.dp, bottom = 24.dp)
    ) {
        AnimateMoreInfoExpandableList(viewModel, document)
    }
}

data class MoreInfoExpandableItem(
    val title: String,
    var isExpanded: Boolean = false
)

@Composable
fun AnimateMoreInfoExpandableList(
    viewModel: AIDataCaptureDemoViewModel,
    document: Document) {
    val element: Element? = document.getElementById("title")
    var htmlString = ""
    if(element != null) {
        htmlString = element.html()
        itemsTitle.add(htmlString)
        itemsTitle.add(stringResource(R.string.recommendation_tips))
    }
    val items = remember { List(itemsTitle.size) { index -> MoreInfoExpandableItem( itemsTitle[index]) } }
    val expandedStates = remember { mutableStateListOf(*BooleanArray(items.size) { false }.toTypedArray()) }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .border(width = 1.dp, color = Variables.borderDefault)
            .fillMaxWidth().padding(24.dp)
            .height(48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        state = listState
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = 24.dp, end = 24.dp)
            ) {
                val elementTip: Element? = document.getElementById("description")
                var summaryHtmlString = AnnotatedString("")
                if(elementTip != null) {
                    summaryHtmlString = AnnotatedString.fromHtml(elementTip.html())
                }
                Text(
                    text = summaryHtmlString,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.mainDefault,
                    )
                )
            }
        }
        itemsIndexed(items, key = { index, _ -> index }) { index, item ->
            ExpandableMoreInfoListItem(
                item = item,
                index = index,
                isExpanded = expandedStates[index],
                onExpandedChange = {
                    for (i in items.indices) {
                        expandedStates[i] = false
                    }
                    expandedStates[index] = it
                },
                viewModel, document
            )
        }
    }
}

@Composable
fun ExpandableMoreInfoListItem(
    item: MoreInfoExpandableItem,
    index: Int,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    viewModel: AIDataCaptureDemoViewModel,
    document: Document
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(12.dp))
            .background(color = Variables.surfaceDefault, shape = RoundedCornerShape(12.dp))
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
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Text(
                text = item.title,
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans_regular)),
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
            exit = shrinkVertically() + fadeOut()
        ) {
            if (item.title.equals(stringResource(R.string.recommendation_tips))) {
                SettingMoreInfoRecommendationTip(viewModel, document)
            } else {
                SettingMoreInfoDetails(viewModel, document)
            }
        }
    }
}

@Composable
fun SettingMoreInfoDetails(viewModel: AIDataCaptureDemoViewModel, document: Document) {

    val element: Element? = document.getElementById("details")
    var htmlString = ""
    if(element != null) {
        htmlString = element.html()
    }
    val htmlSpannableString = Html.fromHtml(htmlString, null, BulletHandler())
    val spannableBuilder = SpannableStringBuilder(htmlSpannableString)
    val bulletSpans = spannableBuilder.getSpans(0, spannableBuilder.length, BulletSpan::class.java)
    val bulletRadius = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        3.toFloat(),
        LocalContext.current.resources.displayMetrics
    ).toInt()
    val gapWidth = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        8.toFloat(),
        LocalContext.current.resources.displayMetrics
    ).toInt()
    val customTypeface = ResourcesCompat.getFont(LocalContext.current, R.font.ibm_plex_sans_regular)
    bulletSpans.forEach {
        val start = spannableBuilder.getSpanStart(it)
        val end = spannableBuilder.getSpanEnd(it)
        spannableBuilder.removeSpan(it)
        spannableBuilder.setSpan(
            CustomBulletSpan(bulletRadius = bulletRadius, gapWidth = gapWidth),
            start,
            end,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
    }
    if (htmlString.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 0.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        textSize = 16f
                        setTextColor(Variables.mainDefault.toArgb())
                        setTypeface(customTypeface)
                    }
                },
                update = { textView ->
                    textView.text = spannableBuilder
                }
            )
        }
    }
}

@Composable
fun SettingMoreInfoRecommendationTip(viewModel: AIDataCaptureDemoViewModel, document: Document) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 0.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        val element: Element? = document.getElementById("recommendation")
        var recommendHtmlString = ""
        if(element != null) {
            recommendHtmlString = element.html()
        }
        val htmlSpannableString = Html.fromHtml(recommendHtmlString, null, BulletHandler())
        val spannableBuilder = SpannableStringBuilder(htmlSpannableString)
        val bulletSpans = spannableBuilder.getSpans(0, spannableBuilder.length, BulletSpan::class.java)
        val bulletRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            3.toFloat(),
            LocalContext.current.resources.displayMetrics
        ).toInt()
        val gapWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8.toFloat(),
            LocalContext.current.resources.displayMetrics
        ).toInt()
        val customTypeface = ResourcesCompat.getFont(LocalContext.current, R.font.ibm_plex_sans_regular)
        bulletSpans.forEach {
            val start = spannableBuilder.getSpanStart(it)
            val end = spannableBuilder.getSpanEnd(it)
            spannableBuilder.removeSpan(it)
            spannableBuilder.setSpan(
                CustomBulletSpan(bulletRadius = bulletRadius, gapWidth = gapWidth),
                start,
                end,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
        if (recommendHtmlString.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 0.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            textSize = 16f
                            setTextColor(Variables.mainDefault.toArgb())
                            setTypeface(customTypeface)
                        }
                    },
                    update = { textView ->
                        textView.text = spannableBuilder
                    }
                )
            }
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
        val elementTip: Element? = document.getElementById("tip")
        var tipHtmlString = AnnotatedString("")
        if(elementTip != null) {
            tipHtmlString = AnnotatedString.fromHtml(elementTip.html())
        }
        Text(
            text = tipHtmlString,
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                fontWeight = FontWeight(400),
                color = Variables.mainDefault,
            )
        )

    }
}
