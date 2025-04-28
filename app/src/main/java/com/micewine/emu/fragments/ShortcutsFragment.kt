package com.micewine.emu.fragments

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.widget.SearchView
import androidx.core.content.edit
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.micewine.emu.R
import com.micewine.emu.activities.GeneralSettingsActivity.Companion.SELECTED_WINE_PREFIX
import com.micewine.emu.activities.MainActivity
import com.micewine.emu.activities.MainActivity.Companion.copyFile
import com.micewine.emu.activities.MainActivity.Companion.setSharedVars
import com.micewine.emu.activities.MainActivity.Companion.usrDir
import com.micewine.emu.activities.MainActivity.Companion.winePrefix
import com.micewine.emu.activities.MainActivity.Companion.winePrefixesDir
import com.micewine.emu.adapters.AdapterGame
import com.micewine.emu.core.HighlightState
import com.micewine.emu.core.RatPackageManager.listRatPackagesId
import com.micewine.emu.databinding.FragmentShortcutsBinding
import com.micewine.emu.fragments.CreatePresetFragment.Companion.WINEPREFIX_PRESET
import com.micewine.emu.fragments.DebugSettingsFragment.Companion.availableCPUs
import com.micewine.emu.fragments.DeleteItemFragment.Companion.DELETE_WINE_PREFIX
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.preferences
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

class ShortcutsFragment : Fragment() {
    private var binding: FragmentShortcutsBinding? = null
    private var rootView: View? = null
    private var layoutManager: GridLayoutManager? = null
    private var itemTouchHelper: ItemTouchHelper? = null
    private var selectedWinePrefixSpinner: Spinner? = null
    private var createWinePrefixButton: ImageButton? = null
    private var deleteWinePrefixButton: ImageButton? = null
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_UPDATE_WINE_PREFIX_SPINNER -> {
                    val prefixName = intent.getStringExtra("prefixName")
                    val winePrefixes = winePrefixesDir.listFiles()?.map { it.name }?.toTypedArray() ?: emptyArray()

                    selectedWinePrefixSpinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, winePrefixes)

                    if (prefixName == null) {
                        selectedWinePrefixSpinner?.setSelection(0)
                    } else {
                        selectedWinePrefixSpinner?.setSelection(winePrefixes.indexOf(prefixName))
                    }
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentShortcutsBinding.inflate(inflater, container, false)
        rootView = binding!!.root

        recyclerView = rootView?.findViewById(R.id.recyclerViewGame)

        initialize(requireContext())

        layoutManager = recyclerView?.layoutManager as GridLayoutManager?

        recyclerView?.layoutManager = GridLayoutManager(requireContext(), getSpanCount())
        recyclerView?.addItemDecoration(GridSpacingItemDecoration(10))

        setAdapter()

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.toolbar_menu, menu)

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView

                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        val query = newText.orEmpty()

                        val filteredList = if (query.isEmpty()) {
                            gameList.map {
                                AdapterGame.GameItem(
                                    it.name,
                                    it.exePath,
                                    it.exeArguments,
                                    it.iconPath
                                )
                            }
                        } else {
                            gameList.filter {
                                it.name.contains(query, ignoreCase = true)
                            }.map {
                                AdapterGame.GameItem(
                                    it.name,
                                    it.exePath,
                                    it.exeArguments,
                                    it.iconPath
                                )
                            }
                        }

                        (recyclerView?.adapter as? AdapterGame)?.updateList(filteredList)

                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return true
            }
        }, viewLifecycleOwner)

        setupDragAndDrop()

        recyclerView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (gameList.size > 1 && (recyclerView?.childCount ?: 0) > 1 &&
                    !preferences!!.getBoolean(HIGHLIGHT_SHORTCUT_PREFERENCE_KEY, false) &&
                    HighlightState.fromOrdinal(preferences!!.getInt(HighlightState.HIGHLIGHT_PREFERENCE_KEY, 0)) == HighlightState.HIGHLIGHT_DONE) {
                    val secondItemView = recyclerView?.findViewHolderForAdapterPosition(1)?.itemView

                    secondItemView?.let { view ->
                        TapTargetView.showFor(requireActivity(),
                            TapTarget.forView(view.findViewById(R.id.img_game), getString(R.string.highlight_shortcuts))
                                .transparentTarget(true)
                                .cancelable(true)
                        )
                    }
                    recyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)

                    preferences!!.edit().apply {
                        putBoolean(HIGHLIGHT_SHORTCUT_PREFERENCE_KEY, true)
                        apply()
                    }
                }
            }
        })

        registerForContextMenu(recyclerView!!)

        selectedWinePrefixSpinner = rootView?.findViewById(R.id.selectedWinePrefixSpinner)

        val winePrefixes = winePrefixesDir.listFiles()?.map { it.name }?.toTypedArray() ?: emptyArray()

        selectedWinePrefixSpinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, winePrefixes)
        selectedWinePrefixSpinner?.setSelection(winePrefixes.indexOf(winePrefix?.name))
        selectedWinePrefixSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                preferences!!.edit().apply {
                    putString(SELECTED_WINE_PREFIX, parent?.selectedItem.toString())
                    apply()
                }

                setSharedVars(requireActivity())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        createWinePrefixButton = rootView?.findViewById(R.id.createWinePrefix)
        createWinePrefixButton?.setOnClickListener {
            lifecycleScope.launch {
                setSharedVars(requireActivity())
                CreatePresetFragment(WINEPREFIX_PRESET).show(requireActivity().supportFragmentManager, "")
            }
        }

        deleteWinePrefixButton = rootView?.findViewById(R.id.deleteWinePrefix)
        deleteWinePrefixButton?.setOnClickListener {
            DeleteItemFragment(DELETE_WINE_PREFIX, requireContext()).show(requireActivity().supportFragmentManager, "")
        }

        activity?.registerReceiver(receiver, object : IntentFilter(ACTION_UPDATE_WINE_PREFIX_SPINNER) {})

        return rootView
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        recyclerView?.layoutManager = GridLayoutManager(requireContext(), getSpanCount())
    }

    private fun setupDragAndDrop() {
        val callback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                return makeMovementFlags(dragFlags, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                if (target.adapterPosition == 0 || viewHolder.adapterPosition == 0) return false

                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                val initialGame = gameList[fromPosition]

                gameList.removeAt(fromPosition)
                gameList.add(toPosition, initialGame)

                recyclerView.adapter?.notifyItemMoved(fromPosition, toPosition)

                saveShortcuts()

                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled(): Boolean = true
            override fun isItemViewSwipeEnabled(): Boolean = false
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper?.attachToRecyclerView(recyclerView)
    }

    private fun dpToPx(dp: Int, context: Context): Float {
        val density = context.resources.displayMetrics.density
        return dp * density
    }

    class GridSpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
        ) {
            outRect.left = spacing
            outRect.right = spacing
            outRect.top = spacing
            outRect.bottom = spacing
        }
    }

    private fun setAdapter() {
        recyclerView?.setAdapter(AdapterGame(gameListNames, requireActivity()))

        gameListNames.clear()

        gameList.forEach {
            addToAdapter(it.name, it.exePath, it.exeArguments, it.iconPath)
        }
    }

    private fun addToAdapter(name: String, exePath: String, exeArguments: String, iconPath: String) {
        gameListNames.add(AdapterGame.GameItem(name, exePath, exeArguments, iconPath))
    }

    private fun getSpanCount(): Int {
        return max(1F, requireActivity().resources.displayMetrics.widthPixels / dpToPx(150, requireContext())).toInt()
    }

    companion object {
        private val gson = Gson()
        private var recyclerView: RecyclerView? = null
        private var gameListNames: MutableList<AdapterGame.GameItem> = mutableListOf()
        private var gameList: MutableList<GameItem> = mutableListOf()

        const val HIGHLIGHT_SHORTCUT_PREFERENCE_KEY = "highlightedShortcut"
        const val ACTION_UPDATE_WINE_PREFIX_SPINNER = "com.micewine.emu.ACTION_UPDATE_WINE_PREFIX_SPINNER"

        const val MESA_DRIVER = 0
        const val ADRENO_TOOLS_DRIVER = 1

        fun initialize(context: Context) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context)!!
            gameList = getGameList()
        }

        fun putWineVirtualDesktop(name: String, enabled: Boolean) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].wineVirtualDesktop = enabled

            saveShortcuts()
        }

        fun getWineVirtualDesktop(name: String): Boolean {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return false

            return gameList[index].wineVirtualDesktop
        }

        fun putCpuAffinity(name: String, cpuCores: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].cpuAffinityCores = cpuCores

            saveShortcuts()
        }

        fun getCpuAffinity(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return availableCPUs.joinToString(",")

            return gameList[index].cpuAffinityCores
        }

        fun putWineServices(name: String, enabled: Boolean) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].wineServices = enabled

            saveShortcuts()
        }

        fun getWineServices(name: String): Boolean {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return false

            return gameList[index].wineServices
        }

        fun putWineESync(name: String, enabled: Boolean) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].wineESync = enabled

            saveShortcuts()
        }

        fun getWineESync(name: String): Boolean {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return true

            return gameList[index].wineESync
        }

        fun putVKD3DVersion(name: String, vkd3dVersion: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].vkd3dVersion = vkd3dVersion

            saveShortcuts()
        }

        fun getVKD3DVersion(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return ""

            return gameList[index].vkd3dVersion
        }

        fun putWineD3DVersion(name: String, wineD3DVersion: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].wineD3DVersion = wineD3DVersion

            saveShortcuts()
        }

        fun getWineD3DVersion(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return ""

            return gameList[index].wineD3DVersion
        }

        fun putDXVKVersion(name: String, dxvkVersion: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].dxvkVersion = dxvkVersion

            saveShortcuts()
        }

        fun getDXVKVersion(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return ""

            return gameList[index].dxvkVersion
        }

        fun putD3DXRenderer(name: String, d3dxRenderer: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].d3dxRenderer = d3dxRenderer

            saveShortcuts()
        }

        fun getD3DXRenderer(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return "DXVK"

            return gameList[index].d3dxRenderer
        }

        private fun putVulkanDriverType(name: String, driverType: Int) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].vulkanDriverType = driverType

            saveShortcuts()
        }

        fun getVulkanDriverType(name: String): Int {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return MESA_DRIVER

            return gameList[index].vulkanDriverType
        }

        fun putVulkanDriver(name: String, driverName: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].vulkanDriver = driverName

            if (driverName.startsWith("AdrenoToolsDriver-")) {
                putVulkanDriverType(name, ADRENO_TOOLS_DRIVER)
            } else (
                putVulkanDriverType(name, MESA_DRIVER)
            )

            saveShortcuts()
        }

        fun getVulkanDriver(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return ""

            return gameList[index].vulkanDriver
        }

        fun putDisplaySettings(name: String, displayMode: String, displayResolution: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].displayMode = displayMode
            gameList[index].displayResolution = displayResolution

            saveShortcuts()
        }

        fun getDisplaySettings(name: String): List<String> {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return listOf("16:9", "1280x720")

            return listOf(gameList[index].displayMode, gameList[index].displayResolution)
        }

        fun putBox64Version(name: String, box64VersionId: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].box64Version = box64VersionId

            saveShortcuts()
        }

        fun getBox64Version(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return listRatPackagesId("Box64-").firstOrNull() ?: ""

            return gameList[index].box64Version
        }

        fun putBox64Preset(name: String, presetName: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].box64Preset = presetName

            saveShortcuts()
        }

        fun getBox64Preset(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return "default"

            return gameList[index].box64Preset
        }

        fun putControllerXInput(name: String, enabled: Boolean, controllerIndex: Int) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].controllersEnableXInput[controllerIndex] = enabled

            saveShortcuts()
        }

        fun getControllerXInput(name: String, controllerIndex: Int): Boolean {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return false

            return gameList[index].controllersEnableXInput[controllerIndex]
        }

        fun putControllerPreset(name: String, presetName: String, controllerIndex: Int) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].controllersPreset[controllerIndex] = presetName

            saveShortcuts()
        }

        fun getControllerPreset(name: String, controllerIndex: Int): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return "default"

            return gameList[index].controllersPreset[controllerIndex]
        }

        fun putVirtualControllerXInput(name: String, enabled: Boolean) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].virtualControllerEnableXInput = enabled

            saveShortcuts()
        }

        fun getVirtualControllerXInput(name: String): Boolean {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return false

            return gameList[index].virtualControllerEnableXInput
        }

        fun putVirtualControllerPreset(name: String, presetName: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].virtualControllerPreset = presetName

            saveShortcuts()
        }

        fun getVirtualControllerPreset(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return "default"

            return gameList[index].virtualControllerPreset
        }

        fun addGameToList(path: String, prettyName: String, icon: String) {
            val gameExists = gameList.any { it.name == prettyName }

            if (gameExists) return

            gameList.add(
                GameItem(
                    prettyName,
                    path,
                    "",
                    icon,
                    "Global",
                    "default",
                    mutableListOf("default", "default", "default", "default"),
                    mutableListOf(false, false, false, false),
                    "default",
                    false,
                    "16:9",
                    "1280x720",
                    "Global",
                    MESA_DRIVER,
                    "DXVK",
                    listRatPackagesId("DXVK").first(),
                    listRatPackagesId("WineD3D").first(),
                    listRatPackagesId("VKD3D").first(),
                    true,
                    false,
                    availableCPUs.joinToString(","),
                    false,
                    false
                )
            )
            gameListNames.add(
                AdapterGame.GameItem(prettyName, path, "", icon)
            )

            saveShortcuts()

            recyclerView?.post {
                recyclerView?.adapter?.notifyItemInserted(gameListNames.size)
            }
        }

        fun removeGameFromList(name: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList.removeAt(index)
            gameListNames.removeAt(index)

            saveShortcuts()

            recyclerView?.adapter?.notifyItemRemoved(index)
        }

        fun editGameFromList(name: String, newName: String, newArguments: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].name = newName
            gameList[index].exeArguments = newArguments
            gameListNames[index].name = newName
            gameListNames[index].exeArguments = newArguments

            saveShortcuts()

            recyclerView?.adapter?.notifyItemChanged(index)
        }

        fun setIconToGame(name: String, context: Context, uri: Uri) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            createIconCache(context, uri, name)

            gameList[index].iconPath = "$usrDir/icons/$name-icon"
            gameListNames[index].iconPath = "$usrDir/icons/$name-icon"

            saveShortcuts()

            recyclerView?.adapter?.notifyItemChanged(index)
        }

        fun getGameIcon(name: String): Bitmap? {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return null

            return BitmapFactory.decodeFile(gameList[index].iconPath)
        }

        fun getGameExeArguments(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return ""

            return gameList[index].exeArguments
        }

        private fun createIconCache(context: Context, uri: Uri, gameName: String) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = File("$usrDir/icons/$gameName-icon").outputStream()

            copyFile(inputStream!!, outputStream)
        }

        fun saveShortcuts() {
            preferences?.edit {
                putString("gameList", gson.toJson(gameList))
                apply()
            }
        }

        private fun getGameList(): MutableList<GameItem> {
            val json = preferences?.getString("gameList", "")
            val listType = object : TypeToken<MutableList<GameItem>>() {}.type
            val gameList = gson.fromJson<MutableList<GameItem>>(json, listType)

            return gameList ?: mutableListOf()
        }

        fun addGameToLauncher(context: Context, name: String) {
            val index = gameList.indexOfFirst { it.name == name }
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)

            if (shortcutManager!!.isRequestPinShortcutSupported) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("exePath", gameList[index].exePath)
                    putExtra("exeArguments", gameList[index].exeArguments)
                }

                val pinShortcutInfo = ShortcutInfo.Builder(context, gameList[index].name)
                    .setShortLabel(gameList[index].name)
                    .setIcon(
                        if (File(gameList[index].iconPath).exists()) {
                            Icon.createWithBitmap(BitmapFactory.decodeFile(gameList[index].iconPath))
                        } else {
                            Icon.createWithResource(context, R.drawable.default_icon)
                        }
                    )

                    .setIntent(intent)
                    .build()

                val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(pinShortcutInfo)
                val successCallback = PendingIntent.getBroadcast(context, 0, pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE)

                shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.intentSender)
            }
        }

        data class GameItem(
            var name: String,
            var exePath: String,
            var exeArguments: String,
            var iconPath: String,
            var box64Version: String,
            var box64Preset: String,
            var controllersPreset: MutableList<String>,
            var controllersEnableXInput: MutableList<Boolean>,
            var virtualControllerPreset: String,
            var virtualControllerEnableXInput: Boolean,
            var displayMode: String,
            var displayResolution: String,
            var vulkanDriver: String,
            var vulkanDriverType: Int,
            var d3dxRenderer: String,
            var dxvkVersion: String,
            var wineD3DVersion: String,
            var vkd3dVersion: String,
            var wineESync: Boolean,
            var wineServices: Boolean,
            var cpuAffinityCores: String,
            var wineVirtualDesktop: Boolean,
            var enableXInput: Boolean
        )
    }
}
