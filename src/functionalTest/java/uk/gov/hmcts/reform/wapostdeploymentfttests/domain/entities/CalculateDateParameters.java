package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities;

public class CalculateDateParameters {

    char plusOrMinus;
    int dayAdjustment;
    boolean workingDays;

    public CalculateDateParameters(char plusOrMinus, int dayAdjustment, boolean workingDays) {
        this.plusOrMinus = plusOrMinus;
        this.dayAdjustment = dayAdjustment;
        this.workingDays = workingDays;
    }


    public char getPlusOrMinus() {
        return plusOrMinus;
    }

    public int getDayAdjustment() {
        return dayAdjustment;
    }

    public boolean isWorkingDays() {
        return workingDays;
    }
}
