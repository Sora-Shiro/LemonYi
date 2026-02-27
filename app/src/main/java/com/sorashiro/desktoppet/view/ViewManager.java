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
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.sorashiro.desktoppet.R;
import com.sorashiro.desktoppet.tool.DPUtil;
import com.sorashiro.desktoppet.tool.LogAndToastUtil;
import com.sorashiro.desktoppet.tool.ScreenUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 管理唯一的全屏 {@link PetCanvasView} 浮窗，以及每只宠物对应的触摸代理窗口。
 *
 * <h3>架构说明</h3>
 * <ul>
 *   <li><b>Canvas 层（渲染）</b>：MATCH_PARENT 全屏透明覆盖层，
 *       设置 {@code FLAG_NOT_TOUCHABLE}，不接收任何触摸事件，
 *       所有宠物的绘制通过 {@code invalidate()} 完成，零 IPC 开销。</li>
 *   <li><b>Touch Proxy 层（输入）</b>：每只宠物对应一个 100×100 dp 透明 View，
 *       大小等于宠物，设置 {@code FLAG_NOT_TOUCH_MODAL}；
 *       宠物区域外的触摸自动透传给底层窗口（如桌面/其他 App），
 *       宠物区域内的触摸委托给 {@link PetCanvasView#handleProxyTouch}。</li>
 * </ul>
 *
 * <h3>性能</h3>
 * Touch proxy 位置通过 {@code WindowManager.updateViewLayout()} 更新，
 * 但 proxy View 本身透明无绘制内容，成本远低于原来的 PetView（含 Bitmap 绘制）。
 */
public class ViewManager {

    private static ViewManager instance = null;

    private final WindowManager mWindowManager;
    private final Context       mContext;

    /** 唯一的全屏 Canvas 覆盖层；首次 showPetView() 时懒创建。 */
    private PetCanvasView mCanvasView;

    /** 每只宠物对应的透明触摸代理 View（100×100 dp，FLAG_NOT_TOUCH_MODAL）。 */
    private final Map<PetModel, View>                          mTouchProxies = new HashMap<>();
    private final Map<PetModel, WindowManager.LayoutParams>    mTouchParams  = new HashMap<>();

    private int mPetWidth;
    private int mPetHeight;

    private ViewManager(Context context) {
        mContext       = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mPetWidth  = DPUtil.dip2px(context, 100);
        mPetHeight = DPUtil.dip2px(context, 100);
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

    // ── 内部辅助 ─────────────────────────────────────────────────────────

    /** 确保全屏 Canvas 覆盖层已添加到 WindowManager。 */
    private void ensureCanvasView() {
        if (mCanvasView != null) return;

        mCanvasView = new PetCanvasView(mContext);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.width   = WindowManager.LayoutParams.MATCH_PARENT;
        params.height  = WindowManager.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.TOP | Gravity.START;
        params.type    = overlayType();
        // FLAG_NOT_TOUCHABLE：canvas 只负责渲染，不接收任何触摸事件
        params.flags   = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        params.format  = PixelFormat.RGBA_8888;

        mWindowManager.addView(mCanvasView, params);

        // 每帧 onDraw 回调：同步所有 touch proxy 窗口位置
        mCanvasView.setPositionChangedCallback(() -> {
            for (PetModel m : mCanvasView.getModels()) {
                updateTouchProxy(m);
            }
        });

        // 宠物内部被移除（如 SINK 动画结束）时同步清理 touch proxy
        mCanvasView.setOnModelRemovedListener(model -> removeTouchProxy(model));
    }

    /** 移除 Canvas 覆盖层并清空引用。 */
    private void destroyCanvasView() {
        if (mCanvasView != null) {
            mCanvasView.setPositionChangedCallback(null);
            mCanvasView.setOnModelRemovedListener(null);
            mWindowManager.removeViewImmediate(mCanvasView);
            mCanvasView = null;
        }
    }

    /** 返回当前 API 对应的 overlay window type。 */
    private int overlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= 24) {
            return WindowManager.LayoutParams.TYPE_PHONE;
        } else {
            return WindowManager.LayoutParams.TYPE_TOAST;
        }
    }

    /** 为 model 添加 touch proxy 窗口（100×100 dp 透明 View）。 */
    private void addTouchProxy(final PetModel model) {
        View proxy = new View(mContext) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return mCanvasView != null && mCanvasView.handleProxyTouch(model, event);
            }
        };

        WindowManager.LayoutParams p = new WindowManager.LayoutParams();
        p.width   = mPetWidth;
        p.height  = mPetHeight;
        p.x       = model.x;
        p.y       = model.y;
        p.gravity = Gravity.TOP | Gravity.START;
        p.type    = overlayType();
        p.flags   = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        p.format  = PixelFormat.TRANSPARENT;

        mWindowManager.addView(proxy, p);
        mTouchProxies.put(model, proxy);
        mTouchParams.put(model, p);
    }

    /** 更新 model 对应 touch proxy 窗口的位置。 */
    private void updateTouchProxy(PetModel model) {
        // 拖拽中不移动 proxy：若 proxy 随 model 移动，event.getX()/getY() 会
        // 相对于新窗口原点重算，导致 VelocityTracker 速度失真，甩出动画无法正确触发。
        if (mCanvasView != null && mCanvasView.isDragging(model)) return;
        View proxy = mTouchProxies.get(model);
        WindowManager.LayoutParams p = mTouchParams.get(model);
        if (proxy == null || p == null) return;
        if (p.x == model.x && p.y == model.y) return; // 无变化则跳过 IPC
        p.x = model.x;
        p.y = model.y;
        mWindowManager.updateViewLayout(proxy, p);
    }

    /** 移除 model 对应的 touch proxy 窗口。 */
    private void removeTouchProxy(PetModel model) {
        View proxy = mTouchProxies.remove(model);
        mTouchParams.remove(model);
        if (proxy != null) {
            mWindowManager.removeViewImmediate(proxy);
        }
    }

    /** 移除所有 touch proxy 窗口。 */
    private void removeAllTouchProxies() {
        for (View proxy : mTouchProxies.values()) {
            mWindowManager.removeViewImmediate(proxy);
        }
        mTouchProxies.clear();
        mTouchParams.clear();
    }

    // ── 公开 API ─────────────────────────────────────────────────────────

    /** 生成一只新宠物，从屏幕顶部随机位置落下。 */
    public void showPetView() {
        int count = (mCanvasView != null) ? mCanvasView.getModels().size() : 0;
        if (count >= 11) {
            LogAndToastUtil.ToastOut(mContext, mContext.getString(R.string.too_much_lemonyi));
            return;
        }
        if (count == 1) {
            LogAndToastUtil.ToastOut(mContext, mContext.getString(R.string.carefully));
        }

        ensureCanvasView();

        int seed = ScreenUtil.getScreenWidth(mContext) - mPetWidth;
        seed = seed < 0 ? 0 : seed;

        Random random = new Random();
        int startX = random.nextInt(seed);
        int startY = -ScreenUtil.getStatusBarHeight(mContext) - mPetHeight;

        PetModel model = mCanvasView.addPet(startX, startY, random.nextBoolean());
        addTouchProxy(model);
    }

    /** 移除所有宠物并销毁覆盖层。 */
    public void removeAllPetView() {
        removeAllTouchProxies();
        if (mCanvasView != null) {
            mCanvasView.removeAll();
            destroyCanvasView();
        }
    }

    /**
     * 按索引移除单只宠物。
     * 若移除后已无宠物，则顺带销毁覆盖层以释放资源。
     */
    public void removePetView(int index) {
        if (mCanvasView == null || mCanvasView.getModels().isEmpty()) return;
        if (index < 0 || index >= mCanvasView.getModels().size()) return;

        PetModel model = mCanvasView.getModels().get(index);
        removeTouchProxy(model);
        mCanvasView.removeModel(model);

        if (mCanvasView.getModels().isEmpty()) {
            destroyCanvasView();
        }
    }
}
