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
import android.util.Log;
import android.widget.Toast;

/**
 * @author Sora
 * @date 2016/11/5
 * <p>
 * A util class about debug and toast.
 * Too lazy to produce this class.
 * 跟调试和显示Toast相关的工具类，懒到一定境界的产物。
 */

public class LogAndToastUtil {

    private static final boolean IF_PRINT = true;

    public static void LogV(String s) {
        if (IF_PRINT) {
            Log.v("aaa", s);
        }
    }

    private static Toast toast;

    public static void ToastOut(Context context, String s) {
        if (toast == null) {
            toast = Toast.makeText(
                    context,
                    s,
                    Toast.LENGTH_LONG);
        } else {
            toast.setText(s);
        }
        toast.show();
    }
}
