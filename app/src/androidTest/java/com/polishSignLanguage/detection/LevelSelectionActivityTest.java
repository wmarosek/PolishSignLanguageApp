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
public class LevelSelectionActivityTest {

    @Rule
    public ActivityScenarioRule<LevelSelectionActivity> scenario =
            new ActivityScenarioRule<LevelSelectionActivity>(LevelSelectionActivity.class);

    @Test
    public void componentsAreDisplayed(){
        onView(withId(R.id.imageView)).check(matches(isDisplayed()));
        onView(withId(R.id.beginner)).check(matches(isDisplayed()));
        onView(withId(R.id.intermediate)).check(matches(isDisplayed()));
        onView(withId(R.id.advanced)).check(matches(isDisplayed()));
    }

    Instrumentation.ActivityMonitor monitorBegginer = getInstrumentation()
            .addMonitor(LevelSelectionActivity.class.getName(), null, false);

    Instrumentation.ActivityMonitor monitorIntermediate = getInstrumentation()
            .addMonitor(LevelSelectionActivity.class.getName(), null, false);

    Instrumentation.ActivityMonitor monitorAdvance = getInstrumentation()
            .addMonitor(LevelSelectionActivity.class.getName(), null, false);

    @Test
    public void randomBegginerTest(){
        onView(withId(R.id.beginner)).perform(click());
        Activity secondActivity = getInstrumentation().waitForMonitorWithTimeout(monitorBegginer, 5000);
        assertNotNull(secondActivity);
    }
    @Test
    public void randomIntermediateTest(){
        onView(withId(R.id.intermediate)).perform(click());
        Activity secondActivity = getInstrumentation().waitForMonitorWithTimeout(monitorBegginer, 5000);
        assertNotNull(secondActivity);
    }
    @Test
    public void randomAdvanceTest(){
        onView(withId(R.id.advanced)).perform(click());
        Activity secondActivity = getInstrumentation().waitForMonitorWithTimeout(monitorBegginer, 5000);
        assertNotNull(secondActivity);
    }

}