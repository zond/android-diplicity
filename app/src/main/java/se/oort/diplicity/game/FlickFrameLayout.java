package se.oort.diplicity.game;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class FlickFrameLayout extends FrameLayout {

    private GestureDetectorCompat gestureDetectorCompat;
    public GameActivity gameActivity;

    public FlickFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        flick();
    }
    private void flick() {
        gestureDetectorCompat = new GestureDetectorCompat(getContext(), new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {

            }

            @Override
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float x, float y) {
                Log.d("Diplicity", "x: " + x);
                if (x < -15000) {
                    gameActivity.nextPhase();
                } else if (x > 15000) {
                    gameActivity.prevPhase();
                }
                return false;
            }
        });
    }

    @Override
    public boolean	onInterceptTouchEvent(MotionEvent ev) {
        Log.d("Diplicity", "onInterceptTouchEvent(" + ev + ")");
        gestureDetectorCompat.onTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }
}
