package ie.setu.carkey.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun isAvailable(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFallback: () -> Unit
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // user cancelled or no biometric enrolled — fall back to password
                    onFallback()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("GoKey")
            .setSubtitle("Verify your identity to continue")
            .setNegativeButtonText("Use Password")
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(info)
    }
}
