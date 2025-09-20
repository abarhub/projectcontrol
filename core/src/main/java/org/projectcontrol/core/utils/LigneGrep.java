package org.projectcontrol.core.utils;

import org.apache.commons.lang3.Range;

import java.util.List;

public class LigneGrep {
    private int noLigne;
    private String ligne;
    private boolean trouve;
    private List<Interval> range;

    public int getNoLigne() {
        return noLigne;
    }

    public void setNoLigne(int noLigne) {
        this.noLigne = noLigne;
    }

    public String getLigne() {
        return ligne;
    }

    public void setLigne(String ligne) {
        this.ligne = ligne;
    }

    public boolean isTrouve() {
        return trouve;
    }

    public void setTrouve(boolean trouve) {
        this.trouve = trouve;
    }

    public List<Interval> getRange() {
        return range;
    }

    public void setRange(List<Interval> range) {
        this.range = range;
    }

    @Override
    public String toString() {
        return "LigneGrep{" +
                "noLigne=" + noLigne +
                ", ligne='" + ligne + '\'' +
                ", trouve=" + trouve +
                ", range=" + range +
                '}';
    }
}
