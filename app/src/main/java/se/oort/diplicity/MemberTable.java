package se.oort.diplicity;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
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

public class MemberTable extends TableLayout {
    private List<GameScore> scores;
    private List<PhaseState> phaseStates;
    private PhaseMeta phaseMeta;
    private Game game;
    private AttributeSet attributeSet;
    private TableRow.LayoutParams rowParams =
            new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT, 0.0f);

    public MemberTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.attributeSet = attrs;
        int margin = getResources().getDimensionPixelSize(R.dimen.member_table_margin);
        rowParams.bottomMargin = margin;
        rowParams.topMargin = margin;
        rowParams.leftMargin = margin;
        rowParams.rightMargin = margin;
    }
    public void setScores(List<GameScore> scores) {
        this.scores = scores;
    }
    public void setPhaseStates(Game game, PhaseMeta phaseMeta, List<PhaseState> phaseStates) {
        this.phaseMeta = phaseMeta;
        this.phaseStates = phaseStates;
        this.game = game;
    }
    public void setMembers(final RetrofitActivity retrofitActivity, final List<Member> members) {
        removeAllViews();
        for (final Member member : members) {
            boolean rowOK = true;
            TableRow tableRow = new TableRow(retrofitActivity);
            tableRow.setLayoutParams(rowParams);
            UserView userView = new UserView(retrofitActivity, attributeSet);
            TableRow.LayoutParams userParams =
                    new TableRow.LayoutParams(
                            TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT, 1.0f);
            userView.setLayoutParams(userParams);
            userView.setUser(retrofitActivity, member.User);
            tableRow.addView(userView);
            if (member.Nation != null && !member.Nation.equals("")) {
                TextView nation = new TextView(retrofitActivity);
                nation.setLayoutParams(rowParams);
                nation.setText(member.Nation);
                tableRow.addView(nation);
            }
            if (this.scores != null) {
                GameScore foundScore = null;
                for (GameScore score : scores) {
                    if (score.UserId.equals(member.User.Id)) {
                        foundScore = score;
                        break;
                    }
                }
                if (foundScore != null) {
                    TextView scs = new TextView(retrofitActivity);
                    scs.setLayoutParams(rowParams);
                    scs.setText(getResources().getString(R.string._scs, foundScore.SCs));
                    tableRow.addView(scs);
                    TextView points = new TextView(retrofitActivity);
                    points.setLayoutParams(rowParams);
                    points.setText(getResources().getString(R.string._points, foundScore.Score.intValue()));
                    tableRow.addView(points);
                }
            }
            if (this.phaseStates != null) {
                PhaseState foundState = null;
                for (PhaseState state : phaseStates) {
                    if (state.Nation.equals(member.Nation)) {
                        foundState = state;
                        break;
                    }
                }
                if (foundState != null) {
                    final PhaseState finalFoundState = foundState;
                    CheckBox readyToResolve = new CheckBox(retrofitActivity);
                    readyToResolve.setText(R.string.rdy);
                    readyToResolve.setLayoutParams(rowParams);
                    readyToResolve.setChecked(finalFoundState.ReadyToResolve);
                    if (!phaseMeta.Resolved && App.loggedInUser.Id.equals(member.User.Id)) {
                        readyToResolve.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                finalFoundState.ReadyToResolve = b;
                                retrofitActivity.handleReq(
                                        retrofitActivity.phaseStateService.PhaseStateUpdate(finalFoundState, game.ID, phaseMeta.PhaseOrdinal.toString(), member.Nation),
                                        new Sendable<SingleContainer<PhaseState>>() {
                                            @Override
                                            public void send(SingleContainer<PhaseState> phaseStateSingleContainer) {}
                                        }, getResources().getString(R.string.updating_phase_state));
                            }
                        });
                        readyToResolve.setEnabled(true);
                    } else {
                        readyToResolve.setEnabled(false);
                    }
                    tableRow.addView(readyToResolve);
                    CheckBox wantsDIAS = new CheckBox(retrofitActivity);
                    wantsDIAS.setText(R.string.DIAS);
                    wantsDIAS.setLayoutParams(rowParams);
                    wantsDIAS.setChecked(finalFoundState.WantsDIAS);
                    if (!phaseMeta.Resolved && App.loggedInUser.Id.equals(member.User.Id)) {
                        wantsDIAS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                finalFoundState.WantsDIAS = b;
                                retrofitActivity.handleReq(
                                        retrofitActivity.phaseStateService.PhaseStateUpdate(finalFoundState, game.ID, phaseMeta.PhaseOrdinal.toString(), member.Nation),
                                        new Sendable<SingleContainer<PhaseState>>() {
                                            @Override
                                            public void send(SingleContainer<PhaseState> phaseStateSingleContainer) {}
                                        }, getResources().getString(R.string.updating_phase_state));
                            }
                        });
                    } else {
                        wantsDIAS.setEnabled(false);
                    }
                    tableRow.addView(wantsDIAS);
                    CheckBox onProbation = new CheckBox(retrofitActivity);
                    onProbation.setText(R.string.nmr);
                    onProbation.setLayoutParams(rowParams);
                    onProbation.setChecked(finalFoundState.OnProbation);
                    onProbation.setEnabled(false);
                    tableRow.addView(onProbation);
                } else {
                    rowOK = false;
                }
            }
            if (rowOK) {
                addView(tableRow);
            }
        }
    }
}
