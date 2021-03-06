/*
 *  fra2015
 *  https://github.com/geosolutions-it/fra2015
 *  Copyright (C) 2007-2013 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.fra2015.mvc.controller;

import static it.geosolutions.fra2015.mvc.controller.utils.ControllerServices.SESSION_USER;
import freemarker.template.TemplateException;
import it.geosolutions.fra2015.entrypoint.SurveyServiceEntryPoint;
import it.geosolutions.fra2015.mvc.controller.utils.ControllerServices.Profile;
import it.geosolutions.fra2015.mvc.controller.utils.FlashAttributesHandler;
import it.geosolutions.fra2015.mvc.controller.utils.LoggingUtils;
import it.geosolutions.fra2015.mvc.controller.utils.StatusUtils;
import it.geosolutions.fra2015.mvc.validation.TiersValidator;
import it.geosolutions.fra2015.mvc.validation.Validator;
import it.geosolutions.fra2015.server.model.survey.Country;
import it.geosolutions.fra2015.server.model.survey.Status;
import it.geosolutions.fra2015.server.model.user.User;
import it.geosolutions.fra2015.services.UserService;
import it.geosolutions.fra2015.services.mail.FraMailException;
import it.geosolutions.fra2015.services.mail.NotificationSerivice;
import it.geosolutions.fra2015.services.utils.UserUtil;
import it.geosolutions.fra2015.validation.ValidationResult;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Provides Validation check results and allow the contributor submission
 * @author Lorenzo Natali
 * 
 */
@Controller
public class CheckController {
    Logger LOGGER = Logger.getLogger(CheckController.class);
    
    @Autowired
    private SurveyServiceEntryPoint surveyService;
    @Autowired
    private UserService userService;
    
    @Autowired
    private NotificationSerivice notificationService;

    @Autowired
    private Validator validator;
    
    @Autowired
    private TiersValidator tiersValidator;

    @RequestMapping(value ="/check",method = RequestMethod.GET)
    public String printWelcome(ModelMap model, HttpSession session) {
        model.addAttribute("context", "check");
        User su = (User) session.getAttribute(SESSION_USER);
        if (su == null) {
            return "redirect:/";
        }
        String iso3 = UserUtil.getSingleIso3(su);
        Status s = surveyService.getStatus(iso3);
        Boolean canSubmit = su.getCanSubmit();
        if(canSubmit==null){
            canSubmit = Boolean.FALSE;
           

        }
        model.addAttribute("cansubmit",canSubmit);
        if(!StatusUtils.isSubmitAllowed(s)){
            model.addAttribute("denysubmit", true);
            return "index";
        }
        ValidationResult tiersResult = tiersValidator.checkTiers(iso3);
        ValidationResult rulesResult = validator.validate(iso3, tiersResult);
        ValidationResult v = (rulesResult==null)?tiersResult:rulesResult;
        if (v.getSuccess() && tiersResult.getSuccess()) {
            model.addAttribute("allowsubmit", true);
        } else {
            model.addAttribute("validationResult", v);
        }
        return "index";

    }
    /**
     * Provides check for reviewers and editors
     * @param model
     * @param session
     * @param country iso3 of the country
     * @return
     */
    @RequestMapping(value ="/check/{country}",method = RequestMethod.GET)
    public String printWelcome(ModelMap model, HttpSession session,@PathVariable(value = "country") String country) {
        model.addAttribute("context", "check");
        model.addAttribute("country", country);
        User su = (User) session.getAttribute(SESSION_USER);
        String iso3 = country;
        Status status = surveyService.getStatus(iso3);
        String statusLocale= StatusUtils.getStatusLocaleCode(status);

        model.addAttribute("statuscode",statusLocale);
        String targetPage = "";
        if(su==null){
            return "redirect:/"; 
         }
        if(su.getRole().equalsIgnoreCase(Profile.EDITOR.toString())){
            targetPage = "editor";
        }
        else if(su.getRole().equalsIgnoreCase(Profile.REVIEWER.toString())){
            targetPage = "reviewer";
        }
        else{
            targetPage= "redirect:/";
        }
        
        
        ValidationResult tiersResult = tiersValidator.checkTiers(iso3);
        ValidationResult rulesResult = validator.validate(iso3, tiersResult);
        ValidationResult v = (rulesResult==null)?tiersResult:rulesResult;
        if (v.getSuccess() && tiersResult.getSuccess()) {
            model.addAttribute("allowsubmit", true);
        } else {
            model.addAttribute("validationResult", v);
        }
        return targetPage;

    }

   
    /**
     * Manages contributor submission
     * @param request
     * @param model
     * @param session
     * @return
     */
    @RequestMapping(value="/check",method = RequestMethod.POST)
    public String printWelcome(HttpServletRequest request, ModelMap model, HttpSession session) {


        User su = (User) session.getAttribute(SESSION_USER);
        if (su == null ) { //TODO check if the user is contributor
            return "redirect:/";
        }
        //check status
        String iso3 = UserUtil.getSingleIso3(su);
        Status status =surveyService.getStatus(iso3);
        if(!StatusUtils.isSubmitAllowed(status)){
            submitDeniedError(status,model);
            return "index";
            
        }
        status.setStatus(StatusUtils.COMPILED);
        status.setCountry(iso3);
        status.setMessage((String) request.getParameter("submitmessage"));
        status.setLastContributorSubmission(System.currentTimeMillis());
        Country c = surveyService.findCountryByISO3(iso3);
        if(c!=null){
            status.setCountry(iso3);
        }
        
        surveyService.changeStatus(status);
        LOGGER.info("submitted survey:"+status.getCountry());

        List<User> reviewers=  userService.getUsersToNotify("reviewer", iso3, true);
        
        LOGGER.info("----------------- state transition: WORKINPROGRESS-COMPLILED --------------------");
        LOGGER.info("----------------- Selected Users (Reviewers) to notify with Mail: ---------------");
        LOGGER.info(LoggingUtils.printUsernames(reviewers));
        LOGGER.info("----------------------------------------------------------------------------------");
        
         if(reviewers.size()<=0){
           LOGGER.error("No reviewer associated to country " + iso3 );
           //TODO notify someone this error
           
         }
        try{
            notificationService.notifyContributorSubmit(su, status,reviewers);
            model.addAttribute("context", "check");
            model.addAttribute("messageType", "success");
            model.addAttribute("messageCode", "submit.success");
        }catch(MailException  e){
            sumbitNotNotifiedError(model);
            model.addAttribute("context", "messageonly");  
            LOGGER.error("The reviewers were not notified of the message submit because of an Mail Exception",e);
        }catch(TemplateException e){
            model.addAttribute("context", "messageonly");  
            sumbitNotNotifiedError(model);
        }catch(IOException e){
            if(e instanceof FraMailException){
                FlashAttributesHandler.addFlashAttribute(session, "warning", "recipients.not.valid", 10000, null, ((FraMailException) e).getFailedRecipientsNameList());
                LOGGER.error(
                        "Error sending Mail to the following recipients: " + ((FraMailException)e).getFailedRecipientsNameList(),
                        e);
            }
            else{
                model.addAttribute("context", "messageonly");
                sumbitNotNotifiedError(model);
            }
        }
        
        return "index";

    }
    
    private static void sumbitNotNotifiedError(ModelMap model){
        model.addAttribute("messageType", "waring");
        model.addAttribute("messageCode", "submit.notnotified");
        model.addAttribute("messageTimeout",10000);
    }
    
    private static void submitDeniedError(Status s,ModelMap model) {
        
        model.addAttribute("context", "check");
        model.addAttribute("messageType", "waring");
        model.addAttribute("messageCode", "submit.statuserror");
        model.addAttribute("denysubmit", true);
        model.addAttribute("messageTimeout",5000);
        
    }
    private void submissionError(ModelMap model,Exception e,Country c,User us){
        LOGGER.error("There was an error submitting the survey for Country:"+c +"submitted by the user: " + us,e);
        model.addAttribute("context", "check");
        model.addAttribute("messageType", "error");
        model.addAttribute("messageCode", "submit.error");
        model.addAttribute("messageTimeout",10000);
    }
    
    
}
