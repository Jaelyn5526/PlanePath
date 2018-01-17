package jaelyn.path;

import android.graphics.PointF;

/**
 * Created by zaric on 16-12-06.
 */

public class PointTimeBean {
    public PointF point = new PointF();
    public float time;
    public float distance;

    public PointTimeBean(float x, float y, float time, float distance){
        point.set(x, y);
        this.time = time;
        this.distance = distance;
    }

    public PointTimeBean(PointTimeBean bean){
        point.set(bean.point);
        this.time = bean.time;
        this.distance = bean.distance;
    }

    public void set(float x, float y, long time, float distance){
        point.set(x, y);
        this.time = time;
        this.distance = distance;
    }


}
