import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions

object HomeScreenUiDefaults {
    // Top bar height (Material3 default)
    @OptIn(ExperimentalMaterial3Api::class)
    val TopBarHeight = TopAppBarDefaults.TopAppBarExpandedHeight

    // NavBar width as a fraction of screen width
    const val NavBarWidthFraction = 0.95f

    // NavBar animation duration
    const val AnimationDuration = 300

    // Scrim animation duration
    val ScrimAlpha = AppDimensions.WeightHalf
}

