package io.github.kennethchoinfosec.haven.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.kennethchoinfosec.haven.R;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SettingsActivityTest {
    @Test
    public void settingsOpensAndSurvivesRecreationWithOneFragment() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(InstrumentationRegistry.getTargetContext(), SettingsActivity.class);
        // Opening settings does not use the service until a policy is changed.
        intent.putExtra("extras", new Bundle());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SettingsActivity first = (SettingsActivity) instrumentation.startActivitySync(intent);
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() -> assertSettings(first));

        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                SettingsActivity.class.getName(), null, false);
        Activity recreated = null;
        try {
            instrumentation.runOnMainSync(first::recreate);
            recreated = instrumentation.waitForMonitorWithTimeout(monitor, 10000);
            assertNotNull("Settings activity was not recreated", recreated);
            instrumentation.waitForIdleSync();
            SettingsActivity restored = (SettingsActivity) recreated;
            instrumentation.runOnMainSync(() -> assertSettings(restored));
        } finally {
            instrumentation.removeMonitor(monitor);
            Activity last = recreated == null ? first : recreated;
            instrumentation.runOnMainSync(last::finish);
        }
    }

    private static void assertSettings(SettingsActivity activity) {
        assertNotNull(activity.findViewById(R.id.settings_container));
        assertTrue(activity.getSupportFragmentManager().findFragmentById(R.id.settings_container)
                instanceof SettingsFragment);
        assertEquals(1, activity.getSupportFragmentManager().getFragments().size());
    }
}
