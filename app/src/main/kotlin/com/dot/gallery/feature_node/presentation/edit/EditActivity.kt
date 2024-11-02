package com.dot.gallery.feature_node.presentation.edit

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dot.gallery.feature_node.domain.model.editor.SaveFormat
import com.dot.gallery.feature_node.presentation.edit.adjustments.Crop
import com.dot.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            GalleryTheme(
                darkTheme = true
            ) {
                LaunchedEffect(Unit) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            Color.TRANSPARENT,
                            Color.TRANSPARENT,
                        ) { true },
                        navigationBarStyle = SystemBarStyle.auto(
                            Color.TRANSPARENT,
                            Color.TRANSPARENT,
                        ) { true }
                    )
                }
                val viewModel = hiltViewModel<EditViewModel>()

                LaunchedEffect(intent.data) {
                    intent.data?.let {
                        viewModel.setSourceData(this@EditActivity, it)
                    }
                }
                val currentImage by viewModel.currentBitmap.collectAsStateWithLifecycle()
                val targetImage by viewModel.targetBitmap.collectAsStateWithLifecycle()
                val uri by viewModel.uri.collectAsStateWithLifecycle()
                //val isEditingActive by viewModel.isEditingActive.collectAsStateWithLifecycle()
                val canOverride by viewModel.canOverride.collectAsStateWithLifecycle()
                val originalImage by viewModel.originalBitmap.collectAsStateWithLifecycle()
                val appliedAdjustments by viewModel.appliedAdjustments.collectAsStateWithLifecycle()
                val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
                val previewMatrix by viewModel.previewMatrix.collectAsStateWithLifecycle()
                val previewRotation by viewModel.previewRotation.collectAsStateWithLifecycle()

                // Markup states
                val paths by viewModel.paths.collectAsStateWithLifecycle()
                val pathsUndone by viewModel.pathsUndone.collectAsStateWithLifecycle()
                val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
                val previousPosition by viewModel.previousPosition.collectAsStateWithLifecycle()
                val drawMode by viewModel.drawMode.collectAsStateWithLifecycle()
                val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
                val currentPathProperty by viewModel.currentPathProperty.collectAsStateWithLifecycle()


                EditScreen2(
                    canOverride = canOverride,
                    isChanged = appliedAdjustments.isNotEmpty(),
                    isSaving = isSaving,
                    currentImage = currentImage,
                    targetImage = targetImage ?: currentImage,
                    targetUri = uri,
                    originalImage = originalImage,
                    previewMatrix = previewMatrix,
                    previewRotation = previewRotation,
                    appliedAdjustments = appliedAdjustments,
                    currentPosition = currentPosition,
                    paths = paths,
                    pathsUndone = pathsUndone,
                    previousPosition = previousPosition,
                    drawMode = drawMode,
                    currentPathProperty = currentPathProperty,
                    currentPath = currentPath,
                    onClose = {
                        finish()
                    },
                    onOverride = {
                        viewModel.saveOverride(
                            saveFormat = SaveFormat.PNG,
                            onSuccess = {
                                finish()
                            },
                            onFail = {

                            }
                        )
                    },
                    onSaveCopy = {
                        viewModel.saveCopy(
                            saveFormat = SaveFormat.PNG,
                            onSuccess = {
                                finish()
                            },
                            onFail = {

                            }
                        )
                    },
                    onAdjustItemLongClick = viewModel::removeKind,
                    onAdjustmentChange = viewModel::applyAdjustment,
                    onAdjustmentPreview = viewModel::previewAdjustment,
                    onToggleFilter = viewModel::toggleFilter,
                    removeLast = viewModel::removeLast,
                    onCropSuccess = { newImage ->
                        viewModel.applyAdjustment(Crop(newImage))
                    },
                    addPath = viewModel::addPath,
                    clearPathsUndone = viewModel::clearPathsUndone,
                    setCurrentPosition = viewModel::setCurrentPosition,
                    setPreviousPosition = viewModel::setPreviousPosition,
                    setDrawMode = viewModel::setDrawMode,
                    setCurrentPath = viewModel::setCurrentPath,
                    setCurrentPathProperty = viewModel::setCurrentPathProperty,
                    applyDrawing = viewModel::applyDrawing,
                    undoLastPath = viewModel::undoLastPath,
                    redoLastPath = viewModel::redoLastPath
                )
            }
        }
    }

    companion object {

        fun launchEditor(context: Context, uri: Uri) {
            context.startActivity(Intent(context, EditActivity::class.java).apply { data = uri })
        }
    }

}