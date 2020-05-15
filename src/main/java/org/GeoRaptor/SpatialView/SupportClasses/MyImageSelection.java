package org.GeoRaptor.SpatialView.SupportClasses;

import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.io.IOException;

public class MyImageSelection implements Transferable, ClipboardOwner {
    public static DataFlavor myFlavor = DataFlavor.imageFlavor;
    protected Image image;

    public MyImageSelection(Image img) {
        image = img;
    }

    public synchronized DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { myFlavor };
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return myFlavor.equals(flavor);
    }

    public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, 
                                                                         IOException {
        if (!myFlavor.equals(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return image;
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        System.out.println("Error [MyImageSelection.lostOwnership]: Lost ownership of system clipboard");
    }
}
