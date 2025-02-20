package com.winlator.xserver

import android.util.SparseArray
import androidx.core.util.size
import com.winlator.xserver.XResourceManager.OnResourceLifecycleListener
import com.winlator.xserver.events.SelectionClear

class SelectionManager(windowManager: WindowManager) : OnResourceLifecycleListener {

    private val selections = SparseArray<Selection>()

    init {
        windowManager.addOnResourceLifecycleListener(this)
    }

    class Selection {
        var owner: Window? = null
        var client: XClient? = null
    }

    fun setSelection(atom: Int, owner: Window?, client: XClient, timestamp: Int) {
        val selection = getSelection(atom)
        if (selection.owner != null && (owner == null || selection.client != client)) {
            selection.client!!.sendEvent(SelectionClear(timestamp, owner!!, atom))
        }
        selection.owner = owner
        selection.client = client
    }

    fun getSelection(atom: Int): Selection {
        var selection = selections.get(atom)
        if (selection != null) {
            return selection
        }

        selection = Selection()
        selections.put(atom, selection)

        return selection
    }

    override fun onCreateResource(resource: XResource?) {
    }

    override fun onFreeResource(resource: XResource?) {
        for (i in 0..<selections.size) {
            val selection = selections.valueAt(i)
            if (selection.owner == resource) {
                selection.owner = null
            }
        }
    }
}
