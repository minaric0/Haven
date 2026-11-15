package io.github.kennethchoinfosec.haven.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.github.kennethchoinfosec.haven.util.ApplicationInfoWrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AppListCompatibilityTest {
    @Test
    @SuppressWarnings("deprecation")
    public void appListsMatchOriginalPackageVisibilityAndSortOrder() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<IHavenService> service = new AtomicReference<>();
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                service.set(IHavenService.Stub.asInterface(binder));
                connected.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {}
        };
        assertTrue(context.bindService(new Intent(context, HavenService.class), connection,
                Context.BIND_AUTO_CREATE));
        try {
            assertTrue("Haven service did not bind", connected.await(15, TimeUnit.SECONDS));
            PackageManager pm = context.getPackageManager();
            int flags = PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_UNINSTALLED_PACKAGES;
            for (boolean showAll : new boolean[]{false, true}) {
                List<ApplicationInfoWrapper> original = new ArrayList<>();
                // Independent reference: the original per-package launch-intent lookup.
                // This test runs in the personal profile, where no apps are policy-hidden.
                for (ApplicationInfo info : pm.getInstalledApplications(flags)) {
                    if (info.packageName.equals(context.getPackageName())) {
                        continue;
                    }
                    if (showAll || ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                            && (info.flags & ApplicationInfo.FLAG_INSTALLED) != 0)
                            || pm.getLaunchIntentForPackage(info.packageName) != null) {
                        original.add(new ApplicationInfoWrapper(info).loadLabel(pm));
                    }
                }
                original.sort((a, b) -> a.getLabel().compareTo(b.getLabel()));
                CountDownLatch completed = new CountDownLatch(1);
                AtomicReference<List<ApplicationInfoWrapper>> result = new AtomicReference<>();
                service.get().getApps(new IGetAppsCallback.Stub() {
                    @Override
                    public void callback(List<ApplicationInfoWrapper> apps) {
                        result.set(apps);
                        completed.countDown();
                    }
                }, showAll);
                assertTrue("App list did not complete", completed.await(30, TimeUnit.SECONDS));
                assertEquals(packageNames(original), packageNames(result.get()));
            }
        } finally {
            context.unbindService(connection);
        }
    }

    private static List<String> packageNames(List<ApplicationInfoWrapper> apps) {
        List<String> packages = new ArrayList<>();
        for (ApplicationInfoWrapper app : apps) {
            packages.add(app.getPackageName());
        }
        return packages;
    }
}
