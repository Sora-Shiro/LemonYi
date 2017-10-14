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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.sorashiro.desktoppet.data.MyBitmapFactory;
import com.sorashiro.desktoppet.tool.DPUtil;
import com.sorashiro.desktoppet.tool.ScreenUtil;

import java.util.ArrayList;
import java.util.Random;

public class PetView extends View {

    interface PetViewEvent {
        void reDraw(PetView petView, WindowManager.LayoutParams layoutParams);

        void removePet(PetView petView);
    }

    private PetViewEvent mPetViewEvent;

    public PetViewEvent getPetViewEvent() {
        return mPetViewEvent;
    }

    public void setPetViewEvent(PetViewEvent petViewEvent) {
        mPetViewEvent = petViewEvent;
    }

    private static final int FUNCTION     = 0x00000010;
    private static final int DRINK_LIKE   = 0;
    private static final int CRAWL        = 1;
    private static final int CLIMB        = 2;
    private static final int REDRAW       = 3;
    private static final int FLY          = 4;
    private static final int STRUGGLE     = 5;
    public static final  int NORMAL       = 0x00000001;
    public static final  int CHANGE_CHECK = 0x00000002;

    // All status
    public static final int DRAG              = 0x01000001;
    public static final int DRAG_WAIT         = 0x01000022;
    public static final int STRUGGLE_LEFT     = 0x01000025;
    public static final int STRUGGLE_RIGHT    = 0x01000026;
    public static final int SWING_LEFT        = 0x01000F03;
    public static final int SWING_LEFT_BACK   = 0x01000013;
    public static final int SWING_RIGHT       = 0x01000F04;
    public static final int SWING_RIGHT_BACK  = 0x01000014;
    public static final int FALL_1            = 0x00000100;
    public static final int FALL_1_ACC        = 0x00000110;
    public static final int FALL_WAIT         = 0x00000101;
    public static final int FALL_2            = 0x00000102;
    public static final int FALL_3            = 0x10000103;
    public static final int SINK              = 0x000001FF;
    public static final int HIT_X_LOW_BORDER  = 0x100002FF;
    public static final int STAND             = 0x10000200;
    public static final int STAND_WAIT        = 0x10000201;
    public static final int WALK_PRE          = 0x00000300;
    public static final int WALK_1            = 0x00000301;
    public static final int WALK_2            = 0x10000302;
    public static final int WALK_3            = 0x00000303;
    public static final int DRINK_PRE         = 0x00000400;
    public static final int DRINK_1           = 0x10000401;
    public static final int DRINK_2           = 0x00000402;
    public static final int DRINK_3           = 0x00000403;
    public static final int DRINK_4           = 0x10000404;
    public static final int DRINK_5           = 0x10000405;
    public static final int DRINK_6           = 0x10000406;
    public static final int DRINK_7           = 0x10000407;
    public static final int SING_PRE          = 0x10000500;
    public static final int SING_1            = 0x10000501;
    public static final int SING_2            = 0x10000502;
    public static final int SING_3            = 0x10000503;
    public static final int SING_4            = 0x10000504;
    public static final int CRAWL_PRE         = 0x10000600;
    public static final int CRAWL_1           = 0x10000601;
    public static final int CRAWL_2           = 0x10000602;
    public static final int HIT_Y_BORDER      = 0x100007FF;
    public static final int CLIMB_PRE         = 0x10000700;
    public static final int CLIMB_1           = 0x10000701;
    public static final int CLIMB_2           = 0x10000702;
    public static final int CLIMB_3           = 0x10000703;
    public static final int HIT_X_HIGH_BORDER = 0x100008FF;
    public static final int FLY_PRE           = 0x10000800;
    public static final int FLY_1             = 0x10000801;
    public static final int FLY_2             = 0x10000802;
    public static final int FLY_3             = 0x10000803;
    public static final int JUMP_PRE          = 0x10000900;
    public static final int JUMP              = 0x10000901;

    private boolean mIsAttached = false;
    private boolean mCanTouched = true;

    private int mInitWidth;
    private int mInitHeight;
    private boolean mIsLeft      = true;
    private boolean mIsUp        = true;
    private boolean mLegBehavior = true;
    private boolean mFlyBehavior = true;

    private int mStatus = -1;

    private MyHandler mHandler;

    private ObjectAnimator    mObjectAnimator;
    private ArrayList<Bitmap> mBitmaps;

    public PetView(Context context) {
        super(context);
        init();
    }

    public PetView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PetView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {

        mHandler = new MyHandler();

        mInitWidth = DPUtil.dip2px(getContext(), 100);
        mInitHeight = DPUtil.dip2px(getContext(), 100);

        mBitmaps = MyBitmapFactory.getInstance(getContext()).getBitmaps();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mInitWidth, mInitHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (mStatus) {
            case STAND:
                canvas.drawBitmap(getOrientationBitmap(0), 0, 0, null);
                startRandomAnimation();
                setStatus(STAND_WAIT);
                break;
            case STAND_WAIT:
                canvas.drawBitmap(getOrientationBitmap(0), 0, 0, null);
                break;
            case HIT_X_LOW_BORDER:
                canvas.drawBitmap(getOrientationBitmap(0), 0, 0, null);
                break;
            case DRAG:
                canvas.drawBitmap(getOrientationBitmap(32), 0, 0, null);
                mWaitNum++;
                startStrugglePreAnimation(mWaitNum);
                setStatus(DRAG_WAIT);
                break;
            case DRAG_WAIT:
                canvas.drawBitmap(getOrientationBitmap(32), 0, 0, null);
                break;
            case STRUGGLE_LEFT:
                canvas.drawBitmap(mBitmaps.get(4), 0, 0, null);
                break;
            case STRUGGLE_RIGHT:
                canvas.drawBitmap(mBitmaps.get(5), 0, 0, null);
                break;
            case SWING_LEFT:
                canvas.drawBitmap(mBitmaps.get(6), 0, 0, null);
                break;
            case SWING_RIGHT_BACK:
                canvas.drawBitmap(mBitmaps.get(6), 0, 0, null);
                break;
            case SWING_RIGHT:
                canvas.drawBitmap(mBitmaps.get(7), 0, 0, null);
                break;
            case SWING_LEFT_BACK:
                canvas.drawBitmap(mBitmaps.get(7), 0, 0, null);
                break;
            case FALL_1:
                canvas.drawBitmap(getOrientationBitmap(3), 0, 0, null);
                startFallAnimation();
                setStatus(FALL_WAIT);
                break;
            case FALL_1_ACC:
                canvas.drawBitmap(getOrientationBitmap(3), 0, 0, null);
                startFallAccAnimation(m_vX, m_vY, true);
                setStatus(FALL_WAIT);
                break;
            case FALL_WAIT:
                canvas.drawBitmap(getOrientationBitmap(3), 0, 0, null);
                break;
            case FALL_2:
                canvas.drawBitmap(getOrientationBitmap(15), 0, 0, null);
                break;
            case FALL_3:
                canvas.drawBitmap(getOrientationBitmap(16), 0, 0, null);
                break;
            case SINK:
                canvas.drawBitmap(getOrientationBitmap(3), 0, 0, null);
                startSinkAnimation();
                setStatus(FALL_WAIT);
                break;
            case WALK_PRE:
                canvas.drawBitmap(getOrientationBitmap(0), 0, 0, null);
                startWalkAnimation();
                setStatus(WALK_1);
                break;
            case WALK_1:
                canvas.drawBitmap(getOrientationBitmap(0), 0, 0, null);
                break;
            case WALK_2:
                canvas.drawBitmap(getOrientationBitmap(1), 0, 0, null);
                break;
            case WALK_3:
                canvas.drawBitmap(getOrientationBitmap(2), 0, 0, null);
                break;
            case DRINK_PRE:
                canvas.drawBitmap(getOrientationBitmap(10), 0, 0, null);
                startDrinkAnimation();
                setStatus(DRINK_1);
                break;
            case DRINK_1:
                canvas.drawBitmap(getOrientationBitmap(10), 0, 0, null);
                break;
            case DRINK_2:
                canvas.drawBitmap(getOrientationBitmap(23), 0, 0, null);
                break;
            case DRINK_3:
                canvas.drawBitmap(getOrientationBitmap(14), 0, 0, null);
                break;
            case DRINK_4:
                canvas.drawBitmap(getOrientationBitmap(24), 0, 0, null);
                break;
            case DRINK_5:
                canvas.drawBitmap(getOrientationBitmap(25), 0, 0, null);
                break;
            case DRINK_6:
                canvas.drawBitmap(getOrientationBitmap(26), 0, 0, null);
                break;
            case DRINK_7:
                canvas.drawBitmap(getOrientationBitmap(33), 0, 0, null);
                break;
            case SING_PRE:
                canvas.drawBitmap(getOrientationBitmap(27), 0, DPUtil.dip2px(getContext(), 15), null);
                startSingAnimation();
                setStatus(SING_1);
                break;
            case SING_1:
                canvas.drawBitmap(getOrientationBitmap(27), 0, DPUtil.dip2px(getContext(), 15), null);
                break;
            case SING_2:
                canvas.drawBitmap(getOrientationBitmap(30), 0, DPUtil.dip2px(getContext(), 15), null);
                break;
            case SING_3:
                canvas.drawBitmap(getOrientationBitmap(28), 0, DPUtil.dip2px(getContext(), 15), null);
                break;
            case SING_4:
                canvas.drawBitmap(getOrientationBitmap(29), 0, DPUtil.dip2px(getContext(), 15), null);
                break;
            case CRAWL_PRE:
                canvas.drawBitmap(getOrientationBitmap(18), 0, 0, null);
                startFlatAnimation();
                setStatus(CRAWL_2);
                break;
            case CRAWL_1:
                canvas.drawBitmap(getOrientationBitmap(17), 0, 0, null);
                break;
            case CRAWL_2:
                canvas.drawBitmap(getOrientationBitmap(18), 0, 0, null);
                break;
            case HIT_Y_BORDER:
                canvas.drawBitmap(getOrientationBitmap(mLegBehavior ? 12 : 13), 0, 0, null);
                break;
            case CLIMB_PRE:
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) getLayoutParams();
                int x = layoutParams.x;
                if (x >= ScreenUtil.getScreenWidth(getContext()) - mInitWidth / 2) {
                    mIsLeft = false;
                    layoutParams.x = ScreenUtil.getScreenWidth(getContext()) - mInitWidth / 2;
                } else if (x <= -mInitWidth / 2) {
                    mIsLeft = true;
                    layoutParams.x = -mInitWidth / 2;
                }
                mPetViewEvent.reDraw(PetView.this, layoutParams);
                canvas.drawBitmap(getOrientationBitmap(12), 0, 0, null);
                startClimbPreAnimation();
                setStatus(CLIMB_1);
                break;
            case CLIMB_1:
                canvas.drawBitmap(getOrientationBitmap(12), 0, 0, null);
                break;
            case CLIMB_2:
                canvas.drawBitmap(getOrientationBitmap(11), 0, 0, null);
                break;
            case CLIMB_3:
                canvas.drawBitmap(getOrientationBitmap(13), 0, 0, null);
                break;
            case HIT_X_HIGH_BORDER:
                canvas.drawBitmap(getOrientationBitmap(22), 0, 0, null);
                break;
            case FLY_PRE:
                canvas.drawBitmap(getOrientationBitmap(22), 0, 0, null);
                startFlyPreAnimation();
                setStatus(FLY_1);
                break;
            case FLY_1:
                canvas.drawBitmap(getOrientationBitmap(22), 0, 0, null);
                break;
            case FLY_2:
                canvas.drawBitmap(getOrientationBitmap(20), 0, 0, null);
                break;
            case FLY_3:
                canvas.drawBitmap(getOrientationBitmap(21), 0, 0, null);
                break;
            case JUMP_PRE:
                canvas.drawBitmap(getOrientationBitmap(19), 0, 0, null);
                startJumpAnimation();
                setStatus(JUMP);
                break;
            case JUMP:
                canvas.drawBitmap(getOrientationBitmap(19), 0, 0, null);
                break;
        }
        invalidate();
    }

    private Bitmap getOrientationBitmap(int index) {
        return mBitmaps.get(index + (mIsLeft ? 0 : 34));
    }

    private void startJumpAnimation() {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        float duration = 1;
        float x = layoutParams.x;
        double downDistance = ScreenUtil.getScreenHeight(getContext()) / 2;
        int vY = -(int) ((downDistance + 1000 * duration * duration) / duration);
        int vX;
        if (mIsLeft) {
            vX = (int) (-(x + mInitWidth * 2) / duration) - 100;
            vY = 5 * vY - 5500;
        } else {
            vX = (int) ((ScreenUtil.getScreenWidth(getContext()) - x) / duration + 100);
            vY = 5 * vY - 5500;
        }
        startFallAccAnimation(vX, vY, false);
    }

    private void startStruggleAnimation(final int waitNum, final int time) {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(1000);
        Random random = new Random();
        final boolean ori = random.nextBoolean();
        //        mObjectAnimator.setInterpolator(new LinearInterpolator());
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (waitNum == mWaitNum) {
                    float rate = ((float) animation.getCurrentPlayTime()) /
                            ((float) 1000);
                    boolean canChange;
                    // This is how gif work
                    if (rate < 0.25) {
                        canChange = setStatus(ori ? STRUGGLE_LEFT : STRUGGLE_RIGHT);
                    } else if (rate < 0.5) {
                        canChange = setStatus(ori ? STRUGGLE_RIGHT : STRUGGLE_LEFT);
                    } else if (rate < 0.75) {
                        canChange = setStatus(ori ? STRUGGLE_LEFT : STRUGGLE_RIGHT);
                    } else {
                        canChange = setStatus(ori ? STRUGGLE_RIGHT : STRUGGLE_LEFT);
                    }
                    if (canChange) {
                        mPetViewEvent.reDraw(PetView.this, layoutParams);
                    }
                }
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancel = false;

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancel) {
                    setStatus(DRAG_WAIT);
                    if (time == 2) {
                        // Struggle will make the current finger be invalid,
                        // and ACTION_DOWN can reset it
                        mCanTouched = false;
                        setStatus(FALL_1);
                    } else {
                        startStruggleAnimation(waitNum, time + 1);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.setStartDelay(1000);
        mObjectAnimator.start();
    }

    // Check this num to decide whether struggle animation continue
    private int mWaitNum = 0;
    Thread mThread;

    private void startStrugglePreAnimation(final int waitNum) {
        if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
        }
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (waitNum == mWaitNum) {
                    Message msg = Message.obtain();
                    msg.what = STRUGGLE;
                    msg.arg1 = FUNCTION;
                    msg.arg2 = waitNum;
                    mHandler.sendMessage(msg);
                }
            }
        });
        mThread.start();
    }

    // Fly and fly
    private void startFlyAnimation() {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        final int startX = layoutParams.x;
        final int endX = startX + (mIsLeft ? -DPUtil.dip2px(getContext(), 24) : DPUtil.dip2px(getContext(), 24));
        final long duration = 1000;
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(duration);
        //        mObjectAnimator.setInterpolator(new LinearInterpolator());
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (checkHitYBorder(false)) {
                    float rate = ((float) animation.getCurrentPlayTime()) /
                            ((float) duration);
                    boolean canChange;
                    // This is how gif work
                    if (rate < 0.33) {
                        canChange = setStatus(mFlyBehavior ? FLY_1 : FLY_3);
                    } else if (rate <= 0.66) {
                        canChange = setStatus(FLY_2);
                    } else {
                        canChange = setStatus(mFlyBehavior ? FLY_3 : FLY_1);
                    }
                    if (canChange) {
                        layoutParams.x = (int) ((endX - startX) * rate + startX);
                        mPetViewEvent.reDraw(PetView.this, layoutParams);
                    }
                }
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancel = false;

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancel && checkHitYBorder(true)) {
                    mFlyBehavior = !mFlyBehavior;
                    startFlyPreAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.setStartDelay(duration / 6);
        mObjectAnimator.start();
    }


    private void startFlyPreAnimation() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) getLayoutParams();
                int y = layoutParams.y;
                if (y >= -ScreenUtil.getStatusBarHeight(getContext())) {
                    layoutParams.y = -ScreenUtil.getStatusBarHeight(getContext());
                }
                sendMyDelayMessage(REDRAW, FUNCTION, 0);
                while (true) {
                    try {
                        Thread.sleep(1_000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Check if keep this kind of status
                    if (((mStatus & 0x00000F00) ^ 0x00000800) != 0) {
                        return;
                    }
                    Random random = new Random();
                    int rI = random.nextInt(100);
                    // 80% fly, 10% fall, 5% do nothing, 5% change orientation
                    if (rI < 80) {
                        sendMyDelayMessage(FLY, FUNCTION, 0);
                        break;
                    } else if (rI < 90) {
                        sendMyDelayMessage(FALL_1, NORMAL, 0);
                        break;
                    } else if (rI < 95) {
                        mIsLeft = !mIsLeft;
                    }
                }
            }
        });
        thread.start();
    }

    private void startClimbPreAnimation() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) getLayoutParams();
                int x = layoutParams.x;
                if (x >= ScreenUtil.getScreenWidth(getContext()) - mInitWidth / 2) {
                    layoutParams.x = ScreenUtil.getScreenWidth(getContext()) - mInitWidth / 2;
                } else if (x <= -mInitWidth / 2) {
                    layoutParams.x = -mInitWidth / 2;
                }
                sendMyDelayMessage(REDRAW, FUNCTION, 0);
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Check if keep this kind of status
                    if (((mStatus & 0x00000F00) ^ 0x00000700) != 0) {
                        return;
                    }
                    Random random = new Random();
                    int rI = random.nextInt(100);
                    // 80% climb, 10% fall, 5% do nothing, 5% change orientation
                    if (rI < 80) {
                        sendMyDelayMessage(CLIMB, FUNCTION, 0);
                        break;
                    } else if (rI < 90) {
                        sendMyDelayMessage(FALL_1, NORMAL, 0);
                        break;
                    } else if (rI < 95) {
                        mIsUp = !mIsUp;
                    }
                }
            }
        });
        thread.start();
    }

    // Climb Y axis
    private void startClimbAnimation() {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        final int startY = layoutParams.y;
        final int endY = startY + (mIsUp ?
                -DPUtil.dip2px(getContext(), (mLegBehavior ? 40 : 30)) :
                DPUtil.dip2px(getContext(), (mLegBehavior ? 40 : 30)));
        final long duration = 1500;
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(duration);
        //        mObjectAnimator.setInterpolator(new LinearInterpolator());
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (checkHitXBorder(false)) {
                    double rate = ((double) animation.getCurrentPlayTime()) /
                            ((double) duration);
                    rate = mObjectAnimator.getInterpolator().getInterpolation((float) rate);
                    // This is how gif work
                    boolean canChange;
                    if (rate < 0.33) {
                        canChange = setStatus(mLegBehavior ? CLIMB_1 : CLIMB_3);
                    } else if (rate <= 0.66) {
                        canChange = setStatus(CLIMB_2);
                    } else {
                        canChange = setStatus(mLegBehavior ? CLIMB_3 : CLIMB_1);
                    }
                    if (canChange) {
                        layoutParams.y = (int) ((endY - startY) * rate + startY);
                        mPetViewEvent.reDraw(PetView.this, layoutParams);
                    }
                }
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancel = false;

            @Override
            public void onAnimationStart(Animator animation) {
                int x = layoutParams.x;
                if (x >= ScreenUtil.getScreenWidth(getContext()) - mInitWidth / 2) {
                    layoutParams.x = ScreenUtil.getScreenWidth(getContext()) - mInitWidth / 2;
                } else if (x <= -mInitWidth / 2) {
                    layoutParams.x = -mInitWidth / 2;
                }
                mPetViewEvent.reDraw(PetView.this, layoutParams);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancel && checkHitXBorder(true)) {
                    mLegBehavior = !mLegBehavior;
                    startClimbPreAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.start();
    }

    // Flat on the ground, wait for something
    private void startFlatAnimation() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Check if keep this kind of status
                if (((mStatus & 0x00000F00) ^ 0x00000600) != 0) {
                    return;
                }
                Random random = new Random();
                int nextBehavior = random.nextInt(100);
                // 20% stand up, 80% crawl
                if (nextBehavior < 20) {
                    sendMyDelayMessage(STAND, NORMAL, 0);
                } else {
                    sendMyDelayMessage(CRAWL, FUNCTION, 0);
                }
            }
        });
        thread.start();
    }

    // Crawl and crawl
    private void startCrawlAnimation() {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        final int startX = layoutParams.x;
        final int endX = startX + (mIsLeft ? -DPUtil.dip2px(getContext(), 40) : DPUtil.dip2px(getContext(), 40));
        final long duration = 1500;
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(duration);
        //        mObjectAnimator.setInterpolator(new AccelerateInterpolator());
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (checkHitYBorder(false)) {
                    double rate = ((double) animation.getCurrentPlayTime()) /
                            ((double) duration);
                    rate = mObjectAnimator.getInterpolator().getInterpolation((float) rate);
                    // This is how gif work
                    if (rate < 0.7) {
                        setStatus(CRAWL_1);
                    } else if (rate <= 1) {
                        if (setStatus(CRAWL_2)) {
                            layoutParams.x = (int) ((endX - startX) * (rate - 0.7) + startX);
                            mPetViewEvent.reDraw(PetView.this, layoutParams);
                        }
                    }
                }
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancel = false;

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancel && checkHitYBorder(true)) {
                    setStatus(CRAWL_2);
                    startFlatAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.setStartDelay(duration / 6);
        mObjectAnimator.start();
    }

    private void startSingAnimation() {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        final long duration = 5_000;
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(duration);
        //        mObjectAnimator.setInterpolator(new LinearInterpolator());
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float rate = ((float) animation.getCurrentPlayTime()) /
                        ((float) duration);
                if (rate < 0.25) {
                    setStatus(SING_1);
                } else if (rate < 0.5) {
                    setStatus(SING_2);
                } else if (rate < 0.75) {
                    setStatus(SING_3);
                } else if (rate <= 1) {
                    setStatus(SING_4);
                }
                mPetViewEvent.reDraw(PetView.this, layoutParams);
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancel = false;

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancel) {
                    Random random = new Random();
                    int rI = random.nextInt(100);
                    // %80 sing, %20 stand up
                    if (rI < 80) {
                        startSingAnimation();
                    } else if (rI < 100) {
                        setStatus(STAND);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.start();
    }

    // Drink and drink
    private void startDrinkAnimation() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(15_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Check if keep this kind of status
                if (((mStatus & 0x00000F00) ^ 0x00000400) != 0) {
                    return;
                }
                Random random = new Random();
                int nextBehavior = random.nextInt(100);
                // 20% stand up, 80% drink-like
                if (nextBehavior < 20) {
                    sendMyDelayMessage(STAND, NORMAL, 0);
                } else {
                    sendMyDelayMessage(DRINK_LIKE, FUNCTION, 0);
                }
            }
        });
        thread.start();
    }

    private void startDrinkLikeAnimation() {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        final long duration = 2_000;
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(duration);
        // Constant speed to walk
        //        mObjectAnimator.setInterpolator(new LinearInterpolator());
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float rate = ((float) animation.getCurrentPlayTime()) /
                        ((float) duration);
                if (rate < 0.5) {
                    setStatus(DRINK_2);
                } else if (rate < 1) {
                    setStatus(DRINK_3);
                }
                mPetViewEvent.reDraw(PetView.this, layoutParams);
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancel = false;

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancel) {
                    startDrinkLikeMoreAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.start();
    }

    private void startDrinkLikeMoreAnimation() {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        final long duration = 5_000;
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(duration);
        //        mObjectAnimator.setInterpolator(new LinearInterpolator());
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float rate = ((float) animation.getCurrentPlayTime()) /
                        ((float) duration);
                if (rate < 0.25) {
                    setStatus(DRINK_4);
                } else if (rate < 0.5) {
                    setStatus(DRINK_5);
                } else if (rate < 0.75) {
                    setStatus(DRINK_6);
                } else if (rate <= 1) {
                    setStatus(DRINK_7);
                }
                mPetViewEvent.reDraw(PetView.this, layoutParams);
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancel = false;

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancel) {
                    Random random = new Random();
                    int rI = random.nextInt(100);
                    // %50 drink-like-more, %50 stand up
                    if (rI < 50) {
                        startDrinkLikeMoreAnimation();
                    } else if (rI < 100) {
                        setStatus(STAND);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.start();
    }

    // Walk and walk
    private void startWalkAnimation() {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        final int startX = layoutParams.x;
        final int endX = startX + (mIsLeft ? -DPUtil.dip2px(getContext(), 33) : DPUtil.dip2px(getContext(), 33));
        final long duration = 2000;
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(duration);
        // Constant speed to walk
        //        mObjectAnimator.setInterpolator(new LinearInterpolator());
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (checkHitYBorder(false)) {
                    float rate = ((float) animation.getCurrentPlayTime()) /
                            ((float) duration);
                    boolean canChange;
                    // This is how gif work
                    if (rate < 0.33) {
                        canChange = setStatus(WALK_2);
                    } else if (rate < 0.66) {
                        canChange = setStatus(WALK_3);
                    } else {
                        canChange = setStatus(WALK_2);
                    }
                    if (canChange) {
                        layoutParams.x = (int) ((endX - startX) * rate + startX);
                        mPetViewEvent.reDraw(PetView.this, layoutParams);
                    }
                }
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean cancel = false;

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancel && checkHitYBorder(true)) {
                    Random random = new Random();
                    int rI = random.nextInt(100);
                    // %80 walk, %20 stand up
                    if (rI < 80) {
                        setStatus(WALK_PRE);
                    } else {
                        setStatus(STAND);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.setStartDelay(duration / 6);
        mObjectAnimator.start();
    }

    // When the pet stand up, it may do anything
    private void startRandomAnimation() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(3_000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Check if standing
                    if (((mStatus & 0x00000F00) ^ 0x00000200) != 0) {
                        return;
                    }
                    Random random = new Random();

                    int nextBehavior = random.nextInt(1000);
                    // 20% walk, 20% drink, 20% sing, 20% flat, 5% jump, 5% change orientation
                    if (nextBehavior < 200) {
                        sendMyDelayMessage(WALK_PRE, NORMAL, 0);
                        break;
                    } else if (nextBehavior < 400) {
                        sendMyDelayMessage(DRINK_PRE, NORMAL, 0);
                        break;
                    } else if (nextBehavior < 600) {
                        sendMyDelayMessage(SING_PRE, NORMAL, 0);
                        break;
                    } else if (nextBehavior < 800) {
                        sendMyDelayMessage(CRAWL_PRE, NORMAL, 0);
                        break;
                    } else if (nextBehavior < 950) {
                        sendMyDelayMessage(JUMP_PRE, NORMAL, 0);
                        break;
                    } else {
                        mIsLeft = random.nextBoolean();
                    }
                }
            }
        });
        thread.start();
    }

    // Drop pet down, and it sinks, then it disappears
    private void startSinkAnimation() {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        final int startY = layoutParams.y;
        final int endY = ScreenUtil.getScreenHeight(getContext()) + getInitHeight();
        final long duration = 300;
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(duration);
        // Accelerate, like a gravity
        mObjectAnimator.setInterpolator(new AccelerateInterpolator());
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                double rate = ((double) animation.getCurrentPlayTime()) /
                        ((double) duration);
                rate = mObjectAnimator.getInterpolator().getInterpolation((float) rate);
                layoutParams.y = (int) ((endY - startY) * rate + startY);
                if (layoutParams.y > endY) {
                    layoutParams.y = endY;
                }
                if (PetView.this.getWindowAttachCount() > 0) {
                    mPetViewEvent.reDraw(PetView.this, layoutParams);
                }
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean mIsCancel = false;

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mIsCancel) {
                    mPetViewEvent.removePet(PetView.this);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mIsCancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.start();
    }

    // Hit the ground, then stand up
    private void startFallToStandUpAnimation() {
        final WindowManager.LayoutParams layoutParams = (
                WindowManager.LayoutParams) getLayoutParams();
        final long duration = 2500;
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(duration);
        //        mObjectAnimator.setInterpolator(new LinearInterpolator());
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float rate = ((float) animation.getCurrentPlayTime()) /
                        ((float) duration);
                // This is how gif work
                if (rate < 0.4) {
                    setStatus(FALL_2);
                } else if (rate <= 1) {
                    setStatus(FALL_3);
                }
                mPetViewEvent.reDraw(PetView.this, layoutParams);
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean mIsCancel = false;

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mIsCancel) {
                    setStatus(STAND);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mIsCancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.start();
    }

    // Pet Acc falling
    private void startFallAccAnimation(float vX, float vY, boolean yLimit) {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        final int startX = layoutParams.x;
        int endX = (int) (startX + vX);
        final int startY = layoutParams.y;
        final int endY = ScreenUtil.getScreenHeight(getContext()) - mInitHeight - ScreenUtil.getStatusBarHeight(getContext());
        double upDuration;
        double upDistance;
        double downDuration;
        double downDistance;
        // v_y0 :)
        float temp_vY = vY;
        if (temp_vY < 0) {
            // Control temp_vY range
            temp_vY = (temp_vY + 5000) / 5;
            if (temp_vY > -1000) {
                temp_vY = -1000;
            } else if (yLimit && temp_vY < -4000) {
                temp_vY = -4000;
            }
            // duration = [0.5, 2) s, while a = 2000px/s^2
            upDuration = -(temp_vY) / 2000;
            upDistance = -(temp_vY) * (upDuration) -
                    2000 * (upDuration) * (upDuration) / 2;
            // We should multiply 1000 for setDuration is ms
            upDuration *= 1000;
            downDistance = endY - startY + upDistance;
            downDuration = Math.sqrt((downDistance) * 2 / 0.002);
        } else {
            upDuration = 0;
            upDistance = 0;
            if (temp_vY < 1000) {
                temp_vY = 0;
            } else {
                temp_vY /= 1000;
            }
            if (temp_vY > 4.5) {
                endX = startX;
            } else if (temp_vY > 1) {
                endX = (int) (vX / 10 + startX);
            }
            downDistance = endY - startY;
            // vt + (at^2)/2 = distance, so work it out, then get t, in other words, duration
            // t = [-b+sqrt(b^2-4ac)]/(2a)
            double delta = Math.sqrt(temp_vY * temp_vY + 4 * 0.002 * downDistance);
            downDuration = (-temp_vY + delta) / (2 * 0.002);
            if (downDuration < 100) {
                downDuration = 100;
            }
        }
        final int lastEndX = endX;
        final int lastStartY = (int) (startY - upDistance);
        final double lastUpDistance = upDistance;
        final double lastDownDistance = downDistance;
        final double lastUpDuration = upDuration;
        final double lastDownDuration = downDuration;
        final long duration = (long) (upDuration + downDuration);
        if (duration == 0) {
            setStatus(FALL_2);
            startFallToStandUpAnimation();
            return;
        }
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(duration);
        // Accelerate, like a gravity
        final AccelerateInterpolator accelerateInterpolator = new AccelerateInterpolator();
        // If there is -y initial velocity
        final DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (checkHitYBorder(false)) {
                    float rate = ((float) animation.getCurrentPlayTime()) /
                            ((float) duration);
                    layoutParams.x = (int) ((lastEndX - startX) * rate + startX);
                    if (rate <= lastUpDuration / duration) {
                        rate = (float) (animation.getCurrentPlayTime() / lastUpDuration);
                        rate = decelerateInterpolator.getInterpolation(rate);
                        layoutParams.y = (int) (startY - (lastUpDistance) * rate);
                    } else {
                        double nowDownDuration = animation.getCurrentPlayTime() - lastUpDuration;
                        rate = (float) (nowDownDuration / lastDownDuration);
                        rate = accelerateInterpolator.getInterpolation(rate);
                        layoutParams.y = (int) ((lastDownDistance) * rate + lastStartY);
                    }
                    if (layoutParams.y > endY) {
                        layoutParams.y = endY;
                    }
                    mPetViewEvent.reDraw(PetView.this, layoutParams);
                }
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean mIsCancel = false;

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mIsCancel && checkHitYBorder(true)) {
                    setStatus(FALL_2);
                    startFallToStandUpAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mIsCancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.start();
    }

    // Pet falling
    private void startFallAnimation() {
        final WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) getLayoutParams();
        final int startY = layoutParams.y;
        final int endY = ScreenUtil.getScreenHeight(getContext()) - mInitHeight - ScreenUtil.getStatusBarHeight(getContext());
        // a = 0.001px/ms^2 :)
        final long duration = (long) Math.sqrt((endY - startY) * 2 / 0.002);
        if (duration == 0) {
            setStatus(FALL_2);
            startFallToStandUpAnimation();
            return;
        }
        mObjectAnimator = ObjectAnimator.ofInt(
                this, "Top",
                0, 0);
        mObjectAnimator.setDuration(duration);
        // Accelerate, like a gravity
        mObjectAnimator.setInterpolator(new AccelerateInterpolator());
        mObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                double rate = ((double) animation.getCurrentPlayTime()) /
                        ((double) duration);
                rate = mObjectAnimator.getInterpolator().getInterpolation((float) rate);
                layoutParams.y = (int) ((endY - startY) * rate + startY);
                if (layoutParams.y > endY) {
                    layoutParams.y = endY;
                }
                mPetViewEvent.reDraw(PetView.this, layoutParams);
            }
        });
        mObjectAnimator.addListener(new Animator.AnimatorListener() {
            boolean mIsCancel = false;

            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mIsCancel) {
                    setStatus(FALL_2);
                    startFallToStandUpAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mIsCancel = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mObjectAnimator.start();
    }

    public boolean checkHitXBorder(boolean changeState) {
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) getLayoutParams();
        int y = layoutParams.y + (mIsUp ? -10 : 10);
        int endY = ScreenUtil.getScreenHeight(getContext()) - mInitHeight - ScreenUtil.getStatusBarHeight(getContext());
        if (y >= endY) {
            layoutParams.y = endY;
            layoutParams.x = (mIsLeft ?
                    (-mInitWidth / 2 + 30) :
                    (ScreenUtil.getScreenWidth(getContext()) - mInitWidth / 2 - 30));
            if (changeState) {
                setStatus(STAND);
            } else {
                setStatus(HIT_X_LOW_BORDER);
            }
            mPetViewEvent.reDraw(PetView.this, layoutParams);
            return false;
        } else if (y <= -ScreenUtil.getStatusBarHeight(getContext())) {
            layoutParams.y = -ScreenUtil.getStatusBarHeight(getContext());
            layoutParams.x = (!mIsLeft) ?
                    (ScreenUtil.getScreenWidth(getContext()) - mInitWidth / 2 - 10) :
                    (-mInitWidth / 2 + 10);
            if (changeState) {
                mIsLeft = !mIsLeft;
                setStatus(FLY_PRE);
            } else {
                setStatus(HIT_X_HIGH_BORDER);
            }
            mPetViewEvent.reDraw(PetView.this, layoutParams);
            return false;
        }
        return true;
    }

    public boolean checkHitYBorder(boolean changeState) {
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) getLayoutParams();
        int x = layoutParams.x;
        if (x >= ScreenUtil.getScreenWidth(getContext()) - mInitWidth / 2) {
            layoutParams.x = ScreenUtil.getScreenWidth(getContext()) - mInitWidth / 2;
            mIsLeft = false;
            if (changeState) {
                setStatus(CLIMB_PRE);
            } else {
                setStatus(HIT_Y_BORDER);
            }
            mPetViewEvent.reDraw(PetView.this, layoutParams);
            return false;
        } else if (x <= -mInitWidth / 2) {
            layoutParams.x = -mInitWidth / 2;
            mIsLeft = true;
            if (changeState) {
                setStatus(CLIMB_PRE);
            } else {
                setStatus(HIT_Y_BORDER);
            }
            mPetViewEvent.reDraw(PetView.this, layoutParams);
            return false;
        }
        return true;
    }

    public int getStatus() {
        return mStatus;
    }

    private float m_vX;
    private float m_vY;

    public boolean setStatus(int status, float vX, float vY) {
        this.m_vX = vX;
        this.m_vY = vY;
        return setStatus(status);
    }

    public boolean setStatus(int status) {
        // Status Machine, which is used to check
        boolean canChange = true;
        switch (status) {
            case DRAG:
                canChange = true;
                break;
            case SWING_LEFT:
            case SWING_LEFT_BACK:
            case SWING_RIGHT:
            case SWING_RIGHT_BACK:
                canChange = true;
                break;
            case DRAG_WAIT:
                canChange = (mStatus & 0x00000F00) == 0;
                break;
            case STRUGGLE_LEFT:
            case STRUGGLE_RIGHT:
                canChange = ((mStatus & 0x000000F0) ^ 0x00000020) == 0;
                break;
            case HIT_X_LOW_BORDER:
                canChange = true;
                break;
            case STAND:
                canChange = ((mStatus & 0xF0000000) ^ 0x10000000) == 0;
                break;
            case STAND_WAIT:
                canChange = ((mStatus & 0xF0000000) ^ 0x10000000) == 0;
                break;
            case FALL_1:
                canChange = true;
                break;
            case FALL_WAIT:
                canChange = ((mStatus & 0x00000F00) ^ 0x00000100) == 0;
                break;
            case FALL_2:
                canChange = mStatus == FALL_WAIT || mStatus == FALL_1;
                break;
            case FALL_3:
                canChange = mStatus == FALL_2;
                break;
            case SINK:
                canChange = true;
                break;
            case WALK_PRE:
                canChange = mStatus == STAND_WAIT || mStatus == WALK_2;
                break;
            case WALK_1:
            case WALK_2:
            case WALK_3:
                canChange = ((mStatus & 0x00000F00) ^ 0x00000300) == 0;
                break;
            case DRINK_PRE:
                canChange = mStatus == STAND_WAIT;
                break;
            case DRINK_1:
            case DRINK_2:
            case DRINK_3:
            case DRINK_4:
            case DRINK_5:
            case DRINK_6:
            case DRINK_7:
                canChange = ((mStatus & 0x00000F00) ^ 0x00000400) == 0;
                break;
            case SING_PRE:
                canChange = mStatus == STAND_WAIT;
                break;
            case SING_1:
            case SING_2:
            case SING_3:
            case SING_4:
                canChange = ((mStatus & 0x00000F00) ^ 0x00000500) == 0;
                break;
            case CRAWL_PRE:
                canChange = mStatus == STAND_WAIT;
                break;
            case CRAWL_1:
            case CRAWL_2:
                canChange = ((mStatus & 0x00000F00) ^ 0x00000600) == 0;
                break;
            case CLIMB_PRE:
                canChange = true;
                break;
            case HIT_Y_BORDER:
                canChange = true;
                break;
            case CLIMB_1:
            case CLIMB_2:
            case CLIMB_3:
                canChange = ((mStatus & 0x00000F00) ^ 0x00000700) == 0;
                break;
            case FLY_PRE:
                canChange = true;
                break;
            case FLY_1:
            case FLY_2:
            case FLY_3:
                canChange = ((mStatus & 0x00000F00) ^ 0x00000800) == 0;
                break;
            case JUMP_PRE:
                canChange = true;
                break;
            case JUMP:
                canChange = ((mStatus & 0x00000F00) ^ 0x00000900) == 0;
                break;
        }
        if (canChange) {
            mStatus = status;
            invalidate();
        }
        return canChange;
    }

    public boolean isAttached() {
        return mIsAttached;
    }

    public void setAttached(boolean attached) {
        mIsAttached = attached;
    }

    public boolean isCanTouched() {
        return mCanTouched;
    }

    public void setCanTouched(boolean canTouched) {
        mCanTouched = canTouched;
    }

    public int getInitWidth() {
        return mInitWidth;
    }

    public void setInitWidth(int initWidth) {
        mInitWidth = initWidth;
    }

    public int getInitHeight() {
        return mInitHeight;
    }

    public void setInitHeight(int initHeight) {
        mInitHeight = initHeight;
    }

    public ObjectAnimator getObjectAnimator() {
        return mObjectAnimator;
    }

    public void setObjectAnimator(ObjectAnimator objectAnimator) {
        mObjectAnimator = objectAnimator;
    }

    public boolean getIsLeft() {
        return mIsLeft;
    }

    public void setIsLeft(boolean left) {
        mIsLeft = left;
    }

    public boolean isLegBehavior() {
        return mLegBehavior;
    }

    public void setLegBehavior(boolean legBehavior) {
        mLegBehavior = legBehavior;
    }

    public void sendMyDelayMessage(int what, int arg1, long delay) {
        Message message = Message.obtain();
        message.what = what;
        message.arg1 = arg1;
        mHandler.sendMessageDelayed(message, delay);
    }

    public class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case NORMAL:
                    setStatus(msg.what);
                    break;
                case FUNCTION:
                    switch (msg.what) {
                        case DRINK_LIKE:
                            startDrinkLikeAnimation();
                            break;
                        case CRAWL:
                            startCrawlAnimation();
                            break;
                        case CLIMB:
                            startClimbAnimation();
                            break;
                        case REDRAW:
                            mPetViewEvent.reDraw(
                                    PetView.this,
                                    (WindowManager.LayoutParams) getLayoutParams());
                            break;
                        case FLY:
                            startFlyAnimation();
                            break;
                        case STRUGGLE:
                            startStruggleAnimation(msg.arg2, 0);
                    }
                    break;
                case CHANGE_CHECK:
                    int s = msg.what;
                    int type = s & 0x00000F00;
                    int mType = mStatus & 0x00000F00;
                    if (type == mType) {
                        setStatus(msg.what);
                    }
                    break;
            }
        }
    }

}
