package igrek.songbook.dagger;

import javax.inject.Singleton;

import dagger.Component;
import igrek.songbook.activity.MainActivity;
import igrek.songbook.service.activity.ActivityController;
import igrek.songbook.service.activity.AppInitializer;
import igrek.songbook.service.activity.OptionSelectDispatcher;
import igrek.songbook.service.autoscroll.AutoscrollService;
import igrek.songbook.service.chords.ChordsManager;
import igrek.songbook.service.filesystem.ExternalCardService;
import igrek.songbook.service.filesystem.FilesystemService;
import igrek.songbook.service.filetree.FileTreeManager;
import igrek.songbook.service.filetree.ScrollPosBuffer;
import igrek.songbook.service.info.UIResourceService;
import igrek.songbook.service.info.UserInfoService;
import igrek.songbook.service.layout.LayoutController;
import igrek.songbook.service.layout.songpreview.SongPreviewController;
import igrek.songbook.service.layout.songselection.SongSelectionController;
import igrek.songbook.service.preferences.PreferencesService;
import igrek.songbook.service.transpose.ChordsTransposer;
import igrek.songbook.service.window.SoftKeyboardService;
import igrek.songbook.service.window.WindowManagerService;
import igrek.songbook.ui.canvas.CanvasGraphics;
import igrek.songbook.ui.canvas.quickmenu.QuickMenu;
import igrek.songbook.ui.errorcheck.UIErrorHandler;

/**
 * Dagger will be injecting to those classes
 */
@Singleton
@Component(modules = {FactoryModule.class})
public interface FactoryComponent {
	
	void inject(MainActivity there);
	
	/* Services */
	void inject(ExternalCardService there);
	
	void inject(FilesystemService there);
	
	void inject(AppInitializer there);
	
	void inject(ActivityController there);
	
	void inject(WindowManagerService there);
	
	void inject(OptionSelectDispatcher there);
	
	void inject(UIResourceService there);
	
	void inject(UserInfoService there);
	
	void inject(AutoscrollService there);
	
	void inject(ChordsManager there);
	
	void inject(FileTreeManager there);
	
	void inject(PreferencesService there);
	
	void inject(ChordsTransposer there);
	
	void inject(SoftKeyboardService there);
	
	void inject(LayoutController there);
	
	void inject(SongSelectionController there);
	
	void inject(SongPreviewController there);
	
	void inject(ScrollPosBuffer there);
	
	
	void inject(UIErrorHandler there);
	
	void inject(QuickMenu there);
	
	void inject(CanvasGraphics there);
	
}