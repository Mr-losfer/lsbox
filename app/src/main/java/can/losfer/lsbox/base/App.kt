package can.losfer.lsbox.base

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.SystemClock

import com.github.catvod.crawler.JsLoader
import can.losfer.lsbox.BuildConfig
import can.losfer.lsbox.boot.BootGuard
import can.losfer.lsbox.data.AppDataManager
import can.losfer.lsbox.io.FileUtils
import can.losfer.lsbox.net.HttpClients
import can.losfer.lsbox.server.ControlManager
import can.losfer.lsbox.util.AppContextHolder
import can.losfer.lsbox.util.ActivityStack
import can.losfer.lsbox.util.EpgUtil
import can.losfer.lsbox.util.KVKeys
import can.losfer.lsbox.util.KV
import can.losfer.lsbox.util.AppLog
import can.losfer.lsbox.util.LanguageManager
import can.losfer.lsbox.util.Probe
import can.losfer.lsbox.util.VodImages
import com.whl.quickjs.android.QuickJSLoader

import me.jessyan.autosize.AutoSizeConfig
import me.jessyan.autosize.unit.Subunits

class App : Application() {

    override fun attachBaseContext(base: Context) {
        Probe.step("kvInit") { KV.init(base) }
        super.attachBaseContext(LanguageManager.wrap(base))
    }

    private var bootLastMs = 0L

    private fun bootMark(step: String) {
        if (!BuildConfig.DEBUG) return
        val now = SystemClock.elapsedRealtime()
        AppLog.i("echo-boot $step dt=" + (now - bootLastMs) + "ms")
        bootLastMs = now
    }

    override fun onCreate() {
        super.onCreate()
        bootLastMs = SystemClock.elapsedRealtime()
        installStrictMode()
        bootMark("installStrictMode")
        instance = this
        AppContextHolder.install(this)
        BootGuard.install()
        bootMark("bootGuard")
        initParams()
        bootMark("initParams")
        Probe.trace("boot:httpClients") { HttpClients.init() }
        bootMark("httpClients")
        Probe.trace("boot:epgUtil") { EpgUtil.init() }
        bootMark("epgUtil")
        Probe.trace("boot:controlManager") { ControlManager.init(this) }
        bootMark("controlManager")
        Probe.trace("boot:appDataManager") { AppDataManager.init() }
        bootMark("appDataManager")
        Probe.trace("boot:autoSize") {
            AutoSizeConfig.getInstance().setCustomFragment(true).unitsManager
                .setSupportDP(false)
                .setSupportSP(false)
                .setSupportSubunits(Subunits.MM)
        }
        bootMark("autoSize")
        Probe.trace("boot:quickJSLoader") { loadQuickJs() }
        bootMark("quickJSLoader")
        Probe.trace("boot:vodImages") { VodImages.init() }
        bootMark("vodImages")
        Thread({
            FileUtils.ensureNoMediaMarker()
            Probe.trace("boot:cleanPlayerCache") { FileUtils.cleanPlayerCache() }
        }, "exo-cache-boot").start()
        bootMark("onCreateDone")
    }

    private fun loadQuickJs() {
        val previous = StrictMode.allowThreadDiskReads()
        try {
            QuickJSLoader.init()
        } finally {
            StrictMode.setThreadPolicy(previous)
        }
    }

    private fun initParams() {
        KV.put(KVKeys.PLAYER_IS_LIVE, false)
        if (!KV.contains(KVKeys.PLAY_TYPE)) {
            KV.put(KVKeys.PLAY_TYPE, 2)
        } else {
            val playType = KV.get(KVKeys.PLAY_TYPE, 2)
            if (playType == 0 || playType == 1 || (playType > 2 && playType != 13)) {
                KV.put(KVKeys.PLAY_TYPE, 2)
            }
        }
    }

    private fun installStrictMode() {
        if (!BuildConfig.DEBUG) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .penaltyLog()
                .build()
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        JsLoader.destroy()
    }

    fun getCurrentActivity(): Activity {
        return ActivityStack.getInstance().currentActivity()
    }


    companion object {
        private var instance: App? = null

        var dashData: String? = null

        @JvmStatic
        fun getInstance(): App? {
            return instance
        }
    }
}
