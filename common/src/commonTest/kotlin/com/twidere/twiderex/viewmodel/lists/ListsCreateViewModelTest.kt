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
package com.twidere.twiderex.viewmodel.lists

import com.twidere.twiderex.mock.Observer
import com.twidere.twiderex.mock.db.MockCacheDatabase
import com.twidere.twiderex.mock.service.MockListsService
import com.twidere.twiderex.model.AccountDetails
import com.twidere.twiderex.model.MicroBlogKey
import com.twidere.twiderex.notification.InAppNotification
import com.twidere.twiderex.notification.NotificationEvent
import com.twidere.twiderex.repository.AccountRepository
import com.twidere.twiderex.repository.ListsRepository
import com.twidere.twiderex.viewmodel.ViewModelTestBase
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ListsCreateViewModelTest : ViewModelTestBase() {

    private lateinit var mockRepository: ListsRepository

    @MockK
    private lateinit var mockAppNotification: InAppNotification

    @MockK
    private lateinit var mockAccountRepository: AccountRepository

    private val mockAccount: AccountDetails = mockk {
        every { service }.returns(MockListsService())
        every { accountKey }.returns(MicroBlogKey.twitter("123"))
    }

    @MockK
    private lateinit var mockSuccessObserver: Observer<Boolean>

    @MockK
    private lateinit var mockLoadingObserver: Observer<Boolean>

    private var errorNotification: NotificationEvent? = null

    private lateinit var createViewModel: ListsCreateViewModel

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun setUp() {
        super.setUp()
        every { mockAccountRepository.activeAccount }.returns(flowOf(mockAccount))
        mockRepository = ListsRepository(MockCacheDatabase())
        createViewModel = ListsCreateViewModel(
            mockAppNotification,
            mockRepository,
            mockAccountRepository
        ) { success, _ ->
            mockSuccessObserver.onChanged(success)
        }
        every { mockAppNotification.show(any()) }.answers {
            errorNotification = arg(0)
        }
        errorNotification = null
        mockSuccessObserver.onChanged(false)
        scope.launch {
            createViewModel.loading.collect {
                mockLoadingObserver.onChanged(it)
            }
        }
    }

    @Test
    fun createList_successExpectTrue(): Unit = runBlocking(Dispatchers.Main) {
        verifySuccessAndLoadingBefore(mockLoadingObserver, mockSuccessObserver)
        async {
            createViewModel.createList(title = "title", private = false)
        }.await()
        verifySuccessAndLoadingAfter(mockLoadingObserver, mockSuccessObserver, true)
    }

    @Test
    fun createList_failedExpectFalseAndShowNotification(): Unit = runBlocking(Dispatchers.Main) {
        verifySuccessAndLoadingBefore(mockLoadingObserver, mockSuccessObserver)
        assertNull(errorNotification)
        async {
            createViewModel.createList(title = "error", private = false)
        }.await()
        verifySuccessAndLoadingAfter(mockLoadingObserver, mockSuccessObserver, false)
        assertNotNull(errorNotification)
    }

    private fun verifySuccessAndLoadingBefore(
        loadingObserver: Observer<Boolean>,
        successObserver: Observer<Boolean>
    ) {
        verify(exactly = 1) { loadingObserver.onChanged(false) }
        verify { successObserver.onChanged(false) }
    }

    private fun verifySuccessAndLoadingAfter(
        loadingObserver: Observer<Boolean>,
        successObserver: Observer<Boolean>,
        success: Boolean
    ) {
        verify(exactly = 1) { loadingObserver.onChanged(true) }
        verify(exactly = 1) { loadingObserver.onChanged(false) }
        verify(exactly = if (success) 1 else 2) { successObserver.onChanged(success) }
    }
}
