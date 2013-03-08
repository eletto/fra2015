package it.geosolutions.fra2015.tags;

import it.geosolutions.fra2015.mvc.controller.utils.ControllerServices.OperationWR;
import it.geosolutions.fra2015.server.model.user.User;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;

/**
 * Tag that shows a editable text area when the user in session has the role
 * contained in editor attribute, and a div if it has not.
 * 
 * @author Lorenzo Natali
 * 
 */
@SuppressWarnings("serial")
public class RichTextEntry extends TagSupport {
	Logger LOGGER = Logger.getLogger(this.getClass());
	private static String editorStart = "<textarea class='texteditor entry-item' cols='160' rows='10' name='";
	private static String readerStart = "<div id='";
	private static String editorEnd = "</textarea>";
	private static String readerend = "</div>";

	private String editor = "contributor";

	private String operation;

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	private String name;
	private boolean isEditor;

	public int doStartTag() {
		try {
			JspWriter out = pageContext.getOut();
			User user = (User) pageContext.getSession().getAttribute(
					"sessionUser");

			if (user == null) {
				this.isEditor = false;
			} else {
				this.isEditor = editor.equals(user.getRole());
			}
						
			// Check if the operation is valid
            OperationWR op = Utils.validateOperation(operation);
            if(op == null){
                out.print("operation '" + operation + "' isn't a valid operation");
                return (SKIP_BODY);
            }
            boolean forceRead = op.compareTo(OperationWR.WRITE)==0;
            
            //get value
            String value = "";
			if (pageContext.getRequest().getAttribute(this.name) != null) {
				value = (String) pageContext.getRequest().getAttribute(
						this.name);
			}
			// print start tag
			if (this.isEditor && !forceRead) {
				out.print(editorStart + this.name + "'>" + value);
			} else {
				out.print(readerStart + this.name + "'>" + value);
			}

		} catch (IOException ioe) {
			LOGGER.error("Error in SimpleTag: " + ioe);

		}
		return (SKIP_BODY);
	}

	public int doEndTag() throws JspException {

		try {

			JspWriter out = pageContext.getOut();
			if (this.isEditor || "read".equals(operation)) {
				out.print(RichTextEntry.editorEnd);
			} else {
				out.print(RichTextEntry.readerend);
			}

		} catch (IOException ioe) {
			LOGGER.error("Error in SimpleTag: " + ioe);
		}
		return (SKIP_BODY);
	}

	public String getEditor() {
		return editor;
	}

	public void setEditor(String editor) {
		this.editor = editor;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
