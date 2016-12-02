package se.oort.diplicity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.UserStats;

public class UserStatsAdapter extends RecycleAdapter<SingleContainer<UserStats>, UserStatsAdapter.ViewHolder> {

    public class ViewHolder extends RecycleAdapter<SingleContainer<UserStats>, UserStatsAdapter.ViewHolder>.ViewHolder {
        public UserView userView;
        public ViewHolder(View view) {
            super(view);
            userView = (UserView) view.findViewById(R.id.user);
        }
        @Override
        public void bind(SingleContainer<UserStats> user, int pos) {
            userView.setUser(user.Properties.User);
        }
    }
    public UserStatsAdapter(Context ctx, List<SingleContainer<UserStats>> users) {
        super(ctx, users);
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_stats_list_row, parent, false);

        return new ViewHolder(itemView);
    }
}
