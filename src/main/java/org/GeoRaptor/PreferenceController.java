package org.GeoRaptor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;

import oracle.dbtools.raptor.utils.Connections;
import oracle.ide.Context;
import oracle.ide.Ide;
import oracle.ide.config.Preferences;
import oracle.ide.controller.Controller;
import oracle.ide.controller.IdeAction;
import oracle.ide.editor.Editor;
import oracle.javatools.db.DBException;

/**
 * 
 * @author Bessie Gong 
 * @version 24 Jul 2019
 *
 * Blank file, used to react to preferences
 */
public class PreferenceController implements Controller {

	@Override
	public boolean handleEvent(IdeAction arg0, Context arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(IdeAction arg0, Context arg1) {
		// TODO Auto-generated method stub
		return false;
	}

}
