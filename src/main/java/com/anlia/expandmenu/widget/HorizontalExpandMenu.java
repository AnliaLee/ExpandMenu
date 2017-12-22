package com.anlia.expandmenu.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

import com.anlia.expandmenu.R;
import com.anlia.expandmenu.utils.DpOrPxUtils;

/**
 * Created by anlia on 2017/11/9.
 */

public class HorizontalExpandMenu extends RelativeLayout {

    private Context mContext;
    private AttributeSet mAttrs;

    private Path path;
    private Paint buttonIconPaint;//按钮icon画笔
    private ExpandMenuAnim anim;

    private int defaultWidth;//默认宽度
    private int defaultHeight;//默认长度
    private int viewWidth;
    private int viewHeight;
    private float backPathWidth;//绘制子View区域宽度
    private float maxBackPathWidth;//绘制子View区域最大宽度
    private int menuLeft;//menu区域left值
    private int menuRight;//menu区域right值

    private int menuBackColor;//菜单栏背景色
    private float menuStrokeSize;//菜单栏边框线的size
    private int menuStrokeColor;//菜单栏边框线的颜色
    private float menuCornerRadius;//菜单栏圆角半径

    private float buttonIconDegrees;//按钮icon符号竖线的旋转角度
    private float buttonIconSize;//按钮icon符号的大小
    private float buttonIconStrokeWidth;//按钮icon符号的粗细
    private int buttonIconColor;//按钮icon颜色

    private int buttonStyle;//按钮类型
    private int buttonRadius;//按钮矩形区域内圆半径
    private float buttonTop;//按钮矩形区域top值
    private float buttonBottom;//按钮矩形区域bottom值

    private Point rightButtonCenter;//右按钮中点
    private float rightButtonLeft;//右按钮矩形区域left值
    private float rightButtonRight;//右按钮矩形区域right值

    private Point leftButtonCenter;//左按钮中点
    private float leftButtonLeft;//左按钮矩形区域left值
    private float leftButtonRight;//左按钮矩形区域right值

    private boolean isFirstLayout;//是否第一次测量位置，主要用于初始化menuLeft和menuRight的值
    private boolean isExpand;//菜单是否展开，默认为展开
    private boolean isAnimEnd;//动画是否结束
    private float downX = -1;
    private float downY = -1;
    private int expandAnimTime;//展开收起菜单的动画时间

    private View childView;

    /**
     * 根按钮所在位置，默认为右边
     */
    public static final int Right = 0;
    public static final int Left = 1;

    public HorizontalExpandMenu(Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    public HorizontalExpandMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mAttrs = attrs;
        init();
    }

    private void init(){
        TypedArray typedArray = mContext.obtainStyledAttributes(mAttrs, R.styleable.HorizontalExpandMenu);

        defaultWidth = DpOrPxUtils.dip2px(mContext,200);
        defaultHeight = DpOrPxUtils.dip2px(mContext,40);

        menuBackColor = typedArray.getColor(R.styleable.HorizontalExpandMenu_back_color,Color.WHITE);
        menuStrokeSize = typedArray.getDimension(R.styleable.HorizontalExpandMenu_stroke_size,1);
        menuStrokeColor = typedArray.getColor(R.styleable.HorizontalExpandMenu_stroke_color,Color.GRAY);
        menuCornerRadius = typedArray.getDimension(R.styleable.HorizontalExpandMenu_corner_radius,DpOrPxUtils.dip2px(mContext,20));

        buttonStyle = typedArray.getInteger(R.styleable.HorizontalExpandMenu_button_style,Right);
        buttonIconDegrees = 90;
        buttonIconSize = typedArray.getDimension(R.styleable.HorizontalExpandMenu_button_icon_size,DpOrPxUtils.dip2px(mContext,8));
        buttonIconStrokeWidth = typedArray.getDimension(R.styleable.HorizontalExpandMenu_button_icon_stroke_width,8);
        buttonIconColor = typedArray.getColor(R.styleable.HorizontalExpandMenu_button_icon_color,Color.GRAY);

        expandAnimTime = typedArray.getInteger(R.styleable.HorizontalExpandMenu_expand_time,400);

        isFirstLayout = true;
        isExpand = true;
        isAnimEnd = false;

        buttonIconPaint = new Paint();
        buttonIconPaint.setColor(buttonIconColor);
        buttonIconPaint.setStyle(Paint.Style.STROKE);
        buttonIconPaint.setStrokeWidth(buttonIconStrokeWidth);
        buttonIconPaint.setAntiAlias(true);

        path = new Path();
        leftButtonCenter = new Point();
        rightButtonCenter = new Point();
        anim = new ExpandMenuAnim();
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                isAnimEnd = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = measureSize(defaultHeight, heightMeasureSpec);
        int width = measureSize(defaultWidth, widthMeasureSpec);
        viewHeight = height;
        viewWidth = width;
        buttonRadius = viewHeight/2;
        layoutRootButton();
        setMeasuredDimension(viewWidth,viewHeight);

        maxBackPathWidth = viewWidth- buttonRadius *2;
        backPathWidth = maxBackPathWidth;

        //布局代码中如果没有设置background属性则在此处添加一个背景
        if(getBackground()==null){
            setMenuBackground();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        //如果子View数量为0时，onLayout后getLeft()和getRight()才能获取相应数值，menuLeft和menuRight保存menu初始的left和right值
        if(isFirstLayout){
            menuLeft = getLeft();
            menuRight = getRight();
            isFirstLayout = false;
        }
        if(getChildCount()>0){
            childView = getChildAt(0);
            if(isExpand){
                if(buttonStyle == Right){
                    childView.layout(leftButtonCenter.x,(int) buttonTop,(int) rightButtonLeft,(int) buttonBottom);
                }else {
                    childView.layout((int)(leftButtonRight),(int) buttonTop,rightButtonCenter.x,(int) buttonBottom);
                }

                //限制子View在菜单内，LayoutParam类型和当前ViewGroup一致
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(viewWidth,viewHeight);
                layoutParams.setMargins(0,0,buttonRadius *3,0);
                childView.setLayoutParams(layoutParams);
            }else {
                childView.setVisibility(GONE);
            }
        }
        if(getChildCount()>1){//限制直接子View的数量
            throw new IllegalStateException("HorizontalExpandMenu can host only one direct child");
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;//当menu的宽度改变时，重新给viewWidth赋值
        if(isAnimEnd){//防止出现动画结束后菜单栏位置大小测量错误的bug
            if(buttonStyle == Right){
                if(!isExpand){
//                    layout((int)(menuRight - buttonRadius *2-backPathWidth),getTop(), menuRight,getBottom());
                    layout((menuRight - buttonRadius *2),getTop(), menuRight,getBottom());
                }
            }else {
                if(!isExpand){
//                    layout(menuLeft,getTop(),(int)(menuLeft + buttonRadius *2+backPathWidth),getBottom());
                    layout(menuLeft,getTop(),(menuLeft + buttonRadius *2),getBottom());
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        layoutRootButton();
        if(buttonStyle == Right){
            drawRightIcon(canvas);
        }else {
            drawLeftIcon(canvas);
        }

        super.onDraw(canvas);//注意父方法在最后调用，以免icon被遮盖
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                if(backPathWidth==maxBackPathWidth || backPathWidth==0){//动画结束时按钮才生效
                    switch (buttonStyle){
                        case Right:
                            if(x==downX&&y==downY&&y>=buttonTop&&y<=buttonBottom&&x>=rightButtonLeft&&x<=rightButtonRight){
                                expandMenu(expandAnimTime);
                            }
                            break;
                        case Left:
                            if(x==downX&&y==downY&&y>=buttonTop&&y<=buttonBottom&&x>=leftButtonLeft&&x<=leftButtonRight){
                                expandMenu(expandAnimTime);
                            }
                            break;
                    }
                }
                break;
        }
        return true;
    }

    private class ExpandMenuAnim extends Animation {
        public ExpandMenuAnim() {}

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            float left = menuRight - buttonRadius *2;//按钮在右边，菜单收起时按钮区域left值
            float right = menuLeft + buttonRadius *2;//按钮在左边，菜单收起时按钮区域right值
            if(childView!=null) {
                childView.setVisibility(GONE);
            }
            if(isExpand){//打开菜单
                backPathWidth = maxBackPathWidth * interpolatedTime;
                buttonIconDegrees = 90 * interpolatedTime;

                if(backPathWidth==maxBackPathWidth){
                    if(childView!=null) {
                        childView.setVisibility(VISIBLE);
                    }
                }
            }else {//关闭菜单
                backPathWidth = maxBackPathWidth - maxBackPathWidth * interpolatedTime;
                buttonIconDegrees = 90 - 90 * interpolatedTime;
            }
            if(buttonStyle == Right){
                layout((int)(left-backPathWidth),getTop(), menuRight,getBottom());//会调用onLayout重新测量子View位置
            }else {
                layout(menuLeft,getTop(),(int)(right+backPathWidth),getBottom());
            }
            postInvalidate();
        }
    }

    private int measureSize(int defaultSize, int measureSpec) {
        int result = defaultSize;
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);

        if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize;
        } else if (specMode == View.MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize);
        }
        return result;
    }

    /**
     * 设置菜单背景，如果要显示阴影，需在onLayout之前调用
     */
    private void setMenuBackground(){
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(menuBackColor);
        gd.setStroke((int)menuStrokeSize, menuStrokeColor);
        gd.setCornerRadius(menuCornerRadius);
        setBackground(gd);
    }

    /**
     * 测量按钮中点和矩形位置
     */
    private void layoutRootButton(){
        buttonTop = 0;
        buttonBottom = viewHeight;

        rightButtonCenter.x = viewWidth- buttonRadius;
        rightButtonCenter.y = viewHeight/2;
        rightButtonLeft = rightButtonCenter.x- buttonRadius;
        rightButtonRight = rightButtonCenter.x+ buttonRadius;

        leftButtonCenter.x = buttonRadius;
        leftButtonCenter.y = viewHeight/2;
        leftButtonLeft = leftButtonCenter.x- buttonRadius;
        leftButtonRight = leftButtonCenter.x+ buttonRadius;
    }

    /**
     * 绘制左边的按钮
     * @param canvas
     */
    private void drawLeftIcon(Canvas canvas){
        path.reset();
        path.moveTo(leftButtonCenter.x- buttonIconSize, leftButtonCenter.y);
        path.lineTo(leftButtonCenter.x+ buttonIconSize, leftButtonCenter.y);
        canvas.drawPath(path, buttonIconPaint);//划横线

        canvas.save();
        canvas.rotate(-buttonIconDegrees, leftButtonCenter.x, leftButtonCenter.y);//旋转画布，让竖线可以随角度旋转
        path.reset();
        path.moveTo(leftButtonCenter.x, leftButtonCenter.y- buttonIconSize);
        path.lineTo(leftButtonCenter.x, leftButtonCenter.y+ buttonIconSize);
        canvas.drawPath(path, buttonIconPaint);//画竖线
        canvas.restore();
    }

    /**
     * 绘制右边的按钮
     * @param canvas
     */
    private void drawRightIcon(Canvas canvas){
        path.reset();
        path.moveTo(rightButtonCenter.x- buttonIconSize, rightButtonCenter.y);
        path.lineTo(rightButtonCenter.x+ buttonIconSize, rightButtonCenter.y);
        canvas.drawPath(path, buttonIconPaint);//划横线

        canvas.save();
        canvas.rotate(buttonIconDegrees, rightButtonCenter.x, rightButtonCenter.y);//旋转画布，让竖线可以随角度旋转
        path.reset();
        path.moveTo(rightButtonCenter.x, rightButtonCenter.y- buttonIconSize);
        path.lineTo(rightButtonCenter.x, rightButtonCenter.y+ buttonIconSize);
        canvas.drawPath(path, buttonIconPaint);//画竖线
        canvas.restore();
    }

    /**
     * 展开收起菜单
     * @param time 动画时间
     */
    private void expandMenu(int time){
        anim.setDuration(time);
        isExpand = isExpand ?false:true;
        this.startAnimation(anim);
        isAnimEnd = false;
    }
}
