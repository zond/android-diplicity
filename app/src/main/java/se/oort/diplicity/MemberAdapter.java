package se.oort.diplicity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.User;
import se.oort.diplicity.apigen.UserStats;

public class MemberAdapter extends RecycleAdapter<Member, MemberAdapter.ViewHolder> {
    public class ViewHolder extends RecycleAdapter<Member, UserStatsAdapter.ViewHolder>.ViewHolder {
        public UserView userView;
        public TextView nation;
        public ViewHolder(View view) {
            super(view);
            userView = (UserView) view.findViewById(R.id.user);
            nation = (TextView) view.findViewById(R.id.nation);
        }
        @Override
        public void bind(Member member) {
            userView.setUser(member.User);
            if (!member.Nation.equals("")) {
                nation.setText(member.Nation);
                nation.setVisibility(View.VISIBLE);
            } else {
                nation.setVisibility(View.GONE);
            }
        }
    }
    public MemberAdapter(Context ctx, List<Member> members) {
        super(ctx, members);
    }
    @Override
    public MemberAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.member_list_row, parent, false);

        return new ViewHolder(itemView);
    }

}
