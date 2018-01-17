package jaelyn.path;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by zaric on 17-02-24.
 */

public class PathView extends View {

    //onTouch的各种状态
    private static final int STATE_DOWN = 0;
    private static final int STATE_MOVE = 1;
    private static final int STATE_UP = 2;
    private static final int STATE_FLY = 3;
    private static final int STATE_NORMAL = -1;

    //添加path关键点的间隔时间
    private static int ANIM_TIME = 40;

    //速度默认的档次以及时间
    private static final int SPEED_TIME_100 = 3000;
    private static final int SPEED_RANGE_100 = 127;
    private static final int SPEED_TIME_60 = 5000;
    private static final int SPEED_RANGE_60 = 76;
    private static final int SPEED_TIME_30 = 10000;
    private static final int SPEED_RANGE_30 = 38;

    //匀速的时间
    private float standTime = SPEED_TIME_100;
    private float standRange = SPEED_RANGE_100;
    //手指与匀速的缩放系数
    private float timeScale = 1;

    private Handler handler;
    //当前onTouch的状态
    private int touchState = STATE_NORMAL;

    private Paint paint;
    //轨迹图层
    private Bitmap pathBmp;
    private Canvas pathCanvas;

    //飞机图标
    private Bitmap flyBmp;
    private Matrix flyMatrix;
    //轨迹路径
    private Path path;
    private PathMeasure pathMeasure;
    private float mPreX, mPreY;

    //path的关键点信息
    private ArrayList<PointTimeBean> pointBeans = new ArrayList<>();
    //当前手指的位置信息
    private PointTimeBean crtPoinBean;
    //view的范围
    private Rect viewRect;

    //飞机图标与边界的误差
    private int flyPadding;

    //手指DOWN的时间
    private long touchDownTime;
    private FlyAnim flyAnim;
    private int validLenght = 5;
    public PathView(Context context) {
        super(context);
        initView(context);
    }

    public PathView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PathView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public void initView(Context context) {
        //设置画笔
        paint = new Paint();
        paint.setColor(0xff25cfff);
        paint.setStrokeWidth(4);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);

        path = new Path();
        pathMeasure = new PathMeasure();
        flyBmp = BitmapFactory.decodeResource(getResources(), R.mipmap.plane_path);
        flyMatrix = new Matrix();
        handler = new Handler();
        /*handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                pathCanvas.drawPath(path, paint);
                touchState = STATE_FLY;
                startFlyAnim();
            }
        }, 1000);*/
        final float scale = this.getResources().getDisplayMetrics().density;
        validLenght = (int)(validLenght / scale + 0.5f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        flyPadding = flyBmp.getWidth() / 2;
        viewRect = new Rect(flyPadding, flyPadding, w - flyPadding, h - flyPadding);
        drawLine();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(viewRect, paint);
        if (pathBmp == null) {
            pathBmp = Bitmap.createBitmap(getWidth(), getHeight(),
                    Bitmap.Config.ARGB_8888);
            pathCanvas = new Canvas(pathBmp);
        } else {
            canvas.drawBitmap(pathBmp, 0, 0, paint);
        }

        if (touchState == STATE_FLY) {
            canvas.drawBitmap(flyBmp, flyMatrix, paint);
        }

        if (isDrawTest) {
            canvas.drawPath(testPath, testPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            Log.d("touch--> down", touchState+"");
            if (touchState == STATE_FLY){
                finishFlyAnim();
            }
            if (touchState == STATE_NORMAL && isInView(event)) {
                touchState = STATE_DOWN;
                touchDown(event);
                Log.d("touch-->", "down");
                return true;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            Log.d("touch--> move", touchState+"");
            if (touchState == STATE_MOVE | touchState == STATE_DOWN) {
                touchState = STATE_MOVE;
                touchMove(event);
                Log.d("touch-->", "move");
                return true;
            }
            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            Log.d("touch--> up", touchState+"");
            if (touchState == STATE_MOVE | touchState == STATE_DOWN) {
                touchState = STATE_UP;
                Log.d("touch-->", "up");
                touchUp(event);
            }
            break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 手指点下后的处理
     */
    public void touchDown(MotionEvent event) {
        mPreX = event.getX();
        mPreY = event.getY();
        path.moveTo(mPreX, mPreY);
        flyMatrix.postTranslate(event.getX() - flyBmp.getWidth() / 2,
                event.getY() - flyBmp.getHeight() / 2);
        pathCanvas.drawPath(path, paint);
        //记录当前点的信息
        touchDownTime = System.currentTimeMillis();
        crtPoinBean = new PointTimeBean(event.getX(), event.getY(), 0, 0);
        pointBeans.add(crtPoinBean);
        starAddPointThread();
        invalidate();
    }

    /**
     * 手指移动处理
     *
     * @param event
     */
    public void touchMove(MotionEvent event) {
        float x = (mPreX + event.getX()) / 2;
        float y = (mPreY + event.getY()) / 2;
        float pointTime = System.currentTimeMillis() - touchDownTime;
        if (!isInView(event)) {
            touchState = STATE_UP;
            float[] point = touchMoveOut(x, y);
            x = point[0];
            y = point[1];
        }
        path.quadTo(mPreX, mPreY, x, y);
        mPreX = event.getX();
        mPreY = event.getY();

        pathCanvas.drawPath(path, paint);
        //记录当前点的信息
        pathMeasure.setPath(path, false);
        crtPoinBean =
                new PointTimeBean(event.getX(), event.getY(), pointTime, pathMeasure.getLength());
        invalidate();
        //手指移出有效区域，结束手势
        if (!isInView(event)) {
            pointBeans.add(crtPoinBean);
            stopAddPointThread();
            finishTouch();
        }
    }

    /**
     * 手指抬起处理
     *
     * @param event
     */
    public void touchUp(MotionEvent event) {
        path.lineTo(event.getX(), event.getY());
        pathCanvas.drawPath(path, paint);
        //记录当前点的信息
        float pointTime = System.currentTimeMillis() - touchDownTime;
        pathMeasure.setPath(path, false);
        crtPoinBean =
                new PointTimeBean(event.getX(), event.getY(), pointTime, pathMeasure.getLength());
        pointBeans.add(crtPoinBean);
        stopAddPointThread();
        //TODO 测试代码
//        doDrawPathPoint();
        invalidate();
        finishTouch();
    }

    /**
     * 判断是否为有效的手势
     */
    private boolean isValidPath(){
        PathMeasure pathMeasure = new PathMeasure();
        pathMeasure.setPath(path, false);
        if (pathMeasure.getLength() > validLenght){
            return true;
        }
        return false;
    }


    /**
     * 是否在view的范围内
     *
     * @param event
     * @return
     */
    private boolean isInView(MotionEvent event) {
        return viewRect.contains((int) event.getX(), (int) event.getY());
    }

    /**
     * 开启添加关键点的线程
     * 每隔ANIM_TIME这个时间，向pointBeans添加当前点的信息crtPointBean
     */
    private void starAddPointThread() {
        isAddPoint = true;
        handler.postDelayed(addPointRun, ANIM_TIME);
    }

    /**
     * 停止添加关键点的线程
     */
    private void stopAddPointThread() {
        isAddPoint = false;
        handler.removeCallbacks(addPointRun);
    }

    private boolean isAddPoint = false;
    private Runnable addPointRun = new Runnable() {
        @Override
        public void run() {
            if (isAddPoint) {
                pointBeans.add(crtPoinBean);
                handler.postDelayed(this, ANIM_TIME);
            }
        }
    };

    /**
     * 移动状态下，移出边界，采与边界交叉的点
     *
     * @param x 手指真实位置x
     * @param y 手指真实位置y
     * @return point[0]-x, point[1]-y
     */
    private float[] touchMoveOut(float x, float y) {
        float[] point = new float[2];
        if (x >= viewRect.right) {
            point[0] = viewRect.right;
        } else if (x <= viewRect.left) {
            point[0] = viewRect.left;
        } else {
            point[0] = x;
        }

        if (y >= viewRect.bottom) {
            point[1] = viewRect.bottom;
        } else if (y <= viewRect.top) {
            point[1] = viewRect.top;
        } else {
            point[1] = y;
        }
        return point;
    }


    /**
     * 手指结束触碰事件
     */
    private void finishTouch() {
        if (isValidPath()){
            //有效的手势，开始播放动画
            touchState = STATE_FLY;
            timeScale = scalePointTime();
            startFlyAnim();
        }else {
            //无效的手势，重置参数
            resetData();
        }
    }

    /**
     * 供外部调用
     * 调节飞机的飞行速度
     * @param i
     */
    public void setScale(int i){
        switch (i){
        case 0: // 30
            standTime = SPEED_TIME_30;
            standRange = SPEED_RANGE_30;
            break;
        case 1: // 60
            standTime = SPEED_TIME_60;
            standRange = SPEED_RANGE_60;
            break;
        case 2: // 100
            standTime = SPEED_TIME_100;
            standRange = SPEED_RANGE_100;
            break;
        }

        //根据飞机在path上的距离，从新计算动画的播放时间
        float curDistance = flyAnim.curDistance;
        float curTimeScale = scalePointTime();
        if (curDistance == 0){
            flyAnim.time = 0;
            return;
        }
        float animTime = 0;
        for (int j = 1; j < pointBeans.size(); j++) {
            if (curDistance <= pointBeans.get(j).distance){
                float beforeDis = pointBeans.get(j-1).distance;
                float lastDis = pointBeans.get(j).distance;
                float beforeTime = pointBeans.get(j-1).time * curTimeScale;
                float lastTime = pointBeans.get(j).time * curTimeScale;
                float percent = (curDistance - beforeDis) / (lastDis - beforeDis);
                float dxTime = percent * (lastTime - beforeTime);
                animTime = dxTime + beforeTime;
                break;
            }
        }
        flyAnim.time = animTime;
        timeScale = curTimeScale;
    }

    /**
     * 根据速度档次计算缩放系数
     */
    private float scalePointTime() {
        if (pointBeans.size() == 0) {
            return 1;
        }
        float fingerTime = pointBeans.get(pointBeans.size() - 1).time;
        float fingerDic = pointBeans.get(pointBeans.size() - 1).distance;
        float standeV = viewRect.width() / standTime;
        float constantTime = fingerDic / standeV;
        return constantTime / fingerTime;
    }

    /**
     * 开启动画
     */
    private void startFlyAnim() {
        flyAnim = new FlyAnim();
        handler.post(flyAnim);
    }

    /**
     * 飞机飞信的动画类
     */
    private class FlyAnim implements Runnable {
        int durationTime = 16;
        float time = 0;
        PathMeasure pathMeasure = new PathMeasure();
        public float curDistance;

        public FlyAnim() {
            pathMeasure = new PathMeasure();
        }

        @Override
        public void run() {
            if (touchState == STATE_FLY
                    && time <= (pointBeans.get(pointBeans.size()-1).time * timeScale)) {
                float distance = getDisForTime(time);
                if (distance < 0) {
                    finishFlyAnim();
                    return;
                }
                curDistance = distance;
                pathMeasure.setPath(path, false);
                float[] pos = new float[2];
                float[] tan = new float[2];
                pathMeasure.getPosTan(distance, pos, tan);
                flyMatrix.reset();
                flyMatrix.postTranslate(pos[0] - flyBmp.getWidth() / 2,
                        pos[1] - flyBmp.getHeight() / 2);
                postInvalidate();
                time += durationTime;
                handler.post(this);
            } else {
                finishFlyAnim();
            }
        }

        /**
         * 根据动画的当前时间，计算出飞机所在的位置
         * @param time
         * @return
         */
        private float getDisForTime(float time) {
            PointTimeBean beforeBean, lastBean;
            for (int i = 0; i < pointBeans.size(); i++) {
                if (time == (pointBeans.get(i).time * timeScale)) {
                    return pointBeans.get(i).distance;
                } else if (time < (pointBeans.get(i).time  * timeScale)) {
                    Log.d("Tiem--", i +"++" + time + "++"+(pointBeans.get(i).time +"++"+timeScale));
                    float beforeTime = (pointBeans.get(i -1).time  * timeScale);
                    float lastTime = (pointBeans.get(i).time  * timeScale);
                    beforeBean = pointBeans.get(i - 1);
                    lastBean = pointBeans.get(i);
                    float percentTime =
                            (time - beforeTime) / (lastTime - beforeTime);
                    float dx = percentTime * (lastBean.distance - beforeBean.distance);
                    return dx + beforeBean.distance;
                }
            }
            return -1;
        }
    }

    /**
     * 结束飞行runnable
     */
    private void finishFlyAnim(){
        touchState = STATE_NORMAL;
        handler.removeCallbacks(flyAnim);
        resetData();
    }

    /**
     * 重置参数
     */
    private void resetData(){
        touchState = STATE_NORMAL;
        pointBeans.clear();
        flyMatrix.reset();
        path.reset();
        pathMeasure = new PathMeasure();
        if (pathCanvas != null) {
            pathCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
    }



    /**
     * 测试代码
     */
    private void drawLine(){
        //TODO 测试代码
       /* path.moveTo(flyPadding, viewRect.height() / 2 + 2 * flyPadding);
        path.lineTo(viewRect.width() + flyPadding, viewRect.height() / 2 + 2 * flyPadding);
        PathMeasure pathMeasure = new PathMeasure(path, false);
        Log.d("time--width", viewRect.width() + "++++" + pathMeasure.getLength());
        PointTimeBean timeBean1 =
                new PointTimeBean(flyPadding, viewRect.height() / 2 + 2 * flyPadding, 0, 0);
        PointTimeBean timeBean2 = new PointTimeBean(flyPadding + viewRect.width() / 5,
                viewRect.height() / 2 + 2 * flyPadding, 500, viewRect.width() / 5);
        PointTimeBean timeBean3 = new PointTimeBean(flyPadding + 2 * viewRect.width() / 5,
                viewRect.height() / 2 + 2 * flyPadding, 1000, 2 * viewRect.width() / 5);
        PointTimeBean timeBean4 = new PointTimeBean(flyPadding + 3 * viewRect.width() / 5,
                viewRect.height() / 2 + 2 * flyPadding, 1500, 3 * viewRect.width() / 5);
        PointTimeBean timeBean5 = new PointTimeBean(flyPadding + 4 * viewRect.width() / 5,
                viewRect.height() / 2 + 2 * flyPadding, 2000, 4 * viewRect.width() / 5);
        PointTimeBean timeBean6 = new PointTimeBean(flyPadding + 5 * viewRect.width() / 5,
                viewRect.height() / 2 + 2 * flyPadding, 2500, 5 * viewRect.width() / 5);
        pointBeans.add(timeBean1);
        pointBeans.add(timeBean2);
        pointBeans.add(timeBean3);
        pointBeans.add(timeBean4);
        pointBeans.add(timeBean5);
        pointBeans.add(timeBean6);*/
    }

    //测试用的一系列变量
    private boolean isDrawTest = false;
    private Path testPath;
    private Paint testPaint;

    /**
     * 测试代码检查收集的关键点是否正确
     */
    private void doDrawPathPoint() {
        isDrawTest = true;
        testPath = new Path();
        testPath.moveTo(pointBeans.get(0).point.x, pointBeans.get(0).point.y);
        for (int i = 1; i < pointBeans.size(); i++) {
            testPath.lineTo(pointBeans.get(i).point.x, pointBeans.get(i).point.y);
        }
        testPaint = new Paint();
        testPaint.setColor(Color.RED);
        testPaint.setStrokeWidth(4);
        testPaint.setAntiAlias(true);
        testPaint.setDither(true);
        testPaint.setStyle(Paint.Style.STROKE);
    }


}





































