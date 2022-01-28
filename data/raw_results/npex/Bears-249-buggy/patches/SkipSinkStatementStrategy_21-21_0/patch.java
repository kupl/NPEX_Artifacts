package org.imdea.vcd.queue;

import org.imdea.vcd.queue.box.QueueBox;
import org.imdea.vcd.queue.clock.Clock;
import org.imdea.vcd.queue.clock.Dot;
import org.imdea.vcd.queue.clock.MaxInt;

/**
 *
 * @author Vitor Enes
 * @param <E>
 */
public class QueueAddArgs<E extends QueueBox> {

    private Dot dot;
    private Clock<MaxInt> conf;
    private E box;

public QueueAddArgs(org.imdea.vcd.queue.QueueAddArgs queueAddArgs) {
    this.dot = new org.imdea.vcd.queue.clock.Dot(queueAddArgs.getDot());
    /* NPEX_PATCH_BEGINS */
    if (queueAddArgs.getConf() != null) {
        this.conf = new org.imdea.vcd.queue.clock.Clock<>(queueAddArgs.getConf());
    }
    this.box = ((E) (queueAddArgs.box.clone()));
}

    public QueueAddArgs(Dot dot, Clock<MaxInt> conf, E box) {
        this.dot = dot;
        this.conf = conf;
        this.box = box;
    }

    public Dot getDot() {
        return dot;
    }

    public void setDot(Dot dot) {
        this.dot = dot;
    }

    public Clock<MaxInt> getConf() {
        return conf;
    }

    public void setConf(Clock<MaxInt> conf) {
        this.conf = conf;
    }

    public E getBox() {
        return box;
    }

    public void setBox(E box) {
        this.box = box;
    }

    @Override
    public String toString() {
        return dot + " " + conf;
    }

    @Override
    protected Object clone() {
        QueueAddArgs queueAddArgs = new QueueAddArgs(this);
        return queueAddArgs;
    }

}
