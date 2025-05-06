package com.OxGames.Pluvia.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.room.withTransaction
import com.OxGames.Pluvia.BuildConfig
import com.OxGames.Pluvia.PluviaApp
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.data.DownloadInfo
import com.OxGames.Pluvia.data.OwnedGames
import com.OxGames.Pluvia.data.SteamApp
import com.OxGames.Pluvia.data.SteamFriend
import com.OxGames.Pluvia.data.SteamLicense
import com.OxGames.Pluvia.db.PluviaDatabase
import com.OxGames.Pluvia.db.dao.ChangeNumbersDao
import com.OxGames.Pluvia.db.dao.EmoticonDao
import com.OxGames.Pluvia.db.dao.FileChangeListsDao
import com.OxGames.Pluvia.db.dao.FriendMessagesDao
import com.OxGames.Pluvia.db.dao.SteamAppDao
import com.OxGames.Pluvia.db.dao.SteamFriendDao
import com.OxGames.Pluvia.db.dao.SteamLicenseDao
import com.OxGames.Pluvia.enums.LoginResult
import com.OxGames.Pluvia.events.AndroidEvent
import com.OxGames.Pluvia.events.SteamEvent
import com.OxGames.Pluvia.service.callback.EmoticonListCallback
import com.OxGames.Pluvia.service.handler.PluviaHandler
import com.OxGames.Pluvia.utils.SteamUtils
import com.OxGames.Pluvia.utils.generateSteamApp
import com.OxGames.Pluvia.utils.timeChunked
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.networking.steam3.ProtocolTypes
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFamilygroupsSteamclient
import `in`.dragonbra.javasteam.rpc.service.FamilyGroups
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.AuthenticationException
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.contentdownloader.FileManifestProvider
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.discovery.ServerQuality
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.LicenseListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSChangesCallback
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSProductInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.AliasHistoryCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendsListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.NicknameListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.ProfileInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamuser.ChatMode
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
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.NetHelpers
import `in`.dragonbra.javasteam.util.log.LogListener
import `in`.dragonbra.javasteam.util.log.LogManager
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Collections
import java.util.EnumSet
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.io.path.pathString
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

interface SteamServiceInterface {
    suspend fun stop()
    suspend fun logOut()
    suspend fun clearDatabase()
    suspend fun startLoginWithQr()
    suspend fun stopLoginWithQr()
    suspend fun getProfileInfo(steamid: SteamID): ProfileInfoCallback
    suspend fun getOwnedGames(steamid: Long): List<OwnedGames>
    suspend fun initChat(steamid: Long)
    suspend fun sendTypingMessage(steamid: Long)
    suspend fun sendMessage(steamid: Long, value: String)
    suspend fun blockFriend(steamid: Long)
    suspend fun removeFriend(steamid: Long)
    suspend fun requestAliasHistory(steamid: Long)
    suspend fun setNickName(steamid: Long, value: String)
    suspend fun startLoginWithCredentials(username: String, password: String, rememberSession: Boolean, authenticator: IAuthenticator)
    suspend fun notifyRunningProcesses()
    suspend fun closeApp(id: Int, prefixToPath: (String) -> String)
    fun isAppInstalled(appId: Int): Boolean
}

class ServiceBinder(service: SteamService) : Binder() {
    val service: SteamServiceInterface = service
}

@AndroidEntryPoint
class SteamService : Service(), SteamServiceInterface, IChallengeUrlChanged {

    @Inject
    lateinit var db: PluviaDatabase

    @Inject
    lateinit var licenseDao: SteamLicenseDao

    @Inject
    lateinit var appDao: SteamAppDao

    @Inject
    lateinit var friendDao: SteamFriendDao

    @Inject
    lateinit var messagesDao: FriendMessagesDao

    @Inject
    lateinit var emoticonDao: EmoticonDao

    @Inject
    lateinit var changeNumbersDao: ChangeNumbersDao

    @Inject
    lateinit var fileChangeListsDao: FileChangeListsDao

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /* Steam */
    internal var steamClient: SteamClient? = null
    internal var manager: CallbackManager? = null
    internal var steamUser: SteamUser? = null
    internal var steamApps: SteamApps? = null
    internal var steamFriends: SteamFriends? = null
    internal var steamCloud: SteamCloud? = null
    internal var unifiedFriends: SteamUnifiedFriends? = null
    internal var familyGroups: FamilyGroups? = null
    internal val subscriptions: ArrayList<Closeable> = ArrayList()

    private lateinit var notificationHelper: NotificationHelper

    private val appIdFlowSender: MutableSharedFlow<Int> = MutableSharedFlow()
    private val appIdFlowReceiver = appIdFlowSender.asSharedFlow()

    private val appCache = ConcurrentHashMap<Int, Pair<String, Boolean>>()
    private val downloadJobs = ConcurrentHashMap<Int, DownloadInfo>()

    private val serverListPath: String by lazy {
        Paths.get(this.cacheDir.path, "server_list.bin").pathString
    }

    private val depotManifestsPath: String by lazy {
        Paths.get(this.dataDir.path, "Steam", "depot_manifests.zip").pathString
    }

    private val onEndProcess: (AndroidEvent.EndProcess) -> Unit = {
        scope.launch {
            stop()
        }
    }

    private val onPersonaStateChange: (SteamEvent.PersonaStateChange) -> Unit = {
        scope.launch {
            PrefManager.personaState = it.state
            steamFriends!!.setPersonaState(it.state)
        }
    }

    override fun onBind(intent: Intent?): IBinder = ServiceBinder(this)

    override fun onCreate() {
        super.onCreate()
        Timber.i("onCreate")

        PluviaApp.events.on<AndroidEvent.EndProcess, Unit>(onEndProcess)
        PluviaApp.events.on<SteamEvent.PersonaStateChange, Unit>(onPersonaStateChange)

        notificationHelper = NotificationHelper(this)

        // To view log messages in android logcat properly
        val logger = object : LogListener {
            override fun onLog(clazz: Class<*>, message: String?, throwable: Throwable?) {
                val logMessage = message ?: "No message given"
                Timber.i(throwable, "[${clazz.simpleName}] -> $logMessage")
            }

            override fun onError(clazz: Class<*>, message: String?, throwable: Throwable?) {
                val logMessage = message ?: "No message given"
                Timber.e(throwable, "[${clazz.simpleName}] -> $logMessage")
            }
        }
        LogManager.addListener(logger)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("onStartCommand")

        // Notification intents
        when (intent?.action) {
            NotificationHelper.ACTION_EXIT -> {
                Timber.i("Exiting app via notification intent")

                val event = AndroidEvent.EndProcess
                PluviaApp.events.emit(event)

                return START_NOT_STICKY
            }
        }

        if (!isRunning.value) {
            Timber.i("Using server list path: $serverListPath")

            val configuration = SteamConfiguration.create {
                it.withProtocolTypes(EnumSet.of(ProtocolTypes.WEB_SOCKET, ProtocolTypes.TCP))
                it.withCellID(PrefManager.cellId)
                it.withServerListProvider(FileServerListProvider(File(serverListPath)))
                it.withManifestProvider(FileManifestProvider(File(depotManifestsPath)))
            }

            // create our steam client instance
            steamClient = SteamClient(configuration).apply {
                addHandler(PluviaHandler())

                // remove callbacks we're not using.
                removeHandler(SteamGameServer::class.java)
                removeHandler(SteamMasterServer::class.java)
                removeHandler(SteamWorkshop::class.java)
                removeHandler(SteamScreenshots::class.java)
                removeHandler(SteamUserStats::class.java)
            }

            // create the callback manager which will route callbacks to function calls
            manager = CallbackManager(steamClient!!)

            // get the different handlers to be used throughout the service
            steamUser = steamClient!!.getHandler(SteamUser::class.java)
            steamApps = steamClient!!.getHandler(SteamApps::class.java)
            steamFriends = steamClient!!.getHandler(SteamFriends::class.java)
            steamCloud = steamClient!!.getHandler(SteamCloud::class.java)
            unifiedFriends = SteamUnifiedFriends(this)
            familyGroups = steamClient!!.getHandler<SteamUnifiedMessages>()!!.createService<FamilyGroups>()

            with(subscriptions) {
                with(manager!!) {
                    add(subscribe(ConnectedCallback::class.java, ::onConnected))
                    add(subscribe(DisconnectedCallback::class.java, ::onDisconnected))
                    add(subscribe(LoggedOnCallback::class.java, ::onLoggedOn))
                    add(subscribe(LoggedOffCallback::class.java, ::onLoggedOff))
                    add(subscribe(PersonaStateCallback::class.java, ::onPersonaStateReceived))
                    add(subscribe(LicenseListCallback::class.java, ::onLicenseList))
                    add(subscribe(NicknameListCallback::class.java, ::onNicknameList))
                    add(subscribe(FriendsListCallback::class.java, ::onFriendsList))
                    add(subscribe(EmoticonListCallback::class.java, ::onEmoticonList))
                    add(subscribe(AliasHistoryCallback::class.java, ::onAliasHistory))
                    add(subscribe(PICSChangesCallback::class.java, ::onPicsChanges))
                    add(subscribe(PICSProductInfoCallback::class.java, ::onPicsProduct))
                }
            }

            _isRunning.value = true

            // we should use Dispatchers.IO here since we are running a sleeping/blocking function
            // "The idea is that the IO dispatcher spends a lot of time waiting (IO blocked),
            // while the Default dispatcher is intended for CPU intensive tasks, where there
            // is little or no sleep."
            // source: https://stackoverflow.com/a/59040920
            scope.launch {
                while (isRunning.value) {
                    // logD("runWaitCallbacks")

                    try {
                        manager!!.runWaitCallbacks(1.seconds.inWholeMilliseconds)
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

        scope.launch { stop() }
    }

    override suspend fun stop() {
        Timber.i("Stopping Steam service")
        if (steamClient != null && steamClient!!.isConnected) {
            _isRunning.value = true

            steamClient!!.disconnect()

            while (isStopping.value) {
                delay(200L)
            }

            // the reason we don't clearValues() here is because the onDisconnect
            // callback does it for us
        } else {
            clearValues()
        }
    }

    private fun connectToSteam() {
        CoroutineScope(Dispatchers.Default).launch {
            // this call errors out if run on the main thread
            steamClient!!.connect()

            delay(5000)

            if (!isConnected.value) {
                Timber.w("Failed to connect to Steam, marking endpoint bad and force disconnecting")

                try {
                    steamClient!!.servers.tryMark(
                        endPoint = steamClient!!.currentEndpoint,
                        protocolTypes = EnumSet.of(ProtocolTypes.WEB_SOCKET, ProtocolTypes.TCP),
                        quality = ServerQuality.BAD,
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to mark endpoint as bad:")
                }

                try {
                    steamClient!!.disconnect()
                } catch (e: Exception) {
                    Timber.e(e, "There was an issue when disconnecting:")
                }
            }
        }
    }

    private fun clearValues() {
        _loginResult.value = LoginResult.Failed
        _isRunning.value = false
        _isConnected.value = false
        _isLoggingOut.value = false
        _isWaitingForQRAuth.value = false

        steamClient = null
        steamUser = null
        steamApps = null
        steamFriends = null
        steamCloud = null

        subscriptions.forEach { it.close() }
        subscriptions.clear()
        manager = null

        unifiedFriends?.close()
        unifiedFriends = null

        _isStopping.value = false
        retryAttempt.set(0)

        PluviaApp.events.off<AndroidEvent.EndProcess, Unit>(onEndProcess)
        PluviaApp.events.off<SteamEvent.PersonaStateChange, Unit>(onPersonaStateChange)
        PluviaApp.events.clearAllListenersOf<SteamEvent<Any>>()
    }

    private fun reconnect() {
        notificationHelper.notify("Retrying...")

        _isConnected.value = false

        val event = SteamEvent.Disconnected
        PluviaApp.events.emit(event)

        steamClient!!.disconnect()
    }

    private suspend fun getAppDirPath(appId: Int): String {
        val app = appDao.findApp(appId)
        var appName = app?.config?.installDir.orEmpty()

        if (appName.isEmpty()) {
            appName = app?.name.orEmpty()
        }

        // TODO
        val default = Paths.get(dataDir.path, "Steam", "steamapps", "common").pathString
        val appInstallPath = PrefManager.getString("app_install_path", default)
        return Paths.get(appInstallPath, appName).pathString
    }

    override suspend fun initChat(steamid: Long) {
        scope.launch {
            steamClient!!.getHandler<PluviaHandler>()!!.getEmoticonList()
            unifiedFriends!!.getRecentMessages(steamid)
            unifiedFriends!!.ackMessage(steamid)
        }
    }

    override fun isAppInstalled(appId: Int): Boolean = runBlocking(Dispatchers.IO) {
        val appDownloadInfo = downloadJobs[appId]
        val isNotDownloading = appDownloadInfo == null || appDownloadInfo.getProgress() >= 1f

        if (!isNotDownloading) {
            return@runBlocking false
        }

        appCache[appId]?.let { cacheEntry ->
            if (cacheEntry.second) {
                return@runBlocking true
            }

            val isNowInstalled = runBlocking(Dispatchers.IO) {
                Files.exists(Paths.get(cacheEntry.first))
            }

            if (isNowInstalled) {
                appCache[appId] = Pair(cacheEntry.first, true)
            }

            return@runBlocking isNowInstalled
        }

        val path = getAppDirPath(appId)

        val isInstalled = runBlocking(Dispatchers.IO) {
            Files.exists(Paths.get(path))
        }

        appCache[appId] = Pair(path, isInstalled)

        return@runBlocking isInstalled
    }

    override suspend fun closeApp(id: Int, prefixToPath: (String) -> String) {
        TODO("Not yet implemented")
    }

    override suspend fun notifyRunningProcesses() {
        TODO("Not yet implemented")
    }

    override suspend fun startLoginWithCredentials(
        username: String,
        password: String,
        rememberSession: Boolean,
        authenticator: IAuthenticator,
    ) {
        try {
            Timber.i("Logging in via credentials.")

            steamClient?.let { steamClient ->
                val authDetails = AuthSessionDetails().apply {
                    this.username = username.trim()
                    this.password = password.trim()
                    this.persistentSession = rememberSession
                    this.authenticator = authenticator
                    this.deviceFriendlyName = SteamUtils.getMachineName(this@SteamService)
                }

                val event = SteamEvent.LogonStarted(username)
                PluviaApp.events.emit(event)

                val authSession = steamClient.authentication.beginAuthSessionViaCredentials(authDetails).await()

                val pollResult = authSession.pollingWaitForResult().await()

                if (pollResult.accountName.isEmpty() && pollResult.refreshToken.isEmpty()) {
                    throw Exception("No account name or refresh token received.")
                }

                login(
                    clientId = authSession.clientID,
                    username = pollResult.accountName,
                    accessToken = pollResult.accessToken,
                    refreshToken = pollResult.refreshToken,
                    rememberSession = rememberSession,
                )
            } ?: run {
                Timber.e("Could not logon: Failed to connect to Steam")

                val event = SteamEvent.LogonEnded(username, LoginResult.Failed, "No connection to Steam")
                PluviaApp.events.emit(event)
            }
        } catch (e: Exception) {
            Timber.e(e, "Login failed")

            val message = when (e) {
                is CancellationException -> "Unknown cancellation"
                is AuthenticationException -> e.result?.name ?: e.message
                else -> e.message ?: e.javaClass.name
            }

            val event = SteamEvent.LogonEnded(username, LoginResult.Failed, message)
            PluviaApp.events.emit(event)
        }
    }

    override suspend fun setNickName(steamid: Long, value: String) {
        val friend = SteamID(steamid)
        steamFriends!!.setFriendNickname(friend, value)
    }

    override suspend fun requestAliasHistory(steamid: Long) {
        steamClient!!.getHandler<SteamFriends>()?.requestAliasHistory(SteamID(steamid))
    }

    override suspend fun removeFriend(steamid: Long) {
        val friend = SteamID(steamid)
        steamFriends!!.removeFriend(friend)
        friendDao.remove(steamid)
    }

    override suspend fun blockFriend(steamid: Long) {
        val friend = SteamID(steamid)
        val result = steamFriends!!.ignoreFriend(friend).await()
        if (result.result == EResult.OK) {
            val blockedFriend = friendDao.findFriend(steamid)
            blockedFriend?.let {
                friendDao.update(it.copy(relation = EFriendRelationship.Blocked))
            }
        }
    }

    override suspend fun sendMessage(steamid: Long, value: String) {
        unifiedFriends!!.sendMessage(steamid, value)
    }

    override suspend fun sendTypingMessage(steamid: Long) {
        unifiedFriends!!.setIsTyping(steamid)
    }

    override suspend fun getOwnedGames(steamid: Long): List<OwnedGames> {
        return unifiedFriends!!.getOwnedGames(steamid)
    }

    override suspend fun getProfileInfo(steamid: SteamID): ProfileInfoCallback {
        return steamFriends!!.requestProfileInfo(steamid).await()
    }

    override suspend fun stopLoginWithQr() {
        Timber.i("Stopping QR polling")
        _isWaitingForQRAuth.value = false
    }

    override suspend fun startLoginWithQr() {
        try {
            Timber.i("Logging in via QR.")

            steamClient?.let { steamClient ->
                _isWaitingForQRAuth.value = true

                val authDetails = AuthSessionDetails().apply {
                    deviceFriendlyName = SteamUtils.getMachineName(this@SteamService)
                }

                val authSession = steamClient.authentication.beginAuthSessionViaQR(authDetails).await()

                // Steam will periodically refresh the challenge url, this callback allows you to draw a new qr code.
                authSession.challengeUrlChanged = this@SteamService

                val qrEvent = SteamEvent.QrChallengeReceived(authSession.challengeUrl)
                PluviaApp.events.emit(qrEvent)

                Timber.d("PollingInterval: ${authSession.pollingInterval.toLong()}")

                var authPollResult: AuthPollResult? = null

                while (isWaitingForQRAuth.value && authPollResult == null) {
                    try {
                        authPollResult = authSession.pollAuthSessionStatus().await()
                    } catch (e: Exception) {
                        Timber.e(e, "Poll auth session status error")
                        throw e
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
                    }

                    delay(authSession.pollingInterval.toLong())
                }

                _isWaitingForQRAuth.value = false

                val event = SteamEvent.QrAuthEnded(authPollResult != null)
                PluviaApp.events.emit(event)

                // there is a chance qr got cancelled and there is no authPollResult
                if (authPollResult == null) {
                    Timber.e("Got no auth poll result")
                    throw Exception("Got no auth poll result")
                }

                login(
                    clientId = authSession.clientID,
                    username = authPollResult.accountName,
                    accessToken = authPollResult.accessToken,
                    refreshToken = authPollResult.refreshToken,
                )
            } ?: run {
                Timber.e("Could not start QR logon: Failed to connect to Steam")

                val event = SteamEvent.QrAuthEnded(success = false, message = "No connection to Steam")
                PluviaApp.events.emit(event)
            }
        } catch (e: Exception) {
            Timber.e(e, "QR failed")

            val message = when (e) {
                is CancellationException -> "QR Session timed out"
                is AuthenticationException -> e.result?.name ?: e.message
                else -> e.message ?: e.javaClass.name
            }

            val event = SteamEvent.QrAuthEnded(success = false, message = message)
            PluviaApp.events.emit(event)
        }
    }

    override suspend fun clearDatabase() {
        db.withTransaction {
            db.emoticonDao().deleteAll()
            db.friendMessagesDao().deleteAllMessages()
            appDao.deleteAll()
            changeNumbersDao.deleteAll()
            fileChangeListsDao.deleteAll()
            friendDao.deleteAll()
            licenseDao.deleteAll()
        }
    }

    override suspend fun logOut() {
        _isLoggingOut.value = true

        performLogOffDuties()

        steamUser!!.logOff()
    }

    private fun queueAppPICSRequests(apps: List<Int>) {
        if (apps.isEmpty()) {
            return
        }

        scope.launch {
            val flow = flowOf(*apps.toTypedArray())
            appIdFlowSender.emitAll(flow)
        }
    }

    private fun login(
        username: String,
        accessToken: String? = null,
        refreshToken: String? = null,
        password: String? = null,
        rememberSession: Boolean = false,
        twoFactorAuth: String? = null,
        emailAuth: String? = null,
        clientId: Long? = null,
    ) {
        // Sensitive info, only print in DEBUG build.
        if (BuildConfig.DEBUG) {
            Timber.d(
                """
                    Login Information:
                     Username: $username
                     AccessToken: $accessToken
                     RefreshToken: $refreshToken
                     Password: $password
                     Remember Session: $rememberSession
                     TwoFactorAuth: $twoFactorAuth
                     EmailAuth: $emailAuth
                """.trimIndent(),
            )
        }

        PrefManager.username = username

        if ((password != null && rememberSession) || refreshToken != null) {
            if (accessToken != null) {
                PrefManager.accessToken = accessToken
            }

            if (refreshToken != null) {
                PrefManager.refreshToken = refreshToken
            }

            if (clientId != null) {
                PrefManager.clientId = clientId
            }
        }

        val event = SteamEvent.LogonStarted(username)
        PluviaApp.events.emit(event)

        steamUser!!.logOn(
            LogOnDetails(
                username = SteamUtils.removeSpecialChars(username).trim(),
                password = password?.let { SteamUtils.removeSpecialChars(it).trim() },
                shouldRememberPassword = rememberSession,
                twoFactorCode = twoFactorAuth,
                authCode = emailAuth,
                accessToken = refreshToken,
                loginID = SteamUtils.getUniqueDeviceId(this),
                machineName = SteamUtils.getMachineName(this),
                chatMode = ChatMode.NEW_STEAM_CHAT,
            ),
        )
    }

    private fun performLogOffDuties() {
        val username = PrefManager.username

        clearUserData()

        val event = SteamEvent.LoggedOut(username)
        PluviaApp.events.emit(event)
    }

    private fun clearUserData() {
        scope.launch {
            PrefManager.clearPreferences()
            clearDatabase()
        }
    }

    // region [REGION] callbacks
    @Suppress("UNUSED_PARAMETER", "unused")
    private fun onConnected(callback: ConnectedCallback) {
        Timber.i("Connected to Steam")

        retryAttempt.set(0)
        _isConnected.value = true

        var isAutoLoggingIn = false

        if (PrefManager.username.isNotEmpty() && PrefManager.refreshToken.isNotEmpty()) {
            isAutoLoggingIn = true

            login(
                username = PrefManager.username,
                refreshToken = PrefManager.refreshToken,
                rememberSession = true,
            )
        }

        val event = SteamEvent.Connected(isAutoLoggingIn)
        PluviaApp.events.emit(event)
    }

    private fun onDisconnected(callback: DisconnectedCallback) {
        Timber.i("Disconnected from Steam. User initiated: ${callback.isUserInitiated}")

        _isConnected.value = false

        if (!isStopping.value && retryAttempt.get() < MAX_RETRY_ATTEMPTS) {
            retryAttempt.incrementAndGet()

            Timber.w("Attempting to reconnect (retry $retryAttempt)")

            // isLoggingOut = false
            val event = SteamEvent.RemotelyDisconnected
            PluviaApp.events.emit(event)

            connectToSteam()
        } else {
            val event = SteamEvent.Disconnected
            PluviaApp.events.emit(event)

            clearValues()

            stopSelf()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun onLoggedOn(callback: LoggedOnCallback) {
        Timber.i("Logged onto Steam: ${callback.result}")

        when (callback.result) {
            EResult.TryAnotherCM -> {
                _loginResult.value = LoginResult.Failed
                reconnect()
            }

            EResult.OK -> {
                // save the current cellid somewhere. if we lose our saved server list, we can use this when retrieving
                // servers from the Steam Directory.
                PrefManager.cellId = callback.cellID

                // Immediately set our steam id.
                userSteamId = steamClient!!.steamID

                // retrieve persona data of logged in user
                scope.launch {
                    steamFriends?.requestFriendInfo(userSteamId!!)
                }

                // Request family share info if we have a familyGroupId.
                if (callback.familyGroupId != 0L) {
                    scope.launch {
                        val request = SteammessagesFamilygroupsSteamclient.CFamilyGroups_GetFamilyGroup_Request.newBuilder().apply {
                            familyGroupid = callback.familyGroupId
                        }.build()

                        familyGroups!!.getFamilyGroup(request).await().let {
                            if (it.result != EResult.OK) {
                                Timber.w("An error occurred loading family group info.")
                                return@launch
                            }

                            val response = it.body

                            Timber.i("Found family share: ${response.name}, with ${response.membersCount} members.")

                            val members = mutableListOf<Int>()
                            response.membersList.forEach { member ->
                                val accountID = SteamID(member.steamid).accountID.toInt()
                                members.add(accountID)
                            }
                            familyMembers = members
                        }
                    }
                }

                // continuously check for pics changes
                continuousPICSChangesChecker()

                // request app pics data when needed
                bufferedPICSGetProductInfo()

                // continuously check for game names that friends are playing.
                continuousFriendChecker()

                // Tell steam we're online, this allows friends to update.
                steamFriends?.setPersonaState(PrefManager.personaState)

                notificationHelper.notify("Connected")

                _loginResult.value = LoginResult.Success
            }

            else -> {
                clearUserData()

                _loginResult.value = LoginResult.Failed

                reconnect()
            }
        }

        val event = SteamEvent.LogonEnded(PrefManager.username, loginResult.value)
        PluviaApp.events.emit(event)
    }

    private fun onLoggedOff(callback: LoggedOffCallback) {
        Timber.i("Logged off of Steam: ${callback.result}")

        notificationHelper.notify("Disconnected...")

        if (isLoggingOut.value || callback.result == EResult.LogonSessionReplaced) {
            performLogOffDuties()

            scope.launch { stop() }
        } else if (callback.result == EResult.LoggedInElsewhere) {
            // received when a client runs an app and wants to forcibly close another
            // client running an app
            val event = SteamEvent.ForceCloseApp
            PluviaApp.events.emit(event)

            reconnect()
        } else {
            reconnect()
        }
    }

    private fun onNicknameList(callback: NicknameListCallback) {
        Timber.d("Nickname list called: ${callback.nicknames.size}")
        scope.launch {
            db.withTransaction {
                friendDao.clearAllNicknames()
                friendDao.updateNicknames(callback.nicknames)
            }
        }
    }

    private fun onFriendsList(callback: FriendsListCallback) {
        Timber.d("onFriendsList ${callback.friendList.size}")
        scope.launch {
            db.withTransaction {
                callback.friendList
                    .filter { it.steamID.isIndividualAccount }
                    .forEach { filteredFriend ->
                        val friendId = filteredFriend.steamID.convertToUInt64()
                        val friend = friendDao.findFriend(friendId)

                        if (friend == null) {
                            // Not in the DB, create them.
                            val friendToAdd = SteamFriend(
                                id = filteredFriend.steamID.convertToUInt64(),
                                relation = filteredFriend.relationship,
                            )

                            friendDao.insert(friendToAdd)
                        } else {
                            // In the DB, update them.
                            val dbFriend = friend.copy(relation = filteredFriend.relationship)
                            friendDao.update(dbFriend)
                        }
                    }

                // Add logged in account if we don't exist yet.
                val selfId = userSteamId!!.convertToUInt64()
                val self = friendDao.findFriend(selfId)

                if (self == null) {
                    val sid = SteamFriend(id = selfId)
                    friendDao.insert(sid)
                }
            }

            // NOTE: Our UI could load too quickly on fresh database, our icon will be "?"
            //  unless relaunched or we nav to a new screen.
            unifiedFriends?.refreshPersonaStates()
        }
    }

    private fun onEmoticonList(callback: EmoticonListCallback) {
        Timber.i("Getting emotes and stickers, size: ${callback.emoteList.size}")
        scope.launch {
            db.withTransaction {
                emoticonDao.replaceAll(callback.emoteList)
            }
        }
    }

    private fun onAliasHistory(callback: AliasHistoryCallback) {
        val names = callback.responses.flatMap { map -> map.names }.map { map -> map.name }
        val event = SteamEvent.OnAliasHistory(names)
        PluviaApp.events.emit(event)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun onPersonaStateReceived(callback: PersonaStateCallback) {
        // Ignore accounts that arent individuals
        if (!callback.friendID.isIndividualAccount) {
            return
        }

        // Ignore states where the name is blank.
        if (callback.name.isEmpty()) {
            return
        }

        // Timber.d("Persona state received: ${callback.name}")

        scope.launch {
            db.withTransaction {
                val id = callback.friendID.convertToUInt64()
                val friend = friendDao.findFriend(id)

                if (friend == null) {
                    Timber.w("onPersonaStateReceived: failed to find friend to update: $id")
                    return@withTransaction
                }

                friendDao.update(
                    friend.copy(
                        statusFlags = callback.statusFlags,
                        state = callback.state,
                        stateFlags = callback.stateFlags,
                        gameAppID = callback.gameAppID,
                        gameID = callback.gameID,
                        gameName = appDao.findApp(callback.gameAppID)?.name ?: callback.gameName,
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

                // Send off an event if we change states.
                if (callback.friendID == steamClient!!.steamID) {
                    friendDao.findFriend(id)?.let { account ->
                        _personaState.value = account
                    }
                }
            }
        }
    }

    private fun onLicenseList(callback: LicenseListCallback) {
        if (callback.result != EResult.OK) {
            Timber.w("Failed to get License list")
            return
        }

        Timber.i("Received License List ${callback.result}, size: ${callback.licenseList.size}")

        scope.launch {
            // Note: I assume with every launch we do, in fact, update the licenses for app the apps if we join or get removed
            //      from family sharing... We really can't test this as there is a 1-year cooldown.
            //      Then 'findStaleLicences' will find these now invalid items to remove.
            val licensesToAdd = callback.licenseList
                .groupBy { it.packageID }
                .map { licensesEntry ->
                    val preferred = licensesEntry.value.firstOrNull {
                        it.ownerAccountID == userSteamId?.accountID?.toInt()
                    } ?: licensesEntry.value.first()
                    SteamLicense(
                        packageId = licensesEntry.key,
                        lastChangeNumber = preferred.lastChangeNumber,
                        timeCreated = preferred.timeCreated,
                        timeNextProcess = preferred.timeNextProcess,
                        minuteLimit = preferred.minuteLimit,
                        minutesUsed = preferred.minutesUsed,
                        paymentMethod = preferred.paymentMethod,
                        licenseFlags = licensesEntry.value
                            .map { it.licenseFlags }
                            .reduceOrNull { first, second ->
                                val combined = EnumSet.copyOf(first)
                                combined.addAll(second)
                                combined
                            } ?: EnumSet.noneOf(ELicenseFlags::class.java),
                        purchaseCode = preferred.purchaseCode,
                        licenseType = preferred.licenseType,
                        territoryCode = preferred.territoryCode,
                        accessToken = preferred.accessToken,
                        ownerAccountId = licensesEntry.value.map { it.ownerAccountID }, // Read note above
                        masterPackageID = preferred.masterPackageID,
                    )
                }
            if (licensesToAdd.isNotEmpty()) {
                Timber.i("Adding ${licensesToAdd.size} licenses")
                db.withTransaction {
                    licenseDao.insert(licensesToAdd)
                }
            }

            val licensesToRemove = licenseDao.findStaleLicences(callback.licenseList.map { it.packageID })
            if (licensesToRemove.isNotEmpty()) {
                Timber.i("Removing ${licensesToRemove.size} (stale) licenses")
                db.withTransaction {
                    val packageIds = licensesToRemove.map { it.packageId }
                    licenseDao.deleteStaleLicenses(packageIds)
                }
            }

            // Get PICS information with the current license database.
            val picsRequests = licenseDao.getAllLicenses().map { PICSRequest(it.packageId, it.accessToken) }
            steamApps!!.picsGetProductInfo(apps = emptyList(), packages = picsRequests)
        }
    }

    private fun onPicsChanges(callback: PICSChangesCallback) {
        if (PrefManager.lastPICSChangeNumber == callback.currentChangeNumber) {
            Timber.w("Change number was the same as last change number, skipping")
            return
        }

        PrefManager.lastPICSChangeNumber = callback.currentChangeNumber

        Timber.d(
            """
                lastChangeNumber: ${callback.lastChangeNumber}
                currentChangeNumber: ${callback.currentChangeNumber}
                isRequiresFullUpdate: ${callback.isRequiresFullUpdate}
                isRequiresFullAppUpdate: ${callback.isRequiresFullAppUpdate}
                isRequiresFullPackageUpdate: ${callback.isRequiresFullPackageUpdate}
                appChangesCount: ${callback.appChanges.size}
                pkgChangesCount: ${callback.packageChanges.size}
            """.trimIndent(),
        )

        scope.launch {
            callback.appChanges.values
                .filter { changeData ->
                    // only queue PICS requests for apps existing in the db that have changed
                    val app = appDao.findApp(changeData.id) ?: return@filter false
                    changeData.changeNumber != app.lastChangeNumber
                }
                .map { it.id }
                .also { Timber.d("onPicsChanges: Queueing ${it.size} app requests") }
                .also(::queueAppPICSRequests)

            val pkgsWithChanges = callback.packageChanges.values
                .filter { changeData ->
                    // only queue PICS requests for pkgs existing in the db that have changed
                    val pkg = licenseDao.findLicense(changeData.id) ?: return@filter false
                    changeData.changeNumber != pkg.lastChangeNumber
                }
            val pkgsForAccessTokens = pkgsWithChanges.filter { it.isNeedsToken }.map { it.id }
            val accessTokens = steamApps?.picsGetAccessTokens(emptyList(), pkgsForAccessTokens)?.await()?.packageTokens ?: emptyMap()
            val picsRequest = pkgsWithChanges.map { PICSRequest(it.id, accessTokens[it.id] ?: 0) }
            Timber.d("onPicsChanges: Queueing ${picsRequest.size} package requests")
            steamApps!!.picsGetProductInfo(apps = emptyList(), packages = picsRequest)
        }
    }

    private fun onPicsProduct(callback: PICSProductInfoCallback) {
        if (callback.packages.isNotEmpty()) {
            Timber.i("onPicsProduct: Received PICS of ${callback.packages.size} package(s)")

            scope.launch {
                // Don't race the queue.
                val queue = Collections.synchronizedList(mutableListOf<Int>())

                db.withTransaction {
                    callback.packages.values.forEach { pkg ->
                        val appIds = pkg.keyValues["appids"].children.map { it.asInteger() }
                        licenseDao.updateApps(pkg.id, appIds)

                        val depotIds = pkg.keyValues["depotids"].children.map { it.asInteger() }
                        licenseDao.updateDepots(pkg.id, depotIds)

                        // Insert a stub row (or update) of SteamApps to the database.
                        appIds.forEach { appid ->
                            val steamApp = appDao.findApp(appid)?.copy(packageId = pkg.id)
                            if (steamApp != null) {
                                appDao.update(steamApp)
                            } else {
                                val stubSteamApp = SteamApp(id = appid, packageId = pkg.id)
                                appDao.insert(stubSteamApp)
                            }
                        }

                        queue.addAll(appIds)
                    }
                }

                // Get PICS information with the app ids.
                queueAppPICSRequests(queue)
            }
        }

        if (callback.apps.isNotEmpty()) {
            Timber.i("onPicsProduct: Received PICS of ${callback.apps.size} apps(s)")

            scope.launch {
                val steamApps = callback.apps.values.mapNotNull { app ->
                    val appFromDb = appDao.findApp(app.id)
                    val packageId = appFromDb?.packageId ?: INVALID_PKG_ID
                    val packageFromDb = if (packageId != INVALID_PKG_ID) licenseDao.findLicense(packageId) else null
                    val ownerAccountId = packageFromDb?.ownerAccountId ?: emptyList()

                    // Apps with -1 for the ownerAccountId should be added.
                    //  This can help with friend game names.

                    // TODO maybe apps with -1 for the ownerAccountId can be stripped with necessities and name.

                    if (app.changeNumber != appFromDb?.lastChangeNumber) {
                        app.keyValues.generateSteamApp().copy(
                            packageId = packageId,
                            ownerAccountId = ownerAccountId,
                            receivedPICS = true,
                            lastChangeNumber = app.changeNumber,
                            licenseFlags = packageFromDb?.licenseFlags ?: EnumSet.noneOf(ELicenseFlags::class.java),
                        )
                    } else {
                        null
                    }
                }

                if (steamApps.isNotEmpty()) {
                    Timber.d("Inserting ${steamApps.size} PICS apps to database")
                    db.withTransaction {
                        appDao.insert(steamApps)
                    }
                }
            }
        }
    }

    override fun onChanged(qrAuthSession: QrAuthSession?) {
        Timber.i("onChanged (QR)")
        qrAuthSession?.let { qr ->
            val event = SteamEvent.QrChallengeReceived(qr.challengeUrl)
            PluviaApp.events.emit(event)
        } ?: run { Timber.w("QR challenge url was null") }
    }
    // endregion

    /**
     * Request changes for apps and packages since a given change number.
     * Checks every [PICS_CHANGE_CHECK_DELAY] seconds.
     * Results are returned in a [PICSChangesCallback]
     */
    private fun continuousPICSChangesChecker() = scope.launch {
        while (isActive && isLoggedIn.value) {
            steamApps!!.picsGetChangesSince(
                lastChangeNumber = PrefManager.lastPICSChangeNumber,
                sendAppChangeList = true,
                sendPackageChangelist = true,
            )

            delay(PICS_CHANGE_CHECK_DELAY)
        }
    }

    /**
     * Continuously check for friends playing games and query for pics if its a game we don't have in the database.
     */
    private fun continuousFriendChecker() = scope.launch {
        while (isActive && isLoggedIn.value) {
            // Initial delay before each check
            delay(20.seconds)

            friendDao.findFriendsInGame()
                .also { friends -> Timber.d("Found ${friends.size} friends in game") }
                .forEach { friend ->
                    appDao.findApp(friend.gameAppID)?.let { app ->
                        if (friend.gameName != app.name) {
                            Timber.d("Updating ${friend.name} with game ${app.name}")
                            friendDao.update(friend.copy(gameName = app.name))
                        }
                    } ?: queueAppPICSRequests(listOf(friend.gameAppID)) // Didn't find the app, we'll get it next time.
                }
        }
    }

    /**
     * A buffered flow to parse so many PICS requests in a given moment.
     */
    private fun bufferedPICSGetProductInfo() = scope.launch {
        appIdFlowReceiver
            .timeChunked(MAX_SIMULTANEOUS_PICS_REQUESTS)
            .buffer(Channel.RENDEZVOUS)
            .collect { appIds ->
                Timber.d("Collected ${appIds.size} app(s) to query PICS")
                steamApps!!.picsGetProductInfo(
                    apps = appIds.map { PICSRequest(id = it) },
                    packages = emptyList(),
                )
            }
    }

    companion object {
        private val PICS_CHANGE_CHECK_DELAY = 60.seconds

        const val MAX_SIMULTANEOUS_PICS_REQUESTS = 50
        const val MAX_RETRY_ATTEMPTS = 20
        const val INVALID_APP_ID: Int = Int.MAX_VALUE
        const val INVALID_PKG_ID: Int = Int.MAX_VALUE

        var userSteamId: SteamID? = null
            private set

        var familyMembers: List<Int> = listOf()
            private set

        private val _personaState = MutableStateFlow<SteamFriend?>(null)
        val personaState = _personaState.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _isConnected = MutableStateFlow(false)
        val isConnected = _isConnected.asStateFlow()

        private val _isLoggedIn = MutableStateFlow(false)
        val isLoggedIn = _isLoggedIn.asStateFlow()

        private val _isStopping = MutableStateFlow(false)
        val isStopping = _isStopping.asStateFlow()

        private val _isLoggingOut = MutableStateFlow(false)
        val isLoggingOut = _isLoggingOut.asStateFlow()

        private val _isWaitingForQRAuth = MutableStateFlow(false)
        val isWaitingForQRAuth = _isWaitingForQRAuth.asStateFlow()

        private val _loginResult = MutableStateFlow(LoginResult.Failed)
        val loginResult = _loginResult.asStateFlow()

        val retryAttempt = AtomicInteger(0)
    }
}
