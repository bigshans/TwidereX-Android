/*
 *  Twidere X
 *
 *  Copyright (C) 2020-2021 Tlaster <tlaster@outlook.com>
 * 
 *  This file is part of Twidere X.
 * 
 *  Twidere X is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  Twidere X is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Twidere X. If not, see <http://www.gnu.org/licenses/>.
 */
package com.twidere.twiderex

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.application
import com.twidere.twiderex.di.ext.get
import com.twidere.twiderex.di.setupModules
import com.twidere.twiderex.init.Initializer
import com.twidere.twiderex.init.TwidereServiceFactoryInitialTask
import com.twidere.twiderex.kmp.LocalPlatformWindow
import com.twidere.twiderex.kmp.PlatformWindow
import com.twidere.twiderex.preferences.PreferencesHolder
import com.twidere.twiderex.preferences.ProvidePreferences
import com.twidere.twiderex.utils.CustomTabSignInChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import moe.tlaster.kfilepicker.FilePicker
import moe.tlaster.precompose.PreComposeWindow
import moe.tlaster.precompose.navigation.NavController
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.awt.Desktop

fun DesktopApp() {
    startKoin {
        printLogger()
        setupModules()
    }
    val preferencesHolder = get<PreferencesHolder>()
    val navController = NavController()
    val scope = MainScope()
    try {
        Desktop.getDesktop().setOpenURIHandler { event ->
            println("onEvent:${event.uri}")
            onDeeplink(url = event.uri.toString(), navController = navController, scope = scope)
        }
    } catch (e: UnsupportedOperationException) {
        e.printStackTrace()
    }
    application {
        Initializer.withScope(rememberCoroutineScope())
            .add(TwidereServiceFactoryInitialTask())
            .execute()
        ProvidePreferences(preferencesHolder) {
            PreComposeWindow(
                onCloseRequest = {
                    stopKoin()
                    exitApplication()
                },
                title = "Twidere X"
            ) {
                FilePicker.init(window)
                CompositionLocalProvider(
                    LocalPlatformWindow provides PlatformWindow(),
                ) {
                    App(navController = navController)
                }
            }
        }
    }
}

private fun onDeeplink(url: String, navController: NavController, scope: CoroutineScope) {
    if (CustomTabSignInChannel.canHandle(url)) {
        scope.launch {
            CustomTabSignInChannel.send(url)
        }
    } else {
        navController.navigate(url)
    }
}
