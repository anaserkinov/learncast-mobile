import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL

object Initializer {
    fun initApp() {
//        println("Initializer: Starting setup...")
//        FIRApp.configure()
//        println("Initializer: Firebase project ID: ${FIRApp.defaultApp()?.options!!.projectID()}")
        Napier.base(DebugAntilog())
    }

    @OptIn(ExperimentalForeignApi::class)
    fun googleHandleUrl(url: NSURL): Boolean {
//        return GIDSignIn.sharedInstance.handleURL(url)
        return true
    }
}
