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

package com.sorashiro.desktoppet.tool;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.view.WindowManager;

public class ScreenUtil {

    //获取屏幕宽度
    public static int getScreenWidth(Context context) {
        WindowManager mWindowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        mWindowManager.getDefaultDisplay().getSize(point);
        return point.x;
    }

    //获取屏幕高度
    public static int getScreenHeight(Context context) {
        WindowManager mWindowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        mWindowManager.getDefaultDisplay().getSize(point);
        return point.y;
    }


    /**
     * 获取状态栏高度(px)
     *
     * @return 状态栏高度px
     */
    public static int getStatusBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resources.getDimensionPixelSize(resourceId);
    }

}
