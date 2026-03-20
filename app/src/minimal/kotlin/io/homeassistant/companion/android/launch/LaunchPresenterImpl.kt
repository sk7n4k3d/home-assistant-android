package io.homeassistant.companion.android.launch

import io.homeassistant.companion.android.BuildConfig
import io.homeassistant.companion.android.common.data.integration.DeviceRegistration
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.common.push.PushProviderManager
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

class LaunchPresenterImpl @Inject constructor(
    view: LaunchView,
    serverManager: ServerManager,
    pushProviderManager: PushProviderManager
) : LaunchPresenterBase(view, serverManager, pushProviderManager) {
    override fun resyncRegistration() {
        if (!serverManager.isRegistered()) return
        serverManager.defaultServers.forEach {
            ioScope.launch {
                try {
                    val result = pushProviderManager.selectAndRegister()
                    serverManager.integrationRepository(it.id).updateRegistration(
                        DeviceRegistration(
                            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            pushToken = result?.pushToken,
                            pushUrl = result?.pushUrl ?: result?.pushToken?.let { "" },
                            pushEncrypt = result?.encrypt ?: false
                        )
                    )
                    serverManager.integrationRepository(it.id).getConfig() // Update cached data
                    serverManager.webSocketRepository(it.id).getCurrentUser() // Update cached data
                } catch (e: Exception) {
                    Timber.e(e, "Issue updating Registration")
                }
            }
        }
    }
}
