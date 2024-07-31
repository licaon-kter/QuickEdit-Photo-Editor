package com.abizer_r.quickedit.ui.textMode

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.abizer_r.components.util.ImmutableList
import com.abizer_r.quickedit.utils.textMode.blurBackground.BlurBitmapBackground
import com.abizer_r.components.util.defaultErrorToast
import com.abizer_r.quickedit.ui.common.AnimatedToolbarContainer
import com.abizer_r.quickedit.ui.common.bottomToolbarModifier
import com.abizer_r.quickedit.ui.common.topToolbarModifier
import com.abizer_r.quickedit.ui.textMode.TextModeEvent.*
import com.abizer_r.quickedit.ui.editorScreen.bottomToolbar.BottomToolBarStatic
import com.abizer_r.quickedit.ui.editorScreen.bottomToolbar.TOOLBAR_HEIGHT_MEDIUM
import com.abizer_r.quickedit.ui.editorScreen.bottomToolbar.TOOLBAR_HEIGHT_SMALL
import com.abizer_r.quickedit.ui.editorScreen.bottomToolbar.state.BottomToolbarEvent
import com.abizer_r.quickedit.ui.editorScreen.topToolbar.TextModeTopToolbar
import com.abizer_r.quickedit.ui.textMode.textEditorLayout.TextEditorLayout
import com.abizer_r.quickedit.ui.transformableViews.base.TransformableTextBoxState
import com.abizer_r.quickedit.utils.other.anim.AnimUtils
import com.abizer_r.quickedit.utils.other.bitmap.ImmutableBitmap
import com.abizer_r.quickedit.utils.textMode.TextModeUtils
import com.abizer_r.quickedit.utils.textMode.TextModeUtils.BorderForSelectedViews
import com.abizer_r.quickedit.utils.textMode.TextModeUtils.DrawAllTransformableViews
import com.smarttoolfactory.screenshot.ImageResult
import com.smarttoolfactory.screenshot.ScreenshotBox
import com.smarttoolfactory.screenshot.rememberScreenshotState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TextModeScreen(
    modifier: Modifier = Modifier,
    immutableBitmap: ImmutableBitmap,
    onDoneClicked: (bitmap: Bitmap) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val lifeCycleOwner = LocalLifecycleOwner.current

    val viewModel: TextModeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle(
        lifecycleOwner = lifeCycleOwner
    )

    val bottomToolbarItems = remember {
        ImmutableList(TextModeUtils.getDefaultBottomToolbarItemsList())
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(key1 = Unit) {
        onDispose {
            keyboardController?.hide()
        }
    }

    val topToolbarHeight =  TOOLBAR_HEIGHT_SMALL
    val bottomToolbarHeight = TOOLBAR_HEIGHT_MEDIUM

    var toolbarVisible by remember { mutableStateOf(false) }

    val screenshotState = rememberScreenshotState()

    val defaultTextFont = MaterialTheme.typography.headlineMedium.fontSize
    LaunchedEffect(key1 = Unit) {
        toolbarVisible = true
        delay(AnimUtils.TOOLBAR_EXPAND_ANIM_DURATION_FAST.toLong())
        viewModel.onEvent(
            UpdateTextFont(textFont = defaultTextFont)
        )
        viewModel.onEvent(
            ShowTextField(state.textFieldState)
        )
    }

    val onCloseClickedLambda = remember<() -> Unit> {{
        if (state.textFieldState.isVisible) {
            viewModel.onEvent(HideTextField)
        } else {
            lifeCycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                toolbarVisible = false
                delay(AnimUtils.TOOLBAR_COLLAPSE_ANIM_DURATION_FAST.toLong())
                onBackPressed()
            }
        }
    }}

    BackHandler {
        if (state.textFieldState.isVisible) {
            viewModel.onEvent(HideTextField)
        } else {
            onCloseClickedLambda()
        }
    }

    val onDoneClickedLambda = remember<() -> Unit> {{
        if (state.textFieldState.isVisible) {
            viewModel.onEvent(AddTransformableTextBox(
                textBoxState = TransformableTextBoxState(
                    id = state.textFieldState.textStateId ?: UUID.randomUUID().toString(),
                    text = state.textFieldState.text,
                    textColor = state.textFieldState.getSelectedColor(),
                    textAlign = state.textFieldState.textAlign,
                    textFont = state.textFieldState.textFont
                )
            ))
            viewModel.onEvent(HideTextField)
        } else {
            viewModel.handleStateBeforeCaptureScreenshot()
            lifeCycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                delay(200)  /* Delay to update the selection in ui */
                screenshotState.capture()
            }
        }
    }}

    val handleScreenshotResult = remember<(Bitmap) -> Unit> {{ bitmap ->
        lifeCycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            toolbarVisible = false
            delay(AnimUtils.TOOLBAR_COLLAPSE_ANIM_DURATION_FAST.toLong())
            onDoneClicked(bitmap)
        }
    }}

    when (screenshotState.imageState.value) {
        ImageResult.Initial -> {}
        is ImageResult.Error -> {
            viewModel.shouldGoToNextScreen = false
            context.defaultErrorToast()
        }
        is ImageResult.Success -> {
            if (viewModel.shouldGoToNextScreen) {
                viewModel.shouldGoToNextScreen = false
                screenshotState.bitmap?.let { mBitmap ->
                    handleScreenshotResult(mBitmap)
                } ?: context.defaultErrorToast()
            }
        }
    }

    val onBgClickedLambda = remember<() -> Unit> {{
        viewModel.updateViewSelection(null)
    }}

    val onBottomToolbarEventLambda = remember<(BottomToolbarEvent) -> Unit> {{
        viewModel.onBottomToolbarEvent(it)
    }}


    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val (topToolBar, bottomToolbar, editorBox, editorBoxBgStretched, textInputView) = createRefs()

        val showBottomToolbar = state.collapseToolbar.not()

        AnimatedToolbarContainer(
            toolbarVisible = toolbarVisible,
            modifier = topToolbarModifier(topToolBar)
        ) {
            TextModeTopToolbar(
                modifier = Modifier,
                toolbarHeight = topToolbarHeight,
                onCloseClicked = onCloseClickedLambda,
                onDoneClicked = onDoneClickedLambda
            )
        }

        val bitmap = immutableBitmap.bitmap
        val aspectRatio = bitmap.let {
            bitmap.width.toFloat() / bitmap.height.toFloat()
        }
        ScreenshotBox(
            modifier = Modifier
                .constrainAs(editorBox) {
                    width = Dimension.matchParent
                    height = Dimension.matchParent
                }
                .padding(top = topToolbarHeight, bottom = bottomToolbarHeight)
                .aspectRatio(aspectRatio)
                .clipToBounds(),
            screenshotState = screenshotState
        ) {

            BlurBitmapBackground(
                modifier = Modifier.fillMaxSize(),
                imageBitmap = bitmap.asImageBitmap(),
                shouldBlur = state.showBlurredBg,
                contentScale = if (showBottomToolbar) ContentScale.Fit else ContentScale.Crop,
                blurRadius = 15,
                onBgClicked = onBgClickedLambda
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (showBottomToolbar) {
                    DrawAllTransformableViews(
                        centerAlignModifier = Modifier.align(Alignment.Center),
                        transformableViewsList = state.transformableViewStateList,
                        onTransformableBoxEvent = viewModel::onTransformableBoxEvent
                    )
                }
            }

        }

        Box(
            modifier = Modifier
                .constrainAs(editorBoxBgStretched) {
                    top.linkTo(topToolBar.bottom)
                    bottom.linkTo(bottomToolbar.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
                .clipToBounds()
        ) {


            if (showBottomToolbar) {
                BorderForSelectedViews(
                    centerAlignModifier = Modifier.align(Alignment.Center),
                    transformableViewsList = state.transformableViewStateList,
                    onTransformableBoxEvent = viewModel::onTransformableBoxEvent
                )
            }

        }

        AnimatedVisibility(
            visible = state.textFieldState.isVisible,
            modifier = Modifier.constrainAs(textInputView) {
                top.linkTo(topToolBar.bottom)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            },
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TextEditorLayout(
                modifier = Modifier,
                textFieldState = state.textFieldState,
                onTextModeEvent = viewModel::onEvent
            )
        }


        AnimatedToolbarContainer(
            toolbarVisible = showBottomToolbar && toolbarVisible,
            modifier = bottomToolbarModifier(bottomToolbar)
        ) {
            BottomToolBarStatic(
                modifier = Modifier.fillMaxWidth(),
                toolbarItems = bottomToolbarItems,
                toolbarHeight = bottomToolbarHeight,
                onEvent = onBottomToolbarEventLambda
            )
        }
    }
}