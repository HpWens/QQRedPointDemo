package com.github.redpointdemo.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.redpointdemo.R;

import java.lang.ref.WeakReference;

/**
 * Created by jms on 2016/11/4.
 */

public class RedPointView extends FrameLayout {

    private PointF mCurPoint, mCenterPoint;

    private int mRadius; //小球半径

    private Paint mPaint;   //画笔

    private Path mPath;  //绘制路径

    private boolean mTouch = false; //是否触摸文本  手指抬起后不保留上一次的记录

    private boolean mMoreDragText = true;//防止手指抬起后文本未回到原点 多次拖动小球

    private boolean mTouchText = false; //是否触摸文本  手指抬起后保留上一次的记录

    private boolean mOnlyOneMoreThan = false; //在拉伸过程中 是否有一次超过最大限度

    private boolean mExplodeAnimator = false; //是否显示图片爆炸动画

    private boolean mAllowDrag = true;      //是否允许拖拽文本   爆炸开始后不允许拖拽    

    private float mReleaseValue;   //在拉伸区域内 手指松开（释放）后的动画属性值

    private TextView mDragTextView; //拖拽的文本控件

    private ImageView mExplodeImage;  //爆炸的图片控件

    private static final int DEFAULT_RADIUS = 20; //默认的小球半径宽度

    private int[] mExplodeImages = new int[]{
            R.mipmap.idp,
            R.mipmap.idq,
            R.mipmap.idr,
            R.mipmap.ids,
            R.mipmap.idt};  //爆炸的图片集合

    public RedPointView(Context context) {
        this(context, null);
    }

    public RedPointView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RedPointView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mCurPoint = new PointF();
        mCenterPoint = new PointF();

        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);

        mPath = new Path();

        mDragTextView = new TextView(getContext());
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mDragTextView.setLayoutParams(lp);
        mDragTextView.setPadding(10, 10, 10, 10);
        mDragTextView.setBackgroundResource(R.drawable.tv_bg);
        mDragTextView.setText("99+");

        mExplodeImage = new ImageView(getContext());
        mExplodeImage.setLayoutParams(lp);
        mExplodeImage.setImageResource(R.mipmap.idp);
        mExplodeImage.setVisibility(View.INVISIBLE);

        addView(mDragTextView);
        addView(mExplodeImage);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        layoutDragText();
    }

    /**
     * init drag text layout
     */
    private void layoutDragText() {
        int width = mDragTextView.getWidth();
        int height = mDragTextView.getHeight();
        mDragTextView.layout((int) (mCenterPoint.x - width / 2), (int) (mCenterPoint.y - height / 2)
                , (int) (mCenterPoint.x + width / 2), (int) (mCenterPoint.y + height / 2));
    }

    /**
     * 爆炸图片的位置
     */
    private void layoutExplodeImage() {
        mExplodeImage.setX(mCurPoint.x - mDragTextView.getWidth() / 2);
        mExplodeImage.setY(mCurPoint.y - mDragTextView.getHeight() / 2);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterPoint.x = w / 2;
        mCenterPoint.y = h / 2;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mAllowDrag) {
            canvas.saveLayer(new RectF(0, 0, getWidth(), getHeight()), mPaint, Canvas.ALL_SAVE_FLAG);
            if (mTouch) {

                calculatePath(mCurPoint.x, mCurPoint.y);
                onlyOneMoreThan();

                if (!mOnlyOneMoreThan) {
                    canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, mRadius, mPaint);
                    canvas.drawCircle(mCurPoint.x, mCurPoint.y, mRadius, mPaint);
                    canvas.drawPath(mPath, mPaint);
                }

                mDragTextView.setX(mCurPoint.x - mDragTextView.getWidth() / 2);
                mDragTextView.setY(mCurPoint.y - mDragTextView.getHeight() / 2);

            } else {
                float dx = mCurPoint.x - mCenterPoint.x;
                float dy = mCurPoint.y - mCenterPoint.y;

                if (mTouchText && !mOnlyOneMoreThan) {
                    calculatePath(mCurPoint.x - dx * (1.0f - mReleaseValue),
                            mCurPoint.y - dy * (1.0f - mReleaseValue));
                    canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, mRadius, mPaint);
                    canvas.drawPath(mPath, mPaint);
                }

                mDragTextView.setX(dx * mReleaseValue + mCenterPoint.x - mDragTextView.getWidth() / 2);
                mDragTextView.setY(dy * mReleaseValue + mCenterPoint.y - mDragTextView.getHeight() / 2);

            }
            canvas.restore();
        }
        super.dispatchDraw(canvas);
    }

    /**
     * 获取在拉伸区域内的释放动画
     *
     * @return
     */
    private Animator getReleaseAnimator() {
        final ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 0.0f);
        animator.setDuration(500);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.addUpdateListener(new MyAnimatorUpdateListener(this) {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mReleaseValue = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mMoreDragText = true;
                mTouchText = false;
                mOnlyOneMoreThan = false;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mMoreDragText = false;
            }
        });
        animator.setInterpolator(new OvershootInterpolator());
        return animator;
    }

    /**
     * @return
     */
    private Animator getExplodeAnimator() {
        ValueAnimator animator = ValueAnimator.ofInt(0, mExplodeImages.length - 1);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(1000);
        animator.addUpdateListener(new MyAnimatorUpdateListener(this) {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mExplodeImage.setBackgroundResource(mExplodeImages[(int) animation.getAnimatedValue()]);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mExplodeImage.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mExplodeImage.setVisibility(View.VISIBLE);
                mDragTextView.setVisibility(View.GONE);
                mAllowDrag = false;
            }
        });
        return animator;
    }

    /**
     * 开始释放动画
     */
    private void startReleaseAnimator() {
        getReleaseAnimator().start();
    }

    /**
     * 计算当前路径
     *
     * @param currentX
     * @param currentY
     */
    private void calculatePath(float currentX, float currentY) {

        float centerX = mCenterPoint.x;
        float centerY = mCenterPoint.y;

        float dx = currentX - centerX;
        float dy = currentY - centerY;

        mRadius = calculateRadius(dx, dy);

        double a = Math.atan(dy / dx);

        float offsetX = (float) (mRadius * Math.sin(a));
        float offsetY = (float) (mRadius * Math.cos(a));

        float x1 = centerX + offsetX;
        float y1 = centerY - offsetY;

        float x2 = currentX + offsetX;
        float y2 = currentY - offsetY;

        float x3 = centerX - offsetX;
        float y3 = centerY + offsetY;

        float x4 = currentX - offsetX;
        float y4 = currentY + offsetY;

        float cX = (currentX + centerX) / 2;
        float cY = (currentY + centerY) / 2;

        mPath.reset();
        mPath.moveTo(x1, y1);
        mPath.quadTo(cX, cY, x2, y2);
        mPath.lineTo(x4, y4);
        mPath.quadTo(cX, cY, x3, y3);
        mPath.lineTo(x1, y1);
        mPath.close();

    }

    private void onlyOneMoreThan() {
        if (mRadius == 8) {
            mOnlyOneMoreThan = true;
            mExplodeAnimator = true;
        } else {
            mExplodeAnimator = false;
        }
    }

    private int calculateRadius(float dx, float dy) {
        float distance = (float) Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        int radius = DEFAULT_RADIUS - (int) (distance / 18); //18 根据拉伸情况
        if (radius < 8) {
            radius = 8;
        }
        return radius;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mMoreDragText) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Rect rect = new Rect();
                    int[] location = new int[2];
                    mDragTextView.getLocationOnScreen(location);
                    rect.left = location[0];
                    rect.top = location[1];
                    rect.right = mDragTextView.getWidth() + rect.left;
                    rect.bottom = mDragTextView.getHeight() + rect.top;
                    if (rect.contains((int) event.getRawX(), (int) event.getRawY())) {
                        mTouch = true;
                        mTouchText = true;
                    } else {
                        mTouchText = false;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    mTouch = false;
                    if (mTouchText) {
                        startReleaseAnimator();
                        if (mExplodeAnimator && mAllowDrag) {
                            layoutExplodeImage();
                            getExplodeAnimator().start();
                        }
                    }
                    break;
            }
            mCurPoint.set(event.getX(), event.getY());
            if (mTouch)
                postInvalidate();
        }
        return true;
    }

    private static class MyAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {

        private WeakReference<RedPointView> mWeakReference;

        public MyAnimatorUpdateListener(RedPointView redPointView) {
            mWeakReference = new WeakReference<RedPointView>(redPointView);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            RedPointView redPointView = mWeakReference.get();
            if (redPointView == null) {
                return;
            }
        }
    }
}
