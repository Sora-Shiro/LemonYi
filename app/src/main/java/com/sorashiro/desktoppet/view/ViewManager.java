/*
 * Copyright (C) 2017 Sora Shiro (https://github.com/Sora-Shiro)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sorashiro.desktoppet.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import com.sorashiro.desktoppet.R;
import com.sorashiro.desktoppet.tool.LogAndToastUtil;
import com.sorashiro.desktoppet.tool.ScreenUtil;

import java.util.ArrayList;
import java.util.Random;

public class ViewManager implements PetView.PetViewEvent {

    private static ViewManager instance = null;

    private WindowManager                         mWindowManager;
    private Context                               mContext;
    private ArrayList<PetView>                    mPetViews;
    private ArrayList<WindowManager.LayoutParams> mLayoutParams;

    private ViewManager(Context context) {
        mContext = context;
        init();
    }

    public static ViewManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ViewManager.class) {
                if (instance == null) {
                    instance = new ViewManager(context);
                }
            }
        }
        return instance;
    }

    private void init() {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mPetViews = new ArrayList<>();
        mLayoutParams = new ArrayList<>();
    }


    public void showPetView() {
        if (mPetViews.size() >= 11) {
            LogAndToastUtil.ToastOut(mContext, mContext.getString(R.string.too_much_lemonyi));
            return;
        } else if(mPetViews.size() == 1) {
            LogAndToastUtil.ToastOut(mContext, mContext.getString(R.string.carefully));
        }

        final PetView petView = new PetView(mContext);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.width = petView.getInitWidth();
        params.height = petView.getInitHeight();
        params.gravity = Gravity.TOP | Gravity.START;
        if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Android 8.0 == API26, Android 8.1 same
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= 24) {
            // Android 7.0 == API24, which need to use other type
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_TOAST;
        }

        // FLAG_LAYOUT_NO_LIMITS
        // Window flag: allow window to extend outside of the screen.
        // window大小不再不受手机屏幕大小限制，即window可能超出屏幕之外，这时部分内容在屏幕之外。
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        params.format = PixelFormat.RGBA_8888;
        // Fall down begin
        params.y = -ScreenUtil.getStatusBarHeight(mContext) - petView.getInitHeight();
        // Random fall down pet
        Random random = new Random();
        int seed = ScreenUtil.getScreenWidth(mContext) - petView.getInitHeight();
        seed = seed < 0 ? 0 : seed;
        int rX = random.nextInt(seed);
        params.x = rX;
        petView.setIsLeft(random.nextBoolean());

        View.OnTouchListener onTouchListener = new View.OnTouchListener() {

            float startX;
            float startY;
            float tempX;
            float tempY;

            // Acceleration
            VelocityTracker mVelocityTracker;
            int mMaxVelocity = ViewConfiguration.get(mContext).getScaledMaximumFlingVelocity();
            int mPointerId;

            private void releaseVelocityTracker() {
                if (null != mVelocityTracker) {
                    mVelocityTracker.clear();
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                PetView view = (PetView) v;
                // Struggle will make the current finger be invalid,
                // and ACTION_DOWN can reset it
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    view.setCanTouched(true);
                }
                if(!view.isCanTouched()) {
                    return true;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mPointerId = event.getPointerId(0);
                        mVelocityTracker = VelocityTracker.obtain();
                        petView.setLegBehavior(true);
                        ObjectAnimator animator = view.getObjectAnimator();
                        if (animator != null) {
                            animator.cancel();
                        }
                        startX = event.getRawX();
                        startY = event.getRawY();
                        tempX = event.getRawX();
                        tempY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mVelocityTracker.addMovement(event);
                        float x = event.getRawX() - startX;
                        float y = event.getRawY() - startY;
                        // Magic Formula!! With this, Pet will never be far away from finger
                        // when you move your finger fastly!!
                        params.x = (int) (event.getRawX()) - 160;
                        params.y = (int) (event.getRawY()) - 190;
//                        params.x += x;
//                        params.y += y;
                        if (petView.checkHitYBorder(false)) {
                            // Use VelocityTracker to check swing
                            mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
                            float velocityX = mVelocityTracker.getXVelocity(mPointerId);
                            if (velocityX > 500) {
                                petView.setStatus(PetView.SWING_LEFT);
                            } else if (velocityX < -500) {
                                petView.setStatus(PetView.SWING_RIGHT);
                            } else {
                                if (petView.getStatus() == PetView.SWING_LEFT) {
                                    petView.setStatus(PetView.SWING_LEFT_BACK);
                                    petView.sendMyDelayMessage(PetView.DRAG, PetView.CHANGE_CHECK, 500);
                                } else if (petView.getStatus() == PetView.SWING_RIGHT) {
                                    petView.setStatus(PetView.SWING_RIGHT_BACK);
                                    petView.sendMyDelayMessage(PetView.DRAG, PetView.CHANGE_CHECK, 500);
                                } else if (petView.getStatus() != PetView.SWING_LEFT_BACK &&
                                        petView.getStatus() != PetView.SWING_RIGHT_BACK) {
                                    petView.setStatus(PetView.DRAG);
                                }
                            }
                        }
                        mWindowManager.updateViewLayout(petView, params);
                        startX = event.getRawX();
                        startY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_UP:
                        float endX = event.getRawX();
                        float endY = event.getRawY();
                        if (petView.checkHitYBorder(true)) {
                            // Acc Check
                            if (params.y > ScreenUtil.getScreenHeight(mContext) - view.getInitHeight() - ScreenUtil.getStatusBarHeight(mContext)) {
                                petView.setStatus(PetView.SINK);
                            } else {
                                mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
                                float velocityX = mVelocityTracker.getXVelocity(mPointerId);
                                float velocityY = mVelocityTracker.getYVelocity(mPointerId);
                                if (Math.abs(velocityX) > 100 || velocityY > 100) {
                                    petView.setIsLeft(velocityX < 0);
                                    if (velocityY > -6000 && velocityY < 0) {
                                        velocityY = -6000;
                                    }
                                    petView.setStatus(PetView.FALL_1_ACC, velocityX, velocityY);
                                } else {
                                    petView.setStatus(PetView.FALL_1);
                                }
                            }
                        }
                        mWindowManager.updateViewLayout(petView, params);
                        // If delta-distance greater than 6, then return true, or give event
                        // to OnClickListener
                        if (Math.abs(endX - tempX) > 6 && Math.abs(endY - tempY) > 6) {
                            return true;
                        }

                        releaseVelocityTracker();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        releaseVelocityTracker();

                        break;
                }
                return false;
            }
        };

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        };


        petView.setOnTouchListener(onTouchListener);
        petView.setOnClickListener(onClickListener);
        petView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                petView.setAttached(true);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                petView.setAttached(false);
            }
        });

        petView.setStatus(PetView.FALL_1);
        petView.setPetViewEvent(ViewManager.this);
        mWindowManager.addView(petView, params);

        mPetViews.add(petView);
        mLayoutParams.add(params);

    }

    public void removeAllPetView() {
        while (true) {
            if (mPetViews.size() == 0) {
                break;
            }
            removePetView(0);
        }
    }


    public void removePetView(int index) {
        if (mPetViews.size() == 0) {
            return;
        }
        PetView petView = mPetViews.get(index);
        ObjectAnimator objectAnimator = petView.getObjectAnimator();
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        mWindowManager.removeViewImmediate(petView);
        mPetViews.remove(index);
        mLayoutParams.remove(index);
    }


    @Override
    public void reDraw(PetView petView, WindowManager.LayoutParams layoutParams) {
        if (petView.isAttached()) {
            mWindowManager.updateViewLayout(petView, layoutParams);
        }
    }

    @Override
    public void removePet(PetView petView) {
        int index = mPetViews.indexOf(petView);
        removePetView(index);
    }

}
