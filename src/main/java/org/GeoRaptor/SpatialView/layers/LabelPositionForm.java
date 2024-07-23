/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * LabelPositioningForm.java
 *
 * Created on 20/01/2011, 1:37:22 PM
 */

package org.GeoRaptor.SpatialView.layers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;

import java.util.Enumeration;

import javax.swing.JRadioButton;

import org.GeoRaptor.Constants;
import org.GeoRaptor.tools.PropertiesManager;

/**
 *
 * @author Simon
 */
public class LabelPositionForm extends javax.swing.JDialog {
    
	private static final long serialVersionUID = 4798679466381837271L;

	/** A return status code - returned if Cancel button has been pressed */
    public static final int RET_CANCEL = 0;
    
    /** A return status code - returned if OK button has been pressed */
    public static final int RET_OK = 1;

    private PropertiesManager parentProps = null;
    
    /** Creates new form LabelPositionForm */
    public LabelPositionForm(java.awt.Frame parent, boolean modal, PropertiesManager _props) {
        super(parent, modal);
        parentProps = _props;
        initComponents();
        lblOffset.setText(getOffsetText());
        lblOffset.setToolTipText(this.parentProps.getMsg("TT_OFFSET"));
        pnl9Positions.setOpaque(true);
    }

    private String getOffsetText() {
        return String.format("%s (%s)",this.parentProps.getMsg("LABEL_OFFSET"),String.valueOf(sldrOffset.getValue()));
    }
    
    public boolean wasCancelled() {
        return this.returnStatus == RET_CANCEL;
    }

    public void windowOpened(WindowEvent e) {
        drawGrid(pnl9Positions.getGraphics());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          

    private void initComponents() {

        bgPosition = new javax.swing.ButtonGroup();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        pnl9Positions = new javax.swing.JPanel();
        rbTR = new javax.swing.JRadioButton();
        rbTC = new javax.swing.JRadioButton();
        rbTL = new javax.swing.JRadioButton();
        rbCL = new javax.swing.JRadioButton();
        rbCC = new javax.swing.JRadioButton();
        rbCR = new javax.swing.JRadioButton();
        rbBL = new javax.swing.JRadioButton();
        rbBC = new javax.swing.JRadioButton();
        rbBR = new javax.swing.JRadioButton();
        pnlOffset = new javax.swing.JPanel();
        lblOffset = new javax.swing.JLabel();
        sldrOffset = new javax.swing.JSlider();

        setTitle("Label Position");
        setMinimumSize(null);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        pnl9Positions.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        pnl9Positions.setMaximumSize(new java.awt.Dimension(258, 204));
        pnl9Positions.setMinimumSize(new java.awt.Dimension(258, 204));

        bgPosition.add(rbTR);
        rbTR.setText("TR");
        rbTR.setBorder(null);
        rbTR.setContentAreaFilled(false);

        bgPosition.add(rbTC);
        rbTC.setText("TC");
        rbTC.setBorder(null);
        rbTC.setContentAreaFilled(false);

        bgPosition.add(rbTL);
        rbTL.setText("TL");
        rbTL.setBorder(null);
        rbTL.setContentAreaFilled(false);

        bgPosition.add(rbCL);
        rbCL.setText("CL");
        rbCL.setBorder(null);
        rbCL.setContentAreaFilled(false);

        bgPosition.add(rbCC);
        rbCC.setText("CC");
        rbCC.setBorder(null);
        rbCC.setContentAreaFilled(false);

        bgPosition.add(rbCR);
        rbCR.setText("CR");
        rbCR.setBorder(null);
        rbCR.setContentAreaFilled(false);

        bgPosition.add(rbBL);
        rbBL.setText("BL");
        rbBL.setBorder(null);
        rbBL.setContentAreaFilled(false);

        bgPosition.add(rbBC);
        rbBC.setText("BC");
        rbBC.setBorder(null);
        rbBC.setContentAreaFilled(false);

        bgPosition.add(rbBR);
        rbBR.setText("BR");
        rbBR.setBorder(null);
        rbBR.setContentAreaFilled(false);

        javax.swing.GroupLayout pnl9PositionsLayout = new javax.swing.GroupLayout(pnl9Positions);
        pnl9Positions.setLayout(pnl9PositionsLayout);
        pnl9PositionsLayout.setHorizontalGroup(
            pnl9PositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnl9PositionsLayout.createSequentialGroup()
                .addContainerGap(53, Short.MAX_VALUE)
                .addGroup(pnl9PositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbTL)
                    .addComponent(rbCL)
                    .addComponent(rbBL))
                .addGap(18, 18, 18)
                .addGroup(pnl9PositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(rbTC)
                    .addComponent(rbCC)
                    .addComponent(rbBC))
                .addGap(18, 18, 18)
                .addGroup(pnl9PositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbTR, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(rbCR, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(rbBR, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(50, 50, 50))
        );
        pnl9PositionsLayout.setVerticalGroup(
            pnl9PositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl9PositionsLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(pnl9PositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(rbTL)
                    .addComponent(rbTC)
                    .addComponent(rbTR))
                .addGap(29, 29, 29)
                .addGroup(pnl9PositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(rbCL)
                    .addComponent(rbCC)
                    .addComponent(rbCR))
                .addGap(33, 33, 33)
                .addGroup(pnl9PositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(rbBL)
                    .addComponent(rbBC)
                    .addComponent(rbBR))
                .addContainerGap(39, Short.MAX_VALUE))
        );

        pnlOffset.setMaximumSize(new java.awt.Dimension(278, 68));
        pnlOffset.setMinimumSize(new java.awt.Dimension(278, 68));

        lblOffset.setText("Offset (10)");

        sldrOffset.setMajorTickSpacing(2);
        sldrOffset.setMaximum(10);
        sldrOffset.setMinorTickSpacing(1);
        sldrOffset.setPaintLabels(true);
        sldrOffset.setPaintTicks(true);
        sldrOffset.setSnapToTicks(true);
        sldrOffset.setValue(0);
        sldrOffset.setMaximumSize(new java.awt.Dimension(200, 45));
        sldrOffset.setMinimumSize(new java.awt.Dimension(200, 45));
        sldrOffset.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldrOffsetStateChanged(evt);
            }
        });

        javax.swing.GroupLayout pnlOffsetLayout = new javax.swing.GroupLayout(pnlOffset);
        pnlOffset.setLayout(pnlOffsetLayout);
        pnlOffsetLayout.setHorizontalGroup(
            pnlOffsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlOffsetLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblOffset)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sldrOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        pnlOffsetLayout.setVerticalGroup(
            pnlOffsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlOffsetLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(pnlOffsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(lblOffset)
                    .addComponent(sldrOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(pnl9Positions, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(pnlOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(okButton)
                        .addGap(134, 134, 134)
                        .addComponent(cancelButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnl9Positions, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pnlOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>                        

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        doClose(RET_OK);
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        doClose(RET_CANCEL);
    }//GEN-LAST:event_cancelButtonActionPerformed

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        doClose(RET_OK);
    }//GEN-LAST:event_closeDialog

    private void sldrOffsetStateChanged(javax.swing.event.ChangeEvent evt) {                                        
        lblOffset.setText(getOffsetText());
    }                                       

    private void doClose(int retStatus) {
        this.returnStatus = retStatus;
        setVisible(false);
        dispose();
    }

    public void setLabelOffset(int _offset) {
        this.sldrOffset.setValue(_offset);
    }

    public int getLabelOffset() {
        return this.sldrOffset.getValue();
    }
    
    public void setLabelPosition(Constants.TEXT_OFFSET_POSITION _lPosition) {
        String posnString = _lPosition.toString();
        for (Enumeration<?> e = this.bgPosition.getElements(); e.hasMoreElements(); ) {
            JRadioButton b = (JRadioButton)e.nextElement();
            if (b.getText().equalsIgnoreCase(posnString)) {
                b.setSelected(true);
            }
        }
    }
  
    public Constants.TEXT_OFFSET_POSITION getLabelPosition() {
        for (Enumeration<?> e = this.bgPosition.getElements(); e.hasMoreElements(); ) {
            JRadioButton b = (JRadioButton)e.nextElement();
            if (b.getModel() == this.bgPosition.getSelection()) {
                return Constants.TEXT_OFFSET_POSITION.valueOf(b.getText());
            }
        }
        return Constants.TEXT_OFFSET_POSITION.BL;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgPosition;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel lblOffset;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel pnl9Positions;
    private javax.swing.JPanel pnlOffset;
    private javax.swing.JRadioButton rbBC;
    private javax.swing.JRadioButton rbBL;
    private javax.swing.JRadioButton rbBR;
    private javax.swing.JRadioButton rbCC;
    private javax.swing.JRadioButton rbCL;
    private javax.swing.JRadioButton rbCR;
    private javax.swing.JRadioButton rbTC;
    private javax.swing.JRadioButton rbTL;
    private javax.swing.JRadioButton rbTR;
    private javax.swing.JSlider sldrOffset;
    // End of variables declaration//GEN-END:variables

    private int returnStatus = RET_OK;

    public void drawGrid(Graphics g) 
    {
          // fill with the color you want
          int wide = getWidth();
          int tall = getHeight();
          // go into Graphics2D for all the fine art, more options
          // optional, here I just get variable Stroke sizes
          Graphics2D g2 = (Graphics2D)g;
          g.setColor(Color.GRAY);
          int w = wide / 20;
          int h = tall / 20;
          g2.setColor(Color.BLACK);
          g2.setStroke(new BasicStroke(1));
          // Draw Grid lines
          for (int i = 1; i < 20; i++) {
              // Verticals
              g2.drawLine(i * w, 0, i * w, tall);
              // Horizontals
              g2.drawLine(0, i * h, wide, i * h);
          }
  }

}
