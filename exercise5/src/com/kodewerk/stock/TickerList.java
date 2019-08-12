package com.kodewerk.stock;

import java.util.Iterator;
import java.io.File;
import java.io.FilenameFilter;

/**
 * TickerList
 *
 * @author kirk
 * @version 1.0
 * @since 5:11:22 PM
 */
public class TickerList {

    private String[] fileNames;

    public TickerList() {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept( File dir, String filename) {
                return filename.endsWith( ".xml");
            }
        };
        File dirlist = new File("./data");
        this.fileNames = dirlist.list( filter);
    }

    public Iterator iterator() {
        return new Iterator() {

            private int index = 0;

            public boolean hasNext() {
                return index < fileNames.length;
            }

            public Object next() {
                Object name = fileNames[ index].substring( 0, fileNames[ index].length() - 4);
                index++;
                return name;
            }

            public void remove() {}
        };
    }
}
