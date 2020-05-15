package org.GeoRaptor.OracleSpatial.Metadata;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class DecimalDocument extends PlainDocument 
{

	private static final long serialVersionUID = -6266698489694065093L;

	private int maxCharsInteger;

    private int maxCharsFraction;

    private int integers = 0;

    private int fractions = 0;

    private boolean isDecimalPoint = false;

    public DecimalDocument(int _maxCharsInteger, int _maxCharsFraction) {
        super();
        this.maxCharsInteger = _maxCharsInteger;
        this.maxCharsFraction = _maxCharsFraction;
    }

    public void insertString(int offset, String s,
                             AttributeSet attributeSet) throws BadLocationException {
        int length = this.getLength();
        String text = this.getText(0, length);
        int ix = text.indexOf('.');

        if (ix != -1) {
            isDecimalPoint = true;
            integers = ix;
            fractions = length - ix - 1;
        } else {
            isDecimalPoint = false;
            integers = length;
            fractions = 0;
        }
        // if

        if ((s == null) || s.equals("")) {
            super.insertString(offset, s, attributeSet);
        }

        else if (s.equals("0") || s.equals("1") || s.equals("2") ||
                 s.equals("3") || s.equals("4") || s.equals("5") ||
                 s.equals("6") || s.equals("7") || s.equals("8") ||
                 s.equals("9") || s.equals("+") || s.equals("-")) {
            if (isDecimalPoint) {
                if ((offset <= ix) && (integers < maxCharsInteger)) {
                    super.insertString(offset, s, attributeSet);
                    integers = integers + 1;
                } else if ((offset > ix) && (fractions < maxCharsFraction)) {
                    super.insertString(offset, s, attributeSet);
                    fractions = fractions + 1;
                }
                // if
            } else if (integers < maxCharsInteger) {
                super.insertString(offset, s, attributeSet);
                integers = integers + 1;
            }
            // if
        } else if (s.equals(".") || s.equals(",")) {
            if (!isDecimalPoint) {
                if (s.equals("."))
                    super.insertString(offset, s, attributeSet);
                else
                    super.insertString(offset, ".", attributeSet);

                isDecimalPoint = true;

            }
        }

        else if (s.length() > 1) {
            super.insertString(offset, s, attributeSet);
        }
    }
    
}
