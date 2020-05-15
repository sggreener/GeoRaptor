package org.GeoRaptor.io.Export.ui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;

import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import javax.swing.event.ListSelectionListener;

import oracle.jdeveloper.layout.XYConstraints;
import oracle.jdeveloper.layout.XYLayout;

import org.GeoRaptor.Constants;


public class ExportPanel1 extends JPanel {

    /**
	 * 
	 */
	private static final long serialVersionUID = 5539490974687615916L;
	
	private FlowLayout layoutUI = new FlowLayout();
    private JPanel contentPanel = new JPanel();
    private XYLayout layoutContent = new XYLayout();

    private JLabel lblSchema = new JLabel();
    private JLabel lblObject = new JLabel();
    private JTextField tfSchema = new JTextField();
    private JTextField tfObject = new JTextField();
    private JLabel lblGeometry = new JLabel();
    protected JComboBox<String> cmbGeometries = new JComboBox<>();
    private JLabel lblExportType = new JLabel();
    private JPanel pnlExportType = new JPanel();
    private XYLayout layoutObjectProperties = new XYLayout();
    
    private JPanel pnlObjectProperties = new JPanel();
    private XYLayout layoutEXPORT_TYPE = new XYLayout();
    private JPanel pnlStepOne = new JPanel();
    private XYLayout xYLayout1 = new XYLayout();
    protected JCheckBox cbAttributes = new JCheckBox();
    protected JScrollPane scrlExportTypes = new JScrollPane();
    protected JList<Object> lstExportType = new JList<Object>(Constants.EXPORT_TYPE.values());

    public ExportPanel1() {
        super(new FlowLayout(FlowLayout.LEFT,1, 1));
        jbInit();
        lstExportType.setSelectedIndex(3);  // SHP
    }
    
    private void jbInit() {

        this.setLayout(layoutUI);
        this.setMinimumSize(new Dimension(400, 300));
        this.setMaximumSize(new Dimension(400, 300));
        this.setPreferredSize(new Dimension(400, 300));
        contentPanel.setLayout(layoutContent);

        contentPanel.setSize(new Dimension(400, 300));
        contentPanel.setPreferredSize(new Dimension(400, 300));

        pnlExportType.setBorder(BorderFactory.createTitledBorder("Export Options"));
        pnlExportType.setLayout(layoutObjectProperties);
        pnlObjectProperties.setBorder(BorderFactory.createTitledBorder("Object Properties"));
        pnlObjectProperties.setLayout(layoutEXPORT_TYPE);
        pnlStepOne.setBorder(BorderFactory.createTitledBorder("Step 1: Select geometry column and type of export"));
        pnlStepOne.setLayout(xYLayout1);

        lblSchema.setText("Schema:");
        lblSchema.setHorizontalAlignment(SwingConstants.RIGHT);        
        tfSchema.setPreferredSize(new Dimension(145, 20));
        tfSchema.setMinimumSize(new Dimension(145, 20));
        tfSchema.setMaximumSize(new Dimension(145, 20));
        tfSchema.setEditable(false);
        tfSchema.setFont(new Font("Tahoma", 1, 11));

        lblObject.setText("Object:");
        lblObject.setHorizontalAlignment(SwingConstants.RIGHT);
        tfObject.setPreferredSize(new Dimension(145, 20));
        tfObject.setMaximumSize(new Dimension(145, 20));
        tfObject.setMinimumSize(new Dimension(145, 20));
        tfObject.setEditable(false);
        tfObject.setFont(new Font("Tahoma", 1, 11));
        
        lblGeometry.setText("Geometry:");
        lblGeometry.setHorizontalAlignment(SwingConstants.RIGHT);
        cmbGeometries.setPreferredSize(new Dimension(145, 20));
        cmbGeometries.setMinimumSize(new Dimension(145, 20));
        cmbGeometries.setMaximumSize(new Dimension(145, 20));
        cmbGeometries.setActionCommand("CMB_GEOMETRY"); // Needed for when catching ActionCommand in associated Descriptor
      
        lblExportType.setText("Type:");
        lblExportType.setHorizontalAlignment(SwingConstants.RIGHT); // Neede for when catching ActionCommand in associated Descriptor
        lstExportType.setLayoutOrientation(JList.VERTICAL);
        lstExportType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstExportType.setName("EXPORT_TYPE");
        lstExportType.setSelectedIndex(0);
        scrlExportTypes.setViewportView(lstExportType);

        cbAttributes.setText("Attributes?");
        cbAttributes.setHorizontalTextPosition(SwingConstants.LEFT);
        cbAttributes.setActionCommand("ATTRIBUTES");

        pnlExportType.add(lstExportType, new XYConstraints(80, -4, 65, 70));
        pnlExportType.add(cbAttributes, new XYConstraints(20, 76, 74, 18));
        pnlExportType.add(lblExportType, new XYConstraints(0, 1, 75, 15));
        pnlObjectProperties.add(tfObject, new XYConstraints(80, 41, 245, 20));
        pnlObjectProperties.add(lblObject, new XYConstraints(30, 41, 45, 15));
        pnlObjectProperties.add(tfSchema, new XYConstraints(80, 1, 245, 20));
        pnlObjectProperties.add(lblSchema, new XYConstraints(30, 1, 45, 15));
        pnlObjectProperties.add(cmbGeometries, new XYConstraints(80, 81, 245, 20));
        pnlObjectProperties.add(lblGeometry, new XYConstraints(5, 81, 70, 15));
        pnlStepOne.add(pnlExportType, new XYConstraints(5, 136, 375, 120));
        pnlStepOne.add(pnlObjectProperties, new XYConstraints(5, 6, 375, 130));
        contentPanel.add(pnlStepOne, new XYConstraints(0, 0, 395, 290));
        this.add(contentPanel, new XYConstraints(100, 5, 165, 85));
    }
   
    public void addAttributeActionListener(ActionListener _listenerInDescriptor) {
       this.cbAttributes.addActionListener(_listenerInDescriptor);
       this.cmbGeometries.addActionListener(_listenerInDescriptor);
    }

    public void addListSelectionListener(ListSelectionListener _listenerInDescriptor) {
        this.lstExportType.addListSelectionListener(_listenerInDescriptor);
    }

    public void setNoAttributes() {
        this.cbAttributes.setEnabled(false);
    }
    public boolean isAttributesSelected() {
         return this.cbAttributes.isSelected();
     }

    @SuppressWarnings("unused")
	private ImageIcon getImageIcon() {
        return new ImageIcon((URL)getResource("clouds.jpg"));
    }
    
    public void setSchemaName(String _name) {
        this.tfSchema.setText(_name);      
    }
 
    public void setObjectName(String _name) {
        this.tfObject.setText(_name);      
    }
    
    public void setGeometryColumns(ComboBoxModel<String> _entries) {
        this.cmbGeometries.setModel(_entries);
        this.cmbGeometries.setSelectedIndex(0);
    }

    public Constants.EXPORT_TYPE getExportType() {
        return Constants.EXPORT_TYPE.valueOf(this.lstExportType.getSelectedValue().toString());      
    }

    public void setAttributes(boolean _selected) {
      this.cbAttributes.setSelected(_selected);
    }
    
    private Object getResource(String key) {

        URL url = null;
        String name = key;

        if (name != null) {

            try {
                Class<?> c = Class.forName("org.GeoRaptor.io.Export.ExporterWizard");
                url = c.getResource(name);
            } catch (ClassNotFoundException cnfe) {
                System.err.println("Unable to find org.GeoRaptor.io.Export.ExporterWizard main class");
            }
            return url;
        } else
            return null;

    }

}
