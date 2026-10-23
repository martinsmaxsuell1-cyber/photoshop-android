package com.mynxx.photoshop

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.mynxx.photoshop.databinding.ActivityMainBinding
import com.mynxx.photoshop.filters.FilterManager
import com.mynxx.photoshop.ui.PhotoEditorView
import com.mynxx.photoshop.utils.ImageUtils
import java.io.File
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var tempCameraUri: Uri? = null

    // Filter adjustments state
    private var currentPreset = FilterManager.Preset.ORIGINAL
    private var currentBrightness = 0f // -100 to 100
    private var currentContrast = 1f   // 0.5 to 1.5
    private var currentSaturation = 1f // 0 to 2

    // Activity Result Launchers
    private val pickGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadBitmapFromUri(it) }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            loadBitmapFromUri(tempCameraUri!!)
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, getString(R.string.msg_permission_camera), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTopToolbar()
        setupBottomTools()
        setupBrushPalette()
        setupFilterChips()
        setupAdjustmentSeekBars()
    }

    private fun setupTopToolbar() {
        binding.btnUndo.setOnClickListener {
            binding.photoEditorView.undo()
            updateUndoRedoButtons()
        }

        binding.btnRedo.setOnClickListener {
            binding.photoEditorView.redo()
            updateUndoRedoButtons()
        }

        binding.btnSave.setOnClickListener {
            saveResultImage()
        }
    }

    private fun setupBottomTools() {
        // 1. Camera
        binding.toolBtnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // 2. Gallery
        binding.toolBtnGallery.setOnClickListener {
            pickGalleryLauncher.launch("image/*")
        }

        // 3. Brush
        binding.toolBtnBrush.setOnClickListener {
            toggleSubPanel(binding.panelBrush)
            binding.photoEditorView.currentTool = PhotoEditorView.ToolMode.BRUSH
        }

        // 4. Eraser
        binding.toolBtnEraser.setOnClickListener {
            toggleSubPanel(binding.panelBrush)
            binding.photoEditorView.currentTool = PhotoEditorView.ToolMode.ERASER
        }

        // 5. Filters
        binding.toolBtnFilters.setOnClickListener {
            toggleSubPanel(binding.panelFilters)
            binding.photoEditorView.currentTool = PhotoEditorView.ToolMode.NONE
        }

        // 6. Adjustments
        binding.toolBtnAdjust.setOnClickListener {
            toggleSubPanel(binding.panelAdjustments)
            binding.photoEditorView.currentTool = PhotoEditorView.ToolMode.NONE
        }

        // 7. Rotate 90° Clockwise
        binding.toolBtnRotate.setOnClickListener {
            val currentBmp = binding.photoEditorView.getBitmap()
            if (currentBmp != null) {
                val rotated = ImageUtils.rotateBitmap(currentBmp, 90f)
                binding.photoEditorView.setBitmap(rotated)
                updateUndoRedoButtons()
            } else {
                showToast("Carregue uma imagem antes de girar.")
            }
        }

        // 8. Text
        binding.toolBtnText.setOnClickListener {
            binding.photoEditorView.currentTool = PhotoEditorView.ToolMode.TEXT
            showAddTextDialog()
        }
    }

    private fun toggleSubPanel(targetPanel: View) {
        val isCurrentlyVisible = binding.subToolPanel.visibility == View.VISIBLE && targetPanel.visibility == View.VISIBLE

        if (isCurrentlyVisible) {
            binding.subToolPanel.visibility = View.GONE
            targetPanel.visibility = View.GONE
        } else {
            binding.subToolPanel.visibility = View.VISIBLE
            binding.panelBrush.visibility = View.GONE
            binding.panelFilters.visibility = View.GONE
            binding.panelAdjustments.visibility = View.GONE
            targetPanel.visibility = View.VISIBLE
        }
    }

    private fun setupBrushPalette() {
        val colors = listOf(
            Color.WHITE, Color.BLACK, Color.RED,
            Color.parseColor("#FF9500"), // Orange
            Color.parseColor("#FFCC00"), // Yellow
            Color.parseColor("#34C759"), // Green
            Color.parseColor("#007AFF"), // Blue
            Color.parseColor("#AF52DE")  // Purple
        )

        for (c in colors) {
            val circle = View(this).apply {
                val size = (32 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(10, 0, 10, 0)
                }
                setBackgroundColor(c)
                setOnClickListener {
                    binding.photoEditorView.brushColor = c
                    showToast("Cor selecionada")
                }
            }
            binding.colorPaletteContainer.addView(circle)
        }

        binding.seekBrushSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = max(4f, progress.toFloat())
                binding.photoEditorView.brushSize = size
                binding.photoEditorView.eraserSize = size * 1.5f
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupFilterChips() {
        val presets = listOf(
            "Original" to FilterManager.Preset.ORIGINAL,
            "P&B" to FilterManager.Preset.GRAYSCALE,
            "Sépia" to FilterManager.Preset.SEPIA,
            "Vintage" to FilterManager.Preset.VINTAGE,
            "Inverter" to FilterManager.Preset.INVERT,
            "Quente" to FilterManager.Preset.WARM,
            "Frio" to FilterManager.Preset.COOL
        )

        for ((label, preset) in presets) {
            val btn = MaterialButton(this).apply {
                text = label
                textSize = 12f
                cornerRadius = 20
                setOnClickListener {
                    currentPreset = preset
                    applyFilters()
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(6, 0, 6, 0)
            }
            binding.filterButtonsContainer.addView(btn, params)
        }
    }

    private fun setupAdjustmentSeekBars() {
        // Brightness: progress 0..200 (100 is neutral 0f, 0 is -100f, 200 is +100f)
        binding.seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                currentBrightness = (progress - 100).toFloat()
                applyFilters()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Contrast: progress 0..200 (100 is neutral 1.0f, 0 is 0.5f, 200 is 1.5f)
        binding.seekContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                currentContrast = 0.5f + (progress / 100f) * 0.5f
                applyFilters()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Saturation: progress 0..200 (100 is neutral 1.0f, 0 is 0.0f, 200 is 2.0f)
        binding.seekSaturation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSaturation = progress / 100f
                applyFilters()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun applyFilters() {
        val matrix = FilterManager.createCombinedColorMatrix(
            preset = currentPreset,
            brightness = currentBrightness,
            contrast = currentContrast,
            saturation = currentSaturation
        )
        binding.photoEditorView.setColorMatrix(matrix)
    }

    private fun showAddTextDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.hint_enter_text)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.prompt_add_text))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_add)) { _, _ ->
                val txt = input.text.toString().trim()
                if (txt.isNotEmpty()) {
                    binding.photoEditorView.addText(txt, color = Color.WHITE)
                    updateUndoRedoButtons()
                    showToast("Texto adicionado! Arraste para posicionar.")
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun launchCamera() {
        try {
            val photoFile = File.createTempFile("camera_capture_", ".jpg", cacheDir)
            tempCameraUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(tempCameraUri)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Erro ao abrir a câmera: ${e.message}")
        }
    }

    private fun loadBitmapFromUri(uri: Uri) {
        val bitmap = ImageUtils.decodeSampledBitmapFromUri(this, uri)
        if (bitmap != null) {
            binding.emptyStateView.visibility = View.GONE
            binding.photoEditorView.setBitmap(bitmap)
            updateUndoRedoButtons()
            showToast("Imagem carregada com sucesso!")
        } else {
            showToast("Não foi possível carregar a imagem.")
        }
    }

    private fun saveResultImage() {
        val finalBitmap = binding.photoEditorView.generateFinalBitmap()
        if (finalBitmap == null) {
            showToast("Nenhuma imagem para salvar.")
            return
        }

        val savedUri = ImageUtils.saveBitmapToGallery(this, finalBitmap)
        if (savedUri != null) {
            Snackbar.make(binding.root, getString(R.string.msg_saved_success), Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.ps_success))
                .show()
        } else {
            showToast(getString(R.string.msg_save_error))
        }
    }

    private fun updateUndoRedoButtons() {
        val canUndo = binding.photoEditorView.canUndo()
        val canRedo = binding.photoEditorView.canRedo()
        binding.btnUndo.alpha = if (canUndo) 1.0f else 0.4f
        binding.btnRedo.alpha = if (canRedo) 1.0f else 0.4f
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
