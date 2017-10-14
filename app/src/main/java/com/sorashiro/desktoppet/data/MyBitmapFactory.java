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

package com.sorashiro.desktoppet.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.sorashiro.desktoppet.tool.DPUtil;

import java.util.ArrayList;

public class MyBitmapFactory {

    private Context           mContext;
    private ArrayList<Bitmap> mBitmaps;

    private static MyBitmapFactory instance = null;

    private MyBitmapFactory(Context context) {
        int initWidth = DPUtil.dip2px(context, 100);
        int initHeight = DPUtil.dip2px(context, 100);
        mBitmaps = new ArrayList<>();
        for (int i = 0; i < 34; i++) {
            int id = context.getResources().getIdentifier("tianyi_" + i, "drawable", context.getPackageName());
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, initWidth, initHeight, true);
            mBitmaps.add(scaledBitmap);
        }
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1);
        for (int i = 0; i < 34; i++) {
            Bitmap bitmap = mBitmaps.get(i);
            Bitmap flipBitmap = Bitmap.createBitmap(bitmap, 0, 0, initWidth, initHeight, matrix, false);
            mBitmaps.add(flipBitmap);
        }
    }

    public static MyBitmapFactory getInstance(Context context) {
        if (instance == null) {
            synchronized (MyBitmapFactory.class) {
                if (instance == null) {
                    instance = new MyBitmapFactory(context);
                }
            }
        }
        return instance;
    }

    public ArrayList<Bitmap> getBitmaps() {
        return mBitmaps;
    }

    public void setBitmaps(ArrayList<Bitmap> bitmaps) {
        mBitmaps = bitmaps;
    }
}
