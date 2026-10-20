@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.c030_prashamdamani_assignment2.navigation

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.c030_prashamdamani_assignment2.data.model.Task
import com.example.c030_prashamdamani_assignment2.data.prefs.SettingsStore
import com.example.c030_prashamdamani_assignment2.ui.auth.LoginScreen
import com.example.c030_prashamdamani_assignment2.ui.auth.RegisterScreen
import com.example.c030_prashamdamani_assignment2.ui.auth.SplashScreen
import com.example.c030_prashamdamani_assignment2.ui.calendar.CalendarScreen
import com.example.c030_prashamdamani_assignment2.ui.components.LocalReduceMotion
import com.example.c030_prashamdamani_assignment2.ui.components.TaskActions
import com.example.c030_prashamdamani_assignment2.ui.home.HomeScreen
import com.example.c030_prashamdamani_assignment2.ui.insights.InsightsScreen
import com.example.c030_prashamdamani_assignment2.ui.profile.ProfileScreen
import com.example.c030_prashamdamani_assignment2.ui.reminders.RemindersScreen
import com.example.c030_prashamdamani_assignment2.ui.tasks.TaskDetailScreen
import com.example.c030_prashamdamani_assignment2.ui.tasks.TaskFormScreen
import com.example.c030_prashamdamani_assignment2.ui.tasks.TasksScreen
import com.example.c030_prashamdamani_assignment2.ui.timer.TimerScreen
import com.example.c030_prashamdamani_assignment2.util.SortMode
import com.example.c030_prashamdamani_assignment2.util.rememberNotificationPermissionRequester
import com.example.c030_prashamdamani_assignment2.viewmodel.AuthViewModel
import com.example.c030_prashamdamani_assignment2.viewmodel.TaskViewModel
import com.example.c030_prashamdamani_assignment2.viewmodel.TimerViewModel
import kotlinx.coroutines.launch

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val TASKS = "tasks?filter={filter}"
    const val CALENDAR = "calendar"
    const val REMINDERS = "reminders"
    const val PROFILE = "profile"
    const val INSIGHTS = "insights"
    const val TIMER = "timer?taskId={taskId}"
    const val ADD = "add"
    const val EDIT = "edit/{id}"
    const val DETAIL = "detail/{id}"
    fun tasks(filter: String = "ACTIVE") = "tasks?filter=$filter"
    fun timer(taskId: String? = null) = if (taskId == null) "timer" else "timer?taskId=$taskId"
    fun edit(id: String) = "edit/$id"
    fun detail(id: String) = "detail/$id"
}

private data class BottomItem(val base: String, val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem("home", Routes.HOME, "Home", Icons.Default.Home),
    BottomItem("tasks", Routes.tasks(), "Tasks", Icons.Default.Checklist),
    BottomItem("calendar", Routes.CALENDAR, "Calendar", Icons.Default.CalendarMonth),
    BottomItem("reminders", Routes.REMINDERS, "Reminders", Icons.Default.Notifications),
    BottomItem("profile", Routes.PROFILE, "Profile", Icons.Default.Person)
)

@Composable
fun AppNavigation(openTaskId: String?, onOpenTaskConsumed: () -> Unit) {
    val ctx = LocalContext.current
    val nav = rememberNavController()
    val authVm: AuthViewModel = viewModel()
    val taskVm: TaskViewModel = viewModel()
    val timerVm: TimerViewModel = viewModel()
    val settings = remember { SettingsStore(ctx.applicationContext) }
    val scope = rememberCoroutineScope()

    val auth by authVm.state.collectAsStateWithLifecycle()
    val ui by taskVm.ui.collectAsStateWithLifecycle()
    val categories by taskVm.categories.collectAsStateWithLifecycle()
    val timer by timerVm.state.collectAsStateWithLifecycle()
    val reducePref by settings.reduceMotion.collectAsStateWithLifecycle(initialValue = false)
    val defaultSort by settings.defaultSort.collectAsStateWithLifecycle(initialValue = SortMode.DEFAULT)
    val systemNoAnim = remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val reduce = reducePref || systemNoAnim

    val profile = auth.profile
    val snackbar = remember { SnackbarHostState() }
    val requestNotif = rememberNotificationPermissionRequester()
    val pendingOpen by rememberUpdatedState(openTaskId)
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Load tasks for the signed-in user only; clears everything on logout.
    LaunchedEffect(auth.user?.uid) { taskVm.setUser(auth.user?.uid) }
    LaunchedEffect(auth.user?.uid) { if (auth.user != null) requestNotif() }

    // Session check -> login/home routing.
    LaunchedEffect(auth.checking, auth.user?.uid, currentRoute) {
        if (auth.checking) return@LaunchedEffect
        val route = currentRoute ?: return@LaunchedEffect // nav graph not attached yet
        if (auth.user == null) {
            if (route != Routes.LOGIN && route != Routes.REGISTER) {
                nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } }
            }
        } else if (route == Routes.SPLASH || route == Routes.LOGIN || route == Routes.REGISTER) {
            nav.navigate(Routes.HOME) { popUpTo(nav.graph.id) { inclusive = true } }
            pendingOpen?.let { nav.navigate(Routes.detail(it)); onOpenTaskConsumed() }
        }
    }
    // Notification tap while the app is already running.
    LaunchedEffect(openTaskId, auth.user?.uid, auth.checking, currentRoute) {
        val id = openTaskId ?: return@LaunchedEffect
        val route = currentRoute
        if (!auth.checking && auth.user != null && route != null && route != Routes.SPLASH && route != Routes.LOGIN && route != Routes.REGISTER) {
            nav.navigate(Routes.detail(id)); onOpenTaskConsumed()
        }
    }
    // Snackbars (with Undo for deletes).
    LaunchedEffect(Unit) {
        taskVm.events.collect { e ->
            val r = snackbar.showSnackbar(e.text, actionLabel = if (e.undoable) "Undo" else null, withDismissAction = true, duration = SnackbarDuration.Long)
            if (r == SnackbarResult.ActionPerformed && e.undoable) taskVm.undoDelete()
        }
    }

    val showBottom = currentRoute != null && bottomItems.any { currentRoute.startsWith(it.base) }
    val showFab = currentRoute != null && listOf("home", "tasks", "calendar", "reminders").any { currentRoute.startsWith(it) }

    val actions = TaskActions(
        onToggle = { taskVm.toggleComplete(it) },
        onDelete = { taskVm.deleteTask(it) },
        onOpen = { nav.navigate(Routes.detail(it.id)) },
        onEdit = { nav.navigate(Routes.edit(it.id)) }
    )

    CompositionLocalProvider(LocalReduceMotion provides reduce) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                if (showFab) FloatingActionButton(onClick = { nav.navigate(Routes.ADD) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add task")
                }
            },
            bottomBar = {
                if (showBottom) NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route?.startsWith(item.base) == true } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTop(nav, item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { pad ->
            val enter: EnterTransition = if (reduce) EnterTransition.None else fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 14 }
            val exit: ExitTransition = if (reduce) ExitTransition.None else fadeOut(tween(160))
            val popEnter: EnterTransition = if (reduce) EnterTransition.None else fadeIn(tween(220)) + slideInHorizontally(tween(260)) { -it / 14 }
            val popExit: ExitTransition = if (reduce) ExitTransition.None else fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { it / 14 }

            NavHost(
                navController = nav, startDestination = Routes.SPLASH, modifier = Modifier.padding(pad),
                enterTransition = { enter }, exitTransition = { exit }, popEnterTransition = { popEnter }, popExitTransition = { popExit }
            ) {
                composable(Routes.SPLASH) { SplashScreen() }
                composable(Routes.LOGIN) {
                    LoginScreen(auth, authVm::login, authVm::resetPassword, { nav.navigate(Routes.REGISTER) }, authVm::clearMessages)
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(auth, authVm::register, { nav.popBackStack() }, authVm::clearMessages)
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        profile = profile, ui = ui, actions = actions,
                        onOpenFilter = { nav.navigate(Routes.tasks(it)) },
                        onOpenReminders = { navigateTop(nav, Routes.REMINDERS) },
                        onOpenInsights = { nav.navigate(Routes.INSIGHTS) },
                        onQuickAdd = taskVm::quickAdd,
                        onAddTask = { nav.navigate(Routes.ADD) }
                    )
                }
                composable(
                    Routes.TASKS,
                    arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "ACTIVE" })
                ) { entry ->
                    TasksScreen(profile, ui, categories, entry.arguments?.getString("filter") ?: "ACTIVE", defaultSort, actions) { nav.navigate(Routes.ADD) }
                }
                composable(Routes.CALENDAR) {
                    CalendarScreen(profile, ui.tasks, ui.online, { nav.navigate(Routes.detail(it.id)) }, taskVm::reschedule)
                }
                composable(Routes.REMINDERS) {
                    RemindersScreen(
                        profile, ui, taskVm::setReminder,
                        { nav.navigate(Routes.detail(it.id)) }, { nav.navigate(Routes.edit(it.id)) }, requestNotif
                    )
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        profile = profile, reduceMotion = reducePref, defaultSort = defaultSort,
                        onReduceMotion = { v -> scope.launch { settings.setReduceMotion(v) } },
                        onDefaultSort = { m -> scope.launch { settings.setDefaultSort(m) } },
                        onUpdateProfile = authVm::updateProfile,
                        onOpenInsights = { nav.navigate(Routes.INSIGHTS) },
                        onOpenTimer = { nav.navigate(Routes.timer()) },
                        onLogout = { authVm.logout() }
                    )
                }
                composable(Routes.INSIGHTS) { InsightsScreen(profile, ui) { nav.popBackStack() } }
                composable(Routes.ADD) {
                    TaskFormScreen(null, profile, ui.tasks, categories, { taskVm.saveTask(it) { nav.popBackStack() } }, { nav.popBackStack() }, taskVm::addCategory, requestNotif)
                }
                composable(Routes.EDIT, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments?.getString("id")
                    val task = ui.tasks.firstOrNull { it.id == id }
                    when {
                        task != null -> TaskFormScreen(task, profile, ui.tasks, categories, { taskVm.saveTask(it) { nav.popBackStack() } }, { nav.popBackStack() }, taskVm::addCategory, requestNotif)
                        ui.loading -> com.example.c030_prashamdamani_assignment2.ui.components.LoadingBox()
                        else -> TaskDetailScreen(null, { nav.popBackStack() }, {}, {}, {}, {})
                    }
                }
                composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments?.getString("id")
                    val task = ui.tasks.firstOrNull { it.id == id }
                    if (task == null && ui.loading) com.example.c030_prashamdamani_assignment2.ui.components.LoadingBox()
                    else TaskDetailScreen(
                        task, { nav.popBackStack() }, { nav.navigate(Routes.edit(it.id)) }, taskVm::toggleComplete,
                        { taskVm.deleteTask(it); nav.popBackStack() },
                        { nav.navigate(Routes.timer(it.id)) }
                    )
                }
                composable(
                    Routes.TIMER,
                    arguments = listOf(navArgument("taskId") { type = NavType.StringType; nullable = true; defaultValue = null })
                ) { entry ->
                    val tid = entry.arguments?.getString("taskId")
                    TimerScreen(
                        profile = profile, state = timer, tasks = ui.tasks,
                        presetTask = ui.tasks.firstOrNull { it.id == tid },
                        onBack = { nav.popBackStack() },
                        onConfigure = { timerVm.configure(it) },
                        onLink = { t -> timerVm.link(t?.id, t?.title) },
                        onStart = timerVm::start, onPause = timerVm::pause, onReset = timerVm::reset
                    )
                }
            }
        }
    }
}

private fun navigateTop(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }
}
