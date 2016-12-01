package se.oort.diplicity;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import se.oort.diplicity.apigen.User;

import static android.support.v7.appcompat.R.styleable.View;

public class UserView extends FrameLayout {
    public UserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addView(inflater.inflate(R.layout.user_view, null));
    }
    public void setUser(User user) {
        ((TextView) findViewById(R.id.name)).setText(user.Name);
    }
}
