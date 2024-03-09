/**
 * @class SpatialQueryReview.java
 * *history Simon Greener, December 2nd 2010 Original Coding
 */

package org.GeoRaptor.SpatialView.layers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.GeoRaptor.Constants;
import org.GeoRaptor.MainSettings;
import org.GeoRaptor.SpatialView.SpatialView;
import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.SDO_GEOMETRY;
import org.GeoRaptor.tools.Strings;
import org.GeoRaptor.tools.Tools;
import org.GeoRaptor.util.logging.Logger;

import oracle.spatial.geometry.JGeometry;


/**
 *
 * @author Simon
 */
public class SpatialQueryReview extends JDialog {

	private static final long serialVersionUID = 6239461401678158741L;

	@SuppressWarnings("unused")
	private static final Logger LOGGER = org.GeoRaptor.util.logging.Logging.getLogger("org.GeoRaptor.SpatialView.layers.SpatialQueryReview");

    /**
     * Properties File Manager
     **/
    private static final String propertiesFile = "org.GeoRaptor.SpatialView.SpatialQueryReview";
    protected PropertiesManager propertyManager;

    /**
     * Reference to data structure with properties
     */
    protected iLayer layer;

    protected JGeometry geometry = null;

    protected SpatialView spatialView = null;

    protected Constants.SDO_OPERATORS sdoOperator;

    private String originalSdoGeometryString;

    protected String filterGeometry;
    
    protected boolean CANCELLED;

    private ActionListener createCloseListener;

    /** Creates new form SpatialQueryReview */
    public SpatialQueryReview(JFrame      _parent, 
    		                  boolean     _modal,
    		                  iLayer      _sLayer, 
    		                  JGeometry   _geometry,
    		                  SpatialView _spatialView) {
        super(_parent, _modal);
        try {
            this.propertyManager = new PropertiesManager(propertiesFile);
       		this.geometry        = _geometry;
       		this.spatialView     = _spatialView;
       		this.layer           = _sLayer; 

            initComponents();
            
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            this.setResizable(false);
            this.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        close(true);
                    }
                });
            
            createCloseListener = new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        close(e.getSource() == btnClose);
                    }
                };
                
            this.tfBufferDistance.setInputVerifier(new InputVerifier() {
                    public boolean verify(JComponent comp) {
                        boolean returnValue = true;
                        JTextField textField = (JTextField)comp;
                        try {
                            // This will throw an exception if the value is not an integer
                            Double.parseDouble(textField.getText());
                            modifySQL();
                        } catch (NumberFormatException e) {
                            JOptionPane.showMessageDialog(null, e.getMessage(),
                                                          MainSettings.EXTENSION_NAME,
                                                          JOptionPane.ERROR_MESSAGE);
                            returnValue = false;
                        }
                        return returnValue;
                    }
                });
            
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }

    private void createLayer() 
    {
        SVGraphicLayer sGraphicLayer = null;
        SVQueryLayer sQueryLayer = null;
        iLayer gqLayer = null;
        try {
            if (targetGraphic()) {
                sGraphicLayer = new SVGraphicLayer(this.layer);
                gqLayer = sGraphicLayer;
            } else {
                sQueryLayer = new SVQueryLayer(this.layer);
                gqLayer = sQueryLayer;
            }
            gqLayer.setVisibleName(this.sdoOperator.toString()
                         + " - " + this.layer.getVisibleName());
            gqLayer.setSQL(null);
            gqLayer.setDraw(true);
            gqLayer.setMBRRecalculation(true);
            gqLayer.setGeometry(this.geometry);
            gqLayer.setGeometryType(this.layer.getGeometryType());
            gqLayer.setBufferDistance(getBufferDistance());
            gqLayer.setBuffered(getBufferDistance() != 0.0);
            gqLayer.setRelationshipMask(getRelationshipMask(this.layer.hasIndex()));
            gqLayer.setSdoOperator(getSdoOperator());
            gqLayer.setPrecision(getPrecision());

            boolean success = false;

            // Add to view and ignore return
            if (gqLayer instanceof SVQueryLayer) {
                success = spatialView.addLayer(
                        /*_layer*/sQueryLayer,
                        /*_isDrawn*/ true,
                        /*_isActive*/ true,
                        /*_zoom*/ false
                );
                if (success) {
                    spatialView.getSVPanel().redraw();
                }
            } else if (gqLayer instanceof SVGraphicLayer) {
                try {
                    // Load data into cache using initial SQL
                    sGraphicLayer.setCache();

                    // Now that they cache is filled, compute its extent
                    sGraphicLayer.setLayerMBR();

                    // Display cache.                          
                    //success = spatialView.getSVPanel().addLayerToView(sGraphicLayer,false /*zoom*/);
                    success = spatialView.addLayer(
                            /*_layer*/    sGraphicLayer,
                            /*_isDrawn*/  true,
                            /*_isActive*/ true,
                            /*_zoom*/     false
                    );

                    if (success) {
                        // show attrib and geometry data in bottom tabbed pane
                        spatialView.getSVPanel().getAttDataView().showData(sGraphicLayer.getCache());
                        spatialView.getSVPanel().redraw();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgPrecision = new javax.swing.ButtonGroup();
        rgCreateLayer = new javax.swing.ButtonGroup();
        pnlReview = new javax.swing.JPanel();
        tpReview = new javax.swing.JTabbedPane();
        pnlGeometry = new javax.swing.JPanel();
        scrollTaGeometry = new javax.swing.JScrollPane();
        taGeometry = new javax.swing.JTextArea();
        lblBufferDistance = new javax.swing.JLabel();
        tfBufferDistance = new javax.swing.JTextField();
        cbEditGeometry = new javax.swing.JCheckBox();
        rbPrecisionLayer = new javax.swing.JRadioButton();
        lblPrecision = new javax.swing.JLabel();
        rbPrecisionView = new javax.swing.JRadioButton();
        rbPrecisionNone = new javax.swing.JRadioButton();
        btnCopyClipboard = new javax.swing.JButton();
        lblCreateLayer = new javax.swing.JLabel();
        rbGeometry = new javax.swing.JRadioButton();
        rbGraphic = new javax.swing.JRadioButton();
        pnlRelate = new javax.swing.JPanel();
        cbAnyInteract = new javax.swing.JCheckBox();
        cbContains = new javax.swing.JCheckBox();
        cbCoveredBy = new javax.swing.JCheckBox();
        cbCovers = new javax.swing.JCheckBox();
        cbEqual = new javax.swing.JCheckBox();
        cbInside = new javax.swing.JCheckBox();
        cbOn = new javax.swing.JCheckBox();
        cbOverlapBdyDisjoint = new javax.swing.JCheckBox();
        cbOverlapBdyIntersect = new javax.swing.JCheckBox();
        cbOverlaps = new javax.swing.JCheckBox();
        cbTouch = new javax.swing.JCheckBox();
        btnCreate = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        pnlReview.setBorder(javax.swing.BorderFactory.createTitledBorder("Spatial Query Review"));
        pnlReview.setMaximumSize(new java.awt.Dimension(681, 339));
        pnlReview.setMinimumSize(new java.awt.Dimension(681, 339));

        pnlGeometry.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        pnlGeometry.setMaximumSize(new java.awt.Dimension(664, 260));
        pnlGeometry.setMinimumSize(new java.awt.Dimension(664, 260));
        pnlGeometry.setPreferredSize(new java.awt.Dimension(664, 260));

        scrollTaGeometry.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollTaGeometry.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollTaGeometry.setPreferredSize(new java.awt.Dimension(556, 119));

        taGeometry.setColumns(1);
        taGeometry.setFont(new java.awt.Font("Courier New", 1, 12)); // NOI18N
        taGeometry.setRows(30);
        taGeometry.setTabSize(4);
        scrollTaGeometry.setViewportView(taGeometry);

        lblBufferDistance.setLabelFor(tfBufferDistance);
        lblBufferDistance.setText("Buffer Distance:");

        tfBufferDistance.setText("0");
        tfBufferDistance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfBufferDistanceActionPerformed(evt);
            }
        });

        cbEditGeometry.setText("Edit Geometry");
        cbEditGeometry.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        bgPrecision.add(rbPrecisionLayer);
        rbPrecisionLayer.setSelected(true);
        rbPrecisionLayer.setText("Layer");
        rbPrecisionLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbPrecisionLayerActionPerformed(evt);
            }
        });

        lblPrecision.setLabelFor(rbPrecisionLayer);
        lblPrecision.setText("Precision:");

        bgPrecision.add(rbPrecisionView);
        rbPrecisionView.setText("View");
        rbPrecisionView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbPrecisionViewActionPerformed(evt);
            }
        });

        bgPrecision.add(rbPrecisionNone);
        rbPrecisionNone.setText("None");
        rbPrecisionNone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbPrecisionNoneActionPerformed(evt);
            }
        });

        btnCopyClipboard.setText("Copy to Clipboard");
        btnCopyClipboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCopyClipboardActionPerformed(evt);
            }
        });

        lblCreateLayer.setText("Create Layer:");

        rgCreateLayer.add(rbGeometry);
        rbGeometry.setSelected(true);
        rbGeometry.setText("SQL");
        rbGeometry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbGeometryActionPerformed(evt);
            }
        });

        rgCreateLayer.add(rbGraphic);
        rbGraphic.setText("Graphic");
        rbGraphic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbGraphicActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlGeometryLayout = new javax.swing.GroupLayout(pnlGeometry);
        pnlGeometry.setLayout(pnlGeometryLayout);
        pnlGeometryLayout.setHorizontalGroup(
            pnlGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlGeometryLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollTaGeometry, javax.swing.GroupLayout.DEFAULT_SIZE, 642, Short.MAX_VALUE)
                    .addGroup(pnlGeometryLayout.createSequentialGroup()
                        .addGroup(pnlGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlGeometryLayout.createSequentialGroup()
                                .addComponent(lblBufferDistance)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tfBufferDistance, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(btnCopyClipboard))
                        .addGap(35, 35, 35)
                        .addGroup(pnlGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblPrecision, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblCreateLayer, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnlGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rbPrecisionLayer)
                            .addComponent(rbGraphic))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnlGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rbPrecisionView)
                            .addComponent(rbGeometry))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rbPrecisionNone)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 197, Short.MAX_VALUE)
                        .addComponent(cbEditGeometry)))
                .addContainerGap())
        );
        pnlGeometryLayout.setVerticalGroup(
            pnlGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlGeometryLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollTaGeometry, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbEditGeometry)
                    .addComponent(lblBufferDistance)
                    .addComponent(tfBufferDistance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rbPrecisionLayer)
                    .addComponent(rbPrecisionView)
                    .addComponent(rbPrecisionNone)
                    .addComponent(lblPrecision))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlGeometryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCopyClipboard)
                    .addComponent(lblCreateLayer)
                    .addComponent(rbGraphic)
                    .addComponent(rbGeometry))
                .addGap(13, 13, 13))
        );

        tpReview.addTab("Query Geometry", pnlGeometry);

        cbAnyInteract.setText("ANYINTERACT");
        cbAnyInteract.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        cbAnyInteract.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbAnyInteractActionPerformed(evt);
            }
        });

        cbContains.setText("CONTAINS");
        cbContains.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        cbContains.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbContainsActionPerformed(evt);
            }
        });

        cbCoveredBy.setText("COVEREDBY");
        cbCoveredBy.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        cbCoveredBy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbCoveredByActionPerformed(evt);
            }
        });

        cbCovers.setText("COVERS");
        cbCovers.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        cbCovers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbCoversActionPerformed(evt);
            }
        });

        cbEqual.setText("EQUAL");
        cbEqual.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        cbEqual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbEqualActionPerformed(evt);
            }
        });

        cbInside.setText("INSIDE");
        cbInside.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        cbInside.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbInsideActionPerformed(evt);
            }
        });

        cbOn.setText("ON");
        cbOn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbOnActionPerformed(evt);
            }
        });

        cbOverlapBdyDisjoint.setText("OVERLAPBDYDISJOINT");
        cbOverlapBdyDisjoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbOverlapBdyDisjointActionPerformed(evt);
            }
        });

        cbOverlapBdyIntersect.setText("OVERLAPBDYINTERSECT");
        cbOverlapBdyIntersect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbOverlapBdyIntersectActionPerformed(evt);
            }
        });

        cbOverlaps.setText("OVERLAPS");
        cbOverlaps.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbOverlapsActionPerformed(evt);
            }
        });

        cbTouch.setText("TOUCH");
        cbTouch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbTouchActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlRelateLayout = new javax.swing.GroupLayout(pnlRelate);
        pnlRelate.setLayout(pnlRelateLayout);
        pnlRelateLayout.setHorizontalGroup(
            pnlRelateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlRelateLayout.createSequentialGroup()
                .addGap(129, 129, 129)
                .addGroup(pnlRelateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbEqual, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(cbInside, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(cbAnyInteract, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(cbContains, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(cbCoveredBy, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(cbCovers, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(89, 89, 89)
                .addGroup(pnlRelateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbOn)
                    .addComponent(cbOverlapBdyDisjoint)
                    .addComponent(cbOverlapBdyIntersect)
                    .addComponent(cbOverlaps)
                    .addComponent(cbTouch))
                .addContainerGap(206, Short.MAX_VALUE))
        );
        pnlRelateLayout.setVerticalGroup(
            pnlRelateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlRelateLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(pnlRelateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbAnyInteract)
                    .addComponent(cbOn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlRelateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbOverlapBdyDisjoint)
                    .addComponent(cbContains))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlRelateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbCoveredBy)
                    .addComponent(cbOverlapBdyIntersect))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlRelateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbCovers)
                    .addComponent(cbOverlaps))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlRelateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbEqual)
                    .addComponent(cbTouch))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cbInside)
                .addContainerGap(75, Short.MAX_VALUE))
        );

        tpReview.addTab("SDO_RELATE", pnlRelate);

        btnCreate.setMnemonic('E');
        btnCreate.setText("Create Layer");
        btnCreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateActionPerformed(evt);
            }
        });

        btnClose.setMnemonic('C');
        btnClose.setText("Close");

        javax.swing.GroupLayout pnlReviewLayout = new javax.swing.GroupLayout(pnlReview);
        pnlReview.setLayout(pnlReviewLayout);
        pnlReviewLayout.setHorizontalGroup(
            pnlReviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tpReview)
            .addGroup(pnlReviewLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnCreate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnClose)
                .addContainerGap())
        );
        pnlReviewLayout.setVerticalGroup(
            pnlReviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlReviewLayout.createSequentialGroup()
                .addComponent(tpReview, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlReviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnCreate)
                    .addComponent(btnClose)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlReview, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnlReview, javax.swing.GroupLayout.PREFERRED_SIZE, 337, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tfBufferDistanceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfBufferDistanceActionPerformed
        modifySQL();
    }//GEN-LAST:event_tfBufferDistanceActionPerformed

    private void cbAnyInteractActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbAnyInteractActionPerformed
        modifySQL();
    }//GEN-LAST:event_cbAnyInteractActionPerformed

    private void cbContainsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbContainsActionPerformed
        modifySQL();
    }//GEN-LAST:event_cbContainsActionPerformed

    private void cbCoveredByActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbCoveredByActionPerformed
        modifySQL();
    }//GEN-LAST:event_cbCoveredByActionPerformed

    private void cbCoversActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbCoversActionPerformed
        modifySQL();
    }//GEN-LAST:event_cbCoversActionPerformed

    private void cbEqualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbEqualActionPerformed
        modifySQL();
    }//GEN-LAST:event_cbEqualActionPerformed

    private void cbInsideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbInsideActionPerformed
        modifySQL();
    }//GEN-LAST:event_cbInsideActionPerformed

    private void cbOnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbOnActionPerformed
        modifySQL();
    }//GEN-LAST:event_cbOnActionPerformed

    private void cbOverlapBdyDisjointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbOverlapBdyDisjointActionPerformed
        modifySQL();
    }//GEN-LAST:event_cbOverlapBdyDisjointActionPerformed

    private void cbOverlapBdyIntersectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbOverlapBdyIntersectActionPerformed
        modifySQL();
    }//GEN-LAST:event_cbOverlapBdyIntersectActionPerformed

    private void cbOverlapsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbOverlapsActionPerformed
        modifySQL();
    }//GEN-LAST:event_cbOverlapsActionPerformed

    private void cbTouchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbTouchActionPerformed
        modifySQL();
    }//GEN-LAST:event_cbTouchActionPerformed

    private void rbPrecisionLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbPrecisionLayerActionPerformed
        modifySQL();
    }//GEN-LAST:event_rbPrecisionLayerActionPerformed

    private void rbPrecisionViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbPrecisionViewActionPerformed
        modifySQL();
    }//GEN-LAST:event_rbPrecisionViewActionPerformed

    private void rbPrecisionNoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbPrecisionNoneActionPerformed
        modifySQL();
    }//GEN-LAST:event_rbPrecisionNoneActionPerformed

    private void btnCopyClipboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCopyClipboardActionPerformed
        Tools.doClipboardCopy(taGeometry.getText());
    }//GEN-LAST:event_btnCopyClipboardActionPerformed

    private void rbGraphicActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbGraphicActionPerformed
        modifySQL();
    }//GEN-LAST:event_rbGraphicActionPerformed

    private void rbGeometryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbGeometryActionPerformed
        modifySQL();
    }//GEN-LAST:event_rbGeometryActionPerformed

    private void btnCreateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateActionPerformed
        createLayer(); // Create layer using current settings.
    }//GEN-LAST:event_btnCreateActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgPrecision;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnCopyClipboard;
    private javax.swing.JButton btnCreate;
    private javax.swing.JCheckBox cbAnyInteract;
    private javax.swing.JCheckBox cbContains;
    private javax.swing.JCheckBox cbCoveredBy;
    private javax.swing.JCheckBox cbCovers;
    private javax.swing.JCheckBox cbEditGeometry;
    private javax.swing.JCheckBox cbEqual;
    private javax.swing.JCheckBox cbInside;
    private javax.swing.JCheckBox cbOn;
    private javax.swing.JCheckBox cbOverlapBdyDisjoint;
    private javax.swing.JCheckBox cbOverlapBdyIntersect;
    private javax.swing.JCheckBox cbOverlaps;
    private javax.swing.JCheckBox cbTouch;
    private javax.swing.JLabel lblBufferDistance;
    private javax.swing.JLabel lblCreateLayer;
    private javax.swing.JLabel lblPrecision;
    private javax.swing.JPanel pnlGeometry;
    private javax.swing.JPanel pnlRelate;
    private javax.swing.JPanel pnlReview;
    private javax.swing.JRadioButton rbGeometry;
    private javax.swing.JRadioButton rbGraphic;
    private javax.swing.JRadioButton rbPrecisionLayer;
    private javax.swing.JRadioButton rbPrecisionNone;
    private javax.swing.JRadioButton rbPrecisionView;
    private javax.swing.ButtonGroup rgCreateLayer;
    private javax.swing.JScrollPane scrollTaGeometry;
    private javax.swing.JTextArea taGeometry;
    private javax.swing.JTextField tfBufferDistance;
    private javax.swing.JTabbedPane tpReview;
    // End of variables declaration//GEN-END:variables

    public void initDialog(final iLayer                  _sLayer,
                           final Constants.SDO_OPERATORS _operator,
                           final String                  _filterGeometry)
    {
        this.layer                     = _sLayer;
        this.sdoOperator               = _operator;
        this.filterGeometry            = _filterGeometry;
        this.originalSdoGeometryString = _filterGeometry;
        // btnCreate.addActionListener(createCloseListener);
        btnClose.addActionListener(createCloseListener);

        pnlReview.setBorder(javax.swing.BorderFactory.createTitledBorder(this.propertyManager.getMsg("pnlReview")));
        lblBufferDistance.setText(this.propertyManager.getMsg("lblBufferDistance"));
        cbEditGeometry.setText(this.propertyManager.getMsg("cbEditGeometry"));
        lblPrecision.setText(this.propertyManager.getMsg("lblPrecision"));
        rbPrecisionLayer.setText(this.propertyManager.getMsg("rbPrecisionLayer") + " (" + layer.getPrecision(false) + ")");
        rbPrecisionView.setText(this.propertyManager.getMsg("rbPrecisionView")   + " (" + layer.getSpatialView().getPrecision(false) + ")");
        rbPrecisionNone.setText(this.propertyManager.getMsg("rbPrecisionNone"));
        btnCreate.setText(this.propertyManager.getMsg("btnCreateLayer"));
        btnClose.setText(this.propertyManager.getMsg("btnClose"));
        tpReview.setTitleAt(0, this.propertyManager.getMsg("tpReviewGeometry"));
        tpReview.setTitleAt(1, this.propertyManager.getMsg("tpReviewRelate"));
        btnCopyClipboard.setText(this.propertyManager.getMsg("btnCopyClipboard"));
        lblCreateLayer.setText(this.propertyManager.getMsg("lblCreateLayer"));
        rbGeometry.setSelected(true);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                rbPrecisionLayer.setSelected(true);
                cbEditGeometry.setSelected(false);
                pnlRelate.setEnabled(_operator==Constants.SDO_OPERATORS.RELATE);
                cbAnyInteract.setSelected(true);
                tpReview.setEnabledAt(tpReview.indexOfComponent(pnlRelate),
                                      _operator==Constants.SDO_OPERATORS.RELATE);
                taGeometry.setText(getGeometry()); // getQueryGeometry());
                scrollTaGeometry.setWheelScrollingEnabled(true);
                scrollTaGeometry.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                pack();
                // Set horizontalScrollBar to far left
                JScrollBar horizontalScrollBar = scrollTaGeometry.getHorizontalScrollBar();
                horizontalScrollBar.setValue(horizontalScrollBar.getMinimum());
                scrollTaGeometry.setViewportView(taGeometry);
                repaint();
            }
        });
    }
    
    public Constants.SDO_OPERATORS getSdoOperator() {
        return this.sdoOperator;
    }

    public String getRelationshipMask(boolean _hasIndex) {
        if (this.sdoOperator != Constants.SDO_OPERATORS.RELATE && _hasIndex)
            return "";
        String mask = "";
        if (cbAnyInteract.isSelected())         { mask += "+" + "ANYINTERACT";         }
        if (cbContains.isSelected())            { mask += "+" + "CONTAINS";            }
        if (cbCoveredBy.isSelected())           { mask += "+" + "COVEREDBY";           }
        if (cbCovers.isSelected())              { mask += "+" + "COVERS";              }
        if (cbEqual.isSelected())               { mask += "+" + "EQUAL";               }
        if (cbInside.isSelected())              { mask += "+" + "INSIDE";              }
        if (cbOn.isSelected())                  { mask += "+" + "ON";                  }
        if (cbOverlapBdyDisjoint.isSelected())  { mask += "+" + "OVERLAPBDYDISJOINT";  }
        if (cbOverlapBdyIntersect.isSelected()) { mask += "+" + "OVERLAPBDYINTERSECT"; }
        if (cbOverlaps.isSelected())            { mask += "+" + "OVERLAPS";            }
        if (cbTouch.isSelected())               { mask += "+" + "TOUCH";               }
        return Strings.isEmpty(mask) ? "ANYINTERACT" : Strings.trimAll(mask, '+');
    }

    private void close(boolean canceled) {
        this.CANCELLED = canceled;
        dispose();
    }

    public int getPrecision() {
        if (this.rbPrecisionLayer.isSelected()) {
            return this.layer.getPrecision(false);
        } else if (this.rbPrecisionView.isSelected()) {
            return this.layer.getSpatialView().getPrecision(false);
        } else {
            return Constants.MAX_PRECISION;
        }
    }

    public double getBufferDistance() {
        double bufDistance = 0.0;
        if ( !Strings.isEmpty(tfBufferDistance.getText() ) ) {
            try {
                bufDistance = Double.valueOf(tfBufferDistance.getText()).doubleValue(); 
            } catch (Exception e) {
                bufDistance = 0.0;
            }
        }
        return bufDistance;
    }
    
    public boolean isCanceled() {
        return this.CANCELLED;
    }

    public boolean targetGraphic() {
        return this.rbGraphic.isSelected();
    }

    public String getSQL() {
        return taGeometry.getText();
    }

    public String getGeometry() 
    {
        // Create appropriate filter geometry of desired ordinate precision
        //
        double tolerance = this.layer.getTolerance();
        DecimalFormat dFormatter = null;
        int precision = Constants.MAX_PRECISION;
        String geometryString = originalSdoGeometryString;
        
        if (rbPrecisionNone.isSelected()) {
            geometryString = originalSdoGeometryString;
            dFormatter = Tools.getDecimalFormatter(precision);
        } else {
            if (this.rbPrecisionLayer.isSelected()) {
                precision = this.layer.getPrecision(false);
            } else if (this.rbPrecisionView.isSelected()) {
                precision = this.layer.getSpatialView().getPrecision(false);
            }
            dFormatter = Tools.getDecimalFormatter(precision);
            geometryString =
                SDO_GEOMETRY.applyPrecision(geometryString,
                                            dFormatter,
                                            8) /* 2D * 4 coords = 8 ordinates */;
        }
        // Do we buffer it?
        //
        if (this.getBufferDistance()!= 0) 
        {
            String lengthUnits = Tools.getViewUnits(this.layer.getSpatialView(),
                                                    Constants.MEASURE.LENGTH);
            String bufferParams = ((layer.getSRID().equals(Constants.NULL) || Strings.isEmpty(lengthUnits)) ? "\n)" :
                                   ",\n    'unit=" + lengthUnits + "'\n)");
            /**
             * SDO_GEOM.SDO_BUFFER(geom IN SDO_GEOMETRY,
             *                     dist IN NUMBER,
             *                     tol IN NUMBER
             *                     [, params IN VARCHAR2]
             *                    ) RETURN SDO_GEOMETRY;
            **/
            geometryString = "MDSYS.SDO_GEOM.SDO_BUFFER(\n    " + 
                                  geometryString + ",\n    " + 
                                  tfBufferDistance.getText() + ",\n    " +
                                  dFormatter.format(tolerance) +
                                  bufferParams;
        }
        
        return geometryString;
    }

    private void modifySQL() {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            taGeometry.setText(getGeometry()); // getQuerySQL());
            JScrollBar horizontalScrollBar = scrollTaGeometry.getHorizontalScrollBar();
            horizontalScrollBar.setValue(horizontalScrollBar.getMinimum());
        }
      });

    }

}
