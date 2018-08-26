package igrek.songbook.service.layout.songselection;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import igrek.songbook.R;
import igrek.songbook.dagger.DaggerIoc;
import igrek.songbook.domain.exception.NoParentDirException;
import igrek.songbook.logger.Logger;
import igrek.songbook.logger.LoggerFactory;
import igrek.songbook.service.activity.ActivityController;
import igrek.songbook.service.filetree.FileItem;
import igrek.songbook.service.filetree.FileTreeManager;
import igrek.songbook.service.filetree.ScrollPosBuffer;
import igrek.songbook.service.info.UIResourceService;
import igrek.songbook.service.info.UserInfoService;
import igrek.songbook.service.layout.LayoutController;
import igrek.songbook.service.layout.LayoutState;
import igrek.songbook.service.preferences.PreferencesDefinition;
import igrek.songbook.service.preferences.PreferencesService;
import igrek.songbook.service.window.WindowManagerService;
import igrek.songbook.view.filelist.FileListView;

public class SongSelectionController {
	
	@Inject
	FileTreeManager fileTreeManager;
	@Inject
	Lazy<ActivityController> activityController;
	@Inject
	LayoutController layoutController;
	@Inject
	WindowManagerService windowManagerService;
	@Inject
	PreferencesService preferencesService;
	@Inject
	UserInfoService userInfoService;
	@Inject
	ScrollPosBuffer scrollPosBuffer;
	@Inject
	UIResourceService uiResourceService;
	@Inject
	AppCompatActivity activity;
	@Inject
	Lazy<SongSelectionController> songSelectionController;
	
	private Logger logger = LoggerFactory.getLogger();
	private ActionBar actionBar;
	private FileListView itemsListView;
	
	public SongSelectionController() {
		DaggerIoc.getFactoryComponent().inject(this);
	}
	
	public void showFileList() {
		String currentDir = fileTreeManager.getCurrentDirName();
		List<FileItem> items = fileTreeManager.getItems();
		
		windowManagerService.setFullscreenLocked(false);
		
		activity.setContentView(R.layout.files_list);
		
		//toolbar
		Toolbar toolbar1 = activity.findViewById(R.id.toolbar1);
		activity.setSupportActionBar(toolbar1);
		actionBar = activity.getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setDisplayShowHomeEnabled(true);
		}
		toolbar1.setNavigationOnClickListener(v -> {
			songSelectionController.get().onToolbarBackClickedEvent();
		});
		
		//		userInfo.setMainView(activity.findViewById(R.id.mainLayout));
		
		itemsListView = activity.findViewById(R.id.filesList);
		
		itemsListView.init(activity);
		
		updateFileList(currentDir, items);
	}
	
	public void updateFileList(String currentDir, List<FileItem> items) {
		setTitle(currentDir);
		//lista elementów
		itemsListView.setItems(items);
	}
	
	public void scrollToItem(int position) {
		itemsListView.scrollTo(position);
	}
	
	public void setTitle(String title) {
		actionBar.setTitle(title);
	}
	
	public Integer getCurrentScrollPos() {
		return itemsListView.getCurrentScrollPosition();
	}
	
	public void scrollToPosition(int y) {
		itemsListView.scrollToPosition(y);
	}
	
	public void goUp() {
		try {
			fileTreeManager.goUp();
			updateFileList();
			//scrollowanie do ostatnio otwartego folderu
			restoreScrollPosition(fileTreeManager.getCurrentPath());
		} catch (NoParentDirException e) {
			activityController.get().quit();
		}
	}
	
	private void updateFileList() {
		updateFileList(fileTreeManager.getCurrentDirName(), fileTreeManager.getItems());
		layoutController.setState(LayoutState.SONG_LIST);
	}
	
	private void showFileContent(String filename) {
		layoutController.setState(LayoutState.SONG_PREVIEW);
		fileTreeManager.setCurrentFileName(filename);
		layoutController.showFileContent();
		windowManagerService.keepScreenOn();
	}
	
	private String getHomePath() {
		return preferencesService.getValue(PreferencesDefinition.startPath, String.class);
	}
	
	private boolean isInHomeDir() {
		return fileTreeManager.getCurrentPath().equals(FileTreeManager.trimEndSlash(getHomePath()));
	}
	
	public void homeClicked() {
		if (isInHomeDir()) {
			activityController.get().quit();
		} else {
			String homePath = getHomePath();
			if (homePath == null || homePath.isEmpty() || !new File(homePath).isDirectory()) {
				userInfoService.showInfo(R.string.message_home_not_set);
			}else {
				fileTreeManager.goTo(homePath);
				updateFileList();
			}
		}
	}
	
	public void setHomePath() {
		String homeDir = fileTreeManager.getCurrentPath();
		preferencesService.setValue(PreferencesDefinition.startPath, homeDir);
		preferencesService.saveAll();
		userInfoService.showInfo(R.string.starting_directory_saved, R.string.action_info_ok);
	}
	
	
	public void restoreScrollPosition(String path) {
		Integer savedScrollPos = scrollPosBuffer.restoreScrollPosition(path);
		if (savedScrollPos != null) {
			scrollToPosition(savedScrollPos);
		}
	}
	
	public void showUIHelp() {
		String message = uiResourceService.resString(R.string.ui_help_content);
		String title = uiResourceService.resString(R.string.ui_help);
		userInfoService.showDialog(title, message);
	}
	
	public void onToolbarBackClickedEvent() {
		goUp();
	}
	
	public void onItemClickedEvent(int posistion, FileItem item) {
		scrollPosBuffer.storeScrollPosition(fileTreeManager.getCurrentPath(), getCurrentScrollPos());
		if (item.isDirectory()) {
			fileTreeManager.goInto(item.getName());
			updateFileList();
			scrollToItem(0);
		} else {
			showFileContent(item.getName());
		}
	}
	
	
}
