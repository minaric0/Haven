package io.github.kennethchoinfosec.haven.ui;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.view.ContextThemeWrapper;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.kennethchoinfosec.haven.R;
import io.github.kennethchoinfosec.haven.services.IHavenService;
import io.github.kennethchoinfosec.haven.services.ILoadIconCallback;
import io.github.kennethchoinfosec.haven.util.ApplicationInfoWrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(AndroidJUnit4.class)
public class AppListAdapterTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final RecordingService service = new RecordingService();
    private AppListAdapter adapter;
    private AppListAdapter.ViewHolder holder;
    private Drawable placeholder;
    private Context context;

    @Before
    public void createAdapter() {
        instrumentation.runOnMainSync(() -> {
            context = new ContextThemeWrapper(InstrumentationRegistry.getTargetContext(), R.style.AppTheme);
            placeholder = new BitmapDrawable(context.getResources(), bitmap());
            adapter = new AppListAdapter(service, placeholder);
            adapter.setData(Arrays.asList(app("example.alpha", "Alpha"), app("example.beta", "Beta")));
            holder = adapter.onCreateViewHolder(new FrameLayout(context), 0);
        });
    }

    @Test
    public void createsNoRequestUntilBindingAndCoalescesDuplicateRequests() {
        instrumentation.runOnMainSync(() -> {
            assertEquals(0, service.callbacks.size());
            adapter.onBindViewHolder(holder, 0);
            adapter.onBindViewHolder(holder, 0);
            assertEquals(1, service.callbacks.size());
        });
    }

    @Test
    public void delayedIconDoesNotOverwriteReboundRowAndCacheAvoidsAnotherRequest() {
        Bitmap alpha = bitmap();
        Bitmap beta = bitmap();
        instrumentation.runOnMainSync(() -> {
            adapter.onBindViewHolder(holder, 0);
            complete(0, alpha);
            // The callback's UI work is queued. Recycle before it can run.
            adapter.onViewRecycled(holder);
            adapter.onBindViewHolder(holder, 1);
        });
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() -> {
            assertSame(placeholder, iconView().getDrawable());
            complete(1, beta);
        });
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() -> {
            assertSame(beta, ((BitmapDrawable) iconView().getDrawable()).getBitmap());
            adapter.onBindViewHolder(holder, 0);
            assertSame(alpha, ((BitmapDrawable) iconView().getDrawable()).getBitmap());
            assertEquals(2, service.callbacks.size());
        });
    }

    @Test
    public void refreshDiscardsCallbacksForOldPackageData() {
        Bitmap obsolete = bitmap();
        Bitmap current = bitmap();
        instrumentation.runOnMainSync(() -> {
            adapter.onBindViewHolder(holder, 0);
            adapter.setData(Arrays.asList(app("example.alpha", "Updated Alpha")));
            adapter.onBindViewHolder(holder, 0);
            complete(0, obsolete);
        });
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() -> {
            assertSame(placeholder, iconView().getDrawable());
            complete(1, current);
        });
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() ->
                assertSame(current, ((BitmapDrawable) iconView().getDrawable()).getBitmap()));
    }

    @Test
    public void searchesBothLabelAndPackageAndNormalizesRepeatedQueries() {
        instrumentation.runOnMainSync(() -> {
            adapter.setSearchQuery("  ALPHA  ");
            assertEquals(1, adapter.getItemCount());
            adapter.setSearchQuery("example.beta");
            assertEquals(1, adapter.getItemCount());
            adapter.onBindViewHolder(holder, 0);
            assertEquals("example.beta", ((android.widget.TextView) holder.itemView
                    .findViewById(R.id.list_app_package)).getText().toString());
            adapter.setSearchQuery("missing");
            assertEquals(0, adapter.getItemCount());
            adapter.setSearchQuery(" ");
            assertEquals(2, adapter.getItemCount());
        });
    }

    private ApplicationInfoWrapper app(String packageName, String label) {
        ApplicationInfo info = new ApplicationInfo();
        info.packageName = packageName;
        info.nonLocalizedLabel = label;
        return new ApplicationInfoWrapper(info).loadLabel(context.getPackageManager());
    }

    private ImageView iconView() {
        return holder.itemView.findViewById(R.id.list_app_icon);
    }

    private static Bitmap bitmap() {
        return Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
    }

    private void complete(int index, Bitmap icon) {
        try {
            service.callbacks.get(index).callback(icon);
        } catch (RemoteException e) {
            throw new AssertionError(e);
        }
    }

    private static class RecordingService extends IHavenService.Default {
        final List<ILoadIconCallback> callbacks = new ArrayList<>();

        @Override
        public void loadIcon(ApplicationInfoWrapper info, ILoadIconCallback callback) {
            callbacks.add(callback);
        }
    }
}
