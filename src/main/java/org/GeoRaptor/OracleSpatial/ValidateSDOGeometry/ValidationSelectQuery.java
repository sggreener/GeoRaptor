package org.GeoRaptor.OracleSpatial.ValidateSDOGeometry;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.GeoRaptor.tools.PropertiesManager;
import org.GeoRaptor.tools.Strings;
import org.geotools.util.logging.Logger;
import org.geotools.util.logging.Logging;

import oracle.jdbc.OracleConnection;
import oracle.jdeveloper.layout.XYConstraints;
import oracle.jdeveloper.layout.XYLayout;


public class ValidationSelectQuery extends JDialog {

	private static final long serialVersionUID = 9108367013821785004L;

	private static final Logger LOGGER = Logging.getLogger("org.GeoRaptor.OracleSpatial.ValidateSDOGeometry.ValidateSDOGeometry.ValidationSelectQuery");

    protected ValidateSDOGeometry mainValidationForm = null;
    
    protected PropertiesManager propertyManager = null;
    private static final String propertiesFile    = "org.GeoRaptor.OracleSpatial.ValidateSDOGeometry.Validation";

    private boolean     CANCELLED = false;
    
    private JPanel         pnlSQL = new JPanel();
    private JTextArea       taSQL = new JTextArea();
    private XYLayout   xySQLPanel = new XYLayout();
    private JScrollPane     spSQL = new JScrollPane();
    private JButton         btnOK = new JButton();
    private JButton     btnCancel = new JButton();
    private JButton  btnClipboard = new JButton();
    private FlowLayout   flDialog = new FlowLayout(FlowLayout.CENTER);
    private JCheckBox   cbEditSQL = new JCheckBox();
    private JCheckBox cbNullGeoms = new JCheckBox();
    private JButton      btnReset = new JButton();
    private JCheckBox        cb2D = new JCheckBox();
    @SuppressWarnings("unused")
	private String            sql = "";

    public ValidationSelectQuery(Dialog dialog, 
                                 String _title, 
                                 boolean _modal,
                                 ValidateSDOGeometry _validateForm)
    throws IllegalArgumentException
    {
        super(dialog, _title, _modal);
        try {
            // Get localisation file
            //
            this.propertyManager = new PropertiesManager(ValidationSelectQuery.propertiesFile);
        } catch (Exception e) {
            System.out.println("Problem loading properties file: " + ValidationSelectQuery.propertiesFile + "\n" + e.getMessage());
        }
        this.mainValidationForm = _validateForm;
        if ( this.mainValidationForm == null )
            throw new IllegalArgumentException(this.propertyManager.getMsg("MAIN_VALIDATION_FORM_EXCEPTION"));
        try {
            jbInit();
            this.cbNullGeoms.setSelected(this.mainValidationForm.isNullGeoms()); 
            setPropertyStrings();
            applyPropertyStrings();
            this.addWindowListener(new WindowListener(){
                public void windowOpened(WindowEvent e) {
                    initialiseScrollBars(spSQL);
                }
                public void windowActivated(WindowEvent e) {}
                public void windowClosing(WindowEvent e) { }
                public void windowClosed(WindowEvent e) {}
                public void windowIconified(WindowEvent e) {}
                public void windowDeiconified(WindowEvent e) {}
                public void windowDeactivated(WindowEvent e) {}
            });
            OracleConnection conn = this.mainValidationForm.getConnection();
            if ( conn == null || conn.getMetaData().getDatabaseMajorVersion() < 11 ) {
                this.cb2D.setEnabled(false);
            }
        } catch (Exception e) {
            LOGGER.warn("Problem initialising Validation dialog: \n" + e.getMessage());
        }
    }

    private void jbInit() throws Exception {
        this.setResizable(false);
        this.setSize(new Dimension(507, 425));
        this.getContentPane().setLayout(flDialog);
        
        pnlSQL.setLayout(xySQLPanel);
        pnlSQL.setBorder(BorderFactory.createTitledBorder("SQL"));
        pnlSQL.setSize(new Dimension(495, 468));

        pnlSQL.setMaximumSize(new Dimension(495, 400));
        pnlSQL.setMinimumSize(new Dimension(400, 400));
        pnlSQL.setPreferredSize(new Dimension(495, 400));

        spSQL.setAutoscrolls(true);
        spSQL.setPreferredSize(new Dimension(480, 265));
        spSQL.setMinimumSize(new Dimension(480, 265));
        spSQL.setMaximumSize(new Dimension(800, 500));
        spSQL.setViewportView(this.taSQL);
        taSQL.setSize(new Dimension(475, 260));
        taSQL.setFont(new Font("Courier New", 0, 10));
        taSQL.setPreferredSize(new Dimension(1000, 500));
        taSQL.setMinimumSize(new Dimension(475, 260));
        taSQL.setMaximumSize(new Dimension(1000, 500));
        taSQL.setEditable(false);
        btnOK.setText("OK");
        btnOK.setMaximumSize(new Dimension(65, 20));
        btnOK.setMinimumSize(new Dimension(65, 20));
        btnOK.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnOK_actionPerformed(e);
                }
            });
        btnCancel.setText("Cancel");
        btnCancel.setSize(new Dimension(60, 20));
        btnCancel.setPreferredSize(new Dimension(60, 20));
        btnCancel.setMaximumSize(new Dimension(60, 20));
        btnCancel.setMinimumSize(new Dimension(60, 20));
        btnCancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnCancel_actionPerformed(e);
                }
            });
        btnClipboard.setText("Copy");
        btnClipboard.setToolTipText("Copy SQL to Clipboard");
        btnClipboard.setPreferredSize(new Dimension(65, 20));
        btnClipboard.setMaximumSize(new Dimension(65, 20));
        btnClipboard.setMinimumSize(new Dimension(65, 20));
        btnClipboard.setSize(new Dimension(60, 20));
        btnClipboard.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnClipboard_actionPerformed(e);
                }
            });

        cbEditSQL.setText("Edit");
        cbEditSQL.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cbEditSQL_actionPerformed(e);
                }
            });
        cbNullGeoms.setText("NULL Geoms");
        cbNullGeoms.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cbNullGeoms_actionPerformed(e);
                }
            });
        btnReset.setText("Reset");
        btnReset.setPreferredSize(new Dimension(65, 20));
        btnReset.setMinimumSize(new Dimension(65, 20));
        btnReset.setMaximumSize(new Dimension(65, 20));
        btnReset.setSize(new Dimension(60, 20));
        btnReset.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    btnReset_actionPerformed(e);
                }
            });
        cb2D.setText("2D");
        cb2D.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cb2D_actionPerformed(e);
                }
            });
        pnlSQL.add(cb2D,         new XYConstraints(370, 351, 35, 20));
        pnlSQL.add(cbNullGeoms,  new XYConstraints(285, 351, 80, 20));
        pnlSQL.add(cbEditSQL,    new XYConstraints(235, 351, 45, 20));
        pnlSQL.add(spSQL,        new XYConstraints(5, -4, 480, 350));
        pnlSQL.add(btnOK,        new XYConstraints(5, 351, 65, 20));
        pnlSQL.add(btnReset, new XYConstraints(75, 351, 75, 20));
        pnlSQL.add(btnClipboard, new XYConstraints(155, 351, 70, 20));
        pnlSQL.add(btnCancel,    new XYConstraints(415, 351, 70, 20));
        this.getContentPane().add(pnlSQL);
        this.pack();
    }

    protected String DIALOG_TITLE          = "Validate SDO_GEOMETRY SQL";
    protected String BUTTON_CANCEL         = "Cancel";
    protected String BUTTON_CLOSE          = "Close";
    protected String BUTTON_COPY_CLIPBOARD = "Copy";
    protected String BUTTON_RESET          = "Reset";
    protected String TT_RESET_SQL          = "Resets SQL to GeoRaptor default.";
    protected String TT_COPY_CLIPBOARD     = "Copy SQL to Clipboard";
    protected String TT_WHERE              = "";
    protected String VALIDATION_SQL_WARNING= "";
    protected String VALIDATION_SQL_TITLE  = "Custom SQL Warning";
    protected String CHECK_BOX_NULL_GEOMETRIES = "NULL Sdo_Geometies";

    private void setPropertyStrings() {
        this.DIALOG_TITLE              = this.propertyManager.getMsg("DIALOG_TITLE");
        this.BUTTON_CANCEL             = this.propertyManager.getMsg("BUTTON_CANCEL_VALIDATION");
        this.BUTTON_CLOSE              = this.propertyManager.getMsg("BUTTON_CLOSE");
        this.BUTTON_COPY_CLIPBOARD     = this.propertyManager.getMsg("BUTTON_COPY_CLIPBOARD");
        this.TT_COPY_CLIPBOARD         = this.propertyManager.getMsg("TT_COPY_CLIPBOARD");
        this.BUTTON_RESET              = this.propertyManager.getMsg("BUTTON_RESET");
        this.TT_RESET_SQL              = this.propertyManager.getMsg("TT_RESET_SQL");
        this.TT_WHERE                  = this.propertyManager.getMsg("TT_WHERE");
        this.VALIDATION_SQL_WARNING    = String.format(this.propertyManager.getMsg("VALIDATION_SQL_WARNING"),
                                                       ValidateSDOGeometry.primaryKeyColumnName,
                                                       ValidateSDOGeometry.contextColumn,
                                                       this.mainValidationForm.getGeomColumnName(),
                                                       this.mainValidationForm.getObjectName());
        this.VALIDATION_SQL_TITLE      = this.propertyManager.getMsg("VALIDATION_SQL_TITLE");
        this.CHECK_BOX_NULL_GEOMETRIES = this.propertyManager.getMsg("CHECK_BOX_NULL_GEOMETRIES");
    }
    
    private void applyPropertyStrings() {
        this.setTitle(this.DIALOG_TITLE);
        this.btnCancel.setText(this.BUTTON_CANCEL);
        this.btnOK.setText(this.BUTTON_CLOSE);
        this.btnClipboard.setText(this.BUTTON_COPY_CLIPBOARD);
        this.btnClipboard.setToolTipText(this.TT_COPY_CLIPBOARD);
        this.btnReset.setText(this.BUTTON_RESET);
        this.btnReset.setToolTipText(this.TT_RESET_SQL);
        this.cbNullGeoms.setText(this.CHECK_BOX_NULL_GEOMETRIES);
    }

    private void initialiseScrollBars(JScrollPane _sp) {
      JScrollBar verticalScrollBar   = _sp.getVerticalScrollBar();
      JScrollBar horizontalScrollBar = _sp.getHorizontalScrollBar();
      verticalScrollBar.setValue(verticalScrollBar.getMinimum());
      horizontalScrollBar.setValue(horizontalScrollBar.getMinimum());      
    }
    
    public void setSql(String _SQL) {
        this.taSQL.setText(_SQL);
        this.sql = this.taSQL.getText();
    }

    public String getSql() {
        String sql = this.taSQL.getText();
        if (Strings.isEmpty(sql) )
            return sql;
        else 
            return this.taSQL.getText().replace(";","");
    }

    public void setNullGeoms(boolean _nullGeoms) {
        this.cbNullGeoms.setSelected(_nullGeoms);      
    }
    
    public boolean wasCancelled() {
      return this.CANCELLED;
    }
    
    private void btnCancel_actionPerformed(ActionEvent e) {
        this.CANCELLED = true;
        this.setVisible(false);
    }

    private void btnOK_actionPerformed(ActionEvent e) {
        this.CANCELLED = false;
        this.setVisible(false);
    }

    private void btnClipboard_actionPerformed(ActionEvent e) {
        String validationSQL = this.taSQL.getText();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection ss = new StringSelection(validationSQL);
        clipboard.setContents(ss, ss);
    }

    private void cbEditSQL_actionPerformed(ActionEvent e) {
        if ( cbEditSQL.isSelected() ) {
            JOptionPane.showMessageDialog(this,
                                          this.VALIDATION_SQL_WARNING,
                                          this.VALIDATION_SQL_TITLE,
                                          JOptionPane.WARNING_MESSAGE);
        } 
        this.taSQL.setEditable(this.cbEditSQL.isSelected());
    }

    private void cbNullGeoms_actionPerformed(ActionEvent e) {
        this.mainValidationForm.setNullGeoms(this.cbNullGeoms.isSelected());
        if (Strings.isEmpty(this.taSQL.getText())) {
            this.taSQL.setText(this.mainValidationForm.getInitialSQL());
            return;
        }
        String tSQL = this.taSQL.getText();
        if ( this.cbNullGeoms.isSelected() ) {
            int whereIndex = tSQL.lastIndexOf("WHERE " + this.mainValidationForm.getNullGeomsPredicate());
            if (  whereIndex > -1 ) {
                int andIndex = Math.max(tSQL.lastIndexOf("AND "),tSQL.lastIndexOf("and "));
                if ( andIndex > whereIndex) {
                    this.taSQL.setText(this.taSQL.getText().substring(0, whereIndex+6) +
                                       this.taSQL.getText().substring(whereIndex+6).replaceFirst("[Aa][Nn][Dd][ ]",""));
                    this.taSQL.setText(this.taSQL.getText().replace(this.mainValidationForm.getNullGeomsPredicate(),""));
                } else {
                    this.taSQL.setText(this.taSQL.getText().replace("WHERE " + this.mainValidationForm.getNullGeomsPredicate(),""));                
                }
                return;
            }
            if ( tSQL.lastIndexOf("AND " + this.mainValidationForm.getNullGeomsPredicate()) > -1 ) {
                this.taSQL.setText(this.taSQL.getText().replace("AND " + this.mainValidationForm.getNullGeomsPredicate(),""));
                return;
            }
        } else {
            int whereIndex = tSQL.lastIndexOf("WHERE");
            this.taSQL.setText( this.taSQL.getText() + 
                                ( whereIndex!=-1 ? "AND " : "WHERE ") + this.mainValidationForm.getNullGeomsPredicate());
        }  
    }

    private void btnReset_actionPerformed(ActionEvent e) {
        this.taSQL.setText(this.mainValidationForm.getInitialSQL());
    }

    private void cb2D_actionPerformed(ActionEvent e) {
        this.mainValidationForm.set2D(this.cb2D.isSelected());
        if (Strings.isEmpty(this.taSQL.getText())) {
            this.taSQL.setText(this.mainValidationForm.getInitialSQL());
            return;
        } else {
            String newValidationFunction = this.mainValidationForm.getValidationFunction(false /* not Update*/);
            boolean saveValue = this.cb2D.isSelected(); 
            this.mainValidationForm.set2D(! saveValue);
            String oldValidationFunction = this.mainValidationForm.getValidationFunction(false /* not Update*/);
            this.mainValidationForm.set2D(saveValue);
            this.taSQL.setText(this.taSQL.getText().replace(oldValidationFunction,newValidationFunction));
        }
    }
}
