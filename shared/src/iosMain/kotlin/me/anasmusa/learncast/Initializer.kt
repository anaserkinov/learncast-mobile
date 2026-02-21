import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import me.anasmusa.learncast.KoinUtils
import me.anasmusa.learncast.core.download.downloadManagerFactory
import me.anasmusa.learncast.data.network.TokenManager
import me.anasmusa.learncast.data.network.TokenProvider
import org.koin.mp.KoinPlatform

object Initializer {
    fun initApp(debug: Boolean) {
        if (debug) {
            Napier.base(DebugAntilog())
        }

        KoinUtils.initKoin()

        downloadManagerFactory.invoke().let {
            it.setTokenProvider(
                object : TokenProvider {
                    override fun getTokens(): Pair<String, String>? = runBlocking { KoinPlatform.getKoin().get<TokenManager>().getTokens() }

                    override fun refreshTokens(refreshToken: String) {
                        runBlocking {
                            KoinPlatform.getKoin().get<TokenManager>().refreshToken(refreshToken)
                        }
                    }
                },
            )
        }
    }
}
