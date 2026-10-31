package com.ivy.data.di

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KtorClientModule {
    // Exchange rates are a few hundred KB from a CDN; anything slower than this is treated as
    // offline, so a bad connection can't leave requests hanging.
    private const val CONNECT_TIMEOUT_MS = 10_000L
    private const val SOCKET_TIMEOUT_MS = 15_000L
    private const val REQUEST_TIMEOUT_MS = 30_000L

    private val SENSITIVE_HEADERS = setOf(
        HttpHeaders.Authorization,
        HttpHeaders.Cookie,
        HttpHeaders.SetCookie,
        HttpHeaders.ProxyAuthorization,
    )

    @Provides
    @Singleton
    fun provideKtorClient(
        json: Json,
        @ApplicationContext context: Context,
    ): HttpClient {
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return HttpClient { configure(json, debuggable) }
    }

    internal fun HttpClientConfig<*>.configure(json: Json, debuggable: Boolean) {
        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }

        // Debug builds only, and without bodies: responses can be large and are never needed
        // in release builds (which plant no Timber tree anyway). Credential headers are masked
        // in case a request ever carries one (today's requests are anonymous CDN fetches).
        if (debuggable) {
            install(Logging) {
                level = LogLevel.HEADERS
                sanitizeHeader { it in SENSITIVE_HEADERS }
                logger = object : Logger {
                    override fun log(message: String) {
                        Timber.d(message)
                    }
                }
            }
        }
    }
}
