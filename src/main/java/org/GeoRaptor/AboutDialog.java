/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * AboutDialog.java
 *
 * Created on Nov 12, 2010, 11:45:02 AM
 */

package org.GeoRaptor;

import java.awt.Desktop;

import javax.swing.ImageIcon;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.Tools;
import org.GeoRaptor.util.logging.Logger;


/**
 *
 * @author oracle
 *
 * Use Resources class to replace propertiesFile
 */
public class AboutDialog extends javax.swing.JDialog {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = org.GeoRaptor.util.logging.Logging.getLogger("org.GeoRaptor.AboutDialog");

    private ClassLoader cl = this.getClass().getClassLoader();

    protected PropertiesManager propertyManager;
        
    private static final String propertiesFile = "org.GeoRaptor.Resources";
    
    private static AboutDialog INSTANCE;

	private ImageIcon GeoRaptorLogo;

    public static AboutDialog getInstance() {
        if (AboutDialog.INSTANCE == null) {
            AboutDialog.INSTANCE = new AboutDialog((java.awt.Frame)null, true);
        }
        return AboutDialog.INSTANCE;
    }

    /** Creates new form AboutDialog */
    public AboutDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);

        this.cl = this.getClass().getClassLoader();

        this.propertyManager = new PropertiesManager(AboutDialog.propertiesFile);

        initComponents();

        // Has to be after initComponents.
        //
        this.GeoRaptorLogo = new ImageIcon(this.cl.getResource("org/GeoRaptor/images/GeoRaptorLogo.jpg"));
        this.lblLogo.setIcon(this.GeoRaptorLogo);

        String version = MainSettings.EXTENSION_VERSION; 
        
        String dialog_title = Resources.getString("ABOUT_BOX_TITLE") + " - " + version;
        
        setTitle(dialog_title);
		
        this.taGeoRaptorAboutText.setContentType("text/html");
        String html_doc = this.propertyManager.getMsg("ABOUT_HTML", version, Tools.getVersion());
        
        this.taGeoRaptorAboutText.setText(html_doc);
        this.taGeoRaptorAboutText.setBackground(this.getBackground());
        
        this.taGeoRaptorAboutText.addHyperlinkListener(new HyperlinkListener() {
               @Override
               public void hyperlinkUpdate(HyperlinkEvent e) {
                   if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                       try {
                           Desktop.getDesktop().browse(e.getURL().toURI());
                       } catch (Exception e1) {
                           LOGGER.error("Error opening link " + e.getURL());
                       }
                   }
               }
           });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnClose = new javax.swing.JButton();
        lblLogo = new javax.swing.JLabel();
        scrollGeoRaptorAboutText = new javax.swing.JScrollPane();
        taGeoRaptorAboutText = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setForeground(java.awt.Color.white);
        setMinimumSize(new java.awt.Dimension(875, 600));
        
        btnClose.setText("Close");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        //lblLogo.setIcon(new javax.swing.ImageIcon(cl.getResource("org/GeoRaptor/images/GeoRaptorLogo.jpg")));

        taGeoRaptorAboutText.setEditable(false);
        taGeoRaptorAboutText.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        taGeoRaptorAboutText.setText("GeoRaptor is an open source project.");
        taGeoRaptorAboutText.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        scrollGeoRaptorAboutText.setViewportView(taGeoRaptorAboutText);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(scrollGeoRaptorAboutText, javax.swing.GroupLayout.DEFAULT_SIZE, 679, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblLogo))
                    .addComponent(btnClose))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblLogo)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(scrollGeoRaptorAboutText, javax.swing.GroupLayout.DEFAULT_SIZE, 346, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnClose)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        // TODO add your handling code here:
        this.setVisible(false);
    }//GEN-LAST:event_btnCloseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JLabel lblLogo;
    private javax.swing.JScrollPane scrollGeoRaptorAboutText;
    private javax.swing.JTextPane taGeoRaptorAboutText;
    // End of variables declaration//GEN-END:variables

}
