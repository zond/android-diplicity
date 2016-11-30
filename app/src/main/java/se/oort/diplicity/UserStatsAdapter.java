package se.oort.diplicity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import rx.Single;
import se.oort.diplicity.apigen.GameContainer;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.UserStats;
import se.oort.diplicity.apigen.UserStatsContainer;

public class UserStatsAdapter extends RecycleAdapter<SingleContainer<UserStats>, UserStatsAdapter.ViewHolder> {

    public class ViewHolder extends RecycleAdapter<SingleContainer<UserStats>, UserStatsAdapter.ViewHolder>.ViewHolder {
        public TextView name;
        public ViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.name);
        }
        @Override
        public void bind(SingleContainer<UserStats> user) {
            name.setText(user.Properties.User.Name);
        }
    }
    public UserStatsAdapter(List<SingleContainer<UserStats>> users) {
        super(users);
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_stats_list_row, parent, false);

        return new ViewHolder(itemView);
    }
}
