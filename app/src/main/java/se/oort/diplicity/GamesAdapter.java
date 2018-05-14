package se.oort.diplicity;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.adapter.rxjava.HttpException;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Link;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.Phase;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.game.GameActivity;

public class GamesAdapter extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder> {
    /** Lower case name of the classical variant. Games of this variant are displayed slightly differently in the listings. */
    public static final String CLASSICAL = "classical";
    private RetrofitActivity retrofitActivity;
    private Set<Integer> expandedItems = new AbstractSet<Integer>() {
        private Map<Integer, Object> backend = new ConcurrentHashMap<Integer, Object>();
        @Override
        public boolean add(Integer i) {
            boolean rval = backend.containsKey(i);
            backend.put(i, new Object());
            return rval;
        }
        @Override
        public boolean remove(Object i) {
            boolean rval = backend.containsKey(i);
            backend.remove(i);
            return rval;
        }
        @Override
        public Iterator<Integer> iterator() {
            return backend.keySet().iterator();
        }
        @Override
        public int size() {
            return backend.size();
        }
    };

    public void consumeDiplicityJSON(final MessagingService.DiplicityJSON diplicityJSON) {
        if (diplicityJSON.type.equals("message")) {
            for (SingleContainer<Game> game : items) {
                if (game.Properties.ID.equals(diplicityJSON.gameID)) {
                    Member member = null;
                    for (Member m : game.Properties.Members) {
                        if (m.User.Id.equals(m.User.Id)) {
                            member = m;
                            break;
                        }
                    }
                    if (member != null) {
                        member.UnreadMessages++;
                        notifyDataSetChanged();
                    }
                }
            }
        }
    }

    public class ViewHolder extends RecycleAdapter<SingleContainer<Game>, GamesAdapter.ViewHolder>.ViewHolder {
        TextView desc, variant, deadline, state, rating,
                minReliability, minQuickness, maxHated, maxHater,
                ratingLabel, minReliabilityLabel, minQuicknessLabel,
                maxHatedLabel, maxHaterLabel, nextDeadline;
        MemberTable members;
        RelativeLayout expanded;
        View.OnClickListener delegateClickListener;
        FloatingActionButton joinLeaveButton;
        ImageView alertIcon;
        ImageView readyIcon;
        TextView unreadMessages;
        TextView createdAt;
        TextView startedAt;
        TextView startedAtLabel;
        TextView finishedAt;
        TextView finishedAtLabel;
        ImageView timerIcon;
        ImageView starIcon;
        ImageView barIcon;
        TextView conferenceChatDisabled;
        TextView groupChatDisabled;
        TextView privateChatDisabled;
        ImageView phoneIcon;
        ImageView syncDisabledIcon;
        TextView privateLabel;
        ImageView playlistIcon;
        TextView allocationLabel;
        TextView allocation;

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
            createdAt = (TextView) view.findViewById(R.id.created_at);
            startedAt = (TextView) view.findViewById(R.id.started_at);
            startedAtLabel = (TextView) view.findViewById(R.id.started_at_label);
            finishedAt = (TextView) view.findViewById(R.id.finished_at);
            finishedAtLabel = (TextView) view.findViewById(R.id.finished_at_label);
            timerIcon = (ImageView) view.findViewById(R.id.timer_icon);
            starIcon = (ImageView) view.findViewById(R.id.star_icon);
            barIcon = (ImageView) view.findViewById(R.id.bar_icon);
            conferenceChatDisabled = (TextView) view.findViewById(R.id.disabled_conference_chat_label);
            groupChatDisabled = (TextView) view.findViewById(R.id.disabled_group_chat_label);
            privateChatDisabled = (TextView) view.findViewById(R.id.disabled_private_chat_label);
            phoneIcon = (ImageView) view.findViewById(R.id.phone_icon);
            syncDisabledIcon = (ImageView) view.findViewById(R.id.sync_disabled_icon);
            privateLabel = (TextView) view.findViewById(R.id.private_label);
            playlistIcon = (ImageView) view.findViewById(R.id.playlist_icon);
            allocationLabel = (TextView) view.findViewById(R.id.allocation_label);
            allocation = (TextView) view.findViewById(R.id.allocation);
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
                        game.Properties.MinRating == 0.0 ? "" : retrofitActivity.toString(game.Properties.MinRating),
                        game.Properties.MaxRating == 0.0 ? "" : retrofitActivity.toString(game.Properties.MaxRating)));
                rating.setVisibility(View.VISIBLE);
                ratingLabel.setVisibility(View.VISIBLE);
            } else {
                rating.setVisibility(View.GONE);
                ratingLabel.setVisibility(View.GONE);
            }
            if (game.Properties.MinReliability != 0) {
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
            if (game.Properties.DisableConferenceChat) {
                conferenceChatDisabled.setVisibility(View.VISIBLE);
            } else {
                conferenceChatDisabled.setVisibility(View.GONE);
            }
            if (game.Properties.DisableGroupChat) {
                groupChatDisabled.setVisibility(View.VISIBLE);
            } else {
                groupChatDisabled.setVisibility(View.GONE);
            }
            if (game.Properties.DisablePrivateChat) {
                privateChatDisabled.setVisibility(View.VISIBLE);
            } else {
                privateChatDisabled.setVisibility(View.GONE);
            }
            DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
            createdAt.setText(format.format(game.Properties.CreatedAgo.deadlineAt()));
            if (game.Properties.Started) {
                startedAt.setText(format.format(game.Properties.StartedAgo.deadlineAt()));
            } else {
                startedAt.setVisibility(View.GONE);
                startedAtLabel.setVisibility(View.GONE);
            }
            if (game.Properties.Finished) {
                finishedAt.setText(format.format(game.Properties.FinishedAgo.deadlineAt()));
            } else {
                finishedAt.setVisibility(View.GONE);
                finishedAtLabel.setVisibility(View.GONE);
            }

            if (game.Properties.MinQuickness != 0 || game.Properties.MinReliability != 0) {
                timerIcon.setVisibility(View.VISIBLE);
            } else {
                timerIcon.setVisibility(View.GONE);
            }
            if (game.Properties.MinRating != 0 || game.Properties.MaxRating != 0) {
                starIcon.setVisibility(View.VISIBLE);
            } else {
                starIcon.setVisibility(View.GONE);
            }
            if (game.Properties.MaxHated != 0 || game.Properties.MaxHater != 0) {
                barIcon.setVisibility(View.VISIBLE);
            } else {
                barIcon.setVisibility(View.GONE);
            }
            if (game.Properties.DisableConferenceChat || game.Properties.DisableGroupChat || game.Properties.DisablePrivateChat) {
                phoneIcon.setVisibility(View.VISIBLE);
            } else {
                phoneIcon.setVisibility(View.GONE);
            }
            if (game.Properties.Private) {
                syncDisabledIcon.setVisibility(View.VISIBLE);
                privateLabel.setVisibility(View.VISIBLE);
            } else {
                syncDisabledIcon.setVisibility(View.GONE);
                privateLabel.setVisibility(View.GONE);
            }
            if (game.Properties.NationAllocation == 1) {
                playlistIcon.setVisibility(View.VISIBLE);
                allocation.setText(retrofitActivity.getResources().getString(R.string.preferences));
            } else {
                playlistIcon.setVisibility(View.GONE);
                allocation.setText(retrofitActivity.getResources().getString(R.string.random));
            }

            variant.setText(makeVariantText(game.Properties.Variant));

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
                VariantService.Variant variant = null;
                for (SingleContainer<VariantService.Variant> var : retrofitActivity.getVariants().Properties) {
                    if (var.Properties.Name.equals(game.Properties.Variant)) {
                        variant = var.Properties;
                    }
                }
                if (variant == null || variant.Nations == null) {
                    state.setText(ctx.getResources().getQuantityString(R.plurals.player, game.Properties.NMembers.intValue(), game.Properties.NMembers));
                } else {
                    state.setText(retrofitActivity.getResources().getString(R.string.x_of_y_players, game.Properties.NMembers.intValue(), variant.Nations.size()));
                }
            } else if (game.Properties.NewestPhaseMeta.size() > 0) {
                PhaseMeta phaseMeta = game.Properties.NewestPhaseMeta.get(0);
                state.setText(ctx.getResources().getString(R.string.season_year_type, phaseMeta.Season, phaseMeta.Year, phaseMeta.Type));
            }

            if (expandedItems.contains(pos)) {
                desc.setMaxLines(3);
                expanded.setVisibility(View.VISIBLE);
            } else {
                desc.setMaxLines(1);
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
                                                }, new RetrofitActivity.SingleErrorHandler(404, new Sendable<HttpException>() {
                                                    @Override
                                                    public void send(HttpException e) {
                                                        GamesAdapter.this.items.remove(pos);
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
                        Sendable<List<String>> joiner = new Sendable<List<String>>() {
                            @Override
                            public void send(List<String> strings) {
                                Member member = new Member();
                                member.NationPreferences = TextUtils.join(",", strings);
                                retrofitActivity.handleReq(retrofitActivity.memberService.MemberCreate(member, game.Properties.ID),
                                        new Sendable<SingleContainer<Member>>() {
                                            @Override
                                            public void send(SingleContainer<Member> memberSingleContainer) {
                                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(retrofitActivity);
                                                prefs.edit().putBoolean(MainActivity.HAS_JOINED_GAME_KEY, true).apply();
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
                        };
                        // 1 is preferences
                        if (game.Properties.NationAllocation == 1) {
                            MainActivity.showNationPreferencesDialog(retrofitActivity, game.Properties.Variant, joiner);
                        } else {
                            joiner.send(new ArrayList<String>());
                        }
                    }
                });
            } else {
                joinLeaveButton.setVisibility(View.GONE);
            }

            ((FloatingActionButton) itemView.findViewById(R.id.open_button)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    retrofitActivity.handleReq(
                            retrofitActivity.phaseService.ListPhases(game.Properties.ID),
                            new Sendable<MultiContainer<Phase>>() {
                                @Override
                                public void send(MultiContainer<Phase> phaseMultiContainer) {
                                    retrofitActivity.startActivity(GameActivity.startGameIntent(retrofitActivity, game.Properties, phaseMultiContainer));
                                }
                            }, retrofitActivity.getResources().getString(R.string.loading_state));
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
                        // 1 is preferences
                        final List<String> prefs = new ArrayList<String>(Arrays.asList(finalMember.NationPreferences.split(",")));
                        if (game.Properties.NationAllocation == 1 && !game.Properties.Started) {
                            ListView nationPreference = (ListView) dialog.findViewById(R.id.nation_preferences);
                            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                    retrofitActivity,
                                    android.R.layout.simple_list_item_single_choice,
                                    prefs);
                            nationPreference.setAdapter(adapter);
                            nationPreference.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    Log.d("diplicity", "onclick " + position);
                                    if (position > 0) {
                                        String up = prefs.get(position - 1);
                                        prefs.set(position - 1, prefs.get(position));
                                        prefs.set(position, up);
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                        final TextView aliasView = (TextView) dialog.findViewById(R.id.alias);
                        aliasView.setText(finalMember.GameAlias);
                        ((FloatingActionButton) dialog.findViewById(R.id.update_membership_button)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finalMember.GameAlias = aliasView.getText().toString();
                                finalMember.NationPreferences = TextUtils.join(",", prefs);
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

        /**
         * Create the text to display in the variant field. This will add the word "Variant" before everything except classical games,
         * to try to highlight to users when they are not joining a standard game.
         *
         * @param variantName The name of the variant.
         */
        private Spanned makeVariantText(String variantName) {
            String sourceString;
            if (variantName.equalsIgnoreCase(CLASSICAL)) {
                sourceString = variantName;
            } else {
                sourceString = ctx.getResources().getString(R.string.variant_label, variantName);
            }
            return Html.fromHtml(sourceString);
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
