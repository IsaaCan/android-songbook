package igrek.songbook.dagger;

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import javax.inject.Inject;

import igrek.songbook.BuildConfig;
import igrek.songbook.MainApplication;
import igrek.songbook.dagger.base.DaggerTestComponent;
import igrek.songbook.dagger.base.TestComponent;
import igrek.songbook.dagger.base.TestModule;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = MainApplication.class)
public class DaggerInjectionTest {
	
	@Inject
	Activity activity;
	
	@Before
	public void setUp() {
		MainApplication application = (MainApplication) RuntimeEnvironment.application;
		
		TestComponent component = DaggerTestComponent.builder()
				.factoryModule(new TestModule(application))
				.build();
		
		DaggerIoc.setFactoryComponent(component);
		
		component.inject(this);
	}
	
	@Test
	public void testApplicationInjection() {
		assertThat(activity).isNotNull();
		System.out.println("injected activity: " + activity.toString());
	}
	
}
