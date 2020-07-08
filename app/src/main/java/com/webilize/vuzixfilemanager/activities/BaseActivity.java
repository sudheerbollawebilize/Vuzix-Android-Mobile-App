package com.webilize.vuzixfilemanager.activities;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.webilize.vuzixfilemanager.R;
import com.webilize.vuzixfilemanager.utils.StaticUtils;

public abstract class BaseActivity extends AppCompatActivity {

    private boolean sDisableFragmentAnimations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initComponents();
    }

    abstract void initComponents();

    public void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    public void changeStatusBarColorToAppColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    public void replaceFragment(Fragment fragment, boolean needToAddToBackStack) {
        StaticUtils.hideSoftKeyboard(this);
        String tag = fragment.getClass().getSimpleName();
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        setCustomAnimation(fragmentTransaction, false);
        if (needToAddToBackStack)
            fragmentTransaction.replace(R.id.mainFrame, fragment, tag).addToBackStack(tag).commitAllowingStateLoss();
        else
            fragmentTransaction.replace(R.id.mainFrame, fragment, tag).commitAllowingStateLoss();
    }

    public void replaceFragment(Fragment fragment, boolean needToAddToBackStack, boolean sharedTransition) {
        StaticUtils.hideSoftKeyboard(this);
        String tag = fragment.getClass().getSimpleName();
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        setCustomAnimation(fragmentTransaction, false);
        if (needToAddToBackStack)
            fragmentTransaction.replace(R.id.mainFrame, fragment, tag).addToBackStack(tag).commitAllowingStateLoss();
        else
            fragmentTransaction.replace(R.id.mainFrame, fragment, tag).commitAllowingStateLoss();
    }

    public void replaceFragment(Fragment fragment) {
        StaticUtils.hideSoftKeyboard(this);
        String tag = fragment.getClass().getSimpleName();
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        setCustomAnimation(fragmentTransaction, false);
        fragmentTransaction.replace(R.id.mainFrame, fragment, tag).addToBackStack(tag).commitAllowingStateLoss();
    }

    public void clearAndReplaceFragment(Fragment fragment) {
        clearBackStack();
        StaticUtils.hideSoftKeyboard(this);
        String tag = fragment.getClass().getSimpleName();
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        setCustomAnimation(fragmentTransaction, false);
        fragmentTransaction.replace(R.id.mainFrame, fragment, tag).addToBackStack(tag).commitAllowingStateLoss();
    }

    public void replaceFragment(Fragment fragment, boolean needToAddToBackStack, int containerId) {
        StaticUtils.hideSoftKeyboard(this);
        String tag = fragment.getClass().getSimpleName();
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        setCustomAnimation(fragmentTransaction, false);
        if (needToAddToBackStack)
            fragmentTransaction.replace(containerId, fragment, tag).addToBackStack(tag).commitAllowingStateLoss();
        else
            fragmentTransaction.replace(containerId, fragment, tag).commitAllowingStateLoss();
    }

    public void addFragment(Fragment fragment, boolean needToAddToBackStack) {
        addFragment(fragment, needToAddToBackStack, R.id.mainFrame);
    }

    public void addFragment(Fragment fragment, boolean needToAddToBackStack, int containerId) {
        StaticUtils.hideSoftKeyboard(this);
        String tag = fragment.getClass().getSimpleName();
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        setCustomAnimation(fragmentTransaction, false);
        if (needToAddToBackStack)
            fragmentTransaction.add(containerId, fragment, tag).addToBackStack(tag).commit();
        else
            fragmentTransaction.add(containerId, fragment, tag).commit();
    }

    public void replaceFragment(Fragment fragment, int containerId) {
        StaticUtils.hideSoftKeyboard(this);
        String tag = fragment.getClass().getSimpleName();
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        setCustomAnimation(fragmentTransaction, false);
        fragmentTransaction.replace(containerId, fragment, tag).addToBackStack(tag).commitAllowingStateLoss();
    }

    public void replaceFragmentWithoutAnimation(Fragment fragment, boolean needToAdd) {
        replaceFragmentWithoutAnimation(fragment, R.id.mainFrame, needToAdd);
    }

    public void replaceFragmentWithoutAnimation(Fragment fragment, int containerId, boolean needToAdd) {
        StaticUtils.hideSoftKeyboard(this);
        String tag = fragment.getClass().getSimpleName();
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        sDisableFragmentAnimations = true;
//        setCustomAnimation(fragmentTransaction, false);
        if (needToAdd)
            fragmentTransaction.replace(containerId, fragment, tag).addToBackStack(tag).commitAllowingStateLoss();
        else
            fragmentTransaction.replace(containerId, fragment, tag).commitAllowingStateLoss();
        sDisableFragmentAnimations = false;
    }

    public void replaceFragmentWithBottomToTopAnim(Fragment fragment, int containerId) {
        StaticUtils.hideSoftKeyboard(this);
        String tag = fragment.getClass().getSimpleName();
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        setTopToBottomAnimation(fragmentTransaction, true);
        fragmentTransaction.add(containerId, fragment, tag).addToBackStack(tag).commit();
    }

    public void clearBackStack() {
        FragmentManager fragment = getSupportFragmentManager();
        fragment.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public void clearBackStackCompletely() {
        sDisableFragmentAnimations = true;
        FragmentManager fragment = getSupportFragmentManager();
        fragment.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        sDisableFragmentAnimations = false;
    }

    public void popBackStack() {
        StaticUtils.hideSoftKeyboard(this);
        FragmentManager fragment = getSupportFragmentManager();
        setCustomAnimation(fragment.beginTransaction(), true);
        fragment.popBackStackImmediate();
    }

    private static void setCustomAnimation(FragmentTransaction ft, boolean reverseAnimation) {
        if (!reverseAnimation) {
            ft.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right);
        } else {
            ft.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
        }
    }

    private static void setTopToBottomAnimation(FragmentTransaction ft, boolean bottomToTop) {
        if (bottomToTop) {
//            ft.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom, R.anim.slide_in_top, R.anim.slide_out_bottom);
        } else {
//            ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top, R.anim.slide_in_top, R.anim.slide_out_bottom);
        }
    }

}
