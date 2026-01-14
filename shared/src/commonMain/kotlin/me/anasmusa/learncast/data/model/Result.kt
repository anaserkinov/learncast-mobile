@file:OptIn(ExperimentalContracts::class)

package me.anasmusa.learncast.data.model

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed class Result<out T> {
    data class Success<out T>(
        val data: T,
    ) : Result<T>()

    data class Fail(
        val message: String,
        val tag: String? = null,
    ) : Result<Nothing>()

    fun getOrNull() =
        if (this is Success) {
            data
        } else {
            null
        }
}

inline fun <T> Result<T>.fold(
    onSuccess: (data: T?) -> Unit,
    onFailure: (message: String, tag: String?) -> Unit,
) {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    if (this is Result.Success) {
        onSuccess(data)
    } else if (this is Result.Fail) {
        onFailure(message, tag)
    }
}

inline fun <T> Result<T>.onSuccess(onSuccess: (data: T) -> Unit) {
    if (this is Result.Success) {
        onSuccess(data)
    }
}

inline fun <T> Result<T>.onFailure(onFail: (message: String, tag: String?) -> Unit) {
    if (this is Result.Fail) {
        onFail(message, tag)
    }
}
