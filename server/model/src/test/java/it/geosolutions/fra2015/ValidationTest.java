package it.geosolutions.fra2015;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import it.geosolutions.fra2015.server.model.survey.Entry;
import it.geosolutions.fra2015.server.model.survey.EntryItem;
import it.geosolutions.fra2015.validation.ValidationMessage;
import it.geosolutions.fra2015.validation.ValidationResult;
import it.geosolutions.fra2015.validation.ValidationRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.junit.Test;

public class ValidationTest {

	@Test
	public void ruleTest() {
		ValidationRule v = new ValidationRule();
		v.setCondition(" {{1.4}} === {{1.5}} - ({{1.1}}+{{1.2}}+{{1.3}}) + [[external]] ");
		List<String> vars = v.getVariables();
		String[] varnames = { "1.4", "1.5", "1.1", "1.2", "1.3" };
		String[] values = { "3", "16", "5", "5", "5" };
		Map<String, String> valueMap = new HashMap<String, String>();
		for (int i = 0; i < varnames.length; i++) {
			valueMap.put(varnames[i], values[i]);
		}
		Map<String, String> externals = new HashMap<String, String>();
		externals.put("external", "2");
		List<String> ext = v.getExternalData();
		assertTrue("external".equals(ext.get(0)));
		int i = 0;
		for (String s : vars) {
			assertTrue(varnames[i].equals(s));
			i++;
		}
		try {
			v.evaluate(valueMap,externals);
		} catch (ScriptException e) {
			fail(e.getMessage());
		}

	}
	
	@Test
	public void uniqueMessageTest(){
 
	    ValidationResult re= new ValidationResult();
	    ValidationMessage m1 = new ValidationMessage();
	    ValidationMessage m2 = new ValidationMessage();
	    m1.addElement("1");
	    m1.addElement("1");
	    assertTrue(m1.getElements().size()==1);
	    m1.setMessage("a");
	    m2.setMessage("a");
	    m1.addElement("3");
	    m2.addElement("2");
	    re.addMessage(m1);
	    re.addMessage(m2);
	    assertTrue(re.getMessages().size()==1);

	}

	
	private void initItems(List<EntryItem> entryitemlist) {
		for (int i = 0; i < 100; i++) {
			EntryItem ei = new EntryItem();
			Integer cn = (2000 + (i % 4) * 5);
			ei.setColumnName(cn.toString());
			ei.setRowName(((i / 4) + 1) + "." + ((i % 4) + 1));
			ei.setEntry(new Entry());
			ei.getEntry().setVariable("2");
			entryitemlist.add(ei);
		}

	}
}