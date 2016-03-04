package com.netflix.vms.transformer.hollowoutput;


public class RolloutPhaseWindow implements Cloneable {

    public int phaseOrdinal = java.lang.Integer.MIN_VALUE;
    public AvailabilityWindow phaseWindow = null;

    public boolean equals(Object other) {
        if(other == this)  return true;
        if(!(other instanceof RolloutPhaseWindow))
            return false;

        RolloutPhaseWindow o = (RolloutPhaseWindow) other;
        if(o.phaseOrdinal != phaseOrdinal) return false;
        if(o.phaseWindow == null) {
            if(phaseWindow != null) return false;
        } else if(!o.phaseWindow.equals(phaseWindow)) return false;
        return true;
    }

    public int hashCode() {
        int hashCode = 1;
        hashCode = hashCode * 31 + phaseOrdinal;
        hashCode = hashCode * 31 + (phaseWindow == null ? 1237 : phaseWindow.hashCode());
        return hashCode;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("RolloutPhaseWindow{");
        builder.append("phaseOrdinal=").append(phaseOrdinal);
        builder.append(",phaseWindow=").append(phaseWindow);
        builder.append("}");
        return builder.toString();
    }

    public RolloutPhaseWindow clone() {
        try {
            RolloutPhaseWindow clone = (RolloutPhaseWindow)super.clone();
            clone.__assigned_ordinal = -1;
            return clone;
        } catch (CloneNotSupportedException cnse) { throw new RuntimeException(cnse); }
    }

    @SuppressWarnings("unused")
    private int __assigned_ordinal = -1;
}