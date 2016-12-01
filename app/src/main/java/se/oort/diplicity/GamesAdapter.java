package se.oort.diplicity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.SingleContainer;

public class GamesAdapter extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder> {

    public class ViewHolder extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder>.ViewHolder {
        public TextView desc, variant, deadline;
        public ViewHolder(View view) {
            super(view);
            desc = (TextView) view.findViewById(R.id.desc);
            variant = (TextView) view.findViewById(R.id.variant);
            deadline = (TextView) view.findViewById(R.id.deadline);
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
        }
    }
    public GamesAdapter(List<SingleContainer<Game>> games) {
        super(games);
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.game_list_row, parent, false);

        return new ViewHolder(itemView);
    }
}
