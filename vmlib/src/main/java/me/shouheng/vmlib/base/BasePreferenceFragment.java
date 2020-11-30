package me.shouheng.vmlib.base;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.umeng.analytics.MobclickAgent;

import java.lang.reflect.ParameterizedType;

import me.shouheng.utils.app.ResUtils;
import me.shouheng.utils.permission.Permission;
import me.shouheng.utils.permission.PermissionUtils;
import me.shouheng.utils.permission.callback.OnGetPermissionCallback;
import me.shouheng.utils.stability.L;
import me.shouheng.utils.ui.ToastUtils;
import me.shouheng.vmlib.Platform;
import me.shouheng.vmlib.anno.FragmentConfiguration;
import me.shouheng.vmlib.anno.UmengConfiguration;
import me.shouheng.vmlib.bus.Bus;

/**
 * base preference fragment for mvvm
 *
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2019-10-02 13:15
 */
public abstract class BasePreferenceFragment<U extends BaseViewModel> extends PreferenceFragment  {

    private U vm;

    private boolean useEventBus;

    /** Grouped values with {@link FragmentConfiguration#umeng()}. */
    private String pageName;

    private boolean useUmengManual = false;

    /**
     * Get preferences resources id.
     *
     * @return preferences resources id
     */
    @XmlRes
    protected abstract int getPreferencesResId();

    {
        FragmentConfiguration configuration = this.getClass().getAnnotation(FragmentConfiguration.class);
        if (configuration != null) {
            useEventBus = configuration.useEventBus();
            UmengConfiguration umengConfiguration = configuration.umeng();
            pageName = TextUtils.isEmpty(umengConfiguration.pageName()) ?
                    getClass().getSimpleName() : umengConfiguration.pageName();
            useUmengManual = umengConfiguration.useUmengManual();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (useEventBus) {
            Bus.get().register(this);
        }
        vm = createViewModel();
        super.onCreate(savedInstanceState);
        int preferencesResId = getPreferencesResId();
        if (preferencesResId <= 0) {
            throw new IllegalArgumentException("The subclass must provider a valid preference resources id.");
        }
        addPreferencesFromResource(preferencesResId);
        doCreateView(savedInstanceState);
    }

    /**
     * Do create view business.
     *
     * @param savedInstanceState the saved instance state.
     */
    protected abstract void doCreateView(@Nullable Bundle savedInstanceState);

    /**
     * Initialize view model according to the generic class type. Override this method to
     * add your owen implementation.
     *
     * Add {@link FragmentConfiguration} to the subclass and set {@link FragmentConfiguration#shareViewModel()} true
     * if you want to share view model between several fragments.
     *
     * @return the view model instance.
     */
    protected U createViewModel() {
        Class<U> vmClass = ((Class)((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
        return ViewModelProviders.of((FragmentActivity) getActivity()).get(vmClass);
    }

    protected U getVM() {
        return vm;
    }

    /**
     * Make a simple toast.
     *
     * @param text the content to display
     */
    protected void toast(final CharSequence text) {
        ToastUtils.showShort(text);
    }

    protected void toast(@StringRes final int resId) {
        ToastUtils.showShort(resId);
    }

    /**
     * Post one event by Bus
     *
     * @param event the event to post
     */
    protected void post(Object event) {
        Bus.get().post(event);
    }

    /**
     * Post one sticky event by Bus
     *
     * @param event the sticky event
     */
    protected void postSticky(Object event) {
        Bus.get().postSticky(event);
    }

    /**
     * Check single permission. For multiple permissions at the same time, call
     * {@link #check(OnGetPermissionCallback, int...)}.
     *
     * @param permission the permission to check
     * @param onGetPermissionCallback the callback when got the required permission
     */
    protected void check(@Permission int permission, OnGetPermissionCallback onGetPermissionCallback) {
        if (getActivity() instanceof CommonActivity) {
            PermissionUtils.checkPermissions((CommonActivity) getActivity(), onGetPermissionCallback, permission);
        } else {
            L.i("Request permission failed due to the associated activity was not instance of CommonActivity");
        }
    }

    /**
     * Check multiple permissions at the same time.
     *
     * @param onGetPermissionCallback the callback when got all permissions required.
     * @param permissions the permissions to request.
     */
    protected void check(OnGetPermissionCallback onGetPermissionCallback, @Permission int...permissions) {
        if (getActivity() instanceof CommonActivity) {
            PermissionUtils.checkPermissions((CommonActivity) getActivity(), onGetPermissionCallback, permissions);
        } else {
            L.i("Request permissions failed due to the associated activity was not instance of CommonActivity");
        }
    }

    /**
     * Find preference from string resource key.
     *
     * @param keyRes preference key resources
     * @return       preference
     */
    protected <T extends Preference> T f(@StringRes int keyRes) {
        return (T) findPreference(ResUtils.getString(keyRes));
    }

    protected <T extends Preference> T f(CharSequence key) {
        return (T) findPreference(key);
    }

    /** Get support fragment manager */
    protected FragmentManager sfm() {
        if (getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportFragmentManager();
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (useUmengManual && Platform.DEPENDENCY_UMENG_ANALYTICS) {
            MobclickAgent.onPageStart(pageName);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (useUmengManual && Platform.DEPENDENCY_UMENG_ANALYTICS) {
            MobclickAgent.onPageEnd(pageName);
        }
    }

    @Override
    public void onDestroy() {
        if (useEventBus) {
            Bus.get().unregister(this);
        }
        super.onDestroy();
    }
}
