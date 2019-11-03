package com.github.giji34.t;

class MutableSelectedBlockRange {
    public Loc start;
    public Loc end;

    MutableSelectedBlockRange() {
    }

    void setStart(Loc start) {
        if (start == null) {
            return;
        }
        this.start = start.clone();
    }

    void setEnd(Loc end) {
        if (end == null) {
            return;
        }
        this.end = end.clone();
    }

    boolean isReady() {
        return this.start != null && this.end != null;
    }

    /*nullable*/ SelectedBlockRange isolate() {
        if (this.start == null || this.end == null) {
            return null;
        }
        return new SelectedBlockRange(this.start, this.end);
    }
}
