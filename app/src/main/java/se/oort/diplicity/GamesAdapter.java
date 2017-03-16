package se.oort.diplicity;

import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.adapter.rxjava.HttpException;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Link;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.game.GameActivity;

public class GamesAdapter extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder> {
    private RetrofitActivity retrofitActivity;
    private Set<Integer> expandedItems = new HashSet<>();
    public class ViewHolder extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder>.ViewHolder {
        TextView desc, variant, deadline, state, rating,
                minReliability, minQuickness, maxHated, maxHater,
                ratingLabel, minReliabilityLabel, minQuicknessLabel,
                maxHatedLabel, maxHaterLabel, nextDeadline;
        MemberTable members;
        RelativeLayout expanded;
        View.OnClickListener delegateClickListener;
        FloatingActionButton joinLeaveButton;
        ImageView alertIcon, readyIcon;
        TextView unreadMessages;

        public ViewHolder(View view) {
            super(view);
            unreadMessages = (TextView) view.findViewById(R.id.unread_messages_count);
            alertIcon = (ImageView) view.findViewById(R.id.alert_icon);
            readyIcon = (ImageView) view.findViewById(R.id.ready_icon);
            desc = (TextView) view.findViewById(R.id.desc);
            variant = (TextView) view.findViewById(R.id.variant);
            deadline = (TextView) view.findViewById(R.id.deadline);
            nextDeadline = (TextView) view.findViewById(R.id.next_deadline);
            state = (TextView) view.findViewById(R.id.state);
            rating = (TextView) view.findViewById(R.id.rating);
            minReliability = (TextView) view.findViewById(R.id.min_reliability);
            minQuickness = (TextView) view.findViewById(R.id.min_quickness);
            maxHated = (TextView) view.findViewById(R.id.max_hated);
            maxHater = (TextView) view.findViewById(R.id.max_hater);
            members = (MemberTable) view.findViewById(R.id.member_table);
            expanded = (RelativeLayout) view.findViewById(R.id.expanded);
            ratingLabel = (TextView) view.findViewById(R.id.rating_label);
            minReliabilityLabel = (TextView) view.findViewById(R.id.min_reliability_label);
            minQuicknessLabel = (TextView) view.findViewById(R.id.min_quickness_label);
            maxHatedLabel = (TextView) view.findViewById(R.id.max_hated_label);
            maxHaterLabel = (TextView) view.findViewById(R.id.max_hater_label);
            joinLeaveButton = (FloatingActionButton) view.findViewById(R.id.join_leave_button);
        }
        @Override
        public void bind(final SingleContainer<Game> game, final int pos) {
            Member member = retrofitActivity.getLoggedInMember(game.Properties);
            if (member != null && member.GameAlias != null && !member.GameAlias.equals("")) {
                desc.setVisibility(View.VISIBLE);
                desc.setText(member.GameAlias);
            } else {
                if (game.Properties.Desc == null || game.Properties.Desc.equals("")) {
                    desc.setVisibility(View.GONE);
                } else {
                    desc.setVisibility(View.VISIBLE);
                    desc.setText(game.Properties.Desc);
                }
            }
            if (member != null && game.Properties.Started && !game.Properties.Finished) {
                if (member.NewestPhaseState.OnProbation) {
                    alertIcon.setVisibility(View.VISIBLE);
                    readyIcon.setVisibility(View.GONE);
                } else if (member.NewestPhaseState.ReadyToResolve) {
                    alertIcon.setVisibility(View.GONE);
                    readyIcon.setVisibility(View.VISIBLE);
                } else {
                    alertIcon.setVisibility(View.GONE);
                    readyIcon.setVisibility(View.GONE);
                }
            } else {
                alertIcon.setVisibility(View.GONE);
                readyIcon.setVisibility(View.GONE);
            }
            if (member != null && member.UnreadMessages > 0) {
                unreadMessages.setText("" + member.UnreadMessages);
                unreadMessages.setVisibility(View.VISIBLE);
            } else {
                unreadMessages.setVisibility(View.GONE);
            }
            if (game.Properties.MinRating != 0 || game.Properties.MaxRating != 0) {
                rating.setText(ctx.getResources().getString(
                        R.string.x_to_y,
                        retrofitActivity.toString(game.Properties.MinRating),
                        retrofitActivity.toString(game.Properties.MaxRating)));
                rating.setVisibility(View.VISIBLE);
                ratingLabel.setVisibility(View.VISIBLE);
            } else {
                rating.setVisibility(View.GONE);
                ratingLabel.setVisibility(View.GONE);
            }
            if (game.Properties.MinRating != 0) {
                minReliability.setText(retrofitActivity.toString(game.Properties.MinReliability));
                minReliability.setVisibility(View.VISIBLE);
                minReliabilityLabel.setVisibility(View.VISIBLE);
            } else {
                minReliability.setVisibility(View.GONE);
                minReliabilityLabel.setVisibility(View.GONE);
            }
            if (game.Properties.MinQuickness != 0) {
                minQuickness.setText(retrofitActivity.toString(game.Properties.MinQuickness));
                minQuickness.setVisibility(View.VISIBLE);
                minQuicknessLabel.setVisibility(View.VISIBLE);
            } else {
                minQuickness.setVisibility(View.GONE);
                minQuicknessLabel.setVisibility(View.GONE);
            }
            if (game.Properties.MaxHated != 0) {
                maxHated.setText(retrofitActivity.toString(game.Properties.MaxHated));
                maxHated.setVisibility(View.VISIBLE);
                maxHatedLabel.setVisibility(View.VISIBLE);
            } else {
                maxHated.setVisibility(View.GONE);
                maxHatedLabel.setVisibility(View.GONE);
            }
            if (game.Properties.MaxHater != 0) {
                maxHater.setText(retrofitActivity.toString(game.Properties.MaxHater));
                maxHater.setVisibility(View.VISIBLE);
                maxHaterLabel.setVisibility(View.VISIBLE);
            } else {
                maxHater.setVisibility(View.GONE);
                maxHaterLabel.setVisibility(View.GONE);
            }

            variant.setText(game.Properties.Variant);

            deadline.setText(App.minutesToDuration(game.Properties.PhaseLengthMinutes.intValue()));

            if (
                    game.Properties.Started &&
                    game.Properties.NewestPhaseMeta.size() > 0 &&
                    game.Properties.NewestPhaseMeta.get(0).NextDeadlineIn.millisLeft() > 0) {
                nextDeadline.setText(App.millisToDuration(game.Properties.NewestPhaseMeta.get(0).NextDeadlineIn.millisLeft()));
                nextDeadline.setVisibility(View.VISIBLE);
            } else {
                nextDeadline.setVisibility(View.GONE);
            }

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

            members.setMembers(retrofitActivity, game.Properties, game.Properties.Members);

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
                joinLeaveButton.setVisibility(View.VISIBLE);
                joinLeaveButton.setImageDrawable(ctx.getResources().getDrawable(R.drawable.ic_clear_black_24dp));
                joinLeaveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        retrofitActivity.handleReq(retrofitActivity.memberService.MemberDelete(game.Properties.ID, retrofitActivity.getLoggedInUser().Id),
                                new Sendable<SingleContainer<Member>>() {
                                    @Override
                                    public void send(SingleContainer<Member> memberSingleContainer) {
                                        retrofitActivity.handleReq(retrofitActivity.gameService.GameLoad(game.Properties.ID),
                                                new Sendable<SingleContainer<Game>>() {
                                                    @Override
                                                    public void send(SingleContainer<Game> gameSingleContainer) {
                                                        GamesAdapter.this.items.set(pos, gameSingleContainer);
                                                        GamesAdapter.this.notifyDataSetChanged();
                                                    }
                                                }, new RetrofitActivity.ErrorHandler(404, new Sendable<HttpException>() {
                                                    @Override
                                                    public void send(HttpException e) {
                                                        GamesAdapter.this.items.remove(pos);
                                                        GamesAdapter.this.notifyDataSetChanged();
                                                        GamesAdapter.this.expandedItems.remove(pos);
                                                        for (int i : GamesAdapter.this.expandedItems) {
                                                            if (i > pos) {
                                                                GamesAdapter.this.expandedItems.remove(i);
                                                                GamesAdapter.this.expandedItems.add(i+1);
                                                            }
                                                        }
                                                        if (GamesAdapter.this.items.size() == 0) {
                                                            retrofitActivity.findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
                                                            retrofitActivity.findViewById(R.id.content_list).setVisibility(View.GONE);
                                                        }
                                                        GamesAdapter.this.notifyDataSetChanged();
                                                    }
                                                }), ctx.getResources().getString(R.string.updating));
                                    }
                                }, ctx.getResources().getString(R.string.leaving_game));
                    }
                });
            } else if (hasJoin) {
                joinLeaveButton.setVisibility(View.VISIBLE);
                joinLeaveButton.setImageDrawable(ctx.getResources().getDrawable(android.R.drawable.ic_input_add));
                joinLeaveButton.setOnClickListener(new View.OnClickListener() {
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
                                                        GamesAdapter.this.notifyDataSetChanged();
                                                    }
                                                }, ctx.getResources().getString(R.string.updating));
                                    }
                                }, ctx.getResources().getString(R.string.joining_game));
                    }
                });
            } else {
                joinLeaveButton.setVisibility(View.GONE);
            }

            ((FloatingActionButton) itemView.findViewById(R.id.open_button)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (game.Properties.NewestPhaseMeta != null && game.Properties.NewestPhaseMeta.size() > 0) {
                        GameActivity.startGameActivity(retrofitActivity, game.Properties, game.Properties.NewestPhaseMeta.get(0));
                    } else {
                        GameActivity.startGameActivity(retrofitActivity, game.Properties, null);
                    }
                }
            });

            FloatingActionButton editMembershipButton = (FloatingActionButton) itemView.findViewById(R.id.edit_membership_button);
            if (member != null) {
                final Member finalMember = member;
                editMembershipButton.setVisibility(View.VISIBLE);
                editMembershipButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final AlertDialog dialog = new AlertDialog.Builder(retrofitActivity).setView(R.layout.edit_membership_dialog).show();
                        final TextView aliasView = (TextView) dialog.findViewById(R.id.alias);
                        aliasView.setText(finalMember.GameAlias);
                        ((FloatingActionButton) dialog.findViewById(R.id.update_membership_button)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finalMember.GameAlias = aliasView.getText().toString();
                                retrofitActivity.handleReq(
                                        retrofitActivity.memberService.MemberUpdate(finalMember, game.Properties.ID, finalMember.User.Id),
                                        new Sendable<SingleContainer<Member>>() {
                                            @Override
                                            public void send(SingleContainer<Member> memberSingleContainer) {
                                                dialog.dismiss();
                                                notifyDataSetChanged();
                                            }
                                        }, retrofitActivity.getResources().getString(R.string.updating_membership));
                            }
                        });
                    }
                });
            } else {
                editMembershipButton.setVisibility(View.GONE);
            }
        }
    }
    public GamesAdapter(RetrofitActivity activity, List<SingleContainer<Game>> games) {
        super(activity, games);
        retrofitActivity = activity;
    }

    public void clearExpanded() {
        expandedItems.clear();
        notifyDataSetChanged();
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
                notifyDataSetChanged();
            }
        };
        itemView.setOnClickListener(clickListener);
        vh.delegateClickListener = clickListener;
        return vh;
    }
}
