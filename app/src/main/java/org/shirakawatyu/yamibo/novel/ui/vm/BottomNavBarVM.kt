package org.shirakawatyu.yamibo.novel.ui.vm

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.shirakawatyu.yamibo.novel.ui.state.BottomNavBarState

class BottomNavBarVM : ViewModel() {
    private val _uiState = MutableStateFlow(BottomNavBarState())
    val uiState = _uiState.asStateFlow()
    private val pageList = listOf("MangaHomePage", "FavoritePage", "BBSPage", "MinePage")
    private val _refreshingRoutes = MutableStateFlow<Set<String>>(emptySet())
    val refreshingRoutes = _refreshingRoutes.asStateFlow()
    var isBbsAtRoot by mutableStateOf(true)
    var isMineAtRoot by mutableStateOf(true)
    var showBottomNavBar by mutableStateOf(true)
        private set
    private val _refreshEvent = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val refreshEvent = _refreshEvent.asSharedFlow()
    private val _goHomeEvent = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val goHomeEvent = _goHomeEvent.asSharedFlow()

    fun triggerRefresh(route: String, delayMillis: Long = 0L) {
        _refreshingRoutes.update { it + route }
        viewModelScope.launch {
            if (delayMillis > 0L) kotlinx.coroutines.delay(delayMillis)
            _refreshEvent.emit(route)
            kotlinx.coroutines.delay(15_000L)
            finishRefresh(route)
        }
    }

    fun finishRefresh(route: String) {
        _refreshingRoutes.update { it - route }
    }

    fun returnToHome(
        index: Int,
        currentRoute: String?,
        navController: NavController,
        notifyHome: Boolean = true
    ) {
        if (index < 0 || index >= pageList.size) {
            Log.e("BottomNavBarVM", "Invalid navigation index $index")
            return
        }
        val targetRoute = pageList[index]
        val routeChanged = currentRoute != targetRoute
        if (routeChanged) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
        if (!notifyHome) return
        if (routeChanged) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(64L)
                _goHomeEvent.emit(targetRoute)
            }
        } else {
            _goHomeEvent.tryEmit(targetRoute)
        }
    }

    fun setBottomNavBarVisibility(visible: Boolean) {
        showBottomNavBar = visible
    }
}
