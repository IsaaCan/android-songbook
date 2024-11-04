package mwongela.songbook.activity

import androidx.appcompat.app.AppCompatActivity
import mwongela.songbook.inject.LazyExtractor
import mwongela.songbook.inject.LazyInject
import mwongela.songbook.inject.appFactory
import mwongela.songbook.system.PermissionService
import mwongela.songbook.system.SystemKeyDispatcher

/*
    Main Activity starter pack
    Workaround for reusing finished activities by Android
 */
class MainActivityData(
    appInitializer: LazyInject<AppInitializer> = appFactory.appInitializer,
    activityController: LazyInject<ActivityController> = appFactory.activityController,
    optionSelectDispatcher: LazyInject<OptionSelectDispatcher> = appFactory.optionSelectDispatcher,
    systemKeyDispatcher: LazyInject<SystemKeyDispatcher> = appFactory.systemKeyDispatcher,
    permissionService: LazyInject<PermissionService> = appFactory.permissionService,
    activityResultDispatcher: LazyInject<ActivityResultDispatcher> = appFactory.activityResultDispatcher,
) : AppCompatActivity() {
    val appInitializer by LazyExtractor(appInitializer)
    val activityController by LazyExtractor(activityController)
    val optionSelectDispatcher by LazyExtractor(optionSelectDispatcher)
    val systemKeyDispatcher by LazyExtractor(systemKeyDispatcher)
    val permissionService by LazyExtractor(permissionService)
    val activityResultDispatcher by LazyExtractor(activityResultDispatcher)
}