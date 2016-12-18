package se.oort.diplicity.game;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;

import se.oort.diplicity.ChannelService;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Member;

public class ChannelTable extends TableLayout {
    private AttributeSet attributeSet;
    private TableRow.LayoutParams wrapContentParams =
            new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT, 0.0f);

    private void setMargins(TableRow.LayoutParams params) {
        int margin = getResources().getDimensionPixelSize(R.dimen.member_table_margin);
        params.bottomMargin = margin;
        params.topMargin = margin;
        params.leftMargin = margin;
        params.rightMargin = margin;
    }
    public ChannelTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.attributeSet = attrs;
        setMargins(wrapContentParams);
    }
    public void setChannels(final RetrofitActivity retrofitActivity,
                            final Game game,
                            final Member member,
                            List<ChannelService.Channel> channels) {
        removeAllViews();
        for (final ChannelService.Channel channel : channels) {
            TableRow tableRow = new TableRow(retrofitActivity);
            TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT);
            tableRow.setLayoutParams(rowParams);

            TextView name = new TextView(retrofitActivity);
            TableRow.LayoutParams nameParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            setMargins(nameParams);
            name.setLayoutParams(nameParams);
            name.setText(TextUtils.join(", ", channel.Members));
            tableRow.addView(name);

            TextView messages = new TextView(retrofitActivity);
            messages.setLayoutParams(wrapContentParams);
            messages.setText(getResources().getQuantityString(R.plurals.message, channel.NMessages));
            tableRow.addView(messages);

            TextView unread = new TextView(retrofitActivity);
            unread.setLayoutParams(wrapContentParams);
            unread.setText(getResources().getString(R.string._unread, channel.NMessagesSince.NMessages));
            tableRow.addView(unread);

            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            FloatingActionButton button = (FloatingActionButton) inflater.inflate(R.layout.channel_expand_button, null);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(retrofitActivity, PressActivity.class);
                    intent.putExtra(PressActivity.SERIALIZED_CHANNEL_KEY, RetrofitActivity.serialize(channel));
                    intent.putExtra(PressActivity.SERIALIZED_GAME_KEY, RetrofitActivity.serialize(game));
                    if (member != null) {
                        intent.putExtra(PressActivity.SERIALIZED_MEMBER_KEY, RetrofitActivity.serialize(member));
                    }
                    retrofitActivity.startActivity(intent);
                }
            });
            button.setLayoutParams(wrapContentParams);
            tableRow.addView(button);

            addView(tableRow);
        }
    }
}
