package app.gamenative.service

import app.gamenative.enums.LoginResult
import app.gamenative.enums.LoginScreen
import app.gamenative.ui.data.UserLoginState
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CompletableFuture

class SteamAuthenticator(var loginState : MutableStateFlow<UserLoginState>,
                         var submitChannel : Channel<String>,
                         var viewModelScope : CoroutineScope) : IAuthenticator {

    var useGuardTotp: Boolean = false

    override fun acceptDeviceConfirmation(): CompletableFuture<Boolean> {
        Timber.tag("UserLoginViewModel").i("Two-Factor, device confirmation")

        if (useGuardTotp) {
            return CompletableFuture.completedFuture(false)
        }

        loginState.update { currentState ->
            currentState.copy(
                loginResult = LoginResult.DeviceConfirm,
                loginScreen = LoginScreen.TWO_FACTOR,
                isLoggingIn = false,
                lastTwoFactorMethod = "steam_guard",
            )
        }

        return CompletableFuture.completedFuture(true)
    }

    override fun getDeviceCode(previousCodeWasIncorrect: Boolean): CompletableFuture<String> {
        Timber.tag("UserLoginViewModel").d("Two-Factor, device code")

        loginState.update { currentState ->
            currentState.copy(
                loginResult = LoginResult.DeviceAuth,
                loginScreen = LoginScreen.TWO_FACTOR,
                isLoggingIn = false,
                previousCodeIncorrect = previousCodeWasIncorrect,
                lastTwoFactorMethod = "authenticator_code",
            )
        }

        return CompletableFuture<String>().apply {
            viewModelScope.launch {
                val code = submitChannel.receive()
                complete(code)
            }
        }
    }

    override fun getEmailCode(
        email: String?,
        previousCodeWasIncorrect: Boolean,
    ): CompletableFuture<String> {
        Timber.tag("UserLoginViewModel").d("Two-Factor, asking for email code")

        loginState.update { currentState ->
            currentState.copy(
                loginResult = LoginResult.EmailAuth,
                loginScreen = LoginScreen.TWO_FACTOR,
                isLoggingIn = false,
                email = email,
                previousCodeIncorrect = previousCodeWasIncorrect,
                lastTwoFactorMethod = "email_code",
            )
        }

        return CompletableFuture<String>().apply {
            viewModelScope.launch {
                val code = submitChannel.receive()
                complete(code)
            }
        }
    }
}
