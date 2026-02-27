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

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Message;

/**
 * 单个桌宠的数据模型。
 *
 * 原先散落在 PetView 中的坐标、状态、速度等字段统一收拢到这里，
 * 以便 PetCanvasView 用纯 Canvas 坐标管理所有宠物的位置，
 * 不再依赖 WindowManager.LayoutParams.x/y 做定位。
 */
public class PetModel {

    // ── 屏幕坐标（像素，原点为屏幕左上角，含 status bar） ──────────────
    public int x;
    public int y;

    // ── 宠物尺寸（由 PetCanvasView 统一写入，100dp 转像素） ─────────────
    public int width;
    public int height;

    // ── 状态机 ──────────────────────────────────────────────────────────
    public int status = -1;

    // ── 运动方向 ─────────────────────────────────────────────────────────
    public boolean isLeft      = true;   // 朝左为 true
    public boolean isUp        = true;   // 爬墙时向上为 true
    public boolean legBehavior = true;   // 控制爬墙/行走步伐交替
    public boolean flyBehavior = true;   // 控制飞行翅膀交替

    // ── 触摸状态 ─────────────────────────────────────────────────────────
    public boolean canTouched = true;

    // ── 抛体初速度（FALL_1_ACC / JUMP 时由外部写入） ─────────────────────
    public float vX;
    public float vY;

    // ── 挣扎计数（防止旧的挣扎动画在拖拽释放后继续执行） ────────────────
    public int waitNum = 0;

    // ── 当前正在运行的动画（ValueAnimator 纯作计时器） ────────────────────
    public ValueAnimator objectAnimator;

    // ── 挣扎预备线程（需要能 interrupt） ────────────────────────────────
    public Thread struggleThread;

    // ── 每个宠物独立的 Handler（运行在主线程） ──────────────────────────
    public Handler handler;

    public PetModel() {
    }

    /**
     * 取当前帧 bitmap 在 bitmaps 列表中的索引。
     * 前 34 帧朝左，后 34 帧朝右（水平镜像）。
     */
    public int bitmapIndex(int frameIndex) {
        return frameIndex + (isLeft ? 0 : 34);
    }

    /** 取消正在运行的动画（若有）。 */
    public void cancelAnimator() {
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
    }

    /** 向自身 Handler 发送带延迟的消息。 */
    public void sendDelayMessage(int what, int arg1, long delay) {
        if (handler == null) return;
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg1 = arg1;
        handler.sendMessageDelayed(msg, delay);
    }

    /** 向自身 Handler 发送带延迟的消息（含 arg2，用于传递 waitNum 等附加值）。 */
    public void sendDelayMessage(int what, int arg1, int arg2, long delay) {
        if (handler == null) return;
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        handler.sendMessageDelayed(msg, delay);
    }
}
