import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;

final class IssueRatio {
    private final int totalIssuesCount;
    private final int bugCount;
    private final double ratio;

    IssueRatio(int totalIssuesCount, int bugCount) {
        this.totalIssuesCount = totalIssuesCount;
        this.bugCount = bugCount;
        ratio = BigDecimal.valueOf(bugCount).divide(BigDecimal.valueOf(totalIssuesCount), new MathContext(2, RoundingMode.HALF_UP)).doubleValue();
    }

    public double getRatio() {
        return ratio;
    }

    public int getTotalIssuesCount() {
        return totalIssuesCount;
    }

    public int getBugCount() {
        return bugCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IssueRatio that = (IssueRatio) o;

        if (totalIssuesCount != that.totalIssuesCount) return false;
        return bugCount == that.bugCount;
    }

    @Override
    public int hashCode() {
        int result = totalIssuesCount;
        result = 31 * result + bugCount;
        return result;
    }

    @Override
    public String toString() {

        final StringBuffer sb = new StringBuffer("IssueRatio{");
        sb.append("totalIssuesCount=").append(totalIssuesCount);
        sb.append(", bugCount=").append(bugCount);
        sb.append(", ratio=").append(NumberFormat.getPercentInstance().format(ratio));
        sb.append('}');
        return sb.toString();
    }
}
