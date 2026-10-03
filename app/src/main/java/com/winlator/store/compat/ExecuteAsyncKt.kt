@file:JvmName("ExecuteAsyncKt")

/**
 * okhttp3.coroutines.executeAsync 兼容实现
 *
 * JavaSteam 的 Depot 下载器引用了 okhttp3.coroutines.ExecuteAsyncKt，
 * 但 OkHttp 4.9.3 不包含 okhttp-coroutines artifact。
 * 使用 suspendCoroutine（kotlin-stdlib 原生提供）实现，
 * 避免对 kotlinx-coroutines 版本的依赖。
 */
package okhttp3.coroutines

import kotlin.coroutines.suspendCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

@Suppress("RedundantSuspendModifier")
suspend fun Call.executeAsync(): Response = suspendCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            cont.resumeWith(Result.success(response))
        }
        override fun onFailure(call: Call, e: IOException) {
            cont.resumeWith(Result.failure(e))
        }
    })
}
