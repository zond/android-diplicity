package se.oort.diplicity;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;

import se.oort.diplicity.apigen.GameScore;
import se.oort.diplicity.apigen.Member;

public class MemberTable extends TableLayout {
    private List<GameScore> scores;
    private AttributeSet attributeSet;
    private TableRow.LayoutParams rowParams =
            new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT);

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
    public void setMembers(RetrofitActivity retrofitActivity, List<Member> members) {
        for (Member member : members) {
            TableRow tableRow = new TableRow(retrofitActivity);
            tableRow.setLayoutParams(rowParams);
            UserView userView = new UserView(retrofitActivity, attributeSet);
            userView.setLayoutParams(rowParams);
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
            addView(tableRow);
        }
    }
}
