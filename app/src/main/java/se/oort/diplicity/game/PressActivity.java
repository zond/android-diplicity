package se.oort.diplicity.game;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;

import se.oort.diplicity.ChannelService;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;

public class PressActivity extends RetrofitActivity {

    public static final String SERIALIZED_CHANNEL_KEY = "serialized_channel";

    public ChannelService.Channel channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_press);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        byte[] serializedChannel = getIntent().getByteArrayExtra(SERIALIZED_CHANNEL_KEY);
        channel = (ChannelService.Channel) unserialize(serializedChannel);
        setTitle(TextUtils.join(", ", channel.Members));
    }
}
