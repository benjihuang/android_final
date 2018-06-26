package com.example.user.moneygamentut;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Game extends Activity {
    private int						screenWidth, screenHeight;				// 螢幕長寬
    private int						level		= 10;						// 鈔票數
    private int						speed		= 5;						// 落下速度
    private int						score		= 0;						// 分數
    private float					mScaleFactor;
    private ScaleGestureDetector	mScaleDetector;
    private Canvas					canvas;
    private ArrayList<Bill>			bills		= new ArrayList<Bill>();
    private MyCountDown				cdTimer;								// 倒數計時器
    private String					remainingTime;							// 剩餘時間
    Thread							mainLoop;
    BillMove						billMove;								// 執行落下動作
    private boolean					terminate	= false;					// 處理Thread結束

    /* 畫面繪出與觸碰事件 */
    class MySurfaceView extends SurfaceView implements Runnable {
        BitmapDrawable	background;
        Paint			paint	= new Paint();

        public MySurfaceView(Context context) {
            super(context);

            /* 取得螢幕長寬 */
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;

            /* 設定背景圖 */
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.grid);
            background = new BitmapDrawable(context.getResources(), bitmap);
            background.setBounds(0, 0, screenWidth, screenHeight);

            paint.setColor(Color.BLACK);
            paint.setTextSize(70);

            initilizeGame(context); // 隨機重置圖片位置

            mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
            mScaleFactor = screenHeight; // 重設手勢範圍

            mainLoop = new Thread(this);
            mainLoop.start();
        }

        /* 畫面繪出 */
        void doDraw() {
            canvas = getHolder().lockCanvas();
            if (canvas != null) {
                background.draw(canvas);

                for (int i = 0; i < bills.size(); i++) {
                    Bitmap bitmap = bills.get(i).bitmap;
                    int left = bills.get(i).left;
                    int top = bills.get(i).top;

                    canvas.drawBitmap(bitmap, left, top, null);
                }

                String s = "Score" + String.valueOf(score);
                canvas.drawText(s, 5, 200, paint); // 畫出分數
                canvas.drawText(remainingTime, 5, 100, paint); // 畫出剩餘秒數：
                getHolder().unlockCanvasAndPost(canvas);
            }
        }

        public void run() {
            while (!terminate) {
                doDraw();
            }
        }

        /* 觸碰事件 */
        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            mScaleDetector.onTouchEvent(ev); // 讓 ScaleGestureDetector 檢查所有事件

            final int action = ev.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP: {
                    final float x = ev.getX();
                    final float y = ev.getY();

                    for (int i = 0; i < bills.size(); i++) {
                        Bitmap bitmap = bills.get(i).bitmap;
                        int range = Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2; // 設定抓取範圍

                        int dollar = bills.get(i).dollar;
                        int left = bills.get(i).left - range;
                        int top = bills.get(i).top - range;
                        int right = bills.get(i).left + bitmap.getWidth() + range;
                        int bottum = bills.get(i).top + bitmap.getHeight() + range;

                        // 檢查點是否在鈔票上
                        if (x >= left && x <= right && y >= top && y <= bottum) {
                            score += dollar; // 加分
                            bills.set(i, ResetBill());
                        }
                    }

                    mScaleFactor = 0; // 重設手勢範圍
                    break;
                }
            }
            return true;
        }

        /* 偵測抓取動作(縮放手勢) */
        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                mScaleFactor = detector.getCurrentSpan(); // 取得目前兩點間距離
                invalidate();
                return true;
            }
        }
    }

    /* 控制鈔票下降速度 */
    class BillMove extends Thread {
        void doMove() {
            for (int i = 0; i < bills.size(); i++) {
                bills.get(i).top += speed; // 下降幅度
                // 重置跑出畫面的鈔票
                if (bills.get(i).top > screenHeight) {
                    bills.set(i, ResetBill());
                }
            }

            try {
                Thread.sleep(40); // 限制位置更新速率
            } catch (Exception ex) {
            }
        }

        public void run() {
            while (!terminate) {
                doMove();
            }
        }
    }

    /* 倒數計時器 */
    public class MyCountDown extends CountDownTimer {
        Context	context;

        public MyCountDown(long millisInFuture, long countDownInterval, Context context) {
            super(millisInFuture, countDownInterval);
            this.context = context;
        }

        @Override
        public void onFinish() {
            remainingTime = "Time 0 s";

            Button button = new Button(context);
            button.setText("OK");
            final Dialog dialog = new Dialog(context);
            dialog.setTitle("You get " + score);
            dialog.setContentView(button);
            dialog.show();
            button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                    Game.this.finish();
                }
            });

            terminate = true;
            mainLoop.interrupt();
            billMove.interrupt();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            remainingTime = "Time " + (millisUntilFinished / 1000 + 1) + " s";
        }
    }

    /* 將圖重新放在畫面上方隨機位置 */
    private void initilizeGame(Context context) {
        bills.clear();

        for (int i = 0; i < level; i++) {
            Bill bill = ResetBill();
            bills.add(bill);
        }

        score = 0; // 重置分數

        cdTimer = new MyCountDown(30000, 500, context); // 倒數計時器
        cdTimer.start();

        billMove = new BillMove(); // 處理鈔票移動的Thread
        billMove.start();
    }

    /* 隨機產生一張鈔票 */
    private Bill ResetBill() {
        Random r = new Random();

        Bitmap bitmap;
        int dollar = 100;
        int j = r.nextInt(100);
        if (j < 70) {
            // 100元
            bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.bill100);
        } else if (j < 90) {
            // 500元
            dollar = 500;
            bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.bill500);
        } else {
            // 1000元
            dollar = 1000;
            bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.bill1000);
        }

        int left = r.nextInt(screenWidth - bitmap.getWidth());
        int top = 0 - bitmap.getHeight() - r.nextInt(screenHeight) * 2;
        Bill bill = new Bill(bitmap, dollar, left, top);

        return bill;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new MySurfaceView(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        terminate = true;
        mainLoop.interrupt();
        billMove.interrupt();
        cdTimer.cancel();
    }
}
