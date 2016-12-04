package se.oort.diplicity;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.adapter.rxjava.HttpException;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Link;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.SingleContainer;

public class GamesAdapter extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder> {
    private RetrofitActivity retrofitActivity;
    private Set<Integer> expandedItems = new HashSet<>();
    public class ViewHolder extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder>.ViewHolder {
        TextView desc, variant, deadline, state, rating,
                minReliability, minQuickness, maxHated, maxHater,
                ratingLabel, minReliabilityLabel, minQuicknessLabel,
                maxHatedLabel, maxHaterLabel;
        RecyclerView members;
        RelativeLayout expanded;
        View.OnClickListener delegateClickListener;
        FloatingActionButton button;

        public ViewHolder(View view) {
            super(view);
            desc = (TextView) view.findViewById(R.id.desc);
            variant = (TextView) view.findViewById(R.id.variant);
            deadline = (TextView) view.findViewById(R.id.deadline);
            state = (TextView) view.findViewById(R.id.state);
            rating = (TextView) view.findViewById(R.id.rating);
            minReliability = (TextView) view.findViewById(R.id.min_reliability);
            minQuickness = (TextView) view.findViewById(R.id.min_quickness);
            maxHated = (TextView) view.findViewById(R.id.max_hated);
            maxHater = (TextView) view.findViewById(R.id.max_hater);
            members = (RecyclerView) view.findViewById(R.id.member_list);
            expanded = (RelativeLayout) view.findViewById(R.id.expanded);
            ratingLabel = (TextView) view.findViewById(R.id.rating_label);
            minReliabilityLabel = (TextView) view.findViewById(R.id.min_reliability_label);
            minQuicknessLabel = (TextView) view.findViewById(R.id.min_quickness_label);
            maxHatedLabel = (TextView) view.findViewById(R.id.max_hated_label);
            maxHaterLabel = (TextView) view.findViewById(R.id.max_hater_label);
            button = (FloatingActionButton) view.findViewById(R.id.join_leave_button);
        }
        @Override
        public void bind(final SingleContainer<Game> game, final int pos) {
            if (game.Properties.Desc == null || game.Properties.Desc.equals("")) {
                desc.setVisibility(View.GONE);
            } else {
                desc.setText(game.Properties.Desc);
            }
            if (game.Properties.MinRating != 0 || game.Properties.MaxRating != 0) {
                rating.setText(ctx.getResources().getString(R.string.x_to_y, game.Properties.MinRating, game.Properties.MaxRating));
                rating.setVisibility(View.VISIBLE);
                ratingLabel.setVisibility(View.VISIBLE);
            } else {
                rating.setVisibility(View.GONE);
                ratingLabel.setVisibility(View.GONE);
            }
            if (game.Properties.MinRating != 0) {
                minReliability.setText("" + game.Properties.MinReliability);
                minReliability.setVisibility(View.VISIBLE);
                minReliabilityLabel.setVisibility(View.VISIBLE);
            } else {
                minReliability.setVisibility(View.GONE);
                minReliabilityLabel.setVisibility(View.GONE);
            }
            if (game.Properties.MinQuickness != 0) {
                minQuickness.setText("" + game.Properties.MinQuickness);
                minQuickness.setVisibility(View.VISIBLE);
                minQuicknessLabel.setVisibility(View.VISIBLE);
            } else {
                minQuickness.setVisibility(View.GONE);
                minQuicknessLabel.setVisibility(View.GONE);
            }
            if (game.Properties.MaxHated != 0) {
                maxHated.setText("" + game.Properties.MaxHated);
                maxHated.setVisibility(View.VISIBLE);
                maxHatedLabel.setVisibility(View.VISIBLE);
            } else {
                maxHated.setVisibility(View.GONE);
                maxHatedLabel.setVisibility(View.GONE);
            }
            if (game.Properties.MaxHater != 0) {
                maxHater.setText("" + game.Properties.MaxHater);
                maxHater.setVisibility(View.VISIBLE);
                maxHaterLabel.setVisibility(View.VISIBLE);
            } else {
                maxHater.setVisibility(View.GONE);
                maxHaterLabel.setVisibility(View.GONE);
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

            if (expandedItems.contains(pos)) {
                expanded.setVisibility(View.VISIBLE);
            } else {
                expanded.setVisibility(View.GONE);
            }

            LinearLayoutManager membersLayoutManager = new LinearLayoutManager(ctx);
            membersLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            members.setLayoutManager(membersLayoutManager);
            members.setAdapter(new MemberAdapter(ctx, game.Properties.Members, delegateClickListener));

            boolean hasLeave = false;
            boolean hasJoin = false;
            for (Link link : game.Links) {
                if (link.Rel.equals("leave")) {
                    hasLeave = true;
                    hasJoin = false;
                }
                if (link.Rel.equals("join")) {
                    hasJoin = true;
                    hasLeave = false;
                }
            }
            if (hasLeave) {
                button.setVisibility(View.VISIBLE);
                button.setImageDrawable(ctx.getResources().getDrawable(android.R.drawable.ic_input_delete, null));
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        retrofitActivity.handleReq(retrofitActivity.memberService.MemberDelete(game.Properties.ID, App.loggedInUser.Id),
                                new Sendable<SingleContainer<Member>>() {
                                    @Override
                                    public void send(SingleContainer<Member> memberSingleContainer) {
                                        retrofitActivity.handleReq(retrofitActivity.gameService.GameLoad(game.Properties.ID),
                                                new Sendable<SingleContainer<Game>>() {
                                                    @Override
                                                    public void send(SingleContainer<Game> gameSingleContainer) {
                                                        GamesAdapter.this.items.set(pos, gameSingleContainer);
                                                        GamesAdapter.this.notifyItemChanged(pos);
                                                    }
                                                }, new RetrofitActivity.ErrorHandler(404, new Sendable<HttpException>() {
                                                    @Override
                                                    public void send(HttpException e) {
                                                        GamesAdapter.this.items.remove(pos);
                                                        GamesAdapter.this.notifyItemRemoved(pos);
                                                        GamesAdapter.this.expandedItems.remove(pos);
                                                        for (int i : GamesAdapter.this.expandedItems) {
                                                            if (i > pos) {
                                                                GamesAdapter.this.expandedItems.remove(i);
                                                                GamesAdapter.this.expandedItems.add(i+1);
                                                            }
                                                        }
                                                    }
                                                }), ctx.getResources().getString(R.string.updating));
                                    }
                                }, ctx.getResources().getString(R.string.leaving_game));
                    }
                });
            } else if (hasJoin) {
                button.setVisibility(View.VISIBLE);
                button.setImageDrawable(ctx.getResources().getDrawable(android.R.drawable.ic_input_add, null));
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        retrofitActivity.handleReq(retrofitActivity.memberService.MemberCreate(new Member(), game.Properties.ID),
                                new Sendable<SingleContainer<Member>>() {
                                    @Override
                                    public void send(SingleContainer<Member> memberSingleContainer) {
                                        retrofitActivity.handleReq(retrofitActivity.gameService.GameLoad(game.Properties.ID),
                                                new Sendable<SingleContainer<Game>>() {
                                                    @Override
                                                    public void send(SingleContainer<Game> gameSingleContainer) {
                                                        GamesAdapter.this.items.set(pos, gameSingleContainer);
                                                        GamesAdapter.this.notifyItemChanged(pos);
                                                    }
                                                }, ctx.getResources().getString(R.string.updating));
                                    }
                                }, ctx.getResources().getString(R.string.joining_game));
                    }
                });
            } else {
                button.setVisibility(View.GONE);
            }
        }
    }
    public GamesAdapter(RetrofitActivity activity, List<SingleContainer<Game>> games) {
        super(activity, games);
        retrofitActivity = activity;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.game_list_row, parent, false);
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
