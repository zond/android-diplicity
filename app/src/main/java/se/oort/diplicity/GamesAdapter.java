package se.oort.diplicity;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Phase;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.SingleContainer;

public class GamesAdapter extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder> {

    public class ViewHolder extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder>.ViewHolder {
        public TextView desc, variant, deadline, state;
        public RecyclerView members;
        public ViewHolder(View view) {
            super(view);
            desc = (TextView) view.findViewById(R.id.desc);
            variant = (TextView) view.findViewById(R.id.variant);
            deadline = (TextView) view.findViewById(R.id.deadline);
            state = (TextView) view.findViewById(R.id.state);
            members = (RecyclerView) view.findViewById(R.id.member_list);
        }
        @Override
        public void bind(SingleContainer<Game> game) {
            if (game.Properties.Desc == null || game.Properties.Desc.equals("")) {
                desc.setVisibility(View.GONE);
            } else {
                desc.setText(game.Properties.Desc);
            }

            variant.setText(game.Properties.Variant);

            long days = game.Properties.PhaseLengthMinutes / (60 * 24);
            long hours = (game.Properties.PhaseLengthMinutes - (60 * 24 * days)) / 60;
            long minutes = game.Properties.PhaseLengthMinutes - (60 * 24 * days) - (60 * hours);
            List<String> timeLabelList = new ArrayList<String>();
            if (days > 0) {
                timeLabelList.add("" + days + "d");
            }
            if (hours > 0) {
                timeLabelList.add("" + hours + "h");
            }
            if (minutes > 0) {
                timeLabelList.add("" + minutes + "m");
            }
            StringBuffer timeLabel = new StringBuffer();
            for (int i = 0; i < timeLabelList.size(); i++) {
                timeLabel.append(timeLabelList.get(i));
                if (i < timeLabelList.size() - 1) {
                    timeLabel.append(", ");
                }
            }
            deadline.setText(timeLabel.toString());

            if (!game.Properties.Started) {
                state.setText(ctx.getResources().getQuantityString(R.plurals.player, game.Properties.NMembers.intValue(), game.Properties.NMembers));
            } else if (game.Properties.NewestPhaseMeta.size() > 0) {
                PhaseMeta phaseMeta = game.Properties.NewestPhaseMeta.get(0);
                state.setText(ctx.getResources().getString(R.string.season_year_type, phaseMeta.Season, phaseMeta.Year, phaseMeta.Type));
            }

            LinearLayoutManager membersLayoutManager = new LinearLayoutManager(ctx);
            membersLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            members.setLayoutManager(membersLayoutManager);
            members.setAdapter(new MemberAdapter(ctx, game.Properties.Members));
        }
    }
    public GamesAdapter(Context ctx, List<SingleContainer<Game>> games) {
        super(ctx, games);
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.game_list_row, parent, false);

        return new ViewHolder(itemView);
    }
}
