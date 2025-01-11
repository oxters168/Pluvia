package com.OxGames.Pluvia

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.room.withTransaction
import com.OxGames.Pluvia.data.AppInfo
import com.OxGames.Pluvia.data.BranchInfo
import com.OxGames.Pluvia.data.ConfigInfo
import com.OxGames.Pluvia.data.DepotInfo
import com.OxGames.Pluvia.data.DownloadInfo
import com.OxGames.Pluvia.data.GameProcessInfo
import com.OxGames.Pluvia.data.LaunchInfo
import com.OxGames.Pluvia.data.LibraryAssetsInfo
import com.OxGames.Pluvia.data.LibraryCapsuleInfo
import com.OxGames.Pluvia.data.LibraryHeroInfo
import com.OxGames.Pluvia.data.LibraryLogoInfo
import com.OxGames.Pluvia.data.ManifestInfo
import com.OxGames.Pluvia.data.PackageInfo
import com.OxGames.Pluvia.data.PostSyncInfo
import com.OxGames.Pluvia.data.SaveFilePattern
import com.OxGames.Pluvia.data.SteamFriend
import com.OxGames.Pluvia.data.UFS
import com.OxGames.Pluvia.data.UserFileInfo
import com.OxGames.Pluvia.data.UserFilesDownloadResult
import com.OxGames.Pluvia.data.UserFilesUploadResult
import com.OxGames.Pluvia.db.PluviaDatabase
import com.OxGames.Pluvia.enums.AppType
import com.OxGames.Pluvia.enums.ControllerSupport
import com.OxGames.Pluvia.enums.Language
import com.OxGames.Pluvia.enums.LoginResult
import com.OxGames.Pluvia.enums.OS
import com.OxGames.Pluvia.enums.OSArch
import com.OxGames.Pluvia.enums.PathType
import com.OxGames.Pluvia.enums.ReleaseState
import com.OxGames.Pluvia.enums.SaveLocation
import com.OxGames.Pluvia.enums.SyncResult
import com.OxGames.Pluvia.events.AndroidEvent
import com.OxGames.Pluvia.events.SteamEvent
import com.OxGames.Pluvia.utils.FileUtils
import com.OxGames.Pluvia.utils.SteamUtils
import com.google.android.play.core.ktx.bytesDownloaded
import com.google.android.play.core.ktx.requestCancelInstall
import com.google.android.play.core.ktx.requestInstall
import com.google.android.play.core.ktx.requestSessionState
import com.google.android.play.core.ktx.status
import com.google.android.play.core.ktx.totalBytesToDownload
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.winlator.xenvironment.ImageFs
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.enums.EOSType
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.networking.steam3.ProtocolTypes
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChat_RequestFriendPersonaStates_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientObjects.ECloudPendingRemoteOperation
import `in`.dragonbra.javasteam.rpc.service.Chat
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.AuthenticationException
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.contentdownloader.ContentDownloader
import `in`.dragonbra.javasteam.steam.contentdownloader.FileManifestProvider
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.discovery.ServerQuality
import `in`.dragonbra.javasteam.steam.handlers.steamapps.GamePlayedInfo
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.LicenseListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSProductInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.AppFileChangeList
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.AppFileInfo
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendsListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.NicknameListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStatesCallback
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamworkshop.SteamWorkshop
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration
import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.NetHelpers
import `in`.dragonbra.javasteam.util.crypto.CryptoHelper
import `in`.dragonbra.javasteam.util.log.LogListener
import `in`.dragonbra.javasteam.util.log.LogManager
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Date
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import java.util.zip.ZipInputStream
import javax.inject.Inject
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.time.measureTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

@AndroidEntryPoint
class SteamService : Service(), IChallengeUrlChanged {

    @Inject
    lateinit var db: PluviaDatabase

    private lateinit var notificationHelper: NotificationHelper

    private var _callbackManager: CallbackManager? = null
    private var _steamClient: SteamClient? = null
    private var _steamUser: SteamUser? = null
    private var _steamApps: SteamApps? = null
    private var _steamFriends: SteamFriends? = null
    private var _steamCloud: SteamCloud? = null
    private var _unifiedMessages: SteamUnifiedMessages? = null
    private var _unifiedChat: Chat? = null

    private val _callbackSubscriptions: ArrayList<Closeable> = ArrayList()

    private var _loginResult: LoginResult = LoginResult.Failed

    private var retryAttempt = 0

    private val packageInfo = ConcurrentHashMap<Int, PackageInfo>()
    private val appInfo = ConcurrentHashMap<Int, AppInfo>()

    private val dbScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val MAX_RETRY_ATTEMPTS = 20
        const val AVATAR_BASE_URL = "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/"
        const val MISSING_AVATAR_URL = "${AVATAR_BASE_URL}fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"
        const val INVALID_APP_ID: Int = Int.MAX_VALUE
        const val INVALID_PKG_ID: Int = Int.MAX_VALUE
        const val INVALID_DEPOT_ID: Int = Int.MAX_VALUE
        const val INVALID_MANIFEST_ID: Long = Long.MAX_VALUE

        private val PROTOCOL_TYPES = EnumSet.of(ProtocolTypes.TCP, ProtocolTypes.UDP)

        private var instance: SteamService? = null

        private val downloadJobs = ConcurrentHashMap<Int, DownloadInfo>()

        var isConnecting: Boolean = false
            private set
        var isStopping: Boolean = false
            private set
        var isConnected: Boolean = false
            private set
        var isRunning: Boolean = false
            private set
        var isLoggingIn: Boolean = false
            private set
        var isLoggingOut: Boolean = false
            private set
        val isLoggedIn: Boolean
            get() = instance?._steamClient?.steamID?.run { isValid } == true
        var isWaitingForQRAuth: Boolean = false
            private set
        var isReceivingLicenseList: Boolean = false
            private set
        var isRequestingPkgInfo: Boolean = false
            private set
        var isRequestingAppInfo: Boolean = false
            private set

        private val serverListPath: String
            get() = Paths.get(instance!!.cacheDir.path, "server_list.bin").pathString

        private val depotManifestsPath: String
            get() = Paths.get(instance!!.dataDir.path, "Steam", "depot_manifests.zip").pathString

        val defaultAppInstallPath: String
            get() = Paths.get(instance!!.dataDir.path, "Steam", "steamapps", "common").pathString

        val defaultAppStagingPath: String
            get() = Paths.get(instance!!.dataDir.path, "Steam", "steamapps", "staging").pathString

        val userSteamId: SteamID?
            get() = instance?._steamClient?.steamID

        fun setPersonaState(state: EPersonaState) {
            instance?._steamFriends?.setPersonaState(state)
        }

        fun requestUserPersona() {
            CoroutineScope(Dispatchers.Default).launch {
                userSteamId?.let {
                    // in order to get user avatar url and other info
                    instance?._steamFriends?.requestFriendInfo(it)
                }
            }
        }

        fun getPersonaStateOf(steamId: SteamID): SteamFriend? {
            return runBlocking {
                instance!!.db
                    .steamFriendDao()
                    .findFriend(steamId.convertToUInt64())
                    .first()
            }
        }

        fun getAppList(filter: EnumSet<AppType>): List<AppInfo> {
            return instance?.appInfo?.values?.filter { filter.contains(it.type) } ?: emptyList()
        }

        fun getPkgInfoOf(appId: Int): PackageInfo? {
            return instance?.packageInfo?.values?.firstOrNull {
                // logD("Pkg (${it.packageId}) apps: ${it.appIds.joinToString(",")}")
                it.appIds.contains(appId)
            }
        }

        fun getAppInfoOf(appId: Int): AppInfo? {
            return instance?.appInfo?.values?.firstOrNull {
                it.appId == appId
            }
        }

        fun getAppDownloadInfo(appId: Int): DownloadInfo? {
            return downloadJobs[appId]
        }

        fun isAppInstalled(appId: Int): Boolean {
            val appDownloadInfo = getAppDownloadInfo(appId)
            val isNotDownloading = appDownloadInfo == null || appDownloadInfo.getProgress() >= 1f
            val appDirPath = Paths.get(getAppDirPath(appId))
            val pathExists = Files.exists(appDirPath)

            // logD("isDownloading: $isNotDownloading && pathExists: $pathExists && appDirPath: $appDirPath")

            return isNotDownloading && pathExists
        }

        fun getAppDlc(appId: Int): Map<Int, DepotInfo> {
            return getAppInfoOf(appId)?.let {
                it.depots.filter { it.value.dlcAppId != INVALID_APP_ID }
            } ?: emptyMap()
        }

        fun getOwnedAppDlc(appId: Int): Map<Int, DepotInfo> = getAppDlc(appId).filter {
            getPkgInfoOf(it.value.dlcAppId)?.let { pkg ->
                instance?._steamClient?.let { steamClient ->
                    pkg.ownerAccountId == steamClient.steamID.accountID.toInt()
                }
            } == true
        }

        fun getDownloadableDepots(appId: Int): Map<Int, DepotInfo> = getAppInfoOf(appId)?.depots?.filter { depotEntry ->
            val depot = depotEntry.value

            (depot.manifests.isNotEmpty() || depot.sharedInstall) &&
                (depot.osList.contains(OS.windows) || (!depot.osList.contains(OS.linux) && !depot.osList.contains(OS.macos))) &&
                (depot.osArch == OSArch.Arch64 || depot.osArch == OSArch.Unknown) &&
                (depot.dlcAppId == INVALID_APP_ID || getOwnedAppDlc(appId).containsKey(depot.depotId))
        } ?: emptyMap()

        fun getAppDirPath(appId: Int): String {
            var appName = getAppInfoOf(appId)?.config?.installDir.orEmpty()

            if (appName.isEmpty()) {
                appName = getAppInfoOf(appId)?.name.orEmpty()
            }

            return Paths.get(PrefManager.appInstallPath, appName).pathString
        }

        fun deleteApp(appId: Int): Boolean {
            CoroutineScope(Dispatchers.IO).launch {
                instance?.db?.appChangeNumbers()?.deleteByAppId(appId)
                instance?.db?.appFileChangeLists()?.deleteByAppId(appId)
            }

            val appDirPath = getAppDirPath(appId)

            return File(appDirPath).deleteRecursively()
        }

        fun downloadApp(appId: Int): DownloadInfo? {
            return getAppInfoOf(appId)?.let { appInfo ->
                Timber.i("App contains ${appInfo.depots.size} depot(s): ${appInfo.depots.keys}")
                downloadApp(appId, getDownloadableDepots(appId).keys.toList(), "public")
            }
        }

        fun downloadApp(appId: Int, depotIds: List<Int>, branch: String): DownloadInfo? {
            if (downloadJobs.contains(appId)) {
                Timber.w("Could not start new download job for $appId since one already exists")
                return getAppDownloadInfo(appId)
            }

            if (depotIds.isEmpty()) {
                Timber.w("No depots to download for $appId")
                return null
            }

            Timber.i("Found ${depotIds.size} depot(s) to download: $depotIds")

            val needsImageFsDownload = !ImageFs.find(instance!!).rootDir.exists() &&
                !FileUtils.assetExists(instance!!.assets, "imagefs.txz")
            val indexOffset = if (needsImageFsDownload) 1 else 0
            var moduleInstallSessionId = -1
            val downloadInfo = DownloadInfo(depotIds.size + indexOffset).also { downloadInfo ->
                downloadInfo.setDownloadJob(
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // TODO: put this somewhere more appropriate and deal with user confirmation status
                            if (needsImageFsDownload) {
                                Timber.i("imagefs.txz will be downloaded")
                                val splitManager = SplitInstallManagerFactory.create(instance!!)
                                if (!splitManager.installedModules.contains("ubuntufs")) {
                                    moduleInstallSessionId = splitManager.requestInstall(listOf("ubuntufs"))
                                    var isInstalling = true
                                    do {
                                        val sessionState = splitManager.requestSessionState(moduleInstallSessionId)
                                        // logD("imagefs.txz session state status: ${sessionState.status}")
                                        when (sessionState.status) {
                                            SplitInstallSessionStatus.INSTALLED -> isInstalling = false
                                            SplitInstallSessionStatus.PENDING,
                                            SplitInstallSessionStatus.INSTALLING,
                                            SplitInstallSessionStatus.DOWNLOADED,
                                            SplitInstallSessionStatus.DOWNLOADING,
                                                -> {
                                                if (!isActive) {
                                                    splitManager.cancelInstall(moduleInstallSessionId)
                                                    break
                                                }
                                                val downloadPercent =
                                                    sessionState.bytesDownloaded.toFloat() / sessionState.totalBytesToDownload
                                                // logD("imagefs.txz download percent: $downloadPercent")
                                                downloadInfo.setProgress(downloadPercent, 0)
                                                delay(100)
                                            }

                                            else -> {
                                                cancel("Failed to install ubuntufs module: ${sessionState.status}")
                                            }
                                        }
                                    } while (isInstalling)
                                    val installedProperly = splitManager.installedModules.contains("ubuntufs")
                                    Timber.i("imagefs.txz module installed properly: $installedProperly")
                                } else {
                                    Timber.e("Missing imagefs.txz but ubuntufs module already installed")
                                }
                            }
                            depotIds.forEachIndexed { jobIndex, depotId ->
                                // TODO: download shared install depots to a common location
                                ContentDownloader(instance!!._steamClient!!).downloadApp(
                                    appId = appId,
                                    depotId = depotId,
                                    installPath = PrefManager.appInstallPath,
                                    stagingPath = PrefManager.appStagingPath,
                                    branch = branch,
                                    // maxDownloads = 1,
                                    onDownloadProgress = { downloadInfo.setProgress(it, jobIndex + indexOffset) },
                                    parentScope = coroutineContext.job as CoroutineScope,
                                ).await()
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Download failed")
                            if (moduleInstallSessionId != -1) {
                                val splitManager = SplitInstallManagerFactory.create(instance!!)
                                val sessionState = splitManager.requestSessionState(moduleInstallSessionId)
                                if (sessionState.status == SplitInstallSessionStatus.DOWNLOADING ||
                                    sessionState.status == SplitInstallSessionStatus.DOWNLOADED ||
                                    sessionState.status == SplitInstallSessionStatus.INSTALLING
                                ) {
                                    splitManager.requestCancelInstall(moduleInstallSessionId)
                                }
                            }
                        }

                        downloadJobs.remove(appId)
                    },
                )
            }

            downloadJobs[appId] = downloadInfo

            return downloadInfo
        }

        fun getWindowsLaunchInfos(appId: Int): List<LaunchInfo> {
            return getAppInfoOf(appId)?.let { appInfo ->
                appInfo.config.launch.filter { launchInfo ->
                    // since configOS was unreliable and configArch was even more unreliable
                    launchInfo.executable.endsWith(".exe")
                }
            } ?: emptyList()
        }

        /**
         * Default timeout to use when making requests
         */
        var requestTimeout = 10000L

        /**
         * Default timeout to use when reading the response body
         */
        var responseBodyTimeout = 60000L

        var syncInProgress: Boolean = false

        const val MAX_USER_FILE_RETRIES = 3

        fun notifyRunningProcesses(vararg gameProcesses: GameProcessInfo) {
            instance?.let { steamInstance ->
                val gamesPlayed = gameProcesses.mapNotNull { gameProcess ->
                    getAppInfoOf(gameProcess.appId)?.let { appInfo ->
                        getPkgInfoOf(gameProcess.appId)?.let { pkgInfo ->
                            appInfo.branches[gameProcess.branch]?.let { branch ->
                                val processId = gameProcess.processes
                                    .firstOrNull { it.parentIsSteam }
                                    ?.processId
                                    ?: gameProcess.processes.firstOrNull()?.processId
                                    ?: 0

                                GamePlayedInfo(
                                    gameId = gameProcess.appId.toLong(),
                                    processId = processId,
                                    ownerId = pkgInfo.ownerAccountId,
                                    // TODO: figure out what this is and un-hardcode
                                    launchSource = 100,
                                    gameBuildId = branch.buildId.toInt(),
                                    processIdList = gameProcess.processes,
                                )
                            }
                        }
                    }
                }

                Timber.i(
                    "GameProcessInfo:" +
                        gamesPlayed.joinToString("\n") {
                            "\n\tprocessId: ${it.processId}" +
                                "\n\tgameId: ${it.gameId}" +
                                "\n\tprocesses: ${
                                    it.processIdList.joinToString("\n") {
                                        "\n\t\tprocessId: ${it.processId}" +
                                            "\n\t\tprocessIdParent: ${it.processIdParent}" +
                                            "\n\t\tparentIsSteam: ${it.parentIsSteam}"
                                    }
                                }"
                        },
                )

                steamInstance._steamApps?.notifyGamesPlayed(
                    gamesPlayed = gamesPlayed,
                    clientOsType = EOSType.AndroidUnknown,
                )
            }
        }

        fun beginLaunchApp(
            appId: Int,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            ignorePendingOperations: Boolean = false,
            preferredSave: SaveLocation = SaveLocation.None,
            prefixToPath: (String) -> String,
        ): Deferred<PostSyncInfo> = parentScope.async {
            if (syncInProgress) {
                Timber.w("Cannot launch app when sync already in progress")
                return@async PostSyncInfo(SyncResult.InProgress)
            }

            syncInProgress = true

            var syncResult = PostSyncInfo(SyncResult.UnknownFail)

            PrefManager.clientId?.let { clientId ->
                instance?.let { steamInstance ->
                    getAppInfoOf(appId)?.let { appInfo ->
                        steamInstance._steamCloud?.let { steamCloud ->
                            val postSyncInfo = syncUserFiles(
                                appInfo = appInfo,
                                clientId = clientId,
                                steamInstance = steamInstance,
                                steamCloud = steamCloud,
                                preferredSave = preferredSave,
                                parentScope = parentScope,
                                prefixToPath = prefixToPath,
                            ).await()

                            // steamCloud.appCloudSyncStats(
                            //     appId = appId,
                            //     platformType = EPlatformType.Win32,
                            //     preload = false,
                            //     blockingAppLaunch = true,
                            //     filesUploaded = postSyncInfo?.filesUploaded ?: 0,
                            //     filesDownloaded = postSyncInfo?.filesDownloaded ?: 0,
                            //     filesDeleted = postSyncInfo?.filesDeleted ?: 0,
                            //     filesManaged = postSyncInfo?.filesManaged ?: 0,
                            //     bytesUploaded = postSyncInfo?.bytesUploaded ?: 0,
                            //     bytesDownloaded = postSyncInfo?.bytesDownloaded ?: 0,
                            //     microsecTotal = postSyncInfo?.microsecTotal ?: 0,
                            //     microsecInitCaches = postSyncInfo?.microsecInitCaches ?: 0,
                            //     microsecValidateState = postSyncInfo?.microsecValidateState ?: 0,
                            //     microsecAcLaunch = postSyncInfo?.microsecAcLaunch ?: 0,
                            //     microsecAcPrepUserFiles = postSyncInfo?.microsecAcPrepUserFiles ?: 0,
                            //     microsecAcExit = postSyncInfo?.microsecAcExit ?: 0,
                            //     microsecBuildSyncList = postSyncInfo?.microsecBuildSyncList ?: 0,
                            //     microsecDeleteFiles = postSyncInfo?.microsecDeleteFiles ?: 0,
                            //     microsecDownloadFiles = postSyncInfo?.microsecDownloadFiles ?: 0,
                            //     microsecUploadFiles = postSyncInfo?.microsecUploadFiles ?: 0,
                            // )

                            postSyncInfo?.let {
                                syncResult = it

                                if (it.syncResult == SyncResult.Success || it.syncResult == SyncResult.UpToDate) {
                                    Timber.i(
                                        "Signaling app launch:\n\tappId: %d\n\tclientId: %s\n\tosType: %s",
                                        appId,
                                        PrefManager.clientId,
                                        EOSType.AndroidUnknown,
                                    )

                                    val pendingRemoteOperations = steamCloud.signalAppLaunchIntent(
                                        appId = appId,
                                        clientId = clientId,
                                        machineName = SteamUtils.getMachineName(steamInstance),
                                        ignorePendingOperations = ignorePendingOperations,
                                        osType = EOSType.AndroidUnknown,
                                    ).await()

                                    if (pendingRemoteOperations.isNotEmpty() && !ignorePendingOperations) {
                                        syncResult = PostSyncInfo(
                                            SyncResult.PendingOperations,
                                            pendingRemoteOperations = pendingRemoteOperations,
                                        )
                                    } else if (ignorePendingOperations &&
                                        pendingRemoteOperations.any {
                                            it.operation == ECloudPendingRemoteOperation.k_ECloudPendingRemoteOperationAppSessionActive
                                        }
                                    ) {
                                        steamInstance._steamUser!!.kickPlayingSession()
                                    }
                                    // else {
                                    //     val gameId = GameID()
                                    //     gameId.appID = appId
                                    //     // TODO: un-hardcode
                                    //     gameId.appType = GameID.GameType.APP
                                    //     // TODO: un-hardcode
                                    //     gameId.modID = 0
                                    //     steamInstance._steamCloud?.sendClientAppUsageEvent(
                                    //         gameId = gameId,
                                    //         // TODO: un-hardcode
                                    //         usageEvent = EAppUsageEvent.GameLaunch,
                                    //         // TODO: un-hardcode
                                    //         offline = 0,
                                    //     )
                                    // }
                                }
                            }
                        }
                    }
                }
            }

            syncInProgress = false

            return@async syncResult
        }

        fun closeApp(
            appId: Int,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            prefixToPath: (String) -> String,
        ) = parentScope.async {
            if (syncInProgress) {
                Timber.w("Cannot close app when sync already in progress")
                return@async
            }

            syncInProgress = true

            PrefManager.clientId?.let { clientId ->
                instance?.let { steamInstance ->
                    getAppInfoOf(appId)?.let { appInfo ->
                        steamInstance._steamCloud?.let { steamCloud ->
                            val postSyncInfo = syncUserFiles(
                                appInfo = appInfo,
                                clientId = clientId,
                                steamInstance = steamInstance,
                                steamCloud = steamCloud,
                                parentScope = parentScope,
                                prefixToPath = prefixToPath,
                            ).await()
                            // steamCloud.appCloudSyncStats(
                            //     appId = appId,
                            //     platformType = EPlatformType.Win32,
                            //     preload = false,
                            //     blockingAppLaunch = false,
                            //     filesUploaded = postSyncInfo?.filesUploaded ?: 0,
                            //     filesDownloaded = postSyncInfo?.filesDownloaded ?: 0,
                            //     filesDeleted = postSyncInfo?.filesDeleted ?: 0,
                            //     filesManaged = postSyncInfo?.filesManaged ?: 0,
                            //     bytesUploaded = postSyncInfo?.bytesUploaded ?: 0,
                            //     bytesDownloaded = postSyncInfo?.bytesDownloaded ?: 0,
                            //     microsecTotal = postSyncInfo?.microsecTotal ?: 0,
                            //     microsecInitCaches = postSyncInfo?.microsecInitCaches ?: 0,
                            //     microsecValidateState = postSyncInfo?.microsecValidateState ?: 0,
                            //     microsecAcLaunch = postSyncInfo?.microsecAcLaunch ?: 0,
                            //     microsecAcPrepUserFiles = postSyncInfo?.microsecAcPrepUserFiles ?: 0,
                            //     microsecAcExit = postSyncInfo?.microsecAcExit ?: 0,
                            //     microsecBuildSyncList = postSyncInfo?.microsecBuildSyncList ?: 0,
                            //     microsecDeleteFiles = postSyncInfo?.microsecDeleteFiles ?: 0,
                            //     microsecDownloadFiles = postSyncInfo?.microsecDownloadFiles ?: 0,
                            //     microsecUploadFiles = postSyncInfo?.microsecUploadFiles ?: 0,
                            // )
                            steamCloud.signalAppExitSyncDone(
                                appId = appId,
                                clientId = clientId,
                                uploadsCompleted = postSyncInfo?.uploadsCompleted == true,
                                uploadsRequired = postSyncInfo?.uploadsRequired == false,
                            )
                        }
                    }
                }
            }

            syncInProgress = false
        }

        fun getProotTime(context: Context): Long {
            val imageFs = ImageFs.find(context)

            if (!imageFs.rootDir.exists()) {
                return 0
            }

            val nativeLibraryDir = context.applicationInfo.nativeLibraryDir

            val command = arrayOf(
                "$nativeLibraryDir/libproot.so",
                "--kill-on-exit",
                "--rootfs=${imageFs.rootDir}",
                "--cwd=${ImageFs.USER}",
                "--bind=/dev",
                "--bind=${imageFs.rootDir}/tmp/shm:/dev/shm",
                "--bind=/proc",
                "--bind=/sys",
                "/usr/bin/env",
                "HOME=/home/${ImageFs.USER}",
                "USER=${ImageFs.USER}",
                "TMPDIR=/tmp",
                "LC_ALL=en_US.utf8",
                // Set PATH environment variable
                "PATH=${imageFs.winePath}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "LD_LIBRARY_PATH=/usr/lib/aarch64-linux-gnu:/usr/lib/arm-linux-gnueabihf",
                "date",
                "+%s%3N",
            )

            val envVars = arrayOf(
                "PROOT_TMP_DIR=${Paths.get(context.filesDir.absolutePath, "tmp")}",
                "PROOT_LOADER=$nativeLibraryDir/libproot-loader.so",
            )

            val process = Runtime.getRuntime().exec(command, envVars, imageFs.rootDir)

            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val error = errorReader.readLine()

            if (error != null) {
                Timber.e("ProotTime: Error: $error")
            }

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine().orEmpty()
            process.waitFor()

            Timber.i("ProotTime: Output: $output")

            return output.toLongOrNull() ?: 0
        }

        data class FileChanges(
            val filesDeleted: List<UserFileInfo>,
            val filesModified: List<UserFileInfo>,
            val filesCreated: List<UserFileInfo>,
        )

        /**
         * [Steam Auto Cloud](https://partner.steamgames.com/doc/features/cloud#steam_auto-cloud)
         */
        fun syncUserFiles(
            appInfo: AppInfo,
            clientId: Long,
            steamInstance: SteamService,
            steamCloud: SteamCloud,
            preferredSave: SaveLocation = SaveLocation.None,
            parentScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
            prefixToPath: (String) -> String,
        ): Deferred<PostSyncInfo?> = parentScope.async {
            val postSyncInfo: PostSyncInfo?

            Timber.i("Retrieving save files of ${appInfo.name}")

            val printFileChangeList: (AppFileChangeList) -> Unit = { fileList ->
                @SuppressLint("BinaryOperationInTimber")
                Timber.i(
                    "GetAppFileListChange($appInfo.appId):" +
                        "\n\tTotal Files: ${fileList.files.size}" +
                        "\n\tCurrent Change Number: ${fileList.currentChangeNumber}" +
                        "\n\tIs Only Delta: ${fileList.isOnlyDelta}" +
                        "\n\tApp BuildID Hwm: ${fileList.appBuildIDHwm}" +
                        "\n\tPath Prefixes: \n\t\t${fileList.pathPrefixes.joinToString("\n\t\t")}" +
                        "\n\tMachine Names: \n\t\t${fileList.machineNames.joinToString("\n\t\t")}" +
                        fileList.files.joinToString {
                            "\n\t${it.filename}:" +
                                "\n\t\tshaFile: ${it.shaFile}" +
                                "\n\t\ttimestamp: ${it.timestamp}" +
                                "\n\t\trawFileSize: ${it.rawFileSize}" +
                                "\n\t\tpersistState: ${it.persistState}" +
                                "\n\t\tplatformsToSync: ${it.platformsToSync}" +
                                "\n\t\tpathPrefixIndex: ${it.pathPrefixIndex}" +
                                "\n\t\tmachineNameIndex: ${it.machineNameIndex}"
                        },
                )
            }

            val getPathTypePairs: (AppFileChangeList) -> List<Pair<String, String>> = { fileList ->
                fileList.pathPrefixes
                    .map {
                        val matchResults = Regex("%\\w+%").findAll(it).map { it.value }.toList()

                        Timber.i("Mapping prefix $it and found $matchResults")

                        matchResults
                    }
                    .flatten()
                    .distinct()
                    .map { Pair(it, prefixToPath(it)) }
            }

            val convertPrefixes: (AppFileChangeList) -> List<String> = { fileList ->
                val pathTypePairs = getPathTypePairs(fileList)

                fileList.pathPrefixes.map { prefix ->
                    var modified = prefix

                    pathTypePairs.forEach {
                        modified = modified.replace(it.first, it.second)
                    }

                    modified
                }
            }

            val getFilePrefix: (AppFileInfo, AppFileChangeList) -> String = { file, fileList ->
                if (file.pathPrefixIndex < fileList.pathPrefixes.size) {
                    Paths.get(fileList.pathPrefixes[file.pathPrefixIndex]).pathString
                } else {
                    Paths.get("%${PathType.GameInstall.name}%").pathString
                }
            }

            val getFilePrefixPath: (AppFileInfo, AppFileChangeList) -> String = { file, fileList ->
                Paths.get(getFilePrefix(file, fileList), file.filename).pathString
            }

            val getFullFilePath: (AppFileInfo, AppFileChangeList) -> Path = { file, fileList ->
                val convertedPrefixes = convertPrefixes(fileList)

                if (file.pathPrefixIndex < fileList.pathPrefixes.size) {
                    Paths.get(convertedPrefixes[file.pathPrefixIndex], file.filename)
                } else {
                    Paths.get(getAppDirPath(appInfo.appId), file.filename)
                }
            }

            val getFilesDiff: (List<UserFileInfo>, List<UserFileInfo>) -> Pair<Boolean, FileChanges> = { currentFiles, oldFiles ->
                val overlappingFiles = currentFiles.filter { currentFile ->
                    oldFiles.any { currentFile.prefixPath == it.prefixPath }
                }

                val newFiles = currentFiles.filter { currentFile ->
                    !oldFiles.any { currentFile.prefixPath == it.prefixPath }
                }

                val deletedFiles = oldFiles.filter { oldFile ->
                    !currentFiles.any { oldFile.prefixPath == it.prefixPath }
                }

                val modifiedFiles = overlappingFiles.filter { file ->
                    oldFiles.first {
                        it.prefixPath == file.prefixPath
                    }.let {
                        Timber.i("Comparing SHA of ${it.prefixPath} and ${file.prefixPath}")
                        Timber.i("[${it.sha.joinToString(", ")}]\n[${file.sha.joinToString(", ")}]")

                        !it.sha.contentEquals(file.sha)
                    }
                }

                val changesExist = newFiles.isNotEmpty() || deletedFiles.isNotEmpty() || modifiedFiles.isNotEmpty()

                Pair(changesExist, FileChanges(deletedFiles, modifiedFiles, newFiles))
            }

            val hasHashConflicts: (Map<String, List<UserFileInfo>>, AppFileChangeList) -> Boolean =
                { localUserFiles, fileList ->
                    fileList.files.any { file ->
                        Timber.i("Checking for " + "${getFilePrefix(file, fileList)} in ${localUserFiles.keys}")

                        localUserFiles[getFilePrefix(file, fileList)]?.let { localUserFile ->
                            localUserFile.firstOrNull {
                                Timber.i("Comparing ${file.filename} and ${it.filename}")

                                it.filename == file.filename
                            }?.let {
                                Timber.i("Comparing SHA of ${getFilePrefixPath(file, fileList)} and ${it.prefixPath}")
                                Timber.i("[${file.shaFile.joinToString(", ")}]\n[${it.sha.joinToString(", ")}]")

                                !file.shaFile.contentEquals(it.sha)
                            }
                        } == true
                    }
                }

            val hasHashConflictsOrRemoteMissingFiles: (Map<String, List<UserFileInfo>>, AppFileChangeList) -> Boolean =
                { localUserFiles, fileList ->
                    localUserFiles.values.any {
                        it.any { localUserFile ->
                            fileList.files.firstOrNull { cloudFile ->
                                val cloudFilePath = getFilePrefixPath(cloudFile, fileList)

                                val localFilePath = Paths.get(
                                    localUserFile.prefix,
                                    localUserFile.filename,
                                ).pathString

                                Timber.i("Comparing $cloudFilePath and $localFilePath")

                                cloudFilePath == localFilePath
                            }?.let {
                                Timber.i("Comparing SHA of ${getFilePrefixPath(it, fileList)} and ${localUserFile.prefixPath}")
                                Timber.i("[${it.shaFile.joinToString(", ")}]\n[${localUserFile.sha.joinToString(", ")}]")

                                it.shaFile.contentEquals(localUserFile.sha)
                            } != true
                        }
                    }
                }

            val getLocalUserFilesAsPrefixMap: () -> Map<String, List<UserFileInfo>> = {
                appInfo.ufs.saveFilePatterns
                    .filter { userFile -> userFile.root.isWindows() }
                    .associate { userFile ->
                        Pair(
                            Paths.get(userFile.prefix).pathString,
                            FileUtils.findFiles(
                                Paths.get(prefixToPath(userFile.root.toString()), userFile.path),
                                userFile.pattern,
                            ).map {
                                val sha = CryptoHelper.shaHash(Files.readAllBytes(it))

                                Timber.i("Found ${it.pathString}\n\tin ${userFile.prefix}\n\twith sha [${sha.joinToString(", ")}]")

                                UserFileInfo(userFile.root, userFile.path, it.name, Files.getLastModifiedTime(it).toMillis(), sha)
                            }.collect(Collectors.toList()),
                        )
                    }
            }

            val fileChangeListToUserFiles: (AppFileChangeList) -> List<UserFileInfo> = { appFileListChange ->
                val pathTypePairs = getPathTypePairs(appFileListChange)

                appFileListChange.files.map {
                    UserFileInfo(
                        root = if (it.pathPrefixIndex < pathTypePairs.size) {
                            PathType.from(pathTypePairs[it.pathPrefixIndex].first)
                        } else {
                            PathType.GameInstall
                        },
                        path = if (it.pathPrefixIndex < pathTypePairs.size) {
                            appFileListChange.pathPrefixes[it.pathPrefixIndex]
                        } else {
                            ""
                        },
                        filename = it.filename,
                        timestamp = it.timestamp.time,
                        sha = it.shaFile,
                    )
                }
            }

            val buildUrl: (Boolean, String, String) -> String = { useHttps, urlHost, urlPath ->
                val scheme = if (useHttps) "https://" else "http://"
                "$scheme${urlHost}$urlPath"
            }

            val prootTimestampToDate: (Long) -> Date = { originalTimestamp ->
                val androidTimestamp = System.currentTimeMillis()
                val prootTimestamp = getProotTime(steamInstance)
                val timeDifference = androidTimestamp - prootTimestamp
                val adjustedTimestamp = originalTimestamp + timeDifference

                Timber.i("Android: $androidTimestamp, PRoot: $prootTimestamp, $originalTimestamp -> $adjustedTimestamp")

                Date(adjustedTimestamp)
            }

            val downloadFiles: (AppFileChangeList, CoroutineScope) -> Deferred<UserFilesDownloadResult> = { fileList, parentScope ->
                parentScope.async {
                    var filesDownloaded = 0
                    var bytesDownloaded = 0L

                    // val convertedPrefixes = convertPrefixes(fileList)

                    fileList.files.forEach { file ->
                        val prefixedPath = getFilePrefixPath(file, fileList)
                        val actualFilePath = getFullFilePath(file, fileList)

                        Timber.i("$prefixedPath -> $actualFilePath")

                        val fileDownloadInfo = steamCloud.clientFileDownload(appInfo.appId, prefixedPath).await()

                        if (fileDownloadInfo.urlHost.isNotEmpty()) {
                            val httpUrl = with(fileDownloadInfo) {
                                buildUrl(useHttps, urlHost, urlPath)
                            }

                            Timber.i("Downloading $httpUrl")

                            val headers = Headers.headersOf(
                                *fileDownloadInfo.requestHeaders
                                    .map { listOf(it.name, it.value) }
                                    .flatten()
                                    .toTypedArray(),
                            )

                            val request = Request.Builder()
                                .url(httpUrl)
                                .headers(headers)
                                .build()

                            val httpClient = steamInstance._steamClient!!.configuration.httpClient

                            withTimeout(requestTimeout) {
                                val response = httpClient.newCall(request).execute()

                                if (!response.isSuccessful) {
                                    Timber.w("File download of $prefixedPath was unsuccessful")
                                    return@withTimeout
                                }

                                val copyToFile: (InputStream) -> Unit = { input ->
                                    Files.createDirectories(actualFilePath.parent)

                                    FileOutputStream(actualFilePath.toString()).use { fs ->
                                        val bytesRead = input.copyTo(fs)

                                        if (bytesRead != fileDownloadInfo.rawFileSize.toLong()) {
                                            Timber.w("Bytes read from stream of $prefixedPath does not match expected size")
                                        }
                                    }
                                }

                                withTimeout(responseBodyTimeout) {
                                    if (fileDownloadInfo.fileSize != fileDownloadInfo.rawFileSize) {
                                        response.body?.byteStream()?.use { inputStream ->
                                            ZipInputStream(inputStream).use { zipInput ->
                                                val entry = zipInput.nextEntry

                                                if (entry == null) {
                                                    Timber.w("Downloaded user file $prefixedPath has no zip entries")
                                                    return@withTimeout
                                                }

                                                copyToFile(zipInput)

                                                if (zipInput.nextEntry != null) {
                                                    Timber.e("Downloaded user file $prefixedPath has more than one zip entry")
                                                }
                                            }
                                        }
                                    } else {
                                        response.body?.byteStream()?.use { inputStream ->
                                            copyToFile(inputStream)
                                        }
                                    }

                                    filesDownloaded++

                                    bytesDownloaded += fileDownloadInfo.fileSize
                                }
                            }
                        } else {
                            Timber.w("URL host of $prefixedPath was empty")
                        }
                    }

                    UserFilesDownloadResult(filesDownloaded, bytesDownloaded)
                }
            }

            val uploadFiles: (FileChanges, CoroutineScope) -> Deferred<UserFilesUploadResult> = { fileChanges, parentScope ->
                parentScope.async {
                    var filesUploaded = 0
                    var bytesUploaded = 0L

                    val filesToDelete = fileChanges.filesDeleted.map { it.prefixPath }

                    val filesToUpload = fileChanges.filesCreated
                        .union(fileChanges.filesModified)
                        .map { Pair(it.prefixPath, it) }

                    Timber.i(
                        "Beginning app upload batch with ${filesToDelete.size} file(s) to delete " +
                            "and ${filesToUpload.size} file(s) to upload",
                    )

                    val uploadBatchResponse = steamCloud.beginAppUploadBatch(
                        appId = appInfo.appId,
                        machineName = SteamUtils.getMachineName(steamInstance),
                        clientId = clientId,
                        filesToDelete = filesToDelete,
                        filesToUpload = filesToUpload.map { it.first },
                        // TODO: have branch be user selected and use that selection here
                        appBuildId = appInfo.branches["public"]?.buildId ?: 0,
                    ).await()

                    var uploadBatchSuccess = true

                    filesToUpload.map { it.second }.forEach { file ->
                        val absFilePath = file.getAbsPath(prefixToPath)

                        val fileSize = Files.size(absFilePath).toInt()

                        Timber.i("Beginning upload of ${file.prefixPath} whose timestamp is ${file.timestamp}")

                        val uploadInfo = steamCloud.beginFileUpload(
                            appId = appInfo.appId,
                            filename = file.prefixPath,
                            fileSize = fileSize,
                            rawFileSize = fileSize,
                            fileSha = file.sha,
                            // timestamp = prootTimestampToDate(file.timestamp),
                            timestamp = Date(file.timestamp),
                            uploadBatchId = uploadBatchResponse.batchID,
                        ).await()

                        var uploadFileSuccess = true

                        RandomAccessFile(absFilePath.pathString, "r").use { fs ->
                            uploadInfo.blockRequests.forEach { blockRequest ->
                                val httpUrl = buildUrl(
                                    blockRequest.useHttps,
                                    blockRequest.urlHost,
                                    blockRequest.urlPath,
                                )

                                Timber.i("Uploading to $httpUrl")
                                @SuppressLint("BinaryOperationInTimber")
                                Timber.i(
                                    "Block Request:" +
                                        "\n\tblockOffset: ${blockRequest.blockOffset}" +
                                        "\n\tblockLength: ${blockRequest.blockLength}" +
                                        "\n\trequestHeaders:\n\t\t${
                                            blockRequest.requestHeaders.joinToString("\n\t\t") { "${it.name}: ${it.value}" }
                                        }" +
                                        "\n\texplicitBodyData: [${
                                            blockRequest.explicitBodyData.joinToString(
                                                ", ",
                                            )
                                        }]" +
                                        "\n\tmayParallelize: ${blockRequest.mayParallelize}",
                                )

                                val byteArray = ByteArray(blockRequest.blockLength)

                                fs.seek(blockRequest.blockOffset)

                                val bytesRead = fs.read(byteArray, 0, blockRequest.blockLength)

                                Timber.i("Read $bytesRead byte(s) for block")

                                val mediaType = "application/octet-stream".toMediaTypeOrNull()

                                val requestBody = byteArray.toRequestBody(mediaType)

                                // val requestBody = byteArray.toRequestBody()

                                val headers = Headers.headersOf(
                                    *blockRequest.requestHeaders
                                        .map { listOf(it.name, it.value) }
                                        .flatten()
                                        .toTypedArray(),
                                )

                                val request = Request.Builder()
                                    .url(httpUrl)
                                    .put(requestBody)
                                    .headers(headers)
                                    .addHeader("Accept", "text/html,*/*;q=0.9")
                                    .addHeader("accept-encoding", "gzip,identity,*;q=0")
                                    .addHeader("accept-charset", "ISO-8859-1,utf-8,*;q=0.7")
                                    .addHeader("user-agent", "Valve/Steam HTTP Client 1.0")
                                    .build()

                                val httpClient = steamInstance._steamClient!!.configuration.httpClient

                                Timber.i("Sending request to ${request.url} using\n$request")

                                withTimeout(requestTimeout) {
                                    val response = httpClient.newCall(request).execute()

                                    if (!response.isSuccessful) {
                                        Timber.w("Failed to upload part of ${file.prefixPath}: ${response.message}, ${response.body?.string()}")

                                        uploadFileSuccess = false
                                        uploadBatchSuccess = false
                                    }
                                }
                            }
                        }

                        if (uploadFileSuccess) {
                            filesUploaded++
                            bytesUploaded += fileSize
                        }

                        val commitSuccess = steamCloud.commitFileUpload(
                            transferSucceeded = uploadFileSuccess,
                            appId = appInfo.appId,
                            fileSha = file.sha,
                            filename = file.prefixPath,
                        ).await()

                        Timber.i("File ${file.prefixPath} commit success: $commitSuccess")
                    }

                    steamCloud.completeAppUploadBatch(
                        appId = appInfo.appId,
                        batchId = uploadBatchResponse.batchID,
                        batchEResult = if (uploadBatchSuccess) EResult.OK else EResult.Fail,
                    ).await()

                    UserFilesUploadResult(uploadBatchSuccess, uploadBatchResponse.appChangeNumber, filesUploaded, bytesUploaded)
                }
            }

            var syncResult = SyncResult.Success
            var remoteTimestamp = 0L
            var localTimestamp = 0L
            var uploadsRequired = false
            var uploadsCompleted = true

            // sync metrics
            var filesUploaded = 0
            var filesDownloaded = 0
            var filesDeleted = 0
            var filesManaged = 0
            var bytesUploaded = 0L
            var bytesDownloaded = 0L
            var microsecTotal = 0L
            var microsecInitCaches = 0L
            var microsecValidateState = 0L
            var microsecAcLaunch = 0L
            var microsecAcPrepUserFiles = 0L
            var microsecAcExit = 0L
            var microsecBuildSyncList = 0L
            var microsecDeleteFiles = 0L
            var microsecDownloadFiles = 0L
            var microsecUploadFiles = 0L

            microsecTotal = measureTime {
                val localAppChangeNumber = runBlocking {
                    with(instance!!) {
                        db.appChangeNumbers().getByAppId(appInfo.appId)?.changeNumber ?: -1
                    }
                }

                val changeNumber = if (localAppChangeNumber >= 0) localAppChangeNumber else 0
                val appFileListChange = steamCloud.getAppFileListChange(appInfo.appId, changeNumber).await()

                val cloudAppChangeNumber = appFileListChange.currentChangeNumber

                Timber.i("AppChangeNumber: $localAppChangeNumber -> $cloudAppChangeNumber")

                printFileChangeList(appFileListChange)

                // retrieve existing user files from local storage
                val localUserFilesMap: Map<String, List<UserFileInfo>>
                val allLocalUserFiles: List<UserFileInfo>
                microsecInitCaches = measureTime {
                    localUserFilesMap = getLocalUserFilesAsPrefixMap()
                    allLocalUserFiles = localUserFilesMap.map { it.value }.flatten()
                }.inWholeMicroseconds

                val downloadUserFiles: (CoroutineScope) -> Deferred<PostSyncInfo?> = { parentScope ->
                    parentScope.async {
                        Timber.i("Downloading cloud user files")

                        val remoteUserFiles = fileChangeListToUserFiles(appFileListChange)
                        val filesDiff = getFilesDiff(remoteUserFiles, allLocalUserFiles).second
                        microsecDeleteFiles = measureTime {
                            var totalFilesDeleted = 0

                            filesDiff.filesDeleted.forEach {
                                val deleted = Files.deleteIfExists(it.getAbsPath(prefixToPath))
                                if (deleted) totalFilesDeleted++
                            }

                            filesDeleted = totalFilesDeleted
                        }.inWholeMicroseconds

                        microsecDownloadFiles = measureTime {
                            val downloadInfo = downloadFiles(appFileListChange, parentScope).await()
                            filesDownloaded = downloadInfo.filesDownloaded
                            bytesDownloaded = downloadInfo.bytesDownloaded
                        }.inWholeMicroseconds

                        val updatedLocalFiles: Map<String, List<UserFileInfo>>
                        val hasLocalChanges: Boolean
                        microsecValidateState = measureTime {
                            updatedLocalFiles = getLocalUserFilesAsPrefixMap()
                            hasLocalChanges = hasHashConflicts(updatedLocalFiles, appFileListChange)
                            filesManaged = updatedLocalFiles.size
                        }.inWholeMicroseconds

                        // var retries = 0

                        // do {
                        //     downloadFiles(appFileListChange, parentScope).await()
                        //     updatedLocalFiles = getLocalUserFilesAsPrefixMap()
                        //     hasLocalChanges =
                        //         hasHashConflicts(updatedLocalFiles, appFileListChange)
                        // } while (hasLocalChanges && retries++ < MAX_USER_FILE_RETRIES)
                        //

                        if (hasLocalChanges) {
                            Timber.e("Failed to download latest user files after $MAX_USER_FILE_RETRIES tries")

                            syncResult = SyncResult.DownloadFail

                            return@async PostSyncInfo(syncResult)
                        }

                        with(instance!!) {
                            db.appFileChangeLists().insert(appInfo.appId, updatedLocalFiles.map { it.value }.flatten())
                            db.appChangeNumbers().insert(appInfo.appId, cloudAppChangeNumber)
                        }

                        return@async null
                    }
                }

                val uploadUserFiles: (CoroutineScope) -> Deferred<Unit> = { parentScope ->
                    parentScope.async {
                        Timber.i("Uploading local user files")

                        val fileChanges = runBlocking {
                            instance!!.db.appFileChangeLists().getByAppId(appInfo.appId)!!.let {
                                val result = getFilesDiff(allLocalUserFiles, it.userFileInfo)

                                result.second
                            }
                        }

                        uploadsRequired = fileChanges.filesCreated.isNotEmpty() || fileChanges.filesModified.isNotEmpty()

                        val uploadResult: UserFilesUploadResult

                        microsecUploadFiles = measureTime {
                            uploadResult = uploadFiles(fileChanges, parentScope).await()
                            filesUploaded = uploadResult.filesUploaded
                            bytesUploaded = uploadResult.bytesUploaded
                            uploadsCompleted = uploadsRequired && uploadResult.uploadBatchSuccess
                        }.inWholeMicroseconds

                        filesManaged = allLocalUserFiles.size

                        if (uploadResult.uploadBatchSuccess) {
                            with(instance!!) {
                                db.appFileChangeLists().insert(appInfo.appId, allLocalUserFiles)
                                db.appChangeNumbers().insert(appInfo.appId, uploadResult.appChangeNumber)
                            }
                        } else {
                            syncResult = SyncResult.UpdateFail
                        }
                    }
                }

                if (localAppChangeNumber < cloudAppChangeNumber) {
                    // our change number is less than the expected, meaning we are behind and
                    // need to download the new user files, but first we should check that
                    // the local user files are not conflicting with their respective change
                    // number or else that would mean that the user made changes locally and
                    // on a separate device and they must choose between the two
                    microsecAcLaunch = measureTime {
                        var hasLocalChanges: Boolean

                        microsecAcPrepUserFiles = measureTime {
                            hasLocalChanges = runBlocking {
                                with(instance!!) {
                                    db.appFileChangeLists().getByAppId(appInfo.appId)?.let { it ->
                                        getFilesDiff(allLocalUserFiles, it.userFileInfo).first
                                    } == true
                                }
                            }
                        }.inWholeMicroseconds

                        if (!hasLocalChanges) {
                            // we can safely download the new changes since no changes have been
                            // made locally

                            Timber.i("No local changes but new cloud user files")

                            downloadUserFiles(parentScope).await()?.let {
                                return@async it
                            }
                        } else {
                            Timber.i("Found local changes and new cloud user files, conflict resolution...")

                            when (preferredSave) {
                                SaveLocation.Local -> {
                                    // overwrite remote save with the local one
                                    uploadUserFiles(parentScope).await()
                                }

                                SaveLocation.Remote -> {
                                    // overwrite local save with the remote one
                                    downloadUserFiles(parentScope).await()?.let {
                                        return@async it
                                    }
                                }

                                SaveLocation.None -> {
                                    syncResult = SyncResult.Conflict
                                    remoteTimestamp = appFileListChange.files.map { it.timestamp.time }.max()
                                    localTimestamp = allLocalUserFiles.map { it.timestamp }.max()
                                }
                            }
                        }
                    }.inWholeMicroseconds
                } else if (localAppChangeNumber == cloudAppChangeNumber) {
                    // our app change numbers are the same so the file hashes should match
                    // if they do not then that means we have new user files locally that
                    // need uploading
                    microsecAcExit = measureTime {
                        var fileChanges: FileChanges? = null

                        val hasLocalChanges = runBlocking {
                            instance!!.db.appFileChangeLists().getByAppId(appInfo.appId)
                                ?.let {
                                    val result = getFilesDiff(allLocalUserFiles, it.userFileInfo)
                                    fileChanges = result.second
                                    result.first
                                } == true
                        }

                        if (hasLocalChanges) {
                            Timber.i("Found local changes and no new cloud user files")

                            uploadUserFiles(parentScope).await()
                        } else {
                            Timber.i("No local changes and no new cloud user files, doing nothing...")

                            syncResult = SyncResult.UpToDate
                        }
                    }.inWholeMicroseconds
                } else {
                    // our last scenario is if the change number we have is greater than
                    // the change number from the cloud. This scenario should not happen, I
                    // believe, since we get the new app change number after having downloaded
                    // or uploaded from/to the cloud, so we should always be either behind or
                    // on par with the cloud change number, never ahead
                    Timber.e("Local change number greater than cloud $localAppChangeNumber > $cloudAppChangeNumber")

                    syncResult = SyncResult.UnknownFail
                }
            }.inWholeMicroseconds

            postSyncInfo = PostSyncInfo(
                syncResult = syncResult,
                remoteTimestamp = remoteTimestamp,
                localTimestamp = localTimestamp,
                uploadsRequired = uploadsRequired,
                uploadsCompleted = uploadsCompleted,
                filesUploaded = filesUploaded,
                filesDownloaded = filesDownloaded,
                filesDeleted = filesDeleted,
                filesManaged = filesManaged,
                bytesUploaded = bytesUploaded,
                bytesDownloaded = bytesDownloaded,
                microsecTotal = microsecTotal,
                microsecInitCaches = microsecInitCaches,
                microsecValidateState = microsecValidateState,
                microsecAcLaunch = microsecAcLaunch,
                microsecAcPrepUserFiles = microsecAcPrepUserFiles,
                microsecAcExit = microsecAcExit,
                // microsecBuildSyncList = microsecBuildSyncList,
                microsecDeleteFiles = microsecDeleteFiles,
                microsecDownloadFiles = microsecDownloadFiles,
                microsecUploadFiles = microsecUploadFiles,
            )

            postSyncInfo
        }

        fun getAvatarURL(avatarHash: String): String {
            return avatarHash.ifEmpty { null }
                ?.takeIf { str -> str.isNotEmpty() && !str.all { it == '0' } }
                ?.let { "${AVATAR_BASE_URL}${it.substring(0, 2)}/${it}_full.jpg" }
                ?: MISSING_AVATAR_URL
        }

        // fun printAllKeyValues(parent: KeyValue, depth: Int = 0) {
        //     var tabString = ""
        //
        //     for (i in 0..depth) {
        //         tabString += "\t"
        //     }
        //
        //     if (parent.children.isNotEmpty()) {
        //         Timber.i("$tabString${parent.name}")
        //
        //         for (child in parent.children) {
        //             printAllKeyValues(child, depth + 1)
        //         }
        //     } else {
        //         Timber.i("$tabString${parent.name}: ${parent.value}")
        //     }
        // }

        private fun login(
            username: String,
            accessToken: String? = null,
            refreshToken: String? = null,
            password: String? = null,
            shouldRememberPassword: Boolean = false,
            twoFactorAuth: String? = null,
            emailAuth: String? = null,
            clientId: Long? = null,
        ) {
            val steamUser = instance!!._steamUser!!

            // Sensitive info, only print in DEBUG build.
            if (BuildConfig.DEBUG) {
                Timber.d(
                    "Login Information\n\tUsername: " +
                        "$username\n\tAccessToken: " +
                        "$accessToken\n\tRefreshToken: " +
                        "$refreshToken\n\tPassword: " +
                        "$password\n\tShouldRememberPass: " +
                        "$shouldRememberPassword\n\tTwoFactorAuth: " +
                        "$twoFactorAuth\n\tEmailAuth: $emailAuth",
                )
            }

            PrefManager.username = username

            if ((password != null && shouldRememberPassword) || refreshToken != null) {
                if (password != null) {
                    PrefManager.password = password
                }

                if (accessToken != null) {
                    PrefManager.password = ""
                    PrefManager.accessToken = accessToken
                }

                if (refreshToken != null) {
                    PrefManager.password = ""
                    PrefManager.refreshToken = refreshToken
                }

                if (clientId != null) {
                    PrefManager.clientId = clientId
                }
            }

            isLoggingIn = true

            PluviaApp.events.emit(SteamEvent.LogonStarted(username))

            steamUser.logOn(
                LogOnDetails(
                    // Steam strips all non-ASCII characters from usernames and passwords
                    // source: https://github.com/steevp/UpdogFarmer/blob/8f2d185c7260bc2d2c92d66b81f565188f2c1a0e/app/src/main/java/com/steevsapps/idledaddy/LoginActivity.java#L166C9-L168C104
                    // more: https://github.com/winauth/winauth/issues/368#issuecomment-224631002
                    username = SteamUtils.removeSpecialChars(username).trim(),
                    password = if (password != null) {
                        SteamUtils.removeSpecialChars(password)
                            .trim()
                    } else {
                        null
                    },
                    shouldRememberPassword = shouldRememberPassword,
                    twoFactorCode = twoFactorAuth,
                    authCode = emailAuth,
                    accessToken = refreshToken,
                    // Set LoginID to a non-zero value if you have another client connected using the same account,
                    // the same private ip, and same public ip.
                    // source: https://github.com/Longi94/JavaSteam/blob/08690d0aab254b44b0072ed8a4db2f86d757109b/javasteam-samples/src/main/java/in/dragonbra/javasteamsamples/_000_authentication/SampleLogonAuthentication.java#L146C13-L147C56
                    loginID = SteamUtils.getUniqueDeviceId(instance!!),
                    machineName = SteamUtils.getMachineName(instance!!),
                ),
            )
        }

        fun startLoginWithCredentials(
            username: String,
            password: String,
            shouldRememberPassword: Boolean,
            authenticator: IAuthenticator,
        ) {
            Timber.i("Logging in via credentials.")

            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                supervisorScope {
                    try {
                        val steamClient = instance!!._steamClient
                        if (steamClient == null) {
                            Timber.e("Could not logon: Failed to connect to Steam")
                            PluviaApp.events.emit(SteamEvent.LogonEnded(username, LoginResult.Failed))
                            return@supervisorScope
                        }

                        val authDetails = AuthSessionDetails().apply {
                            this.username = username.trim()
                            this.password = password.trim()
                            this.persistentSession = shouldRememberPassword
                            this.authenticator = authenticator
                            this.deviceFriendlyName = SteamUtils.getMachineName(instance!!)
                        }

                        val authSession = steamClient.authentication.beginAuthSessionViaCredentials(authDetails, this).await()
                        PluviaApp.events.emit(SteamEvent.LogonStarted(username))

                        val pollResult = authSession.pollingWaitForResult().await()

                        if (pollResult.accountName.isNotEmpty() && pollResult.refreshToken.isNotEmpty()) {
                            login(
                                clientId = authSession.clientID,
                                username = pollResult.accountName,
                                accessToken = pollResult.accessToken,
                                refreshToken = pollResult.refreshToken,
                                shouldRememberPassword = shouldRememberPassword,
                            )
                        }
                    } catch (e: Exception) {
                        val authException = when {
                            e is AuthenticationException -> e
                            e.cause is AuthenticationException -> e.cause as AuthenticationException
                            else -> null
                        }

                        if (authException != null) {
                            Timber.w(authException, "Authentication failed with result ${authException.result}")
                            PluviaApp.events.emit(SteamEvent.LogonEnded(username, LoginResult.Failed, authException.result!!.name))
                        } else {
                            Timber.e(e, "Login failed")
                            val msg = "Login failed: ${e.message ?: e.javaClass.name}"
                            PluviaApp.events.emit(
                                SteamEvent.LogonEnded(
                                    username = username,
                                    loginResult = LoginResult.Failed,
                                    failMessage = msg,
                                ),
                            )
                        }

                        return@supervisorScope
                    }
                }
            }
        }

        fun startLoginWithQr() {
            Timber.i("Logging in via QR.")

            CoroutineScope(Dispatchers.IO).launch {
                val steamClient = instance!!._steamClient

                if (steamClient != null) {
                    isWaitingForQRAuth = true

                    val authDetails = AuthSessionDetails().apply {
                        deviceFriendlyName = SteamUtils.getMachineName(instance!!)
                    }

                    val authSession = steamClient.authentication.beginAuthSessionViaQR(authDetails, this).await()

                    // Steam will periodically refresh the challenge url, this callback allows you to draw a new qr code.
                    authSession.challengeUrlChanged = instance

                    PluviaApp.events.emit(SteamEvent.QrChallengeReceived(authSession.challengeUrl))

                    Timber.d("PollingInterval: ${authSession.pollingInterval.toLong()}")

                    var authPollResult: AuthPollResult? = null

                    while (isWaitingForQRAuth && authPollResult == null) {
                        try {
                            authPollResult = authSession.pollAuthSessionStatus(this).await()
                        } catch (e: Exception) {
                            Timber.e("Poll auth session status error: $e")

                            break
                        }

                        // Sensitive info, only print in DEBUG build.
                        if (BuildConfig.DEBUG && authPollResult != null) {
                            Timber.d(
                                "AccessToken: %s\nAccountName: %s\nRefreshToken: %s\nNewGuardData: %s",
                                authPollResult.accessToken,
                                authPollResult.accountName,
                                authPollResult.refreshToken,
                                authPollResult.newGuardData ?: "No new guard data",
                            )
                        } else {
                            // logD("AuthPollResult is null")
                        }

                        delay(authSession.pollingInterval.toLong())
                    }

                    isWaitingForQRAuth = false

                    PluviaApp.events.emit(SteamEvent.QrAuthEnded(authPollResult != null))

                    // there is a chance qr got cancelled and there is no authPollResult
                    if (authPollResult != null) {
                        login(
                            clientId = authSession.clientID,
                            username = authPollResult.accountName,
                            accessToken = authPollResult.accessToken,
                            refreshToken = authPollResult.refreshToken,
                        )
                    }
                } else {
                    Timber.e("Could not start QR logon: Failed to connect to Steam")

                    PluviaApp.events.emit(SteamEvent.QrAuthEnded(false))
                }
            }
        }

        fun stopLoginWithQr() {
            Timber.i("Stopping QR polling")

            isWaitingForQRAuth = false
        }

        fun stop() {
            instance?.let { steamInstance ->
                CoroutineScope(Dispatchers.IO).launch {
                    steamInstance.stop()
                }
            }
        }

        fun logOut() {
            CoroutineScope(Dispatchers.Default).launch {
                // isConnected = false

                isLoggingOut = true

                performLogOffDuties()

                val steamUser = instance!!._steamUser!!
                steamUser.logOff()
            }
        }

        private fun clearUserData() {
            PrefManager.clearPreferences()

            CoroutineScope(Dispatchers.IO).launch {
                instance?.db?.appChangeNumbers()?.deleteAll()
                instance?.db?.appFileChangeLists()?.deleteAll()
            }

            isLoggingIn = false
        }

        private fun performLogOffDuties() {
            val username = PrefManager.username

            clearUserData()

            PluviaApp.events.emit(SteamEvent.LoggedOut(username))
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        notificationHelper = NotificationHelper(applicationContext)

        // To view log messages in android logcat properly
        val logger = object : LogListener {
            override fun onLog(clazz: Class<*>, message: String, throwable: Throwable?) {
                Timber.i(throwable, "[${clazz.simpleName}] -> $message")
            }

            override fun onError(clazz: Class<*>, message: String, throwable: Throwable?) {
                Timber.e(throwable, "[${clazz.simpleName}] -> $message")
            }
        }
        LogManager.addListener(logger)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Notification intents
        when (intent?.action) {
            NotificationHelper.ACTION_EXIT -> {
                PluviaApp.events.emit(AndroidEvent.EndProcess)
                return START_NOT_STICKY
            }
        }

        if (!isRunning) {
            Timber.i("Using server list path: $serverListPath")

            val configuration = SteamConfiguration.create {
                it.withProtocolTypes(PROTOCOL_TYPES)
                it.withCellID(PrefManager.cellId)
                it.withServerListProvider(FileServerListProvider(File(serverListPath)))
                it.withManifestProvider(FileManifestProvider(File(depotManifestsPath)))
            }

            // create our steam client instance
            _steamClient = SteamClient(configuration)

            // remove callbacks we're not using.
            _steamClient!!.removeHandler(SteamGameServer::class.java)
            _steamClient!!.removeHandler(SteamMasterServer::class.java)
            _steamClient!!.removeHandler(SteamWorkshop::class.java)
            _steamClient!!.removeHandler(SteamScreenshots::class.java)
            _steamClient!!.removeHandler(SteamUserStats::class.java)

            // create the callback manager which will route callbacks to function calls
            _callbackManager = CallbackManager(_steamClient!!)
            _unifiedMessages = _steamClient!!.getHandler<SteamUnifiedMessages>()

            // get the different handlers to be used throughout the service
            _steamUser = _steamClient!!.getHandler(SteamUser::class.java)
            _steamApps = _steamClient!!.getHandler(SteamApps::class.java)
            _steamFriends = _steamClient!!.getHandler(SteamFriends::class.java)
            _steamCloud = _steamClient!!.getHandler(SteamCloud::class.java)

            // subscribe to the callbacks we are interested in
            with(_callbackSubscriptions) {
                with(_callbackManager!!) {
                    add(subscribe(ConnectedCallback::class.java, ::onConnected))
                    add(subscribe(DisconnectedCallback::class.java, ::onDisconnected))
                    add(subscribe(LoggedOnCallback::class.java, ::onLoggedOn))
                    add(subscribe(LoggedOffCallback::class.java, ::onLoggedOff))
                    add(subscribe(PersonaStatesCallback::class.java, ::onPersonaStateReceived))
                    add(subscribe(LicenseListCallback::class.java, ::onLicenseList))
                    add(subscribe(PICSProductInfoCallback::class.java, ::onPICSProductInfo))
                    add(subscribe(NicknameListCallback::class.java, ::onNicknameList))
                    add(subscribe(FriendsListCallback::class.java, ::onFriendsList))
                }
            }

            isRunning = true

            // we should use Dispatchers.IO here since we are running a sleeping/blocking function
            // "The idea is that the IO dispatcher spends a lot of time waiting (IO blocked),
            // while the Default dispatcher is intended for CPU intensive tasks, where there
            // is little or no sleep."
            // source: https://stackoverflow.com/a/59040920
            CoroutineScope(Dispatchers.IO).launch {
                while (isRunning) {
                    // logD("runWaitCallbacks")

                    try {
                        _callbackManager!!.runWaitCallbacks(1000L)
                    } catch (e: Exception) {
                        Timber.e("runWaitCallbacks failed: $e")
                    }
                }
            }

            connectToSteam()
        }

        val notification = notificationHelper.createForegroundNotification("Starting up...")
        startForeground(1, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationHelper.cancel()

        CoroutineScope(Dispatchers.IO).launch {
            stop()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectToSteam() {
        isConnecting = true

        CoroutineScope(Dispatchers.Default).launch {
            // this call errors out if run on the main thread
            _steamClient!!.connect()

            delay(5000)

            if (!isConnected) {
                Timber.w("Failed to connect to Steam, marking endpoint bad and force disconnecting")

                try {
                    _steamClient!!.servers.tryMark(_steamClient!!.currentEndpoint, PROTOCOL_TYPES, ServerQuality.BAD)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to mark endpoint as bad:")
                }

                try {
                    _steamClient!!.disconnect()
                } catch (e: Exception) {
                    Timber.e(e, "There was an issue when disconnecting:")
                }
            }
        }
    }

    private suspend fun stop() {
        Timber.i("Stopping Steam service")
        if (_steamClient != null && _steamClient!!.isConnected) {
            isStopping = true

            _steamClient!!.disconnect()

            while (isStopping) {
                delay(200L)
            }

            // the reason we don't clearValues() here is because the onDisconnect
            // callback does it for us
        } else {
            clearValues()
        }
    }

    private fun clearValues() {
        _loginResult = LoginResult.Failed
        isRunning = false
        isConnected = false
        isConnecting = false
        isLoggingIn = false
        isLoggingOut = false
        isWaitingForQRAuth = false
        isReceivingLicenseList = false
        isRequestingPkgInfo = false
        isRequestingAppInfo = false

        PrefManager.appInstallPath = defaultAppInstallPath
        PrefManager.appStagingPath = defaultAppStagingPath

        _steamClient = null
        _steamUser = null
        _steamApps = null
        _steamFriends = null
        _steamCloud = null

        for (subscription in _callbackSubscriptions) {
            subscription.close()
        }

        _callbackSubscriptions.clear()
        _callbackManager = null

        _unifiedMessages = null
        _unifiedChat = null

        packageInfo.clear()
        appInfo.clear()

        isStopping = false
        retryAttempt = 0

        PluviaApp.events.clearAllListenersOf<SteamEvent<Any>>()
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    private fun onConnected(callback: ConnectedCallback) {
        Timber.i("Connected to Steam")

        retryAttempt = 0
        isConnecting = false
        isConnected = true

        var isAutoLoggingIn = false

        if (PrefManager.username.isNotEmpty() && (PrefManager.refreshToken.isNotEmpty() || PrefManager.password.isNotEmpty())) {
            isAutoLoggingIn = true

            login(
                username = PrefManager.username,
                refreshToken = PrefManager.refreshToken,
                password = PrefManager.password.ifEmpty { null },
                shouldRememberPassword = PrefManager.password.isNotEmpty(),
            )
        }

        PluviaApp.events.emit(SteamEvent.Connected(isAutoLoggingIn))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onDisconnected(callback: DisconnectedCallback) {
        Timber.i("Disconnected from Steam. User initiated: ${callback.isUserInitiated}")

        isConnected = false

        if (!isStopping && retryAttempt < MAX_RETRY_ATTEMPTS) {
            retryAttempt++

            Timber.w("Attempting to reconnect (retry $retryAttempt)")

            // isLoggingOut = false

            connectToSteam()
        } else {
            PluviaApp.events.emit(SteamEvent.Disconnected)

            clearValues()

            stopSelf()
        }
    }

    private fun reconnect() {
        notificationHelper.notify("Retrying...")

        isConnected = false
        isConnecting = true

        PluviaApp.events.emit(SteamEvent.Disconnected)

        _steamClient!!.disconnect()
    }

    /**
     * Request a fresh state of Friend's PersonaStates
     */
    private fun refreshPersonaStates() {
        val request = CChat_RequestFriendPersonaStates_Request.newBuilder().build()
        _unifiedChat?.requestFriendPersonaStates(request)
    }

    private fun onLoggedOn(callback: LoggedOnCallback) {
        Timber.i("Logged onto Steam: ${callback.result}")

        when (callback.result) {
            EResult.TryAnotherCM -> {
                reconnect()
            }

            EResult.OK -> {
                // save the current cellid somewhere. if we lose our saved server list, we can use this when retrieving
                // servers from the Steam Directory.
                PrefManager.cellId = callback.cellID

                // Create Unified Handlers
                _unifiedChat = _unifiedMessages!!.createService(Chat::class.java)

                // retrieve persona data of logged in user
                requestUserPersona()

                // since we automatically receive the license list from steam on log on
                isReceivingLicenseList = true

                // Tell steam we're online, this allows friends to update.
                _steamFriends?.setPersonaState(PrefManager.personaState)

                notificationHelper.notify("Connected")

                _loginResult = LoginResult.Success
            }

            else -> {
                clearUserData()

                _loginResult = LoginResult.Failed

                reconnect()
            }
        }

        PluviaApp.events.emit(SteamEvent.LogonEnded(PrefManager.username, _loginResult))

        isLoggingIn = false
    }

    private fun onLoggedOff(callback: LoggedOffCallback) {
        Timber.i("Logged off of Steam: ${callback.result}")

        notificationHelper.notify("Disconnected...")

        if (isLoggingOut || callback.result == EResult.LogonSessionReplaced) {
            performLogOffDuties()

            CoroutineScope(Dispatchers.IO).launch {
                stop()
            }
        } else if (callback.result == EResult.LoggedInElsewhere) {
            // received when a client runs an app and wants to forcibly close another
            // client running an app
            PluviaApp.events.emit(SteamEvent.ForceCloseApp)

            reconnect()
        } else {
            reconnect()
        }
    }

    override fun onChanged(qrAuthSession: QrAuthSession?) {
        Timber.i("QR code changed -> ${if (BuildConfig.DEBUG) qrAuthSession?.challengeUrl else "[redacted]"}")

        if (qrAuthSession != null) {
            PluviaApp.events.emit(SteamEvent.QrChallengeReceived(qrAuthSession.challengeUrl))
        }
    }

    private fun onNicknameList(callback: NicknameListCallback) {
        Timber.d("Nickname list called: ${callback.nicknames.size}")
        dbScope.launch {
            db.withTransaction {
                db.steamFriendDao().clearAllNicknames()
                db.steamFriendDao().updateNicknames(callback.nicknames)
            }
        }
    }

    private fun onFriendsList(callback: FriendsListCallback) {
        Timber.d("onFriendsList ${callback.friendList.size}")
        dbScope.launch {
            db.withTransaction {
                callback.friendList
                    .filter { friend ->
                        friend.steamID.isIndividualAccount
                    }
                    .forEach { filteredFriend ->
                        val friendId = filteredFriend.steamID.convertToUInt64()
                        val friend = db.steamFriendDao().findFriend(friendId).first()

                        if (friend == null) {
                            // Not in the DB, create them.
                            val friendToAdd = SteamFriend(
                                id = filteredFriend.steamID.convertToUInt64(),
                                relation = filteredFriend.relationship,
                            )

                            db.steamFriendDao().insert(friendToAdd)
                        } else {
                            // In the DB, update them.
                            db.steamFriendDao().update(
                                friend.copy(relation = filteredFriend.relationship),
                            )
                        }
                    }

                // Add logged in account if we don't exist yet.
                val selfId = userSteamId!!.convertToUInt64()
                val self = db.steamFriendDao().findFriend(selfId).first()

                if (self == null) {
                    db.steamFriendDao().insert(SteamFriend(id = selfId))
                }
            }

            // NOTE: Our UI could load too quickly on fresh database, our icon will be "?"
            //  unless relaunched or we nav to a new screen.
            refreshPersonaStates()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun onPersonaStateReceived(callback: PersonaStatesCallback) {
        // Ignore accounts that arent individuals
        if (callback.friendID.isIndividualAccount.not()) {
            return
        }

        // Ignore states where the name is blank.
        if (callback.name.isEmpty()) {
            return
        }

        // Timber.d("Persona state received: ${callback.name}")

        dbScope.launch {
            db.withTransaction {
                val id = callback.friendID.convertToUInt64()
                val friend = db.steamFriendDao().findFriend(id).first()

                if (friend == null) {
                    Timber.w("onPersonaStateReceived: failed to find friend to update: $id")
                    return@withTransaction
                }

                db.steamFriendDao().update(
                    friend.copy(
                        statusFlags = callback.statusFlags,
                        state = callback.state,
                        stateFlags = callback.stateFlags,
                        gameAppID = callback.gameAppID,
                        gameID = callback.gameID,
                        gameName = callback.gameName,
                        gameServerIP = NetHelpers.getIPAddress(callback.gameServerIP),
                        gameServerPort = callback.gameServerPort,
                        queryPort = callback.queryPort,
                        sourceSteamID = callback.sourceSteamID,
                        gameDataBlob = callback.gameDataBlob.decodeToString(),
                        name = callback.name,
                        avatarHash = callback.avatarHash.toHexString(),
                        lastLogOff = callback.lastLogOff,
                        lastLogOn = callback.lastLogOn,
                        clanRank = callback.clanRank,
                        clanTag = callback.clanTag,
                        onlineSessionInstances = callback.onlineSessionInstances,
                    ),
                )
            }
        }

        // Send off an event if we change states.
        if (callback.friendID == _steamClient!!.steamID) {
            Timber.d("Emitting PersonaStateReceived")

            dbScope.launch {
                val id = callback.friendID.convertToUInt64()
                val friend = db.steamFriendDao().findFriend(id).first()

                PluviaApp.events.emit(SteamEvent.PersonaStateReceived(friend))
            }
        }
    }

    private fun onLicenseList(callback: LicenseListCallback) {
        Timber.i("Received License List ${callback.result}")

        if (callback.result == EResult.OK) {
            for (i in callback.licenseList.indices) {
                val license = callback.licenseList[i]

                packageInfo[license.packageID] = PackageInfo(
                    packageId = license.packageID,
                    receiveIndex = i,
                    ownerAccountId = license.ownerAccountID,
                    lastChangeNumber = license.lastChangeNumber,
                    accessToken = license.accessToken,
                    territoryCode = license.territoryCode,
                    licenseFlags = license.licenseFlags,
                    licenseType = license.licenseType,
                    paymentMethod = license.paymentMethod,
                    purchaseCountryCode = license.purchaseCode,
                    appIds = IntArray(0),
                    depotIds = IntArray(0),
                )
            }

            isRequestingPkgInfo = true

            _steamApps!!.picsGetProductInfo(
                apps = emptyList(),
                packages = callback.licenseList.map { PICSRequest(it.packageID, it.accessToken) },
            )
        }

        isReceivingLicenseList = false
    }

    private fun onPICSProductInfo(callback: PICSProductInfoCallback) {
        // logD("Received PICSProductInfo")

        if (callback.packages.isNotEmpty()) {
            for (pkg in callback.packages.values) {
                // logD("Received pkg ${pkg.id}")

                packageInfo[pkg.id]?.let { pi ->
                    pi.appIds = pkg.keyValues["appids"].children.map { it.asInteger() }.toIntArray()
                    pi.depotIds = pkg.keyValues["depotids"].children.map { it.asInteger() }.toIntArray()
                }
            }

            isRequestingPkgInfo = false
            isRequestingAppInfo = true

            _steamApps?.picsGetProductInfo(
                apps = packageInfo.values
                    .flatMap { it.appIds.asIterable() }
                    .map { PICSRequest(it) },
                packages = emptyList(),
            )
        }

        if (callback.apps.isNotEmpty()) {
            val apps = callback.apps.values.toTypedArray()

            for (i in apps.indices) {
                val app = apps[i]

                val pkg = packageInfo.values.firstOrNull { it.appIds.contains(app.id) }

                // logD("Received app ${app.id}")

                val generateManifest: (List<KeyValue>) -> Map<String, ManifestInfo> = {
                    val output = mutableMapOf<String, ManifestInfo>()

                    for (manifest in it) {
                        output[manifest.name] = ManifestInfo(
                            name = manifest.name,
                            gid = manifest["gid"].asLong(),
                            size = manifest["size"].asLong(),
                            download = manifest["download"].asLong(),
                        )
                    }

                    output
                }

                val toLangImgMap: (List<KeyValue>) -> Map<Language, String> = { keyValues ->
                    keyValues
                        .map {
                            val language: Language = try {
                                Language.valueOf(it.name)
                            } catch (_: Exception) {
                                Timber.w("Language ${it.name} does not exist in enum")
                                Language.unknown
                            }

                            Pair(language, it.value)
                        }
                        .filter { it.first != Language.unknown }
                        .toMap()
                }

                val launchConfigs = app.keyValues["config"]["launch"].children

                appInfo[app.id] = AppInfo(
                    appId = app.id,
                    receiveIndex = packageInfo.values
                        .filter { it.receiveIndex < (pkg?.receiveIndex ?: Int.MAX_VALUE) }
                        .fold(initial = 0) { accum, pkgInfo -> accum + pkgInfo.appIds.size } + i,
                    packageId = pkg?.packageId ?: INVALID_PKG_ID,
                    depots = app.keyValues["depots"].children
                        .filter { currentDepot ->
                            currentDepot.name.toIntOrNull() != null
                        }
                        .associate { currentDepot ->
                            val depotId = currentDepot.name.toInt()

                            // val currentDepot = app.keyValues["depots"]["$depotId"]

                            val manifests = generateManifest(currentDepot["manifests"].children)

                            val encryptedManifests = generateManifest(
                                currentDepot["encryptedManifests"].children,
                            )

                            Pair(
                                depotId,
                                DepotInfo(
                                    depotId = depotId,
                                    dlcAppId = currentDepot["dlcappid"].asInteger(INVALID_APP_ID),
                                    depotFromApp = currentDepot["depotfromapp"].asInteger(
                                        INVALID_APP_ID,
                                    ),
                                    sharedInstall = currentDepot["sharedinstall"].asBoolean(),
                                    osList = OS.from(currentDepot["config"]["oslist"].value),
                                    osArch = OSArch.from(currentDepot["config"]["osarch"].value),
                                    manifests = manifests,
                                    encryptedManifests = encryptedManifests,
                                ),
                            )
                        },
                    branches = app.keyValues["depots"]["branches"].children.associate {
                        Pair(
                            it.name,
                            BranchInfo(
                                name = it.name,
                                buildId = it["buildid"].asLong(),
                                pwdRequired = it["pwdrequired"].asBoolean(),
                                timeUpdated = Date(it["timeupdated"].asLong() * 1000L),
                            ),
                        )
                    },
                    name = app.keyValues["common"]["name"].value.orEmpty(),
                    type = AppType.valueOf(
                        app.keyValues["common"]["type"].value?.lowercase() ?: "invalid",
                    ),
                    osList = OS.from(app.keyValues["common"]["oslist"].value),
                    releaseState = ReleaseState.valueOf(
                        app.keyValues["common"]["releasestate"].value ?: "released",
                    ),
                    metacriticScore = app.keyValues["common"]["metacritic_score"].asByte(),
                    metacriticFullUrl = app.keyValues["common"]["metacritic_fullurl"].value.orEmpty(),
                    logoHash = app.keyValues["common"]["logo"].value.orEmpty(),
                    logoSmallHash = app.keyValues["common"]["logo_small"].value.orEmpty(),
                    iconHash = app.keyValues["common"]["icon"].value.orEmpty(),
                    clientIconHash = app.keyValues["common"]["clienticon"].value.orEmpty(),
                    clientTgaHash = app.keyValues["common"]["clienttga"].value.orEmpty(),
                    smallCapsule = toLangImgMap(app.keyValues["common"]["small_capsule"].children),
                    headerImage = toLangImgMap(app.keyValues["common"]["header_image"].children),
                    libraryAssets = LibraryAssetsInfo(
                        libraryCapsule = LibraryCapsuleInfo(
                            image = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_capsule"]["image"].children),
                            image2x = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_capsule"]["image2x"].children),
                        ),
                        libraryHero = LibraryHeroInfo(
                            image = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_hero"]["image"].children),
                            image2x = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_hero"]["image2x"].children),
                        ),
                        libraryLogo = LibraryLogoInfo(
                            image = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_logo"]["image"].children),
                            image2x = toLangImgMap(app.keyValues["common"]["library_assets_full"]["library_logo"]["image2x"].children),
                        ),
                    ),
                    primaryGenre = app.keyValues["common"]["primary_genre"].asBoolean(),
                    reviewScore = app.keyValues["common"]["review_score"].asByte(),
                    reviewPercentage = app.keyValues["common"]["review_percentage"].asByte(),
                    controllerSupport = ControllerSupport.valueOf(
                        app.keyValues["common"]["controller_support"].value ?: "none",
                    ),
                    demoOfAppId = app.keyValues["common"]["extended"]["demoofappid"].asInteger(),
                    developer = app.keyValues["common"]["extended"]["developer"].value.orEmpty(),
                    publisher = app.keyValues["common"]["extended"]["publisher"].value.orEmpty(),
                    homepageUrl = app.keyValues["common"]["extended"]["homepage"].value.orEmpty(),
                    gameManualUrl = app.keyValues["common"]["extended"]["gamemanualurl"].value.orEmpty(),
                    loadAllBeforeLaunch = app.keyValues["common"]["extended"]["loadallbeforelaunch"].asBoolean(),
                    // dlcAppIds = (app.keyValues["common"]["extended"]["listofdlc"].value).Split(",").Select(uint.Parse).ToArray(),
                    dlcAppIds = IntArray(0),
                    isFreeApp = app.keyValues["common"]["extended"]["isfreeapp"].asBoolean(),
                    dlcForAppId = app.keyValues["common"]["extended"]["dlcforappid"].asInteger(),
                    mustOwnAppToPurchase = app.keyValues["common"]["extended"]["mustownapptopurchase"].asInteger(),
                    dlcAvailableOnStore = app.keyValues["common"]["extended"]["dlcavailableonstore"].asBoolean(),
                    optionalDlc = app.keyValues["common"]["extended"]["optionaldlc"].asBoolean(),
                    gameDir = app.keyValues["common"]["extended"]["gamedir"].value.orEmpty(),
                    installScript = app.keyValues["common"]["extended"]["installscript"].value.orEmpty(),
                    noServers = app.keyValues["common"]["extended"]["noservers"].asBoolean(),
                    order = app.keyValues["common"]["extended"]["order"].asBoolean(),
                    primaryCache = app.keyValues["common"]["extended"]["primarycache"].asInteger(),
                    // validOSList = app.keyValues["common"]["extended"]["validoslist"].value!.Split(",").Select(Enum.Parse<OS>).Aggregate((os1, os2) => os1 | os2),
                    validOSList = EnumSet.of(OS.none),
                    thirdPartyCdKey = app.keyValues["common"]["extended"]["thirdpartycdkey"].asBoolean(),
                    visibleOnlyWhenInstalled = app.keyValues["common"]["extended"]["visibleonlywheninstalled"].asBoolean(),
                    visibleOnlyWhenSubscribed = app.keyValues["common"]["extended"]["visibleonlywhensubscribed"].asBoolean(),
                    launchEulaUrl = app.keyValues["common"]["extended"]["launcheula"].value.orEmpty(),
                    requireDefaultInstallFolder = app.keyValues["common"]["config"]["requiredefaultinstallfolder"].asBoolean(),
                    contentType = app.keyValues["common"]["config"]["contentType"].asInteger(),
                    installDir = app.keyValues["common"]["config"]["installdir"].value.orEmpty(),
                    useLaunchCmdLine = app.keyValues["common"]["config"]["uselaunchcommandline"].asBoolean(),
                    launchWithoutWorkshopUpdates = app.keyValues["common"]["config"]["launchwithoutworkshopupdates"].asBoolean(),
                    useMms = app.keyValues["common"]["config"]["usemms"].asBoolean(),
                    installScriptSignature = app.keyValues["common"]["config"]["installscriptsignature"].value.orEmpty(),
                    installScriptOverride = app.keyValues["common"]["config"]["installscriptoverride"].asBoolean(),
                    config = ConfigInfo(
                        installDir = app.keyValues["config"]["installdir"].value.orEmpty(),
                        launch = launchConfigs.map {
                            LaunchInfo(
                                executable = it["executable"].value?.replace('\\', '/').orEmpty(),
                                workingDir = it["workingdir"].value?.replace('\\', '/').orEmpty(),
                                description = it["description"].value.orEmpty(),
                                type = it["type"].value.orEmpty(),
                                configOS = OS.from(it["config"]["oslist"].value),
                                configArch = OSArch.from(it["config"]["osarch"].value),
                            )
                        }.toTypedArray(),
                        steamControllerTemplateIndex = app.keyValues["config"]["steamcontrollertemplateindex"].asInteger(),
                        steamControllerTouchTemplateIndex = app.keyValues["config"]["steamcontrollertouchtemplateindex"].asInteger(),
                    ),
                    ufs = UFS(
                        quota = app.keyValues["ufs"]["quota"].asInteger(),
                        maxNumFiles = app.keyValues["ufs"]["maxnumfiles"].asInteger(),
                        saveFilePatterns = app.keyValues["ufs"]["savefiles"].children.map {
                            SaveFilePattern(
                                root = PathType.from(it["root"].value),
                                path = it["path"].value.orEmpty(),
                                pattern = it["pattern"].value.orEmpty(),
                            )
                        }.toTypedArray(),
                    ),
                )

                // // val isBaba = app.id == 736260
                // // val isNoita = app.id == 881100
                // // val isHades = app.id == 1145360
                // // val isCS2 = app.id == 730
                // // val isPsuedo = app.id == 2365810
                // // val isPathway = app.id == 546430
                // // val isSeaOfStars = app.id == 1244090
                // // val isMessenger = app.id == 764790
                // // val isWargroove = app.id == 607050
                // // val isTetrisEffect = app.id == 1003590
                // // val isLittleKitty = app.id == 1177980
                // val isFactorio = app.id == 427520
                // if (isFactorio) {
                // 	logD("${app.id}: ${app.keyValues["common"]["name"].value}");
                // 	printAllKeyValues(app.keyValues)
                // 	// getPkgInfoOf(app.id)?.let {
                // 	// 	printAllKeyValues(it.original)
                //     // }
                // }
            }

            isRequestingAppInfo = false

            PluviaApp.events.emit(SteamEvent.AppInfoReceived)
        }
    }
}
