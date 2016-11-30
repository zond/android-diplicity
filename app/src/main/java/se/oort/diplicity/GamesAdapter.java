package se.oort.diplicity;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;

import se.oort.diplicity.apigen.GameContainer;
import se.oort.diplicity.apigen.GamesContainer;
import se.oort.diplicity.apigen.Game;

public class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.ViewHolder> {

    private List<GameContainer> games;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView desc, variant;

        public ViewHolder(View view) {
            super(view);
            desc = (TextView) view.findViewById(R.id.desc);
            variant = (TextView) view.findViewById(R.id.variant);
        }
    }

    public GamesAdapter(List<GameContainer> games) {
        this.games = games;
    }

    public void Clear() {
        int before = this.games.size();
        this.games.clear();
        notifyItemRangeRemoved(0, before);
    }

    public void AddAll(List<GameContainer> games) {
        int before = this.games.size();
        this.games.addAll(games);
        notifyItemRangeInserted(before, games.size());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.game_list_row, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Game game = games.get(position).Properties;
        holder.desc.setText(game.Desc);
        holder.variant.setText(game.Variant);
    }

    @Override
    public int getItemCount() {
        return games.size();
    }
}
