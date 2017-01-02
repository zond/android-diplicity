package se.oort.diplicity;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;

import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.GameScore;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.PhaseState;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.UserStats;

public class UserStatsTable extends TableLayout {
    private AttributeSet attributeSet;
    private UserStats userStats;
    private TableRow.LayoutParams wrapContentParams =
            new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT, 0.0f);
    private TableRow.LayoutParams matchParentParams =
            new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT, 0.0f);

    private void setMargins(TableRow.LayoutParams params) {
        int margin = getResources().getDimensionPixelSize(R.dimen.member_table_margin);
        params.bottomMargin = margin;
        params.topMargin = margin;
        params.leftMargin = margin;
        params.rightMargin = margin;
    }
    public UserStatsTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.attributeSet = attrs;
        setMargins(wrapContentParams);
        setMargins(matchParentParams);
    }
    public void addText(RetrofitActivity retrofitActivity, TableRow tableRow, String text) {
        TextView scs = new TextView(retrofitActivity);
        scs.setLayoutParams(wrapContentParams);
        scs.setText(text);
        tableRow.addView(scs);
    }
    public void addRow(RetrofitActivity retrofitActivity, String text1, String text2) {
        TableRow tableRow = new TableRow(retrofitActivity);
        tableRow.setLayoutParams(matchParentParams);

        addText(retrofitActivity, tableRow, text1);
        addText(retrofitActivity, tableRow, text2);

        addView(tableRow);
    }
    public void setUserStats(RetrofitActivity retrofitActivity, final UserStats userStats) {
        removeAllViews();
        addRow(retrofitActivity, getResources().getString(R.string.rating), retrofitActivity.toString(userStats.Glicko.PracticalRating));
        addRow(retrofitActivity, getResources().getString(R.string.started_games), "" + userStats.StartedGames);
        addRow(retrofitActivity, getResources().getString(R.string.finished_games), "" + userStats.FinishedGames);
        addRow(retrofitActivity, getResources().getString(R.string.solo_wins), "" + userStats.SoloGames);
        addRow(retrofitActivity, getResources().getString(R.string.draws), "" + userStats.DIASGames);
        addRow(retrofitActivity, getResources().getString(R.string.eliminations), "" + userStats.EliminatedGames);
        addRow(retrofitActivity, getResources().getString(R.string.dropped_games), "" + userStats.DroppedGames);
        addRow(retrofitActivity, getResources().getString(R.string.ready_phases), "" + userStats.ReadyPhases);
        addRow(retrofitActivity, getResources().getString(R.string.active_phases), "" + userStats.ActivePhases);
        addRow(retrofitActivity, getResources().getString(R.string.nmr_phases), "" + userStats.NMRPhases);
        addRow(retrofitActivity, getResources().getString(R.string.reliability), retrofitActivity.toString(userStats.Reliability));
        addRow(retrofitActivity, getResources().getString(R.string.quickness), retrofitActivity.toString(userStats.Quickness));
        addRow(retrofitActivity, getResources().getString(R.string.owned_bans), "" + userStats.OwnedBans);
        addRow(retrofitActivity, getResources().getString(R.string.shared_bans), "" + userStats.SharedBans);
        addRow(retrofitActivity, getResources().getString(R.string.hated), retrofitActivity.toString(userStats.Hated));
        addRow(retrofitActivity, getResources().getString(R.string.hater), retrofitActivity.toString(userStats.Hater));
    }
}
