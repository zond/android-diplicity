package se.oort.diplicity;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.LayoutInflater;

import java.util.ArrayList;

import se.oort.diplicity.apigen.GamesContainer;
import se.oort.diplicity.apigen.Game;

public class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.ViewHolder> {

    private GamesContainer gamesContainer;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView desc, variant;

        public ViewHolder(View view) {
            super(view);
            desc = (TextView) view.findViewById(R.id.desc);
            variant = (TextView) view.findViewById(R.id.variant);
        }
    }

    public GamesAdapter(GamesContainer gamesContainer) {
        this.gamesContainer = gamesContainer;
        if (this.gamesContainer.Properties == null) {
            this.gamesContainer.Properties = new ArrayList<>();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.game_list_row, parent, false);

        return new ViewHolder(itemView);
    }

    public void setGamesContainer(GamesContainer gamesContainer) {
        this.gamesContainer = gamesContainer;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Game game = gamesContainer.Properties.get(position).Properties;
        holder.desc.setText(game.Desc);
        holder.variant.setText(game.Variant);
    }

    @Override
    public int getItemCount() {
        return gamesContainer.Properties.size();
    }
}
