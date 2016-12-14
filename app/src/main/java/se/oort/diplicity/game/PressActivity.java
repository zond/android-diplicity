package se.oort.diplicity.game;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import se.oort.diplicity.ChannelService;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;
import se.oort.diplicity.apigen.Message;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.Phase;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.SingleContainer;

public class PressActivity extends RetrofitActivity {

    public static final String SERIALIZED_CHANNEL_KEY = "serialized_channel";

    public ChannelService.Channel channel;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setContentView(R.layout.activity_press);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        byte[] serializedChannel = getIntent().getByteArrayExtra(SERIALIZED_CHANNEL_KEY);
        channel = (ChannelService.Channel) unserialize(serializedChannel);
        setTitle(TextUtils.join(", ", channel.Members));

        handleReq(
                messageService.ListMessages(channel.GameID, TextUtils.join(",", channel.Members)),
                new Sendable<MultiContainer<Message>>() {
                    @Override
                    public void send(final MultiContainer<Message> messageMultiContainer) {
                        final List<Message> messages= new ArrayList<>();
                        for (SingleContainer<Message> messageSingleContainer: messageMultiContainer.Properties) {
                            messages.add(messageSingleContainer.Properties);
                        }
                        ListView messagesView = (ListView) findViewById(R.id.press_messages);
                        messagesView.setAdapter(new BaseAdapter() {
                            @Override
                            public int getCount() {
                                return messages.size();
                            }

                            @Override
                            public Object getItem(int i) {
                                return messages.get(i);
                            }

                            @Override
                            public long getItemId(int i) {
                                return i;
                            }

                            @Override
                            public View getView(int i, View view, ViewGroup viewGroup) {
                                View row = view;
                                if (row == null) {
                                    row = getLayoutInflater().inflate(R.layout.message_list_row, viewGroup, false);
                                }
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(new Date());
                                cal.add(Calendar.SECOND, (int) -(messages.get(i).Age / 1000000000));

                                ((TextView) row.findViewById(R.id.body)).setText(messages.get(i).Body);
                                ((TextView) row.findViewById(R.id.at)).setText(cal.getTime().toString());
                                ((TextView) row.findViewById(R.id.sender)).setText(messages.get(i).Sender);
                                return row;
                            }
                        });
                    }
                }, getResources().getString(R.string.loading_messages));
    }
}
