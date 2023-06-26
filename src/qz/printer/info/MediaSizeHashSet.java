package qz.printer.info;

import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import java.util.*;

/**
 * A sortable <code>HashMap</code> for storing printer page sizes for both imperial (inches) and metric (millimeters)
 */
public class MediaSizeHashSet extends HashSet<MediaSizeHashSet.UnitPair> {
    public class UnitPair implements Comparable {
        private DimensionFloat in;
        private DimensionFloat mm;

        public UnitPair(MediaSize mediaSize) {
            in = new DimensionFloat(mediaSize.getX(MediaPrintableArea.INCH), mediaSize.getY(MediaPrintableArea.INCH));
            mm = new DimensionFloat(mediaSize.getX(MediaPrintableArea.MM), mediaSize.getY(MediaPrintableArea.MM));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {return true;}
            if (o == null || getClass() != o.getClass()) {return false;}
            UnitPair that = (UnitPair)o;
            return in.width == that.in.width && in.height == that.in.height;
        }

        @Override
        public int compareTo(Object o) {
            if (this == o) {return 0;}
            if (o == null || getClass() != o.getClass()) {return -1;}
            return Comparator.comparing((UnitPair p)-> p.in.width)
                    .thenComparing((UnitPair p)-> p.in.height)
                    .compare(this, (UnitPair)o);
        }

        /**
         * Get size as <code>DimensionFloat</code> in inches
         */
        public DimensionFloat getIn() {
            return in;
        }

        /**
         * Get size as <code>DimensionFloat</code> in millimeters
         */
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
            return add(new UnitPair(mediaSize));
        }
        return false;
    }
}
