package se.oort.diplicity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.SingleContainer;

public class GamesAdapter extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder> {

    public class ViewHolder extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder>.ViewHolder {
        public TextView desc, variant;
        public ViewHolder(View view) {
            super(view);
            desc = (TextView) view.findViewById(R.id.desc);
            variant = (TextView) view.findViewById(R.id.variant);
        }
        @Override
        public void bind(SingleContainer<Game> game) {
            desc.setText(game.Properties.Desc);
            variant.setText(game.Properties.Variant);
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
