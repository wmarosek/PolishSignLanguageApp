package com.polishSignLanguage.detection;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.app.Instrumentation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> scenario =
            new ActivityScenarioRule<MainActivity>(MainActivity.class);

    @Test
    public void componentsAreDisplayed(){
        onView(withId(R.id.textView)).check(matches(isDisplayed()));
        onView(withId(R.id.textView2)).check(matches(isDisplayed()));
        onView(withId(R.id.training)).check(matches(isDisplayed()));
        onView(withId(R.id.learning)).check(matches(isDisplayed()));
        onView(withId(R.id.imageView)).check(matches(isDisplayed()));
    }

    Instrumentation.ActivityMonitor monitorLearning = getInstrumentation()
            .addMonitor(LevelSelectionActivity.class.getName(), null, false);

    Instrumentation.ActivityMonitor monitorTraining = getInstrumentation()
            .addMonitor(DetectorActivity.class.getName(), null, false);

    @Test
    public void clickOnBtnLearning(){
        onView(withId(R.id.learning)).perform(click());
        Activity secondActivity = getInstrumentation().waitForMonitorWithTimeout(monitorLearning, 5000);
        assertNotNull(secondActivity);
    }

    @Test
    public void clickOnBtnTraining(){
        onView(withId(R.id.training)).perform(click());
        Activity secondActivity = getInstrumentation().waitForMonitorWithTimeout(monitorTraining, 5000);
        assertNotNull(secondActivity);
    }
}