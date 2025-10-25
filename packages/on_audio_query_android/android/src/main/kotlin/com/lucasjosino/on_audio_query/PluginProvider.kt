package com.lucasjosino.on_audio_query

import android.app.Activity
import android.content.Context
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.lang.ref.WeakReference

/**
 * A singleton used to define all variables/methods that will be used on all plugin.
 *
 * The singleton will provider the ability to 'request' required variables/methods on any moment.
 *
 * All variables/methods should be defined after plugin initialization (activity/context) and
 * dart request (call/result).
 */
object PluginProvider {
    private const val ERROR_MESSAGE =
        "Tried to get one of the methods but the 'PluginProvider' has not initialized"

    /**
    * Define if 'warn' level will show more detailed logging.
    *
    * Will be used when a query produce some error.
    */
    var showDetailedLog: Boolean = false

    private lateinit var context: WeakReference<Context>

    private lateinit var activity: WeakReference<Activity>

    private lateinit var currentCall: MethodCall

    private lateinit var currentResult: MethodChannel.Result

    /**
     * Used to define the current [Activity] and [Context].
     *
     * Should be defined once.
     */
    fun set(activity: Activity) {
        this.context = WeakReference(activity.applicationContext)
        this.activity = WeakReference(activity)
    }

    /**
     * Used to define the current dart request.
     *
     * Should be defined/redefined on every [MethodChannel.MethodCallHandler.onMethodCall] request.
     */
    fun setCurrentMethod(call: MethodCall, result: MethodChannel.Result) {
        this.currentCall = call
        this.currentResult = result
    }

    /**
     * The current plugin 'context'. Defined once.
     *
     * @throws UninitializedPluginProviderException
     * @return [Context]
     */
    fun context(): Context {
        return this.context.get() ?: throw UninitializedPluginProviderException(ERROR_MESSAGE)
    }

    /**
     * The current plugin 'activity'. Defined once.
     *
     * @throws UninitializedPluginProviderException
     * @return [Activity]
     */
    fun activity(): Activity {
        return this.activity.get() ?: throw UninitializedPluginProviderException(ERROR_MESSAGE)
    }

    /**
     * The current plugin 'call'. Will be replace with newest dart request.
     *
     * @throws UninitializedPluginProviderException
     * @return [MethodCall]
     */
    fun call(): MethodCall {
        if (!::currentCall.isInitialized) throw UninitializedPluginProviderException(ERROR_MESSAGE)
        return this.currentCall
    }

    /**
     * The current plugin 'result'. Will be replace with newest dart request.
     *
     * @throws UninitializedPluginProviderException
     * @return [MethodChannel.Result]
     */
    fun result(): MethodChannel.Result {
        if (!::currentResult.isInitialized) throw UninitializedPluginProviderException(ERROR_MESSAGE)
        return this.currentResult
    }

    class UninitializedPluginProviderException(msg: String) : Exception(msg)
}
