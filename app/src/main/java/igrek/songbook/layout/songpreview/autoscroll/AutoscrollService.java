package igrek.songbook.layout.songpreview.autoscroll;

import android.os.Handler;

import javax.inject.Inject;

import dagger.Lazy;
import igrek.songbook.R;
import igrek.songbook.dagger.DaggerIoc;
import igrek.songbook.info.UiInfoService;
import igrek.songbook.info.UiResourceService;
import igrek.songbook.info.logger.Logger;
import igrek.songbook.info.logger.LoggerFactory;
import igrek.songbook.layout.songpreview.SongPreviewLayoutController;
import igrek.songbook.layout.songpreview.render.SongPreview;
import igrek.songbook.persistence.preferences.PreferencesDefinition;
import igrek.songbook.persistence.preferences.PreferencesService;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;

public class AutoscrollService {
	
	private final float MIN_SPEED = 0.001f;
	private final float START_NO_WAITING_MIN_SCROLL = 24.0f;
	private final float AUTOCHANGE_SPEED_SCALE = 0.002f;
	private final float ADD_INITIAL_PAUSE_SCALE = 400.0f;
	private final float AUTOSCROLL_INTERVAL_TIME = 70; // [ms]
	@Inject
	UiInfoService uiInfoService;
	@Inject
	Lazy<SongPreviewLayoutController> songPreviewController;
	@Inject
	UiResourceService uiResourceService;
	@Inject
	PreferencesService preferencesService;
	
	private Logger logger = LoggerFactory.getLogger();
	private AutoscrollState state;
	private long initialPause; // [ms]
	private float autoscrollSpeed; // [em / s]
	private long startTime; // [ms]
	
	private PublishSubject<Float> canvasScrollSubject = PublishSubject.create();
	private PublishSubject<AutoscrollState> scrollStateSubject = PublishSubject.create();
	private PublishSubject<Float> scrollSpeedSubject = PublishSubject.create();
	
	private Handler timerHandler = new Handler();
	private Runnable timerRunnable = () -> {
		if (state == AutoscrollState.OFF)
			return;
		handleAutoscrollStep();
	};
	
	public AutoscrollService() {
		DaggerIoc.getFactoryComponent().inject(this);
		loadPreferences();
		reset();
		
		canvasScrollSubject.observeOn(AndroidSchedulers.mainThread())
				.subscribe(linePartScrolled -> {
					onCanvasScrollEvent(linePartScrolled, getCanvas().getScroll());
				});
	}
	
	private void loadPreferences() {
		initialPause = preferencesService.getValue(PreferencesDefinition.autoscrollInitialPause, Long.class);
		autoscrollSpeed = preferencesService.getValue(PreferencesDefinition.autoscrollSpeed, Float.class);
	}
	
	public void reset() {
		stop();
	}
	
	public void start() {
		float scroll = getCanvas().getScroll();
		if (scroll <= START_NO_WAITING_MIN_SCROLL) {
			start(true);
		} else {
			start(false);
		}
	}
	
	private void start(boolean withWaiting) {
		if (isRunning()) {
			stop();
		}
		if (getCanvas().canScrollDown()) {
			if (withWaiting) {
				state = AutoscrollState.WAITING;
			} else {
				state = AutoscrollState.SCROLLING;
			}
			startTime = System.currentTimeMillis();
			timerHandler.postDelayed(timerRunnable, 0);
		}
		scrollStateSubject.onNext(state);
	}
	
	public void stop() {
		state = AutoscrollState.OFF;
		timerHandler.removeCallbacks(timerRunnable);
		scrollStateSubject.onNext(state);
	}
	
	public boolean isRunning() {
		return state == AutoscrollState.WAITING || state == AutoscrollState.SCROLLING;
	}
	
	private SongPreview getCanvas() {
		return songPreviewController.get().getSongPreview();
	}
	
	private void handleAutoscrollStep() {
		if (state == AutoscrollState.WAITING) {
			long remainingTimeMs = initialPause + startTime - System.currentTimeMillis();
			if (remainingTimeMs <= 0) {
				state = AutoscrollState.SCROLLING;
				timerHandler.postDelayed(timerRunnable, 0);
				onAutoscrollStartedEvent();
			} else {
				long delay = remainingTimeMs > 1000 ? 1000 : remainingTimeMs; // cut off over 1000
				timerHandler.postDelayed(timerRunnable, delay);
				onAutoscrollRemainingWaitTimeEvent(remainingTimeMs);
			}
		} else if (state == AutoscrollState.SCROLLING) {
			// em = speed * time
			float lineheightPart = autoscrollSpeed * AUTOSCROLL_INTERVAL_TIME / 1000;
			if (getCanvas().scrollByLines(lineheightPart)) {
				// scroll once again later
				timerHandler.postDelayed(timerRunnable, (long) AUTOSCROLL_INTERVAL_TIME);
			} else {
				// scroll has come to an end
				stop();
				onAutoscrollEndedEvent();
			}
		}
	}
	
	/**
	 * @param dScroll line Part Scrolled
	 * @param scroll  current scroll
	 */
	public void onCanvasScrollEvent(float dScroll, float scroll) {
		if (state == AutoscrollState.WAITING) {
			if (dScroll > 0) { // skip counting down immediately
				skipInitialPause();
			} else if (dScroll < 0) { // increase inital waitng time
				startTime -= (long) (dScroll * ADD_INITIAL_PAUSE_SCALE);
				long remainingTimeMs = initialPause + startTime - System.currentTimeMillis();
				onAutoscrollRemainingWaitTimeEvent(remainingTimeMs);
			}
		} else if (state == AutoscrollState.SCROLLING) {
			if (dScroll > 0) { // speed up scrolling
				
				autoscrollSpeed += dScroll * AUTOCHANGE_SPEED_SCALE;
				
			} else if (dScroll < 0) {
				if (scroll <= 0) { // scrolling up to the beginning
					// set counting down state with additional time
					state = AutoscrollState.WAITING;
					startTime = System.currentTimeMillis() - initialPause - (long) (dScroll * ADD_INITIAL_PAUSE_SCALE);
					long remainingTimeMs = initialPause + startTime - System.currentTimeMillis();
					onAutoscrollRemainingWaitTimeEvent(remainingTimeMs);
					return;
				} else {
					// slow down scrolling
					float dScrollAbs = -dScroll;
					autoscrollSpeed -= dScrollAbs * AUTOCHANGE_SPEED_SCALE;
				}
			}
			if (autoscrollSpeed < MIN_SPEED)
				autoscrollSpeed = MIN_SPEED;
			
			scrollSpeedSubject.onNext(autoscrollSpeed);
			logger.info("new autoscroll speed: " + autoscrollSpeed + " em / s");
		}
	}
	
	private void onAutoscrollRemainingWaitTimeEvent(long ms) {
		String seconds = Long.toString((ms + 500) / 1000);
		String info = uiResourceService.resString(R.string.autoscroll_starts_in, seconds);
		uiInfoService.showInfoWithAction(info, R.string.action_start_now_autoscroll, this::skipInitialPause);
	}
	
	private void skipInitialPause() {
		state = AutoscrollState.SCROLLING;
		uiInfoService.clearSnackBars();
		onAutoscrollStartedEvent();
	}
	
	public void onAutoscrollStartUIEvent() {
		if (!isRunning()) {
			if (getCanvas().canScrollDown()) {
				start();
				uiInfoService.showInfoWithAction(R.string.autoscroll_started, R.string.action_stop_autoscroll, this::stop);
			} else {
				uiInfoService.showInfo(uiResourceService.resString(R.string.end_of_song_autoscroll_stopped));
			}
		} else {
			onAutoscrollStopUIEvent();
		}
	}
	
	private void onAutoscrollStartedEvent() {
		uiInfoService.showInfoWithAction(R.string.autoscroll_started, R.string.action_stop_autoscroll, this::stop);
	}
	
	private void onAutoscrollEndedEvent() {
		uiInfoService.showInfo(uiResourceService.resString(R.string.end_of_song_autoscroll_stopped));
	}
	
	public void onAutoscrollStopUIEvent() {
		if (isRunning()) {
			stop();
			uiInfoService.showInfo(R.string.autoscroll_stopped);
		}
	}
	
	public void onAutoscrollToggleUIEvent() {
		if (isRunning()) {
			onAutoscrollStopUIEvent();
		} else {
			onAutoscrollStartUIEvent();
		}
	}
	
	public long getInitialPause() {
		return initialPause;
	}
	
	public float getAutoscrollSpeed() {
		return autoscrollSpeed;
	}
	
	public void setInitialPause(long initialPause) {
		this.initialPause = initialPause;
	}
	
	public void setAutoscrollSpeed(float autoscrollSpeed) {
		this.autoscrollSpeed = autoscrollSpeed;
		scrollSpeedSubject.onNext(autoscrollSpeed);
	}
	
	public PublishSubject<Float> getCanvasScrollSubject() {
		return canvasScrollSubject;
	}
	
	public PublishSubject<AutoscrollState> getScrollStateSubject() {
		return scrollStateSubject;
	}
	
	public PublishSubject<Float> getScrollSpeedSubject() {
		return scrollSpeedSubject;
	}
}
