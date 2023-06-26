package qz.printer.info;

import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import java.util.*;

/**
 * A sortable <code>HashMap</code> for storing printer page sizes for both imperial (inches) and metric (millimeters)
 */
public class MediaSizeHashSet extends HashSet<MediaSizeHashSet.Pair> {
    public class Pair implements Comparable {
        private DimensionFloat inches;
        private DimensionFloat mm;

        public Pair(MediaSize mediaSize) {
            inches = new DimensionFloat(mediaSize.getX(MediaPrintableArea.INCH), mediaSize.getY(MediaPrintableArea.INCH));
            mm = new DimensionFloat(mediaSize.getX(MediaPrintableArea.MM), mediaSize.getY(MediaPrintableArea.MM));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {return true;}
            if (o == null || getClass() != o.getClass()) {return false;}
            Pair that = (Pair)o;
            return inches.width == that.inches.width && inches.height == that.inches.height;
        }

        @Override
        public int compareTo(Object o) {
            if (this == o) {return 0;}
            if (o == null || getClass() != o.getClass()) {return -1;}
            return Comparator.comparing((Pair p)-> p.inches.width)
                    .thenComparing((Pair p)-> p.inches.height)
                    .compare(this, (Pair)o);
        }

        public DimensionFloat getInches() {
            return inches;
        }

        public DimensionFloat getMm() {
            return mm;
        }
    }

    /**
     * Simple dimension container using floats
     */
    public class DimensionFloat {
        private float width;
        private float height;

        public DimensionFloat(float width, float height) {
            this.width = width;
            this.height = height;
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }
    }

    /**
     * Adds an imperial (in) and metric (mm) entry of the specified <code>MediaSizeName</code>
     */
    public boolean add(MediaSizeName mediaSizeName) {
        MediaSize mediaSize = MediaSize.getMediaSizeForName(mediaSizeName);
        if(mediaSize != null) {
            return add(new Pair(mediaSize));
        }
        return false;
    }
}
