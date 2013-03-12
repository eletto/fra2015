package it.geosolutions.fra2015.mvc.validation;

import it.geosolutions.fra2015.server.model.survey.Country;
import it.geosolutions.fra2015.server.model.survey.EntryItem;
import it.geosolutions.fra2015.server.model.survey.Value;
import it.geosolutions.fra2015.services.SurveyService;
import it.geosolutions.fra2015.services.exception.BadRequestServiceEx;
import it.geosolutions.fra2015.validation.RuleList;
import it.geosolutions.fra2015.validation.ValidationMessage;
import it.geosolutions.fra2015.validation.ValidationResult;
import it.geosolutions.fra2015.validation.ValidationRule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class Validator implements InitializingBean, ApplicationContextAware {
	private ApplicationContext applicationContext;
	private final static Logger LOGGER = LoggerFactory
			.getLogger(Validator.class);

	File rulesFile;
	RuleList ruleList;
	@Autowired
	private SurveyService surveyService;

	/**
	 * Validates the whole country values
	 * 
	 * @param country
	 * @return
	 */
	public ValidationResult validate(String country) {

		Iterator<ValidationRule> it = ruleList.iterator();
		ValidationResult result = new ValidationResult();
		result.setSuccess(true);
		while (it.hasNext()) {
			ValidationRule rule = it.next();
			List<String> vars = rule.getVariables();
			try {
				List<Value> values = surveyService.getEntryListByVariableName(
						vars, country);
				Country c = surveyService.searchCountry(country);
				Map<String, String> externals = new HashMap<String, String>();
				if (c.getCountryArea() != null && c.getLandArea() != null) {
					externals.put("COUNTRY_AREA", c.getCountryArea() + "");
					externals.put("LAND_AREA", c.getLandArea() + "");
				}
				validateRule(values, rule, result, externals);

			} catch (BadRequestServiceEx e) {
				LOGGER.error("error retriving variables during validation", e);

			}

		}

		return result;
	}

	private void validateRule(List<Value> values, ValidationRule rule,
			ValidationResult result, Map<String, String> externals) {
		Map<String, Map<String, String>> tests = new HashMap<String, Map<String, String>>();
		// get all columns (1990 2000 etc...)
		if (values.size() == 0) {
			result.setSuccess(false);
			ValidationMessage v = new ValidationMessage();
			v.setMessage("validation.notcompiled");
			v.addElement(rule.getEntryId());
			result.addMessage(v);
			return;
		}
		for (Value value : values) {
			// for each column add a map name (1.2) -> value (e.g. 1234)
			Map<String, String> testEntry;
			String colname = value.getEntryItem().getColumnName();
			if (!tests.containsKey(colname)) {
				testEntry = new HashMap<String, String>();
				tests.put(colname, testEntry);
			} else {
				testEntry = tests.get(colname);
			}
			testEntry
					.put(value.getEntryItem().getRowName(), value.getContent());
		}
		// for each column (year)
		ValidationMessage message = null;
		// some validation problems have priority
		boolean alreadyChecked = false;
		for (String key : tests.keySet()) {
			if (alreadyChecked) {
				continue;
			}

			// get the map name ->value
			Map<String, String> test = tests.get(key);
			List<String> missing = checkRuleFields(rule, test);
			if (missing.size() > 0) {

				message = new ValidationMessage();
				message.setMessage("validation.notcompiled");
				message.setElements(missing);
				result.setSuccess(false);
				alreadyChecked = true;

			}

			try {
				boolean success = rule.evaluate(test, externals);
				if (!success) {
					if (message == null) {
						message = new ValidationMessage();
						message.setMessage(rule.getError());
					}
					message.addElement(key);
					result.setSuccess(success);
					
				}
			} catch (ScriptException e) {
				result.setSuccess(false);

				message = new ValidationMessage();
				message.setMessage("validation.parseproblem");
				message.addElement(rule.getCondition());
				alreadyChecked = true;

			}
			
		}
		result.addMessage(message);

	}

	private List<String> checkRuleFields(ValidationRule rule,
			Map<String, String> test) {
		List<String> missing = new ArrayList<String>();
		List<String> vars = rule.getVariables();
		for (String name : vars) {
			;
			if (test.get(name) == null) {
				missing.add(name);
			}
		}
		return missing;
	}

	private Map createMap() {
		// TODO Auto-generated method stub
		return null;
	}

	public RuleList getRuleList() {
		return ruleList;
	}

	public void setRuleList(RuleList ruleList) {
		this.ruleList = ruleList;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub
		try {
			rulesFile = applicationContext.getResource(
					"classpath:validation-rules.xml").getFile();
		} catch (IOException e) {
			LOGGER.error("unable to load validation-rules:" + e);

		}
		ruleList = JAXB.unmarshal(rulesFile, RuleList.class);
		Iterator<ValidationRule> it = ruleList.iterator();
		LOGGER.info(it.hasNext() + "");
		while (it.hasNext()) {
			LOGGER.info(it.next().getDescription());

		}

	}

	@Override
	public void setApplicationContext(ApplicationContext ac)
			throws BeansException {
		this.applicationContext = ac;
	}

}