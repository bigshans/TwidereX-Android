/*
 *  Twidere X
 *
 *  Copyright (C) TwidereProject and Contributors
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

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.twidere.services.microblog.SearchService
import com.twidere.twiderex.defaultLoadCount
import com.twidere.twiderex.paging.source.SearchUserPagingSource
import com.twidere.twiderex.repository.AccountRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

class ListsSearchUserViewModel(
    private val accountRepository: AccountRepository,
    following: Boolean = false,
) : ViewModel() {
    private val account by lazy {
        accountRepository.activeAccount.mapNotNull { it }
    }

    val text = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val source = text.debounce(666L).filterNot { it.isEmpty() }.flatMapLatest {
        account.flatMapLatest { account ->
            Pager(
                config = PagingConfig(
                    pageSize = defaultLoadCount,
                    enablePlaceholders = false,
                )
            ) {
                SearchUserPagingSource(
                    accountKey = account.accountKey,
                    it,
                    account.service as SearchService,
                    following = following
                )
            }.flow
        }
    }.cachedIn(viewModelScope)
}
