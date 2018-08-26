package igrek.songbook.service.layout.songselection;

import java.io.File;

import javax.inject.Inject;

import igrek.songbook.dagger.DaggerIoc;
import igrek.songbook.service.filesystem.ExternalCardService;
import igrek.songbook.service.preferences.PreferencesDefinition;
import igrek.songbook.service.preferences.PreferencesService;

import static igrek.songbook.service.filesystem.FilesystemService.trimEndSlash;

public class HomePathService {
	
	@Inject
	PreferencesService preferencesService;
	@Inject
	ExternalCardService externalCardService;
	
	public HomePathService() {
		DaggerIoc.getFactoryComponent().inject(this);
	}
	
	public String getHomePath() {
		String homePath = preferencesService.getValue(PreferencesDefinition.homePath, String.class);
		if (homePath == null || homePath.isEmpty() || !new File(homePath).isDirectory())
			return null;
		return homePath;
	}
	
	/**
	 * @return start path is home path but if it doesn't exist it is the default path
	 */
	public String getStartPath() {
		String startPath = getHomePath();
		if (startPath == null)
			startPath = externalCardService.getExternalSDPath();
		if (!new File(startPath).isDirectory())
			startPath = "/";
		return startPath;
	}
	
	public void setHomePath(String path) {
		//String homeDir = ;
		preferencesService.setValue(PreferencesDefinition.homePath, path);
		preferencesService.saveAll();
	}
	
	public boolean isInHomeDir(String path) {
		String homePath = getHomePath();
		if (path == null || homePath == null)
			return false;
		return trimEndSlash(path).equals(trimEndSlash(homePath));
	}
}
