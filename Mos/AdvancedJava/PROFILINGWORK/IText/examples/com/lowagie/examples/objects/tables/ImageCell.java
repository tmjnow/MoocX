/*
 * $Id: ImageCell.java,v 1.5 2005/05/09 11:52:47 blowagie Exp $
 * $Name:  $
 *
 * This code is part of the 'iText Tutorial'.
 * You can find the complete tutorial at the following address:
 * http://itextdocs.lowagie.com/tutorial/
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * itext-questions@lists.sourceforge.net
 */
package com.lowagie.examples.objects.tables;

import java.io.FileOutputStream;
import java.io.IOException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * A cell with an Image.
 */
public class ImageCell {

	/**
	 * A cell with an image.
	 * 
	 * @param args
	 *            no arguments needed
	 */
	public static void main(String[] args) {

		System.out.println("Image in a Cell");

		// step 1: creation of a document-object
		Document document = new Document();

		try {
			// step 2:
			PdfWriter.getInstance(document, new FileOutputStream("ImageCell.pdf"));

			// step 3: we open the document
			document.open();
			Image image = Image.getInstance("otsoe.jpg");
			float[] widths = {1f, 4f};
			PdfPTable table = new PdfPTable(widths);
			table.addCell("This is my dog");
			table.addCell(image);
			table.addCell("This two");
			table.addCell(new PdfPCell(image, true));
			table.addCell("This three");
			table.addCell(new PdfPCell(image, false));
			document.add(table);
		} catch (DocumentException de) {
			System.err.println(de.getMessage());
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
		}

		// step 5: we close the document
		document.close();
	}
}