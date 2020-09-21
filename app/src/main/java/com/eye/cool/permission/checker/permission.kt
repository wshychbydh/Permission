package com.eye.cool.permission.checker

import android.content.Context
import androidx.fragment.app.Fragment
import com.eye.cool.permission.PermissionChecker
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.suspendCoroutine

/**
 * Created by ycb on 2020/8/31
 */

/**
 * @return run on ui-thread
 */
suspend fun Context.permissionForResult(
    permission: String
) = suspendCancellableCoroutine<Result> {
  PermissionChecker(
      Request.Builder(this).permission(permission).build()
  ).check(it)
}

/**
 * @return run on ui-thread
 */
suspend fun Context.permissionForResult(
    permissions: Array<String>
) = suspendCancellableCoroutine<Result> {
  PermissionChecker(
      Request.Builder(this).permissions(permissions).build()
  ).check(it)
}

/**
 * @return run on ui-thread
 */
suspend fun Context.permissionForResult(
    permissions: Collection<String>
) = suspendCancellableCoroutine<Result> {
  PermissionChecker(
      Request.Builder(this).permissions(permissions).build()
  ).check(it)
}

/**
 * @return run on ui-thread
 */
suspend fun Fragment.permissionForResult(
    permission: String
) = requireActivity().permissionForResult(permission)

/**
 * @return run on ui-thread
 */
suspend fun Fragment.permissionForResult(
    permissions: Array<String>
) = requireActivity().permissionForResult(permissions)

/**
 * @return run on ui-thread
 */
suspend fun Fragment.permissionForResult(
    permissions: Collection<String>
) = requireActivity().permissionForResult(permissions)

/**
 * @return run on ui-thread
 */
suspend fun permissionForResult(
    builder: Request
) = suspendCancellableCoroutine<Result> {
  PermissionChecker(builder).check(it)
}