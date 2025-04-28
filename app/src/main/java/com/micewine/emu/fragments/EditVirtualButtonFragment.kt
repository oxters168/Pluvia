package com.micewine.emu.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.micewine.emu.R
import com.micewine.emu.activities.VirtualControllerOverlayMapper.Companion.ACTION_INVALIDATE
import com.micewine.emu.controller.XKeyCodes.getKeyNames
import com.micewine.emu.controller.XKeyCodes.getXKeyScanCodes
import com.micewine.emu.views.OverlayView.Companion.SHAPE_CIRCLE
import com.micewine.emu.views.OverlayView.Companion.SHAPE_RECTANGLE
import com.micewine.emu.views.OverlayView.Companion.SHAPE_SQUARE
import com.micewine.emu.views.OverlayView.Companion.analogList
import com.micewine.emu.views.OverlayView.Companion.buttonList
import com.micewine.emu.views.OverlayView.Companion.dpadList
import com.micewine.emu.views.OverlayViewCreator.Companion.ANALOG
import com.micewine.emu.views.OverlayViewCreator.Companion.BUTTON
import com.micewine.emu.views.OverlayViewCreator.Companion.DPAD
import com.micewine.emu.views.OverlayViewCreator.Companion.lastSelectedButton
import com.micewine.emu.views.OverlayViewCreator.Companion.lastSelectedType

class EditVirtualButtonFragment : DialogFragment() {
    @SuppressLint("SetTextI18n")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.fragment_edit_virtual_button, null)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        val radiusSeekbarValue = view.findViewById<TextView>(R.id.radiusSeekbarValue).apply {
            text = "$selectedButtonRadius%"
        }
        val radiusSeekbar = view.findViewById<SeekBar>(R.id.radiusSeekbar).apply {
            max = 400
            min = 100

            setOnSeekBarChangeListener(object :
                OnSeekBarChangeListener {
                @SuppressLint("SetTextI18n")
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    seekBar?.progress = Math.round(progress.toFloat() / 5) * 5
                    radiusSeekbarValue?.text = "${seekBar?.progress}%"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })
        }

        val allKeyNames = getKeyNames(lastSelectedType != ANALOG)
        val shapes = listOf("Circle", "Square", "Rectangle")

        val shapeSpinner = view.findViewById<Spinner>(R.id.shapeSpinner).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, shapes)
            setSelection(
                when (selectedButtonShape) {
                    SHAPE_CIRCLE -> 0
                    SHAPE_SQUARE -> 1
                    SHAPE_RECTANGLE -> 2
                    else -> -1
                }
            )
        }
        val buttonSpinner = view.findViewById<Spinner>(R.id.buttonSpinner).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, allKeyNames)
            setSelection(allKeyNames.indexOf(selectedButtonKeyName))
        }
        val analogUpKeySpinner = view.findViewById<Spinner>(R.id.analogUpKeySpinner).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, allKeyNames)
            setSelection(allKeyNames.indexOf(selectedAnalogUpKeyName))
        }
        val analogDownKeySpinner = view.findViewById<Spinner>(R.id.analogDownKeySpinner).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, allKeyNames)
            setSelection(allKeyNames.indexOf(selectedAnalogDownKeyName))
        }
        val analogLeftKeySpinner = view.findViewById<Spinner>(R.id.analogLeftKeySpinner).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, allKeyNames)
            setSelection(allKeyNames.indexOf(selectedAnalogLeftKeyName))
        }
        val analogRightKeySpinner = view.findViewById<Spinner>(R.id.analogRightKeySpinner).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, allKeyNames)
            setSelection(allKeyNames.indexOf(selectedAnalogRightKeyName))
        }

        radiusSeekbar.progress = selectedButtonRadius

        if (lastSelectedType == ANALOG || lastSelectedType == DPAD) {
            view.findViewById<TextView>(R.id.shapeText).visibility = View.GONE
            view.findViewById<TextView>(R.id.buttonMappingText).visibility = View.GONE

            buttonSpinner.visibility = View.GONE
            shapeSpinner.visibility = View.GONE
        } else if (lastSelectedType == BUTTON) {
            view.findViewById<LinearLayout>(R.id.layoutAnalogUp).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.layoutAnalogDown).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.layoutAnalogLeft).visibility = View.GONE
            view.findViewById<LinearLayout>(R.id.layoutAnalogRight).visibility = View.GONE
        }

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setView(view).create()

        saveButton.setOnClickListener {
            if (lastSelectedType == BUTTON && buttonList.isNotEmpty()) {
                buttonList[lastSelectedButton - 1].keyName = buttonSpinner.selectedItem.toString()
                buttonList[lastSelectedButton - 1].keyCodes = getXKeyScanCodes(buttonSpinner.selectedItem.toString())

                buttonList[lastSelectedButton - 1].radius = radiusSeekbar.progress.toFloat()

                when (shapeSpinner.selectedItem.toString()) {
                    "Circle" -> buttonList[lastSelectedButton - 1].shape = SHAPE_CIRCLE
                    "Square" -> buttonList[lastSelectedButton - 1].shape = SHAPE_SQUARE
                    "Rectangle" -> buttonList[lastSelectedButton - 1].shape = SHAPE_RECTANGLE
                }
            } else if (lastSelectedType == ANALOG && analogList.isNotEmpty()) {
                analogList[lastSelectedButton - 1].upKeyName = analogUpKeySpinner.selectedItem.toString()
                analogList[lastSelectedButton - 1].upKeyCodes = getXKeyScanCodes(analogUpKeySpinner.selectedItem.toString())

                analogList[lastSelectedButton - 1].downKeyName = analogDownKeySpinner.selectedItem.toString()
                analogList[lastSelectedButton - 1].downKeyCodes = getXKeyScanCodes(analogDownKeySpinner.selectedItem.toString())

                analogList[lastSelectedButton - 1].leftKeyName = analogLeftKeySpinner.selectedItem.toString()
                analogList[lastSelectedButton - 1].leftKeyCodes = getXKeyScanCodes(analogLeftKeySpinner.selectedItem.toString())

                analogList[lastSelectedButton - 1].rightKeyName = analogRightKeySpinner.selectedItem.toString()
                analogList[lastSelectedButton - 1].rightKeyCodes = getXKeyScanCodes(analogRightKeySpinner.selectedItem.toString())

                analogList[lastSelectedButton - 1].radius = radiusSeekbar.progress.toFloat()
            } else if (lastSelectedType == DPAD && dpadList.isNotEmpty()) {
                dpadList[lastSelectedButton - 1].upKeyName = analogUpKeySpinner.selectedItem.toString()
                dpadList[lastSelectedButton - 1].upKeyCodes = getXKeyScanCodes(analogUpKeySpinner.selectedItem.toString())

                dpadList[lastSelectedButton - 1].downKeyName = analogDownKeySpinner.selectedItem.toString()
                dpadList[lastSelectedButton - 1].downKeyCodes = getXKeyScanCodes(analogDownKeySpinner.selectedItem.toString())

                dpadList[lastSelectedButton - 1].leftKeyName = analogLeftKeySpinner.selectedItem.toString()
                dpadList[lastSelectedButton - 1].leftKeyCodes = getXKeyScanCodes(analogLeftKeySpinner.selectedItem.toString())

                dpadList[lastSelectedButton - 1].rightKeyName = analogRightKeySpinner.selectedItem.toString()
                dpadList[lastSelectedButton - 1].rightKeyCodes = getXKeyScanCodes(analogRightKeySpinner.selectedItem.toString())

                dpadList[lastSelectedButton - 1].radius = radiusSeekbar.progress.toFloat()
            }

            context?.sendBroadcast(
                Intent(ACTION_INVALIDATE)
            )

            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        return dialog
    }

    companion object {
        var selectedButtonKeyName = ""
        var selectedAnalogUpKeyName = ""
        var selectedAnalogDownKeyName = ""
        var selectedAnalogLeftKeyName = ""
        var selectedAnalogRightKeyName = ""
        var selectedButtonShape = -1
        var selectedButtonRadius = 0
    }
}