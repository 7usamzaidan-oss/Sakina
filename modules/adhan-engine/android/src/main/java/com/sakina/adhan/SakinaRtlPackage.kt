package com.sakina.adhan

import android.app.Application
import com.facebook.react.modules.i18nmanager.I18nUtil
import expo.modules.core.interfaces.ApplicationLifecycleListener
import expo.modules.core.interfaces.Package

/** Arabic is the default language: force RTL before React Native starts, so even the first launch is RTL. */
class SakinaRtlPackage : Package {
  override fun createApplicationLifecycleListeners(context: android.content.Context?): List<ApplicationLifecycleListener> =
    listOf(object : ApplicationLifecycleListener {
      override fun onCreate(application: Application) {
        I18nUtil.instance.allowRTL(application, true)
        I18nUtil.instance.forceRTL(application, true)
      }
    })
}
