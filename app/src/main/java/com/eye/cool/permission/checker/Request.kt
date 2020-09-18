package com.eye.cool.permission.checker

import android.content.Context
import androidx.fragment.app.Fragment
import com.eye.cool.permission.rationale.*
import com.eye.cool.permission.rationale.DefaultRationale
import com.eye.cool.permission.rationale.InstallPackageSettingRationale
import com.eye.cool.permission.rationale.RationaleDelegate
import com.eye.cool.permission.rationale.SettingRationale
import com.eye.cool.permission.support.CompatContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * Created by ycb on 2020/8/31
 */
class Request private constructor(
    internal val context: CompatContext
) {

  internal var permissions = arrayListOf<String>()
  internal var rationale = RationaleDelegate(DefaultRationale())
  internal var rationaleSetting = RationaleDelegate(SettingRationale())
  internal var rationaleInstallPackageSetting = RationaleDelegate(InstallPackageSettingRationale())
  internal var showRationaleSettingWhenDenied = false
  internal var showRationaleWhenRequest = false
  internal var showInstallRationaleWhenRequest = false

  internal var scope = MainScope()

  fun onDestroy() {
    scope.cancel()
    context.release()
  }

  class Builder {

    private val request: Request

    constructor(context: Context) {
      request = Request(CompatContext(context))
    }

    constructor(fragment: Fragment) {
      request = Request(CompatContext(fragment))
    }

    internal constructor(context: CompatContext) {
      request = Request(context)
    }

    /**
     * @param permission Requested permission is required
     */
    fun permission(permission: String): Builder {
      request.permissions.add(permission)
      return this
    }

    /**
     * @param permissions Requested permissions are required
     */
    fun permissions(permissions: Array<String>): Builder {
      request.permissions.addAll(permissions)
      return this
    }

    /**
     * @param permissions Requested permissions are required
     */
    fun permissions(permissions: Collection<String>): Builder {
      request.permissions.addAll(permissions)
      return this
    }


    /**
     * @param showRationaleWhenRequest Show Permission dialog when requesting, default false
     */
    fun showRationaleWhenRequest(showRationaleWhenRequest: Boolean): Builder {
      request.showRationaleWhenRequest = showRationaleWhenRequest
      return this
    }

    /**
     * @param showInstallRationaleWhenRequest Show Install Permission dialog when requesting, default false
     */
    fun showInstallRationaleWhenRequest(showInstallRationaleWhenRequest: Boolean): Builder {
      request.showInstallRationaleWhenRequest = showInstallRationaleWhenRequest
      return this
    }

    /**
     * @param showRationaleSettingWhenDenied Show Settings dialog when permission denied, default true
     */
    fun showRationaleSettingWhenDenied(showRationaleSettingWhenDenied: Boolean = true): Builder {
      request.showRationaleSettingWhenDenied = showRationaleSettingWhenDenied
      return this
    }

    /**
     * @param rationale The dialog that leads to user authorization
     */
    fun rationale(rationale: Rationale): Builder {
      request.rationale = RationaleDelegate(rationale)
      return this
    }

    /**
     * @param rationaleSetting The Settings dialog that leads to user authorize
     */
    fun rationaleSetting(rationaleSetting: Rationale): Builder {
      request.rationaleSetting = RationaleDelegate(rationaleSetting)
      return this
    }

    /**
     * It will only pop up when you request the permission of 'android.Manifest.permission.REQUEST_INSTALL_PACKAGES'
     *
     * @param rationaleInstallPackageSetting The Settings dialog that leads to user authorize
     */
    fun rationaleInstallPackageSetting(rationaleInstallPackageSetting: Rationale): Builder {
      request.rationaleInstallPackageSetting = RationaleDelegate(rationaleInstallPackageSetting)
      return this
    }

    fun build() = request
  }
}