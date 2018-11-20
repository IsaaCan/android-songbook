package igrek.songbook.settings;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.Lazy;
import igrek.songbook.R;
import igrek.songbook.activity.ActivityController;
import igrek.songbook.dagger.DaggerIoc;
import igrek.songbook.info.UiInfoService;
import igrek.songbook.info.UiResourceService;
import igrek.songbook.info.logger.Logger;
import igrek.songbook.info.logger.LoggerFactory;
import igrek.songbook.layout.LayoutController;
import igrek.songbook.layout.LayoutState;
import igrek.songbook.layout.MainLayout;
import igrek.songbook.layout.navigation.NavigationMenuController;
import igrek.songbook.layout.view.SliderController;
import igrek.songbook.model.chords.ChordsNotation;
import igrek.songbook.persistence.preferences.PreferencesService;
import igrek.songbook.songpreview.LyricsManager;
import igrek.songbook.songpreview.autoscroll.AutoscrollService;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;

public class SettingsLayoutController implements MainLayout {
	
	@Inject
	Lazy<ActivityController> activityController;
	@Inject
	LayoutController layoutController;
	@Inject
	UiInfoService uiInfoService;
	@Inject
	UiResourceService uiResourceService;
	@Inject
	AppCompatActivity activity;
	@Inject
	NavigationMenuController navigationMenuController;
	@Inject
	LyricsManager lyricsManager;
	@Inject
	AutoscrollService autoscrollService;
	@Inject
	PreferencesService preferencesService;
	
	private SliderController fontsizeSlider;
	private SliderController autoscrollPauseSlider;
	private SliderController autoscrollSpeedSlider;
	
	private Spinner chordsNotationSpinner;
	private ChordsNotation currentChordsNotation;
	PublishSubject<ChordsNotation> chordsNotationSubject = PublishSubject.create();
	
	private Logger logger = LoggerFactory.getLogger();
	
	public SettingsLayoutController() {
		DaggerIoc.getFactoryComponent().inject(this);
	}
	
	@Override
	public void showLayout(View layout) {
		// Toolbar
		Toolbar toolbar1 = layout.findViewById(R.id.toolbar1);
		activity.setSupportActionBar(toolbar1);
		ActionBar actionBar = activity.getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setDisplayShowHomeEnabled(false);
		}
		// navigation menu button
		ImageButton navMenuButton = layout.findViewById(R.id.navMenuButton);
		navMenuButton.setOnClickListener((v) -> navigationMenuController.navDrawerShow());
		
		SeekBar fontsizeSeekbar = layout.findViewById(R.id.fontsizeSeekbar);
		TextView fontsizeLabel = layout.findViewById(R.id.fontsizeLabel);
		SeekBar autoscrollPauseSeekbar = layout.findViewById(R.id.autoscrollPauseSeekbar);
		TextView autoscrollPauseLabel = layout.findViewById(R.id.autoscrollPauseLabel);
		SeekBar autoscrollSpeedSeekbar = layout.findViewById(R.id.autoscrollSpeedSeekbar);
		TextView autoscrollSpeedLabel = layout.findViewById(R.id.autoscrollSpeedLabel);
		
		float fontsize = lyricsManager.getFontsize();
		fontsizeSlider = new SliderController(fontsizeSeekbar, fontsizeLabel, fontsize, 5, 100) {
			@Override
			public String generateLabelText(float value) {
				return uiResourceService.resString(R.string.settings_font_size, roundDecimal(value, "#.#"));
			}
		};
		
		float autoscrollInitialPause = autoscrollService.getInitialPause();
		autoscrollPauseSlider = new SliderController(autoscrollPauseSeekbar, autoscrollPauseLabel, autoscrollInitialPause, 0, 90000) {
			@Override
			public String generateLabelText(float value) {
				return uiResourceService.resString(R.string.settings_scroll_initial_pause, msToS(value));
			}
		};
		
		float autoscrollSpeed = autoscrollService.getAutoscrollSpeed();
		autoscrollSpeedSlider = new SliderController(autoscrollSpeedSeekbar, autoscrollSpeedLabel, autoscrollSpeed, 0, 1.0f) {
			@Override
			public String generateLabelText(float value) {
				return uiResourceService.resString(R.string.settings_autoscroll_speed, roundDecimal(value, "#.####"));
			}
		};
		
		// chords notation
		currentChordsNotation = lyricsManager.getChordsNotation();
		chordsNotationSpinner = layout.findViewById(R.id.chordsNotationSpinner);
		ChordsNotationAdapter adapter = new ChordsNotationAdapter(activity, ChordsNotation.values(), uiResourceService);
		chordsNotationSpinner.setAdapter(adapter);
		chordsNotationSpinner.setSelection(adapter.getPosition(currentChordsNotation));
		chordsNotationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				onChordsNotationSelected(position);
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		Observable.merge(fontsizeSlider.getValueSubject(), autoscrollPauseSlider.getValueSubject(), autoscrollSpeedSlider
				.getValueSubject(), chordsNotationSubject)
				.debounce(200, TimeUnit.MILLISECONDS)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(value -> saveSettings());
		
	}
	
	private void onChordsNotationSelected(int position) {
		Object selectedItem = chordsNotationSpinner.getSelectedItem();
		if (selectedItem != null) {
			currentChordsNotation = (ChordsNotation) selectedItem;
		}
		chordsNotationSubject.onNext(currentChordsNotation);
	}
	
	private String msToS(float ms) {
		return Integer.toString((int) ((ms + 500) / 1000));
	}
	
	private String roundDecimal(float f, String format) {
		DecimalFormat df = new DecimalFormat(format);
		df.setRoundingMode(RoundingMode.HALF_UP);
		return df.format(f);
	}
	
	private void saveSettings() {
		float fontsize = fontsizeSlider.getValue();
		lyricsManager.setFontsize(fontsize);
		
		float autoscrollInitialPause = autoscrollPauseSlider.getValue();
		autoscrollService.setInitialPause((int) autoscrollInitialPause);
		float autoscrollSpeed = autoscrollSpeedSlider.getValue();
		autoscrollService.setAutoscrollSpeed(autoscrollSpeed);
		
		Object selectedItem = chordsNotationSpinner.getSelectedItem();
		if (selectedItem != null) {
			currentChordsNotation = (ChordsNotation) selectedItem;
			lyricsManager.setChordsNotation(currentChordsNotation);
		}
		
		// do not save to preferences service, they will be saved on activity stop
	}
	
	@Override
	public LayoutState getLayoutState() {
		return LayoutState.SETTINGS;
	}
	
	@Override
	public int getLayoutResourceId() {
		return R.layout.settings;
	}
	
	@Override
	public void onBackClicked() {
		layoutController.showLastSongSelectionLayout();
	}
	
	@Override
	public void onLayoutExit() {
	}
}