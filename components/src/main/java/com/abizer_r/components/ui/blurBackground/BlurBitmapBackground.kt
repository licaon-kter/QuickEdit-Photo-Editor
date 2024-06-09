package com.abizer_r.components.ui.blurBackground

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import com.abizer_r.components.R
import com.abizer_r.components.theme.SketchDraftTheme
import com.skydoves.cloudy.Cloudy

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BlurBitmapBackground(
    modifier: Modifier,
    imageBitmap: ImageBitmap,
    shouldBlur: Boolean,
    blurRadius: Int = 15,
    onBgClicked: () -> Unit
) {
    /**
     * WORK AROUND - depending on case (textField visible or not), adding/removing a "Cloudy" composable
     * REASON - Seems like the "Cloudy" composable is converting the view to bitmap before blurring (or something like that)
     *          So, when the textField is not visible, there is a permanent image of the newly created textBox with the selection layout.
     *          This creates a bug, and I'm not able to figure out how to prevent this
     */
    if (shouldBlur) {
        Log.e("TEST_BLUR", "BlurBitmapBackground: ", )
        Cloudy(
            modifier = modifier,
            radius = blurRadius
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = imageBitmap,
                contentScale = ContentScale.Inside,
                contentDescription = null,
                alpha = 0.3f
            )
        }

    } else {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter {
                    onBgClicked()
//                        viewModel.updateViewSelection(null)
                    true
                },
            bitmap = imageBitmap,
            contentScale = ContentScale.Fit,
            contentDescription = null,
            alpha = 1f
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewBitmapBg() {
    SketchDraftTheme {

        BlurBitmapBackground(
            modifier = Modifier.fillMaxSize(),
            imageBitmap = ImageBitmap.imageResource(id = R.drawable.placeholder_image_1),
            shouldBlur = false,
            onBgClicked = {}
        )
    }
}