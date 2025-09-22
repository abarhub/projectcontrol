package org.projectcontrol.core.utils;

import java.util.List;
import java.util.Objects;

public class LigneGrep {
    private int noLigne;
    private String ligne;
    private boolean trouve;
    private List<Interval> range;

    public LigneGrep(int noLigne, String ligne, boolean trouve, List<Interval> range) {
        this.noLigne = noLigne;
        this.ligne = ligne;
        this.trouve = trouve;
        this.range = range;
    }

    public LigneGrep() {
    }

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LigneGrep ligneGrep = (LigneGrep) o;
        return noLigne == ligneGrep.noLigne && trouve == ligneGrep.trouve &&
                Objects.equals(ligne, ligneGrep.ligne) &&
                Objects.equals(range, ligneGrep.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noLigne, ligne, trouve, range);
    }
}
