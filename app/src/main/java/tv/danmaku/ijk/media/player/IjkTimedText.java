/*
 * Copyright (C) 2016 Zheng Yuan <zhengyuan10503@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tv.danmaku.ijk.media.player;

import android.graphics.Rect;

import java.lang.String;

public final class IjkTimedText {

    private Rect mTextBounds = null;
    private String mTextChars = null;
    /**
     * int planeWidth = bitmapData[0];
     * int planeHeight = bitmapData[1];
     * int bitmapX = bitmapData[2];
     * int bitmapY = bitmapData[3];
     * int bitmapWidth = bitmapData[4];
     * int bitmapHeight = bitmapData[5];
     * Bitmap bitmap = Bitmap.createBitmap(bitmapData, 6, bitmapWidth, bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
     */
    private int[] mBitmapData = null;

    public IjkTimedText(Rect bounds, String text) {
        mTextBounds = bounds;
        mTextChars = text;
    }

    public IjkTimedText(int[] bitmap) {
        this.mBitmapData = bitmap;
    }

    public Rect getBounds() {
        return mTextBounds;
    }


    public int[] getBitmapData() {
        return mBitmapData;
    }

    public String getText() {
        return mTextChars;
    }
}
