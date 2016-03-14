package qz.ui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Tres on 2/28/2015.
 * <p/>
 * <code>ArrayList</code> which is linked to an internal <code>JList</code> and synced with its
 * <code>DefaultListModel</code>
 * <p/>
 * Updates to the <code>JList</code> are non-blocking and submitted on the Event Dispatch thread.
 * Class created to reduce the amount of thread-safe calls cluttering up the <code>>SiteManagerDialog</code.
 */
public class ContainerList<D> extends ArrayList<D> {

    private JList<D> certList;
    private SyncedListModel listModel;
    private Object tag;

    public ContainerList() {
        super();
        this.listModel = new SyncedListModel();
        this.certList = new JList<D>(listModel);
    }

    /**
     * Sets a miscellaneous <code>Object</code> placeholder
     *
     * @param tag A generic <code>Object</code> placeholder
     */
    public void setTag(Object tag) {
        this.tag = tag;
    }

    public Object getTag() {
        return tag;
    }

    public JList getList() {
        return certList;
    }

    @Override
    public boolean remove(final Object o) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listModel.skipNextSuperCall();
                listModel.removeElement(o);
            }
        });
        return super.remove(o);
    }

    @Override
    public boolean add(final D element) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listModel.skipNextSuperCall();
                listModel.addElement(element);
            }
        });
        return super.add(element);
    }

    @Override
    public void add(final int index, final D element) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listModel.skipNextSuperCall();
                listModel.add(index, element);
            }
        });
        super.add(index, element);
    }

    @Override
    public boolean addAll(final Collection<? extends D> collection) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for(D element : collection) {
                    listModel.skipNextSuperCall();
                    listModel.addElement(element);
                }
            }
        });
        return super.addAll(collection);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends D> c) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int counter = 0;
                for(D element : c) {
                    listModel.skipNextSuperCall();
                    listModel.add(index + counter++, element);
                }
            }
        });
        return super.addAll(index, c);
    }

    @Override
    public void clear() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listModel.skipNextSuperCall();
                listModel.removeAllElements();
            }
        });
        super.clear();
    }

    @Override
    protected void removeRange(final int fromIndex, final int toIndex) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                listModel.skipNextSuperCall();
                listModel.removeRange(fromIndex, toIndex);
            }
        });
        super.removeRange(fromIndex, toIndex);
    }

    /**
     * An internal subclass of <code>>DefaultListModel</code> to support the sync-back of elements in case the list
     * model is modified directly.
     */
    private class SyncedListModel extends DefaultListModel<D> {
        public SyncedListModel() {
            super();
        }

        AtomicBoolean performSuperCall = new AtomicBoolean(true);

        /**
         * Flag to prevent adding items twice or circular references. This intentionally resets any time data is added
         * or removed, so it must be called before each operation to be honored.
         */
        public void skipNextSuperCall() {
            this.performSuperCall.set(false);
        }

        @Override
        public void add(int index, D element) {
            super.add(index, element);
            if (performSuperCall.getAndSet(true) && !ContainerList.this.contains(element)) {
                ContainerList.super.add(element);
            }
        }

        @Override
        public void addElement(D element) {
            super.addElement(element);
            if (performSuperCall.getAndSet(true) && !ContainerList.this.contains(element)) {
                ContainerList.super.add(element);
            }
        }

        @Override
        public D remove(int index) {
            D removed = super.remove(index);
            if (performSuperCall.getAndSet(true)) {
                ContainerList.super.remove(index);
            }
            return removed;
        }

        @Override
        public boolean removeElement(Object o) {
            boolean success = super.removeElement(o);
            if (performSuperCall.getAndSet(true)) {
                ContainerList.super.remove(o);
            }
            return success;
        }

        @Override
        public void removeElementAt(int index) {
            super.removeElementAt(index);
            if (performSuperCall.getAndSet(true)) {
                ContainerList.super.remove(index);
            }
            super.removeElementAt(index);
        }

        @Override
        public void removeAllElements() {
            super.removeAllElements();
            if (performSuperCall.getAndSet(true)) {
                ContainerList.super.clear();
            }
        }

        @Override
        public void removeRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex);
            if (performSuperCall.getAndSet(true)) {
                ContainerList.super.removeRange(fromIndex, toIndex);
            }
        }

        @Override
        public D set(int index, D element) {
            D certificate = super.set(index, element);
            if (performSuperCall.getAndSet(true)) {
                ContainerList.super.set(index, element);
            }
            return certificate;
        }
    }
}
