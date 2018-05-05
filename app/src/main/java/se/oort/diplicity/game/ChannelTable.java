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
import se.oort.diplicity.VariantService;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.Phase;
import se.oort.diplicity.apigen.SingleContainer;

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
                            List<ChannelService.Channel> channels,
                            final MultiContainer<Phase> phases) {
        removeAllViews();
        for (final ChannelService.Channel channel : channels) {
            TableRow tableRow = new TableRow(retrofitActivity);
            TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT);
            tableRow.setLayoutParams(rowParams);

            SingleContainer<VariantService.Variant> variant = null;
            for (SingleContainer<VariantService.Variant> v : retrofitActivity.getVariants().Properties) {
                if (v.Properties.Name.equals(game.Variant)) {
                    variant = v;
                    break;
                }
            }

            TextView name = new TextView(retrofitActivity);
            TableRow.LayoutParams nameParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            setMargins(nameParams);
            name.setLayoutParams(nameParams);
            if (variant != null && channel.Members.size() == variant.Properties.Nations.size()) {
                name.setText(getResources().getString(R.string.everyone));
            } else {
                name.setText(TextUtils.join(", ", channel.Members));
            }
            tableRow.addView(name);

            TextView messages = new TextView(retrofitActivity);
            messages.setLayoutParams(wrapContentParams);
            messages.setText(getResources().getQuantityString(R.plurals.message, channel.NMessages, channel.NMessages));
            tableRow.addView(messages);

            if (member != null) {
                TextView unread = new TextView(retrofitActivity);
                unread.setLayoutParams(wrapContentParams);
                unread.setText(getResources().getString(R.string._unread, channel.NMessagesSince.NMessages));
                tableRow.addView(unread);
            }

            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            FloatingActionButton button = (FloatingActionButton) inflater.inflate(R.layout.expand_button, null);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    getContext().startActivity(PressActivity.startPressIntent(getContext(), game, channel, member, phases));
                }
            });
            button.setLayoutParams(wrapContentParams);
            tableRow.addView(button);

            addView(tableRow);
        }
    }
}
