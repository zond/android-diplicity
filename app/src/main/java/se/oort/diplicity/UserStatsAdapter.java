package se.oort.diplicity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.UserStats;

public class UserStatsAdapter extends RecycleAdapter<SingleContainer<UserStats>, UserStatsAdapter.ViewHolder> {
    private RetrofitActivity retrofitActivity;
    private Set<Integer> expandedItems = new HashSet<>();
    private StatsEmitter emitter = null;
    public class ViewHolder extends RecycleAdapter<SingleContainer<UserStats>, UserStatsAdapter.ViewHolder>.ViewHolder {
        UserView userView;
        TextView sortLabel;
        UserStatsTable expanded;

        View.OnClickListener delegateClickListener;
        public ViewHolder(View view) {
            super(view);
            userView = (UserView) view.findViewById(R.id.user);
            sortLabel = (TextView) view.findViewById(R.id.sort_label);
            expanded = (UserStatsTable) view.findViewById(R.id.expanded);
        }
        @Override
        public void bind(SingleContainer<UserStats> user, int pos) {
            userView.setUser(retrofitActivity, user.Properties.User);
            expanded.setUserStats(retrofitActivity, user.Properties);
            if (emitter != null) {
                sortLabel.setText(emitter.emit(user.Properties));
            }
            if (expandedItems.contains(pos)) {
                expanded.setVisibility(View.VISIBLE);
            } else {
                expanded.setVisibility(View.GONE);
            }
        }
    }
    public interface StatsEmitter {
        public String emit(UserStats stats);
    }
    public UserStatsAdapter(RetrofitActivity retrofitActivity, List<SingleContainer<UserStats>> users) {
        super(retrofitActivity, users);
        this.retrofitActivity = retrofitActivity;
    }
    public void setEmitter(StatsEmitter emitter) {
        this.emitter = emitter;
    }
    public void clearExpanded() {
        expandedItems.clear();
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_stats_list_row, parent, false);
        final ViewHolder vh = new ViewHolder(itemView);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (expandedItems.contains(vh.getAdapterPosition())) {
                    expandedItems.remove(vh.getAdapterPosition());
                } else {
                    expandedItems.add(vh.getAdapterPosition());
                }
                notifyItemChanged(vh.getAdapterPosition());
            }
        };
        itemView.setOnClickListener(clickListener);
        vh.delegateClickListener = clickListener;

        return vh;
    }
}
