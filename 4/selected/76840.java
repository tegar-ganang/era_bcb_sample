package net.googlecode.demenkov.controllers;

import net.googlecode.demenkov.domains.Faculty;
import net.googlecode.demenkov.domains.Group;
import net.googlecode.demenkov.domains.Person;
import net.googlecode.demenkov.domains.University;
import net.googlecode.demenkov.services.PersonService;
import net.googlecode.demenkov.utils.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * Controller for actions with person: login, registration, edit profile
 *
 * @author Demenkov Yura
 */
@Controller
public class PersonController {

    protected final Logger logger = Logger.getLogger(PersonController.class);

    private Map<String, Person> personsWaitingForConfirm = new TreeMap<String, Person>();

    @Autowired
    private PersonService personService;

    /**
     * Gets authorizated person's email
     *
     * @return person's email
     */
    private String getUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ((UserDetails) principal).getUsername();
    }

    /**
     * Puts into parametrs Map lists of university names and faculty names of first university in list
     *
     * @param model Map to put lists of university names and faculty names of first university in list
     */
    private void putFacultiesAndUniversities(Map model) {
        List<String> universities = new ArrayList<String>();
        for (University university : personService.getAllUniversities()) {
            universities.add(university.getUniversityName());
        }
        List<String> faculties = new ArrayList<String>();
        for (Faculty fac : personService.findAllFacultiesByUniversityName(universities.iterator().next())) {
            faculties.add(fac.getFacultyName());
        }
        model.put("universities", universities.toArray());
        model.put("faculties", faculties.toArray());
    }

    /**
     * Redirects to your home page
     *
     * @return your home page
     */
    @RequestMapping("/")
    public String root() {
        return "redirect:home";
    }

    /**
     * Redirects to your home page
     *
     * @return your home page
     */
    @RequestMapping("/index")
    public String index() {
        return "redirect:home";
    }

    /**
     * Shows login page
     *
     * @return login page
     */
    @RequestMapping("/login")
    public String loginForm() {
        return "login";
    }

    /**
     * Gets your person after authorization, builds list of your top-5 groupmates and friends
     *
     * @param request with parametrs for view-page
     * @return your home page with top-5 groupmates
     */
    @RequestMapping("/home")
    public String logIn(HttpServletRequest request) {
        try {
            Person person = personService.findPersonByEmail(getUserEmail());
            request.setAttribute("person", person);
            List<Person> groupmates = new ArrayList<Person>(person.getGroup().getPersons());
            Collections.sort(groupmates, new PersonComparatorByRating());
            if (groupmates.size() < 5) {
                request.setAttribute("groupmates", groupmates);
            } else {
                request.setAttribute("groupmates", groupmates.subList(0, 5));
            }
            List<Person> friends = new ArrayList<Person>(person.getConfirmedFriends());
            Collections.sort(friends, new PersonComparatorByRating());
            if (friends.size() < 5) {
                request.setAttribute("friends", friends);
            } else {
                request.setAttribute("friends", friends.subList(0, 5));
            }
            request.setAttribute("unreadCount", person.getUnreadMessages().size());
            request.setAttribute("unconfirmedCount", person.getUnconfirmedFriends().size());
            return "home";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "redirect:login";
        }
    }

    /**
     * Puts into parametr new Person and redirects to registration page
     *
     * @param map with parametrs for view-page
     * @return page with registration form
     */
    @RequestMapping(value = "/registerform")
    public String registerForm(Map map) {
        map.put("person", new Person());
        putFacultiesAndUniversities(map);
        return "register";
    }

    /**
     * Sends to registration page list of faculty names of university with university name from request with parametrs
     *
     * @param request  with parametrs for view-page
     * @param response to write faculty names to the view-page
     */
    @RequestMapping(value = "/registergetfacs", method = RequestMethod.POST)
    @ResponseBody
    public void sendFaculties(HttpServletRequest request, HttpServletResponse response) {
        String selectedUniversity = request.getParameter("univername");
        try {
            PrintWriter wr = response.getWriter();
            for (Faculty fac : personService.findAllFacultiesByUniversityName(selectedUniversity)) {
                wr.println(fac.getFacultyName());
            }
            wr.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Sends a list of existing groupnames of faculty with facultyname from request parametr
     *
     * @param request  with parametrs for view-page
     * @param response to write group names to the view-page
     */
    @RequestMapping(value = "/registergetgroups", method = RequestMethod.POST)
    @ResponseBody
    public void sendGroups(HttpServletRequest request, HttpServletResponse response) {
        String selectedUniversity = request.getParameter("univername");
        String selectedFaculty = request.getParameter("facultyname");
        try {
            PrintWriter wr = response.getWriter();
            for (Group group : personService.findAllGroupsByFacultyNameAndUniversityName(selectedFaculty, selectedUniversity)) {
                wr.println(group.getGroupName());
            }
            wr.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Returns counts of unread messages and unconfirmed friends
     *
     * @param response with response to view-page
     */
    @RequestMapping(value = "/getunreadunconfirmedcount", method = RequestMethod.GET)
    @ResponseBody
    public void sentUnreadAndUnconfirmedCount(HttpServletResponse response) {
        Person person = personService.findPersonByEmail(getUserEmail());
        try {
            PrintWriter out = response.getWriter();
            out.println(person.getUnreadMessages().size());
            out.println(person.getUnconfirmedFriends().size());
            out.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Gets Person from registration form, validates it, and if it's alright, puts person into WaitingList with it's code
     * waiting for registration confirm and sends letter with link on email to confirm registration
     *
     * @param person  filled from registration form
     * @param result  with errors in registration
     * @param request with parametrs for view-page
     * @param model   map with parametrs for view-page
     * @return registration page with propose to confirm registration via email or with error messages
     */
    @RequestMapping(value = "/registration", method = RequestMethod.POST)
    public String putPersonIntoWaitingForConfirmList(@ModelAttribute("person") Person person, BindingResult result, HttpServletRequest request, Map model) {
        PersonValidatorForRegistration personValidatorForRegistration = new PersonValidatorForRegistration();
        personValidatorForRegistration.validate(person, result);
        if (result.hasErrors()) {
            if (!request.getParameter("cpassword").equals(person.getPassword())) {
                String error = "Your confirm password doesn't match with password";
                request.setAttribute("error", error);
            }
            request.setAttribute("person", person);
            putFacultiesAndUniversities(model);
            return "register";
        }
        if (personService.findPersonByEmail(person.getEmail()) != null) {
            if (!request.getParameter("cpassword").equals(person.getPassword())) {
                String error = "Your confirm password doesn't match with password";
                request.setAttribute("error", error);
            }
            String msg = "This email has been already registered";
            request.setAttribute("msg", msg);
            request.setAttribute("person", person);
            putFacultiesAndUniversities(model);
            return "register";
        }
        if (!request.getParameter("cpassword").equals(person.getPassword())) {
            String error = "Your confirm password doesn't match with password";
            request.setAttribute("error", error);
            putFacultiesAndUniversities(model);
            return "register";
        }
        VerificateCodeGenerator codeGenerator = new VerificateCodeGenerator();
        MailManager mail = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getSession().getServletContext()).getBean(MailManager.class);
        String code = codeGenerator.generateCode();
        String url = "http://" + request.getServerName() + ":" + request.getServerPort() + request.getRequestURI() + "?code=" + code;
        try {
            mail.sendEmailMessage(person.getEmail(), url);
            logger.info("Confirm letter with code=" + code + " was sent to " + person.getEmail());
            personsWaitingForConfirm.put(code, person);
            model.put("person", new Person());
            request.setAttribute("msg", "We sent you mail with instructions for registration confirm. Please, visit your email.");
            putFacultiesAndUniversities(model);
            return "register";
        } catch (MessagingException e) {
            logger.error(e.getMessage(), e);
            request.setAttribute("msg", "There are troubles with mail-server.");
            putFacultiesAndUniversities(model);
            return "register";
        }
    }

    /**
     * Looks for code from request parametr in PersonWaitingList and saves person in database, if code in there,
     * or redirects to registration page
     *
     * @param request with parametrs for view-page
     * @return registration confirm page if code contains in PersonWaitingList or registration page if not
     */
    @RequestMapping(value = "/registration", method = RequestMethod.GET)
    public String confirmRegistration(HttpServletRequest request) {
        String code = request.getParameter("code");
        if ((code != null) && (personsWaitingForConfirm.containsKey(code))) {
            Person person = personsWaitingForConfirm.get(code);
            personsWaitingForConfirm.remove(code);
            personService.createPerson(person);
            return "regconfirm";
        }
        return "redirect:registerform";
    }

    /**
     * Redirects to person's profile edit page
     *
     * @param request with parametrs for view-page
     * @param model   map with parametrs for view-page
     * @return edit profile page
     */
    @RequestMapping(value = "/goedit")
    public String redirectingToEditProfile(HttpServletRequest request, Map model) {
        model.put("person", personService.findPersonByEmail(getUserEmail()));
        if (request.getParameter("msg") != null) {
            model.put("msg", request.getParameter("msg"));
        }
        putFacultiesAndUniversities(model);
        return "edit";
    }

    /**
     * Gets person from edit profile page, validates it, and if it's alright saves changes,
     * if not - returns to edit page with error massages
     *
     * @param person  filled from edit profile page
     * @param result  with errors from edit profile page
     * @param request with parametrs for view-page
     * @param model   map with parametrs for view-page
     * @return edit profile page with confirm saving changes massage or error messages, if there were errors
     */
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public String editProfile(@ModelAttribute("person") Person person, BindingResult result, HttpServletRequest request, Map model) {
        PersonValidatorForEditProfile personValidator = new PersonValidatorForEditProfile();
        personValidator.validate(person, result);
        Person oldPerson = personService.findPersonByEmail(getUserEmail());
        person.setPersonId(oldPerson.getPersonId());
        person.setEmail(oldPerson.getEmail());
        person.setAvatar(oldPerson.getAvatar());
        person.setRating(oldPerson.getRating());
        if (result.hasErrors()) {
            if ((!person.getPassword().isEmpty()) && (!request.getParameter("cpassword").isEmpty()) && (!request.getParameter("cpassword").equals(person.getPassword()))) {
                String confirmError = "Your confirm password doesn't match with password";
                request.setAttribute("confirmError", confirmError);
            }
            if (!request.getParameter("oldpassword").equals(oldPerson.getPassword())) {
                String oldPassError = "Your old password is incorrect";
                request.setAttribute("oldPassError", oldPassError);
            }
            request.setAttribute("person", person);
            putFacultiesAndUniversities(model);
            return "edit";
        }
        if ((!person.getPassword().isEmpty()) && (!request.getParameter("oldpassword").equals(oldPerson.getPassword()))) {
            String oldPassError = "Your new password doesn't match with old password";
            request.setAttribute("oldPassError", oldPassError);
            if ((!person.getPassword().isEmpty()) && (!request.getParameter("cpassword").isEmpty()) && (!request.getParameter("cpassword").equals(person.getPassword()))) {
                String confirmError = "Your confirm password doesn't match with password";
                request.setAttribute("confirmError", confirmError);
                request.setAttribute("person", person);
                putFacultiesAndUniversities(model);
                return "edit";
            }
        }
        if ((!person.getPassword().isEmpty()) && (!request.getParameter("cpassword").isEmpty()) && (!request.getParameter("cpassword").equals(person.getPassword()))) {
            String confirmError = "Your confirm password doesn't match with password";
            request.setAttribute("confirmError", confirmError);
            request.setAttribute("person", person);
            putFacultiesAndUniversities(model);
            return "edit";
        }
        if (person.getPassword().isEmpty()) {
            person.setPassword(oldPerson.getPassword());
        }
        personService.updatePerson(person);
        request.setAttribute("msg", "Changes have been saved");
        request.setAttribute("person", person);
        putFacultiesAndUniversities(model);
        return "edit";
    }

    /**
     * Gets file from edit profile page, and if file type is "jpeg", uploads and puts as person's avatar
     *
     * @param file    from edit profile page
     * @param request with parametrs for view-page
     * @param model   map with parametrs for view-page
     * @return edit page with confirm saving changes message or file upload error message
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String uploadPhoto(@RequestParam("data") MultipartFile file, HttpServletRequest request, Map model) {
        Person oldPerson = personService.findPersonByEmail(getUserEmail());
        File outputFile = new File("src/main/webapp/resources/img/avatars/" + oldPerson.getPersonId() + ".jpg");
        if (file != null) {
            try {
                if (file.getContentType().equals("image/jpeg")) {
                    InputStream in = file.getInputStream();
                    FileOutputStream out = new FileOutputStream(outputFile);
                    if (outputFile.exists()) {
                        outputFile.delete();
                        outputFile.createNewFile();
                    } else {
                        outputFile.createNewFile();
                    }
                    while (in.available() > 0) {
                        out.write(in.read());
                    }
                    oldPerson.setAvatar(String.valueOf(oldPerson.getPersonId()));
                    personService.updatePerson(oldPerson);
                    in.close();
                    out.close();
                } else {
                    request.setAttribute("msg", "You chose incorrect image");
                    request.setAttribute("person", oldPerson);
                    putFacultiesAndUniversities(model);
                    return "edit";
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        request.setAttribute("msg", "Your avatar has been changed");
        request.setAttribute("person", oldPerson);
        putFacultiesAndUniversities(model);
        return "edit";
    }
}
