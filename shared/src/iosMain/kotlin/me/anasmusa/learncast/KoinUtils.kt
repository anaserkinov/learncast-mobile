package me.anasmusa.learncast

import kotlinx.cinterop.BetaInteropApi
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import kotlin.reflect.KClass

object KoinUtils {
    private var koin: Koin? = null

    fun initKoin() =
        startKoin {
            modules(getModules())
        }.also {
            koin = it.koin
        }

    @OptIn(BetaInteropApi::class)
    fun <T> koinGet(
        kClass: KClass<*>,
        qualifier: Qualifier? = null,
        parameter: ParametersDefinition? = null,
    ): T = koin!!.get(kClass, qualifier, null)
}
