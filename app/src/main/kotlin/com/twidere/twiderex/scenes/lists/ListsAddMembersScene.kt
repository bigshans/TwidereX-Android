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
package com.twidere.twiderex.scenes.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import com.twidere.twiderex.R
import com.twidere.twiderex.component.foundation.AppBar
import com.twidere.twiderex.component.foundation.AppBarDefaults
import com.twidere.twiderex.component.foundation.AppBarNavigationButton
import com.twidere.twiderex.component.foundation.InAppNotificationScaffold
import com.twidere.twiderex.component.foundation.SwipeToRefreshLayout
import com.twidere.twiderex.component.foundation.TextInput
import com.twidere.twiderex.component.lazy.collectAsLazyPagingItems
import com.twidere.twiderex.component.lazy.ui.LazyUiUserList
import com.twidere.twiderex.component.navigation.LocalNavigator
import com.twidere.twiderex.di.assisted.assistedViewModel
import com.twidere.twiderex.extensions.refreshOrRetry
import com.twidere.twiderex.extensions.viewModel
import com.twidere.twiderex.model.MicroBlogKey
import com.twidere.twiderex.model.ui.UiUser
import com.twidere.twiderex.ui.LocalActiveAccount
import com.twidere.twiderex.ui.TwidereScene
import com.twidere.twiderex.viewmodel.lists.ListsUserModifyViewModel
import com.twidere.twiderex.viewmodel.search.SearchUserViewModel
import kotlinx.coroutines.flow.flowOf

@Composable
fun ListsAddMembersScene(
    listKey: MicroBlogKey,
) {
    val account = LocalActiveAccount.current ?: return
    val viewModel = assistedViewModel<ListsUserModifyViewModel.AssistedFactory, ListsUserModifyViewModel>(
        account, listKey.id
    ) {
        it.create(account, listKey.id)
    }
    viewModel.toString()

    var keyword by rememberSaveable {
        mutableStateOf("")
    }

    TwidereScene {
        InAppNotificationScaffold(
            topBar = {
                Surface(elevation = AppBarDefaults.TopAppBarElevation) {
                    Column {
                        AppBar(
                            navigationIcon = {
                                AppBarNavigationButton(Icons.Default.Close)
                            },
                            title = {
                                Text(text = stringResource(id = R.string.scene_lists_users_add_title))
                            },
                            elevation = 0.dp,
                            actions = {
                                IconButton(
                                    enabled = viewModel.pendingMap.isNotEmpty(),
                                    onClick = {
                                        /*TODO Submit*/
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = stringResource(id = R.string.common_controls_actions_confirm),
                                        tint = if (viewModel.pendingMap.isNotEmpty()) MaterialTheme.colors.primary else LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                                    )
                                }
                            }
                        )
                        Row(
                            modifier = Modifier.padding(ListsAddMembersSceneDefaults.SearchInput.ContentPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = stringResource(id = R.string.scene_search_title),
                                modifier = Modifier.padding(ListsAddMembersSceneDefaults.SearchInput.Icon.Padding)
                            )
                            TextInput(
                                value = keyword,
                                placeholder = {
                                    Text(text = stringResource(id = R.string.scene_lists_users_add_search))
                                },
                                onValueChange = {
                                    keyword = it
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
        ) {
            Column {
                // search result
                SearchResultsContent(
                    keyword = keyword,
                    pendingList = viewModel.pendingMap.values.toMutableList(),
                    onAction = {
                        viewModel.addToOrRemoveFromPendingLists(it)
                    }
                ) {
                    viewModel.isInPendingList(it)
                }
            }
        }
    }
}

@Composable
private fun SearchResultsContent(keyword: String, pendingList: List<UiUser>, onAction: (user: UiUser) -> Unit, statusChecker: (user: UiUser) -> Boolean) {
    val account = LocalActiveAccount.current ?: return
    val viewModel = viewModel(
        account,
        keyword,
    ) {
        SearchUserViewModel(account, keyword)
    }
    val source = viewModel.source.collectAsLazyPagingItems()
    val navigator = LocalNavigator.current
    SwipeToRefreshLayout(
        refreshingState = source.loadState.refresh is LoadState.Loading,
        onRefresh = {
            source.refreshOrRetry()
        }
    ) {
        LazyUiUserList(
            items = if (keyword.isNotEmpty()) source else flowOf(PagingData.from(pendingList)).collectAsLazyPagingItems(),
            onItemClicked = { navigator.user(it) },
            action = {
                Column(
                    modifier = Modifier
                        .requiredHeight(IntrinsicSize.Max)
                        .padding(ListsAddMembersSceneDefaults.SearchContent.Action.ContentPadding)
                        .clickable {
                            onAction(it)
                        },
                    verticalArrangement = Arrangement.Center
                ) {
                    val pending = statusChecker(it)
                    if (pending) {
                        Text(
                            text = stringResource(id = R.string.scene_lists_users_menu_actions_remove),
                            style = MaterialTheme.typography.button,
                            color = Color(0xFFFF3B30),
                        )
                    } else {
                        Text(

                            text = stringResource(id = R.string.scene_lists_users_menu_actions_add),
                            style = MaterialTheme.typography.button,
                            color = MaterialTheme.colors.primary,
                        )
                    }
                }
            }
        )
    }
}

private object ListsAddMembersSceneDefaults {
    object SearchInput {
        val ContentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        )
        object Icon {
            val Padding = PaddingValues(end = 23.dp)
        }
    }
    object SearchContent {
        object Action {
            val ContentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
        }
    }
}
