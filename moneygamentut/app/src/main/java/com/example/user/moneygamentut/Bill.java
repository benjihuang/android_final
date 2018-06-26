package com.example.user.moneygamentut;

import android.graphics.Bitmap;

public class Bill {
    public int		left;
    public int		top;
    public Bitmap	bitmap;
    public int		dollar;

    public Bill(Bitmap bitmap, int dollar, int left, int top) {
        this.bitmap = bitmap;
        this.dollar = dollar;
        this.left = left;
        this.top = top;
    }
}
