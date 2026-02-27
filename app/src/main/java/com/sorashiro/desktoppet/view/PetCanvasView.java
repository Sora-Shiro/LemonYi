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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.sorashiro.desktoppet.data.MyBitmapFactory;
import com.sorashiro.desktoppet.tool.DPUtil;
import com.sorashiro.desktoppet.tool.ScreenUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 全屏透明 Canvas 覆盖层，统一管理并渲染所有桌宠。
 *
 * 与原 PetView 的核心区别：
 *   - 宠物坐标存在 PetModel.x / PetModel.y（Canvas 内部坐标，原点为 content area 左上角）
 *   - 移动宠物只调 invalidate()，不再调 WindowManager.updateViewLayout()（消除 IPC 开销）
 *   - 一个 View 绘制所有宠物
 */
public class PetCanvasView extends View {

    // ── Handler 消息 arg1 常量 ────────────────────────────────────────────
    static final int FUNCTION     = 0x00000010;
    static final int DRINK_LIKE   = 0;
    static final int CRAWL_MSG    = 1;
    static final int CLIMB_MSG    = 2;
    static final int REDRAW       = 3;
    static final int FLY_MSG      = 4;
    static final int STRUGGLE     = 5;
    public static final int NORMAL       = 0x00000001;
    public static final int CHANGE_CHECK = 0x00000002;

    // ── 所有状态常量（与 PetView 保持一致） ──────────────────────────────
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

    // ── Fields ───────────────────────────────────────────────────────────
    private final List<PetModel> mModels = new ArrayList<>();
    private ArrayList<Bitmap>    mBitmaps;
    private int                  mPetWidth;
    private int                  mPetHeight;
    private int                  mSingOffsetY; // SING 系列帧在 Y 方向的偏移
    /** 实际 canvas 高度（onSizeChanged 回调后有效）；用于替代 getScreenHeight()-statusBar。 */
    private int                  mCanvasHeight;
    /** 每次 onDraw 后通知外部（用于同步 touch proxy 窗口位置）。 */
    private Runnable             mPositionChangedCallback;
    /** removeModel 时通知外部（用于清理 touch proxy 窗口）。 */
    private OnModelRemovedListener mOnModelRemovedListener;

    public interface OnModelRemovedListener {
        void onModelRemoved(PetModel model);
    }

    public void setOnModelRemovedListener(OnModelRemovedListener listener) {
        mOnModelRemovedListener = listener;
    }

    // 触摸追踪
    private PetModel        mDraggingModel;
    private float           mTouchStartRawX;
    private float           mTouchStartRawY;
    private VelocityTracker mVelocityTracker;
    private int             mPointerId;
    private final int       mMaxVelocity;

    public PetCanvasView(Context context) {
        super(context);
        mMaxVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        init();
    }

    private void init() {
        mPetWidth    = DPUtil.dip2px(getContext(), 100);
        mPetHeight   = DPUtil.dip2px(getContext(), 100);
        mSingOffsetY = DPUtil.dip2px(getContext(), 15);
        mBitmaps     = MyBitmapFactory.getInstance(getContext()).getBitmaps();
        // 初始 fallback：屏幕高度减去状态栏，待 onSizeChanged 后被实际值替换
        mCanvasHeight = ScreenUtil.getScreenHeight(getContext())
                - ScreenUtil.getStatusBarHeight(getContext());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h > 0) mCanvasHeight = h;
    }

    // ── 宠物生命周期管理 ─────────────────────────────────────────────────

    /** 在指定位置新增一只宠物，返回其 model。 */
    public PetModel addPet(int startX, int startY, boolean isLeft) {
        PetModel model  = new PetModel();
        model.x         = startX;
        model.y         = startY;
        model.width     = mPetWidth;
        model.height    = mPetHeight;
        model.isLeft    = isLeft;
        model.handler   = new PetHandler(this, model);
        mModels.add(model);
        setStatus(model, FALL_1);
        invalidate();
        return model;
    }

    /** 移除并清理一只宠物。 */
    public void removeModel(PetModel model) {
        model.cancelAnimator();
        if (model.struggleThread != null) {
            model.struggleThread.interrupt();
        }
        mModels.remove(model);
        if (mOnModelRemovedListener != null) {
            mOnModelRemovedListener.onModelRemoved(model);
        }
        invalidate();
    }

    /** 移除所有宠物。 */
    public void removeAll() {
        for (PetModel m : new ArrayList<>(mModels)) {
            removeModel(m);
        }
    }

    public List<PetModel> getModels() {
        return mModels;
    }

    // ── 绘制 ─────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        // 对列表做快照，防止并发修改（动画回调可能在主线程移除 model）
        for (PetModel model : new ArrayList<>(mModels)) {
            drawModel(canvas, model);
        }
        // 通知外部同步 touch proxy 窗口位置（ViewManager 注册此回调）
        if (mPositionChangedCallback != null) mPositionChangedCallback.run();
        // 持续重绘，与原 PetView 末尾的 invalidate() 等价
        invalidate();
    }

    /** 注册每帧回调，供 ViewManager 同步 touch proxy 窗口位置。 */
    public void setPositionChangedCallback(Runnable callback) {
        mPositionChangedCallback = callback;
    }

    /** 供 ViewManager 查询：该 model 当前是否正被拖拽中。 */
    public boolean isDragging(PetModel model) {
        return model != null && model == mDraggingModel;
    }

    private void drawModel(Canvas canvas, PetModel model) {
        final int x = model.x;
        final int y = model.y;
        switch (model.status) {
            case STAND:
                canvas.drawBitmap(bmp(model, 0), x, y, null);
                startRandomAnimation(model);
                setStatus(model, STAND_WAIT);
                break;
            case STAND_WAIT:
                canvas.drawBitmap(bmp(model, 0), x, y, null);
                break;
            case HIT_X_LOW_BORDER:
                canvas.drawBitmap(bmp(model, 0), x, y, null);
                break;
            case DRAG:
                canvas.drawBitmap(bmp(model, 32), x, y, null);
                model.waitNum++;
                startStrugglePreAnimation(model, model.waitNum);
                setStatus(model, DRAG_WAIT);
                break;
            case DRAG_WAIT:
                canvas.drawBitmap(bmp(model, 32), x, y, null);
                break;
            case STRUGGLE_LEFT:
                canvas.drawBitmap(mBitmaps.get(4), x, y, null);
                break;
            case STRUGGLE_RIGHT:
                canvas.drawBitmap(mBitmaps.get(5), x, y, null);
                break;
            case SWING_LEFT:
            case SWING_RIGHT_BACK:
                canvas.drawBitmap(mBitmaps.get(6), x, y, null);
                break;
            case SWING_RIGHT:
            case SWING_LEFT_BACK:
                canvas.drawBitmap(mBitmaps.get(7), x, y, null);
                break;
            case FALL_1:
                canvas.drawBitmap(bmp(model, 3), x, y, null);
                startFallAnimation(model);
                setStatus(model, FALL_WAIT);
                break;
            case FALL_1_ACC:
                canvas.drawBitmap(bmp(model, 3), x, y, null);
                startFallAccAnimation(model, model.vX, model.vY, true);
                setStatus(model, FALL_WAIT);
                break;
            case FALL_WAIT:
                canvas.drawBitmap(bmp(model, 3), x, y, null);
                break;
            case FALL_2:
                canvas.drawBitmap(bmp(model, 15), x, y, null);
                break;
            case FALL_3:
                canvas.drawBitmap(bmp(model, 16), x, y, null);
                break;
            case SINK:
                canvas.drawBitmap(bmp(model, 3), x, y, null);
                startSinkAnimation(model);
                setStatus(model, FALL_WAIT);
                break;
            case WALK_PRE:
                canvas.drawBitmap(bmp(model, 0), x, y, null);
                startWalkAnimation(model);
                setStatus(model, WALK_1);
                break;
            case WALK_1:
                canvas.drawBitmap(bmp(model, 0), x, y, null);
                break;
            case WALK_2:
                canvas.drawBitmap(bmp(model, 1), x, y, null);
                break;
            case WALK_3:
                canvas.drawBitmap(bmp(model, 2), x, y, null);
                break;
            case DRINK_PRE:
                canvas.drawBitmap(bmp(model, 10), x, y, null);
                startDrinkAnimation(model);
                setStatus(model, DRINK_1);
                break;
            case DRINK_1:
                canvas.drawBitmap(bmp(model, 10), x, y, null);
                break;
            case DRINK_2:
                canvas.drawBitmap(bmp(model, 23), x, y, null);
                break;
            case DRINK_3:
                canvas.drawBitmap(bmp(model, 14), x, y, null);
                break;
            case DRINK_4:
                canvas.drawBitmap(bmp(model, 24), x, y, null);
                break;
            case DRINK_5:
                canvas.drawBitmap(bmp(model, 25), x, y, null);
                break;
            case DRINK_6:
                canvas.drawBitmap(bmp(model, 26), x, y, null);
                break;
            case DRINK_7:
                canvas.drawBitmap(bmp(model, 33), x, y, null);
                break;
            case SING_PRE:
                canvas.drawBitmap(bmp(model, 27), x, y + mSingOffsetY, null);
                startSingAnimation(model);
                setStatus(model, SING_1);
                break;
            case SING_1:
                canvas.drawBitmap(bmp(model, 27), x, y + mSingOffsetY, null);
                break;
            case SING_2:
                canvas.drawBitmap(bmp(model, 30), x, y + mSingOffsetY, null);
                break;
            case SING_3:
                canvas.drawBitmap(bmp(model, 28), x, y + mSingOffsetY, null);
                break;
            case SING_4:
                canvas.drawBitmap(bmp(model, 29), x, y + mSingOffsetY, null);
                break;
            case CRAWL_PRE:
                canvas.drawBitmap(bmp(model, 18), x, y, null);
                startFlatAnimation(model);
                setStatus(model, CRAWL_2);
                break;
            case CRAWL_1:
                canvas.drawBitmap(bmp(model, 17), x, y, null);
                break;
            case CRAWL_2:
                canvas.drawBitmap(bmp(model, 18), x, y, null);
                break;
            case HIT_Y_BORDER:
                canvas.drawBitmap(bmp(model, model.legBehavior ? 12 : 13), x, y, null);
                break;
            case CLIMB_PRE:
                clampToBorderX(model);
                canvas.drawBitmap(bmp(model, 12), model.x, model.y, null);
                startClimbPreAnimation(model);
                setStatus(model, CLIMB_1);
                break;
            case CLIMB_1:
                canvas.drawBitmap(bmp(model, 12), x, y, null);
                break;
            case CLIMB_2:
                canvas.drawBitmap(bmp(model, 11), x, y, null);
                break;
            case CLIMB_3:
                canvas.drawBitmap(bmp(model, 13), x, y, null);
                break;
            case HIT_X_HIGH_BORDER:
                canvas.drawBitmap(bmp(model, 22), x, y, null);
                break;
            case FLY_PRE:
                canvas.drawBitmap(bmp(model, 22), x, y, null);
                startFlyPreAnimation(model);
                setStatus(model, FLY_1);
                break;
            case FLY_1:
                canvas.drawBitmap(bmp(model, 22), x, y, null);
                break;
            case FLY_2:
                canvas.drawBitmap(bmp(model, 20), x, y, null);
                break;
            case FLY_3:
                canvas.drawBitmap(bmp(model, 21), x, y, null);
                break;
            case JUMP_PRE:
                canvas.drawBitmap(bmp(model, 19), x, y, null);
                startJumpAnimation(model);
                setStatus(model, JUMP);
                break;
            case JUMP:
                canvas.drawBitmap(bmp(model, 19), x, y, null);
                break;
        }
    }

    /** 根据宠物方向取 bitmap（前 34 帧朝左，后 34 帧朝右）。 */
    private Bitmap bmp(PetModel model, int index) {
        return mBitmaps.get(model.bitmapIndex(index));
    }

    // ── 触摸处理 ─────────────────────────────────────────────────────────

    /**
     * 全屏 canvas 的触摸分发：只有命中某只宠物的 DOWN 事件才消费，
     * 否则返回 false 让事件透传给底层窗口。
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mDraggingModel = findModelAt(event.getX(), event.getY());
            if (mDraggingModel == null) {
                return false;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        PetModel model = mDraggingModel;
        if (model == null) return false;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            model.canTouched = true;
        }
        if (!model.canTouched) return true;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPointerId       = event.getPointerId(0);
                mVelocityTracker = VelocityTracker.obtain();
                model.legBehavior = true;
                model.cancelAnimator();
                mTouchStartRawX = event.getRawX();
                mTouchStartRawY = event.getRawY();
                break;

            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(event);
                // 保持与原 PetView 相同的"魔法公式"，让宠物紧跟手指
                model.x = (int) event.getRawX() - 160;
                model.y = (int) event.getRawY() - 190;
                if (checkHitYBorder(model, false)) {
                    mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
                    float vx = mVelocityTracker.getXVelocity(mPointerId);
                    if (vx > 500) {
                        setStatus(model, SWING_LEFT);
                    } else if (vx < -500) {
                        setStatus(model, SWING_RIGHT);
                    } else {
                        if (model.status == SWING_LEFT) {
                            setStatus(model, SWING_LEFT_BACK);
                            model.sendDelayMessage(DRAG, CHANGE_CHECK, 500);
                        } else if (model.status == SWING_RIGHT) {
                            setStatus(model, SWING_RIGHT_BACK);
                            model.sendDelayMessage(DRAG, CHANGE_CHECK, 500);
                        } else if (model.status != SWING_LEFT_BACK
                                && model.status != SWING_RIGHT_BACK) {
                            setStatus(model, DRAG);
                        }
                    }
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                float endRawX = event.getRawX();
                float endRawY = event.getRawY();
                if (checkHitYBorder(model, true)) {
                    int groundY = mCanvasHeight - mPetHeight;
                    if (model.y > groundY) {
                        setStatus(model, SINK);
                    } else {
                        mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
                        float vx = mVelocityTracker.getXVelocity(mPointerId);
                        float vy = mVelocityTracker.getYVelocity(mPointerId);
                        if (Math.abs(vx) > 100 || Math.abs(vy) > 100) {
                            model.isLeft = vx < 0;
                            if (vy > -6000 && vy < 0) vy = -6000;
                            model.vX = vx;
                            model.vY = vy;
                            setStatus(model, FALL_1_ACC);
                        } else {
                            setStatus(model, FALL_1);
                        }
                    }
                }
                releaseVelocityTracker();
                mDraggingModel = null;
                // 位移 < 6px 视为 click（返回 false 让 OnClickListener 生效）
                return !(Math.abs(endRawX - mTouchStartRawX) > 6
                        && Math.abs(endRawY - mTouchStartRawY) > 6);

            case MotionEvent.ACTION_CANCEL:
                releaseVelocityTracker();
                mDraggingModel = null;
                break;
        }
        return true;
    }

    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * 供 touch proxy 窗口调用：将宠物触摸事件委托给 canvas 的触摸逻辑。
     * 使用 getRawX()/getRawY() 定位，与 canvas 的坐标系一致。
     */
    public boolean handleProxyTouch(PetModel model, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mDraggingModel = model;
        }
        return onTouchEvent(event);
    }

    /**
     * 命中测试：找到触摸点下的 PetModel。
     * event.getX()/getY() 是相对于 View 左上角的坐标，
     * 与 model.x/y（content area 内坐标）直接可比。
     */
    private PetModel findModelAt(float viewX, float viewY) {
        // 从顶层（列表末尾）往下找，保证点击最上层的宠物
        for (int i = mModels.size() - 1; i >= 0; i--) {
            PetModel m = mModels.get(i);
            if (viewX >= m.x && viewX <= m.x + m.width
                    && viewY >= m.y && viewY <= m.y + m.height) {
                return m;
            }
        }
        return null;
    }

    // ── 边界检测（与 PetView 逻辑相同，操作 model.x/y） ─────────────────

    /** 检测并处理 X 轴（顶部/底部）边界碰撞。 */
    public boolean checkHitXBorder(PetModel model, boolean changeState) {
        int sw  = ScreenUtil.getScreenWidth(getContext());
        int sb  = ScreenUtil.getStatusBarHeight(getContext());
        int endY = mCanvasHeight - mPetHeight;
        int y    = model.y + (model.isUp ? -10 : 10);

        if (y >= endY) {
            model.y = endY;
            model.x = model.isLeft
                    ? (-mPetWidth / 2 + 30)
                    : (sw - mPetWidth / 2 - 30);
            setStatus(model, changeState ? STAND : HIT_X_LOW_BORDER);
            invalidate();
            return false;
        } else if (y <= -sb) {
            model.y = -sb;
            model.x = !model.isLeft
                    ? (sw - mPetWidth / 2 - 10)
                    : (-mPetWidth / 2 + 10);
            if (changeState) {
                model.isLeft = !model.isLeft;
                setStatus(model, FLY_PRE);
            } else {
                setStatus(model, HIT_X_HIGH_BORDER);
            }
            invalidate();
            return false;
        }
        return true;
    }

    /** 检测并处理 Y 轴（左侧/右侧）边界碰撞。 */
    public boolean checkHitYBorder(PetModel model, boolean changeState) {
        int sw = ScreenUtil.getScreenWidth(getContext());
        if (model.x >= sw - mPetWidth / 2) {
            model.x      = sw - mPetWidth / 2;
            model.isLeft = false;
            setStatus(model, changeState ? CLIMB_PRE : HIT_Y_BORDER);
            invalidate();
            return false;
        } else if (model.x <= -mPetWidth / 2) {
            model.x      = -mPetWidth / 2;
            model.isLeft = true;
            setStatus(model, changeState ? CLIMB_PRE : HIT_Y_BORDER);
            invalidate();
            return false;
        }
        return true;
    }

    /** 将宠物 x 坐标钳位到左/右边界（爬墙前调用）。 */
    private void clampToBorderX(PetModel model) {
        int sw = ScreenUtil.getScreenWidth(getContext());
        if (model.x >= sw - mPetWidth / 2) {
            model.isLeft = false;
            model.x      = sw - mPetWidth / 2;
        } else if (model.x <= -mPetWidth / 2) {
            model.isLeft = true;
            model.x      = -mPetWidth / 2;
        }
    }

    // ── 状态机（与 PetView.setStatus 完全对应，改用 model.status） ───────

    public boolean setStatus(PetModel model, int status) {
        boolean canChange;
        switch (status) {
            // 无条件可切换的状态
            case DRAG:
            case SWING_LEFT:  case SWING_LEFT_BACK:
            case SWING_RIGHT: case SWING_RIGHT_BACK:
            case HIT_X_LOW_BORDER:
            case FALL_1: case FALL_1_ACC:
            case SINK:
            case CLIMB_PRE:
            case HIT_Y_BORDER:
            case FLY_PRE:
            case JUMP_PRE:
                canChange = true;
                break;
            case DRAG_WAIT:
                canChange = (model.status & 0x00000F00) == 0;
                break;
            case STRUGGLE_LEFT:
            case STRUGGLE_RIGHT:
                canChange = ((model.status & 0x000000F0) ^ 0x00000020) == 0;
                break;
            case STAND:
            case STAND_WAIT:
                canChange = ((model.status & 0xF0000000) ^ 0x10000000) == 0;
                break;
            case FALL_WAIT:
                canChange = ((model.status & 0x00000F00) ^ 0x00000100) == 0;
                break;
            case FALL_2:
                canChange = model.status == FALL_WAIT || model.status == FALL_1;
                break;
            case FALL_3:
                canChange = model.status == FALL_2;
                break;
            case WALK_PRE:
                canChange = model.status == STAND_WAIT || model.status == WALK_2;
                break;
            case WALK_1: case WALK_2: case WALK_3:
                canChange = ((model.status & 0x00000F00) ^ 0x00000300) == 0;
                break;
            case DRINK_PRE:
                canChange = model.status == STAND_WAIT;
                break;
            case DRINK_1: case DRINK_2: case DRINK_3:
            case DRINK_4: case DRINK_5: case DRINK_6: case DRINK_7:
                canChange = ((model.status & 0x00000F00) ^ 0x00000400) == 0;
                break;
            case SING_PRE:
                canChange = model.status == STAND_WAIT;
                break;
            case SING_1: case SING_2: case SING_3: case SING_4:
                canChange = ((model.status & 0x00000F00) ^ 0x00000500) == 0;
                break;
            case CRAWL_PRE:
                canChange = model.status == STAND_WAIT;
                break;
            case CRAWL_1: case CRAWL_2:
                canChange = ((model.status & 0x00000F00) ^ 0x00000600) == 0;
                break;
            case CLIMB_1: case CLIMB_2: case CLIMB_3:
                canChange = ((model.status & 0x00000F00) ^ 0x00000700) == 0;
                break;
            case FLY_1: case FLY_2: case FLY_3:
                canChange = ((model.status & 0x00000F00) ^ 0x00000800) == 0;
                break;
            case JUMP:
                canChange = ((model.status & 0x00000F00) ^ 0x00000900) == 0;
                break;
            default:
                canChange = false;
        }
        if (canChange) {
            model.status = status;
        }
        return canChange;
    }

    // ── 动画实现 ──────────────────────────────────────────────────────────

    private void startRandomAnimation(PetModel model) {
        new Thread(() -> {
            while (true) {
                try { Thread.sleep(3_000); } catch (InterruptedException e) { return; }
                if (((model.status & 0x00000F00) ^ 0x00000200) != 0) return;
                Random random = new Random();
                int next = random.nextInt(1000);
                if      (next < 200) { model.sendDelayMessage(WALK_PRE,  NORMAL, 0); break; }
                else if (next < 400) { model.sendDelayMessage(DRINK_PRE, NORMAL, 0); break; }
                else if (next < 600) { model.sendDelayMessage(SING_PRE,  NORMAL, 0); break; }
                else if (next < 800) { model.sendDelayMessage(CRAWL_PRE, NORMAL, 0); break; }
                else if (next < 950) { model.sendDelayMessage(JUMP_PRE,  NORMAL, 0); break; }
                else                 { model.isLeft = random.nextBoolean(); }
            }
        }).start();
    }

    private void startWalkAnimation(PetModel model) {
        final int startX = model.x;
        final int endX   = startX + (model.isLeft
                ? -DPUtil.dip2px(getContext(), 33) : DPUtil.dip2px(getContext(), 33));
        final long duration = 2000;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.addUpdateListener(a -> {
            if (checkHitYBorder(model, false)) {
                float rate = a.getCurrentPlayTime() / (float) duration;
                boolean changed;
                if      (rate < 0.33) changed = setStatus(model, WALK_2);
                else if (rate < 0.66) changed = setStatus(model, WALK_3);
                else                  changed = setStatus(model, WALK_2);
                if (changed) { model.x = (int) ((endX - startX) * rate + startX); invalidate(); }
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancel && checkHitYBorder(model, true)) {
                    setStatus(model, new Random().nextInt(100) < 80 ? WALK_PRE : STAND);
                }
            }
        });
        anim.setStartDelay(duration / 6);
        model.objectAnimator = anim;
        anim.start();
    }

    private void startFallAnimation(PetModel model) {
        final int startY = model.y;
        final int endY   = mCanvasHeight - mPetHeight;
        final long duration = (long) Math.sqrt((endY - startY) * 2.0 / 0.002);
        if (duration == 0) { setStatus(model, FALL_2); startFallToStandUpAnimation(model); return; }
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.addUpdateListener(a -> {
            double rate = anim.getInterpolator().getInterpolation(
                    a.getCurrentPlayTime() / (float) duration);
            model.y = (int) ((endY - startY) * rate + startY);
            if (model.y > endY) model.y = endY;
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancel) { setStatus(model, FALL_2); startFallToStandUpAnimation(model); }
            }
        });
        model.objectAnimator = anim;
        anim.start();
    }

    private void startFallAccAnimation(PetModel model, float vX, float vY, boolean yLimit) {
        final int startX = model.x;
        int endX = (int) (startX + vX);
        final int startY = model.y;
        final int endY   = mCanvasHeight - mPetHeight;
        double upDur, upDist, downDur, downDist;
        float tv = vY;
        if (tv < 0) {
            tv = (tv + 5000) / 5;
            if (tv > -1000) tv = -1000;
            else if (yLimit && tv < -4000) tv = -4000;
            upDur   = -tv / 2000.0;
            upDist  = -tv * upDur - 2000 * upDur * upDur / 2;
            upDur  *= 1000;
            downDist = endY - startY + upDist;
            downDur  = Math.sqrt(downDist * 2 / 0.002);
        } else {
            upDur = 0; upDist = 0;
            if (tv < 1000) tv = 0; else tv /= 1000;
            if      (tv > 4.5) endX = startX;
            else if (tv > 1)   endX = (int) (vX / 10 + startX);
            downDist = endY - startY;
            double delta = Math.sqrt(tv * tv + 4 * 0.002 * downDist);
            downDur = (-tv + delta) / (2 * 0.002);
            if (downDur < 100) downDur = 100;
        }
        final int    fEndX      = endX;
        final int    fStartY2   = (int) (startY - upDist);
        final double fUpDist    = upDist,   fDownDist = downDist;
        final double fUpDur     = upDur,    fDownDur  = downDur;
        final long   totalDur   = (long) (upDur + downDur);
        if (totalDur == 0) { setStatus(model, FALL_2); startFallToStandUpAnimation(model); return; }
        final AccelerateInterpolator accel   = new AccelerateInterpolator();
        final DecelerateInterpolator decel   = new DecelerateInterpolator();
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(totalDur);
        anim.addUpdateListener(a -> {
            if (checkHitYBorder(model, false)) {
                float rate = a.getCurrentPlayTime() / (float) totalDur;
                model.x = (int) ((fEndX - startX) * rate + startX);
                if (rate <= fUpDur / totalDur) {
                    float r = decel.getInterpolation((float)(a.getCurrentPlayTime() / fUpDur));
                    model.y = (int) (startY - fUpDist * r);
                } else {
                    float r = accel.getInterpolation(
                            (float)((a.getCurrentPlayTime() - fUpDur) / fDownDur));
                    model.y = (int) (fDownDist * r + fStartY2);
                }
                if (model.y > endY) model.y = endY;
                invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancel && checkHitYBorder(model, true)) {
                    setStatus(model, FALL_2); startFallToStandUpAnimation(model);
                }
            }
        });
        model.objectAnimator = anim;
        anim.start();
    }

    private void startFallToStandUpAnimation(PetModel model) {
        final long duration = 1300;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.addUpdateListener(a -> {
            float rate = a.getCurrentPlayTime() / (float) duration;
            setStatus(model, rate < 0.23 ? FALL_2 : FALL_3);
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) { if (!cancel) setStatus(model, STAND); }
        });
        model.objectAnimator = anim;
        anim.start();
    }

    private void startSinkAnimation(PetModel model) {
        final int startY = model.y;
        final int endY   = mCanvasHeight + mPetHeight;
        final long duration = 300;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.addUpdateListener(a -> {
            double rate = anim.getInterpolator().getInterpolation(
                    a.getCurrentPlayTime() / (float) duration);
            model.y = (int) ((endY - startY) * rate + startY);
            if (model.y > endY) model.y = endY;
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) { if (!cancel) removeModel(model); }
        });
        model.objectAnimator = anim;
        anim.start();
    }

    private void startJumpAnimation(PetModel model) {
        float dur = 1f;
        double downDist = mCanvasHeight / 2.0;
        int jvY = -(int) ((downDist + 1000 * dur * dur) / dur);
        int jvX;
        if (model.isLeft) {
            jvX = (int) (-(model.x + mPetWidth * 2) / dur) - 100;
            jvY = 5 * jvY - 5500;
        } else {
            jvX = (int) ((ScreenUtil.getScreenWidth(getContext()) - model.x) / dur + 100);
            jvY = 5 * jvY - 5500;
        }
        model.vX = jvX; model.vY = jvY;
        startFallAccAnimation(model, jvX, jvY, false);
    }

    private void startClimbPreAnimation(PetModel model) {
        new Thread(() -> {
            clampToBorderX(model);
            model.sendDelayMessage(REDRAW, FUNCTION, 0);
            while (true) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
                if (((model.status & 0x00000F00) ^ 0x00000700) != 0) return;
                int rI = new Random().nextInt(100);
                if      (rI < 80) { model.sendDelayMessage(CLIMB_MSG, FUNCTION, 0); break; }
                else if (rI < 90) { model.sendDelayMessage(FALL_1,    NORMAL,   0); break; }
                else if (rI < 95) { model.isUp = !model.isUp; }
            }
        }).start();
    }

    private void startClimbAnimation(PetModel model) {
        final int startY = model.y;
        final int endY   = startY + (model.isUp
                ? -DPUtil.dip2px(getContext(), model.legBehavior ? 40 : 30)
                :  DPUtil.dip2px(getContext(), model.legBehavior ? 40 : 30));
        final long duration = 1500;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.addUpdateListener(a -> {
            if (checkHitXBorder(model, false)) {
                double rate = anim.getInterpolator().getInterpolation(
                        (float)(a.getCurrentPlayTime() / (double) duration));
                boolean changed;
                if      (rate < 0.33) changed = setStatus(model, model.legBehavior ? CLIMB_1 : CLIMB_3);
                else if (rate <= 0.66) changed = setStatus(model, CLIMB_2);
                else                   changed = setStatus(model, model.legBehavior ? CLIMB_3 : CLIMB_1);
                if (changed) { model.y = (int) ((endY - startY) * rate + startY); invalidate(); }
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationStart(Animator a) { clampToBorderX(model); invalidate(); }
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancel && checkHitXBorder(model, true)) {
                    model.legBehavior = !model.legBehavior; startClimbPreAnimation(model);
                }
            }
        });
        model.objectAnimator = anim;
        anim.start();
    }

    private void startFlyPreAnimation(PetModel model) {
        new Thread(() -> {
            int sb = ScreenUtil.getStatusBarHeight(getContext());
            if (model.y >= -sb) model.y = -sb;
            model.sendDelayMessage(REDRAW, FUNCTION, 0);
            while (true) {
                try { Thread.sleep(1_000); } catch (InterruptedException e) { return; }
                if (((model.status & 0x00000F00) ^ 0x00000800) != 0) return;
                int rI = new Random().nextInt(100);
                if      (rI < 80) { model.sendDelayMessage(FLY_MSG, FUNCTION, 0); break; }
                else if (rI < 90) { model.sendDelayMessage(FALL_1,  NORMAL,   0); break; }
                else if (rI < 95) { model.isLeft = !model.isLeft; }
            }
        }).start();
    }

    private void startFlyAnimation(PetModel model) {
        final int startX = model.x;
        final int endX   = startX + (model.isLeft
                ? -DPUtil.dip2px(getContext(), 24) : DPUtil.dip2px(getContext(), 24));
        final long duration = 1000;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.addUpdateListener(a -> {
            if (checkHitYBorder(model, false)) {
                float rate = a.getCurrentPlayTime() / (float) duration;
                boolean changed;
                if      (rate < 0.33) changed = setStatus(model, model.flyBehavior ? FLY_1 : FLY_3);
                else if (rate <= 0.66) changed = setStatus(model, FLY_2);
                else                   changed = setStatus(model, model.flyBehavior ? FLY_3 : FLY_1);
                if (changed) { model.x = (int) ((endX - startX) * rate + startX); invalidate(); }
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancel && checkHitYBorder(model, true)) {
                    model.flyBehavior = !model.flyBehavior; startFlyPreAnimation(model);
                }
            }
        });
        anim.setStartDelay(duration / 6);
        model.objectAnimator = anim;
        anim.start();
    }

    private void startFlatAnimation(PetModel model) {
        new Thread(() -> {
            try { Thread.sleep(5_000); } catch (InterruptedException e) { return; }
            if (((model.status & 0x00000F00) ^ 0x00000600) != 0) return;
            int next = new Random().nextInt(100);
            if (next < 20) model.sendDelayMessage(STAND,     NORMAL,   0);
            else           model.sendDelayMessage(CRAWL_MSG, FUNCTION, 0);
        }).start();
    }

    private void startCrawlAnimation(PetModel model) {
        final int startX = model.x;
        final int endX   = startX + (model.isLeft
                ? -DPUtil.dip2px(getContext(), 40) : DPUtil.dip2px(getContext(), 40));
        final long duration = 1500;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.addUpdateListener(a -> {
            if (checkHitYBorder(model, false)) {
                double rate = anim.getInterpolator().getInterpolation(
                        (float)(a.getCurrentPlayTime() / (double) duration));
                if (rate < 0.7) {
                    setStatus(model, CRAWL_1);
                } else {
                    if (setStatus(model, CRAWL_2)) {
                        model.x = (int) ((endX - startX) * (rate - 0.7) + startX);
                        invalidate();
                    }
                }
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancel && checkHitYBorder(model, true)) {
                    setStatus(model, CRAWL_2); startFlatAnimation(model);
                }
            }
        });
        anim.setStartDelay(duration / 6);
        model.objectAnimator = anim;
        anim.start();
    }

    private void startDrinkAnimation(PetModel model) {
        new Thread(() -> {
            try { Thread.sleep(15_000); } catch (InterruptedException e) { return; }
            if (((model.status & 0x00000F00) ^ 0x00000400) != 0) return;
            int next = new Random().nextInt(100);
            if (next < 20) model.sendDelayMessage(STAND,      NORMAL,   0);
            else           model.sendDelayMessage(DRINK_LIKE, FUNCTION, 0);
        }).start();
    }

    private void startDrinkLikeAnimation(PetModel model) {
        final long duration = 2_000;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.addUpdateListener(a -> {
            float rate = a.getCurrentPlayTime() / (float) duration;
            setStatus(model, rate < 0.5 ? DRINK_2 : DRINK_3);
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancel) startDrinkLikeMoreAnimation(model);
            }
        });
        model.objectAnimator = anim;
        anim.start();
    }

    private void startDrinkLikeMoreAnimation(PetModel model) {
        final long duration = 5_000;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.addUpdateListener(a -> {
            float rate = a.getCurrentPlayTime() / (float) duration;
            if      (rate < 0.25) setStatus(model, DRINK_4);
            else if (rate < 0.5)  setStatus(model, DRINK_5);
            else if (rate < 0.75) setStatus(model, DRINK_6);
            else                  setStatus(model, DRINK_7);
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancel) {
                    if (new Random().nextInt(100) < 50) startDrinkLikeMoreAnimation(model);
                    else                                setStatus(model, STAND);
                }
            }
        });
        model.objectAnimator = anim;
        anim.start();
    }

    private void startSingAnimation(PetModel model) {
        final long duration = 5_000;
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.addUpdateListener(a -> {
            float rate = a.getCurrentPlayTime() / (float) duration;
            if      (rate < 0.25) setStatus(model, SING_1);
            else if (rate < 0.5)  setStatus(model, SING_2);
            else if (rate < 0.75) setStatus(model, SING_3);
            else                  setStatus(model, SING_4);
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancel) {
                    if (new Random().nextInt(100) < 80) startSingAnimation(model);
                    else                                setStatus(model, STAND);
                }
            }
        });
        model.objectAnimator = anim;
        anim.start();
    }

    private void startStrugglePreAnimation(PetModel model, final int waitNum) {
        if (model.struggleThread != null && model.struggleThread.isAlive()) {
            model.struggleThread.interrupt();
        }
        model.struggleThread = new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) { return; }
            if (waitNum == model.waitNum) {
                model.sendDelayMessage(STRUGGLE, FUNCTION, waitNum, 0);
            }
        });
        model.struggleThread.start();
    }

    private void startStruggleAnimation(PetModel model, final int waitNum, final int time) {
        final boolean ori = new Random().nextBoolean();
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1000);
        anim.addUpdateListener(a -> {
            if (waitNum == model.waitNum) {
                float rate = a.getCurrentPlayTime() / 1000f;
                boolean changed;
                if      (rate < 0.25) changed = setStatus(model, ori ? STRUGGLE_LEFT  : STRUGGLE_RIGHT);
                else if (rate < 0.5)  changed = setStatus(model, ori ? STRUGGLE_RIGHT : STRUGGLE_LEFT);
                else if (rate < 0.75) changed = setStatus(model, ori ? STRUGGLE_LEFT  : STRUGGLE_RIGHT);
                else                  changed = setStatus(model, ori ? STRUGGLE_RIGHT : STRUGGLE_LEFT);
                if (changed) invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancel = false;
            @Override public void onAnimationCancel(Animator a) { cancel = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancel) {
                    setStatus(model, DRAG_WAIT);
                    if (time == 2) { model.canTouched = false; setStatus(model, FALL_1); }
                    else           { startStruggleAnimation(model, waitNum, time + 1); }
                }
            }
        });
        anim.setStartDelay(1000);
        model.objectAnimator = anim;
        anim.start();
    }

    // ── 每个 PetModel 独立的 Handler（运行在主线程） ─────────────────────

    static class PetHandler extends Handler {
        private final PetCanvasView mView;
        private final PetModel      mModel;

        PetHandler(PetCanvasView view, PetModel model) {
            mView  = view;
            mModel = model;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case NORMAL:
                    mView.setStatus(mModel, msg.what);
                    break;
                case FUNCTION:
                    switch (msg.what) {
                        case DRINK_LIKE: mView.startDrinkLikeAnimation(mModel);              break;
                        case CRAWL_MSG:  mView.startCrawlAnimation(mModel);                  break;
                        case CLIMB_MSG:  mView.startClimbAnimation(mModel);                  break;
                        case REDRAW:     mView.invalidate();                                 break;
                        case FLY_MSG:    mView.startFlyAnimation(mModel);                    break;
                        case STRUGGLE:   mView.startStruggleAnimation(mModel, msg.arg2, 0);  break;
                    }
                    break;
                case CHANGE_CHECK:
                    int type  = msg.what   & 0x00000F00;
                    int mType = mModel.status & 0x00000F00;
                    if (type == mType) {
                        mView.setStatus(mModel, msg.what);
                    }
                    break;
            }
        }
    }
}
