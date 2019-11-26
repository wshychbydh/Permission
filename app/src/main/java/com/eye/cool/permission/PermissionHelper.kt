package com.eye.cool.permission

import android.Manifest.permission.WRITE_VOICEMAIL
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.*

/**
 * The permissions for all requests must be declared in the manifest.
 * Created by cool on 2018/4/13.
 */
class PermissionHelper private constructor(private var context: Context) {

  private var rationale: Rationale? = null
  private var rationaleSetting: Rationale? = null
  private var rationaleInstallPackagesSetting: Rationale? = null
  private var callback: ((authorise: Boolean) -> Unit)? = null
  private var authoriseCallback: ((authorise: Int) -> Unit)? = null
  private var permissions: Array<String>? = null
  private var showRationaleSettingWhenDenied = true
  private var showRationaleWhenRequest = true
  private var deniedPermissionCallback: ((Array<String>) -> Unit)? = null

  /**
   * If targetApi or SDK is less than 23,
   * support checks for [camera | recorder | storage]'s permissions,
   * and returns true for all other permissions
   */
  fun request() {
    if (permissions == null || permissions!!.isEmpty()) {
      callback?.invoke(true)
      authoriseCallback?.invoke(AuthoriseType.TYPE_GRANTED)
      return
    }
    val target = context.applicationInfo.targetSdkVersion
    if (target >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestPermission(context)
    } else {
      val deniedPermissions = requestPermissionBelow23()
      if (deniedPermissions.isNotEmpty() && showRationaleSettingWhenDenied) {
        rationaleSetting?.showRationale(context, deniedPermissions.toTypedArray(), null)
      } else {
        val hasDeniedPermissions = deniedPermissions.isNotEmpty()
        if (hasDeniedPermissions) {
          deniedPermissionCallback?.invoke(deniedPermissions.toTypedArray())
        }
        callback?.invoke(!hasDeniedPermissions)
        authoriseCallback?.invoke(if (hasDeniedPermissions) AuthoriseType.TYPE_DENIED else AuthoriseType.TYPE_GRANTED)
      }
    }
  }

  private fun requestPermissionBelow23(): List<String> {
    val deniedPermissions = arrayListOf<String>()
    permissions?.forEach {
      val available = when (it) {
        in Permission.CAMERA -> {
          PermissionUtil.isCameraAvailable()
        }
        in Permission.STORAGE -> {
          PermissionUtil.isCacheDirAvailable(context) && PermissionUtil.isExternalDirAvailable()
        }

        in Permission.MICROPHONE -> {
          PermissionUtil.isRecordAvailable()
        }
        else -> {
          //fixme Other permission's checking
          true
        }
      }
      if (!available) {
        deniedPermissions.add(it)
      }
    }
    return deniedPermissions
  }

  @TargetApi(Build.VERSION_CODES.M)
  private fun requestPermission(context: Context) {
    val deniedPermissions = getDeniedPermissions(context, permissions)
    when {
      deniedPermissions.isEmpty() -> {
        callback?.invoke(true)
        authoriseCallback?.invoke(AuthoriseType.TYPE_GRANTED)
      }
      showRationaleWhenRequest -> rationale?.showRationale(context, deniedPermissions) {
        if (it) {
          requestPermission(deniedPermissions)
        } else {
          deniedPermissionCallback?.invoke(deniedPermissions)
          callback?.invoke(false)
          authoriseCallback?.invoke(AuthoriseType.TYPE_DENIED)
        }
      }
      else -> requestPermission(deniedPermissions)
    }
  }

  private fun requestPermission(permissions: Array<String>) {
    PermissionActivity.requestPermission(context, permissions) { requestPermissions, grantResults ->
      verifyPermissions(requestPermissions, grantResults)
    }
  }

  private fun verifyPermissions(permissions: Array<String>, grantResults: IntArray) {
    // Verify that each required permissions has been granted, otherwise all granted
    val deniedPermissions = arrayListOf<String>()
    grantResults.forEachIndexed { index, result ->
      if (result != PackageManager.PERMISSION_GRANTED) {
        deniedPermissions.add(permissions[index])
      }
    }

    if (deniedPermissions.isNullOrEmpty()) {
      callback?.invoke(true)
      authoriseCallback?.invoke(AuthoriseType.TYPE_GRANTED)
    } else {
      val deniedArray = deniedPermissions.toTypedArray()
      if (showRationaleSettingWhenDenied && hasAlwaysDeniedPermission(deniedArray)) {
        if (deniedArray.size == 1 && deniedArray[0] == android.Manifest.permission.REQUEST_INSTALL_PACKAGES) {
          val installPermission = arrayOf(android.Manifest.permission.REQUEST_INSTALL_PACKAGES)
          rationaleInstallPackagesSetting?.showRationale(context, installPermission) {
            if (!it) {
              deniedPermissionCallback?.invoke(installPermission)
            }
            callback?.invoke(it)
            authoriseCallback?.invoke(if (it) AuthoriseType.TYPE_SETTING_ALLOW else AuthoriseType.TYPE_SETTING_CANCELED)
          }
        } else {
          deniedPermissionCallback?.invoke(deniedArray)
          callback?.invoke(false)
          rationaleSetting?.showRationale(context, deniedArray) {
            authoriseCallback?.invoke(if (it) AuthoriseType.TYPE_SETTING_ALLOW else AuthoriseType.TYPE_SETTING_CANCELED)
          }
        }
      } else {
        deniedPermissionCallback?.invoke(deniedArray)
        callback?.invoke(false)
        authoriseCallback?.invoke(AuthoriseType.TYPE_DENIED)
      }
    }
  }

  /**
   * Has always been denied permissions.
   */
  private fun hasAlwaysDeniedPermission(deniedPermissions: Array<String>): Boolean {
    for (permission in deniedPermissions) {
      if (!isNeedShowRationalePermission(context, permission)) {
        return true
      }
    }
    return false
  }

  class Builder(private var context: Context) {
    private var rationale: Rationale? = null
    private var rationaleSetting: Rationale? = null
    private var rationaleInstallPackagesSetting: Rationale? = null
    private var callback: ((authorise: Boolean) -> Unit)? = null
    private var authoriseCallback: ((authorise: Int) -> Unit)? = null
    private var permissions = LinkedHashSet<String>()
    private var showRationaleSettingWhenDenied = true
    private var showRationaleWhenRequest = true
    private var deniedPermissionCallback: ((Array<String>) -> Unit)? = null

    /**
     * @param permission Requested permission is required
     */
    fun permission(permission: String): Builder {
      if (!permission.startsWith("android.permission") || permission != WRITE_VOICEMAIL) return this
      permissions.add(permission)
      return this
    }

    /**
     * @param permissions Requested permissions are required
     */
    fun permissions(permissions: Array<String>): Builder {
      val filtered = permissions.filter { it.startsWith("android.permission") || it == WRITE_VOICEMAIL }
      this.permissions.addAll(filtered)
      return this
    }

    /**
     * @param permissions Requested permissions are required
     */
    fun permissions(permissions: Collection<String>): Builder {
      val filtered = permissions.filter { it.startsWith("android.permission") || it == WRITE_VOICEMAIL }
      this.permissions.addAll(filtered)
      return this
    }

    /**
     * @param callback Authorization result callback, true was granted all, false otherwise
     */
    fun permissionCallback(callback: ((authorise: Boolean) -> Unit)? = null): Builder {
      this.callback = callback
      return this
    }

    /**
     * @param callback Authorization result callback, true was granted all, false otherwise
     */
    fun permissionAuthoriseCallback(authoriseCallback: ((authorise: Int) -> Unit)? = null): Builder {
      this.authoriseCallback = authoriseCallback
      return this
    }

    /**
     * The denied permission is returned through this callback
     * @param callback Returns permission to reject
     */
    fun deniedPermissionCallback(callback: ((Array<String>) -> Unit)? = null): Builder {
      this.deniedPermissionCallback = callback
      return this
    }

    /**
     * @param rationale Dialog box that prompts the user for authorization
     * @param showRationaleWhenRequest Show Permission dialog when requesting, default true
     */
    fun rationale(rationale: Rationale?, showRationaleWhenRequest: Boolean): Builder {
      this.rationale = rationale
      this.showRationaleWhenRequest = showRationaleWhenRequest
      return this
    }

    /**
     * @param rationaleSetting The Settings dialog box that guides the user to authorize
     * @param showRationaleSettingWhenDenied Show Settings dialog when permission denied, default true
     */
    fun rationaleSetting(rationaleSetting: Rationale?, showRationaleSettingWhenDenied: Boolean = true): Builder {
      this.rationaleSetting = rationaleSetting
      this.showRationaleSettingWhenDenied = showRationaleSettingWhenDenied
      return this
    }

    /**
     * It will only pop up when you request the permission of 'android.Manifest.permission.REQUEST_INSTALL_PACKAGES'
     *
     * @param rationaleInstallPackagesSetting The Settings dialog box that guides the user to authorize
     * @param showRationaleSettingWhenDenied Show Settings dialog when permission denied, default true
     */
    fun rationaleInstallPackagesSetting(rationaleInstallPackagesSetting: Rationale?, showRationaleSettingWhenDenied: Boolean = true): Builder {
      this.rationaleInstallPackagesSetting = rationaleInstallPackagesSetting
      this.showRationaleSettingWhenDenied = showRationaleSettingWhenDenied
      return this
    }

    fun build(): PermissionHelper {
      val permissionHelper = PermissionHelper(context)
      permissionHelper.permissions = permissions.toTypedArray()
      permissionHelper.callback = callback
      permissionHelper.rationale = rationale ?: DefaultRationale()
      permissionHelper.rationaleSetting = rationaleSetting ?: SettingRationale()
      permissionHelper.rationaleInstallPackagesSetting = rationaleInstallPackagesSetting
          ?: InstallPackagesSettingRationale()
      permissionHelper.showRationaleSettingWhenDenied = showRationaleSettingWhenDenied
      permissionHelper.showRationaleWhenRequest = showRationaleWhenRequest
      permissionHelper.deniedPermissionCallback = deniedPermissionCallback
      return permissionHelper
    }
  }

  companion object {

    private fun isNeedShowRationalePermission(context: Context, permission: String): Boolean {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
      val packageManager = context.packageManager
      val pkManagerClass = packageManager.javaClass
      return try {
        val method = pkManagerClass.getMethod("shouldShowRequestPermissionRationale", String::class.java)
        if (!method.isAccessible) method.isAccessible = true
        method.invoke(packageManager, permission) as Boolean? ?: false
      } catch (ignored: Exception) {
        false
      }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun getDeniedPermissions(context: Context, permissions: Array<String>?): Array<String> {
      val requestList = mutableListOf<String>()
      permissions?.forEach {
        if (context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) {
          requestList.add(it)
        }
      }
      return requestList.toTypedArray()
    }
  }
}