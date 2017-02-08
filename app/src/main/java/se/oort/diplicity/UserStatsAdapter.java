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
    private StatsEmitter emitter = null;
    public class ViewHolder extends RecycleAdapter<SingleContainer<UserStats>, UserStatsAdapter.ViewHolder>.ViewHolder {
        UserView userView;
        TextView sortLabel;
        View row;

        View.OnClickListener delegateClickListener;
        public ViewHolder(View view) {
            super(view);
            row = view;
            userView = (UserView) view.findViewById(R.id.user);
            sortLabel = (TextView) view.findViewById(R.id.sort_label);
        }
        @Override
        public void bind(SingleContainer<UserStats> user, int pos) {
            userView.setUser(retrofitActivity, user.Properties.User);
            if (emitter != null) {
                sortLabel.setText(emitter.emit(user.Properties));
            }
            row.setOnClickListener(userView.getAvatarClickListener(retrofitActivity, user.Properties.User));
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
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_stats_list_row, parent, false);
        return new ViewHolder(itemView);
    }
}
