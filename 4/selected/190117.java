package com.shekhar.moviestore.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.apache.commons.io.IOUtils;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;
import com.shekhar.moviestore.domain.Movie;

@RequestMapping("/movies")
@Controller
@RooWebScaffold(path = "movies", formBackingObject = Movie.class)
public class MovieController {

    private static final String STORAGE_PATH = System.getProperty("OPENSHIFT_DATA_DIR") == null ? "/home/shekhar/tmp/" : System.getProperty("OPENSHIFT_DATA_DIR");

    @RequestMapping(params = "form", produces = "text/html")
    public String createForm(Model uiModel) {
        populateEditForm(uiModel, new Movie());
        return "movies/create";
    }

    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String create(@Valid Movie movie, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, movie);
            return "movies/create";
        }
        CommonsMultipartFile multipartFile = movie.getFile();
        String orgName = multipartFile.getOriginalFilename();
        uiModel.asMap().clear();
        System.out.println(orgName);
        String[] split = orgName.split("\\.");
        movie.setFileName(split[0]);
        movie.persist();
        String filePath = STORAGE_PATH + orgName;
        File dest = new File(filePath);
        try {
            multipartFile.transferTo(dest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "redirect:/movies/" + encodeUrlPathSegment(movie.getId().toString(), httpServletRequest);
    }

    @RequestMapping(value = "/image/{fileName}", method = RequestMethod.GET)
    public void getImage(@PathVariable String fileName, HttpServletRequest req, HttpServletResponse res) throws Exception {
        File file = new File(STORAGE_PATH + fileName + ".jpg");
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Pragma", "no-cache");
        res.setDateHeader("Expires", 0);
        res.setContentType("image/jpg");
        ServletOutputStream ostream = res.getOutputStream();
        IOUtils.copy(new FileInputStream(file), ostream);
        ostream.flush();
        ostream.close();
    }

    @RequestMapping(value = "/{id}", produces = "text/html")
    public String show(@PathVariable("id") Long id, Model uiModel) {
        uiModel.addAttribute("movie", Movie.findMovie(id));
        uiModel.addAttribute("itemId", id);
        return "movies/show";
    }

    @RequestMapping(produces = "text/html")
    public String list(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel) {
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
            uiModel.addAttribute("movies", Movie.findMovieEntries(firstResult, sizeNo));
            float nrOfPages = (float) Movie.countMovies() / sizeNo;
            uiModel.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
            uiModel.addAttribute("movies", Movie.findAllMovies());
        }
        return "movies/list";
    }

    @RequestMapping(method = RequestMethod.PUT, produces = "text/html")
    public String update(@Valid Movie movie, BindingResult bindingResult, Model uiModel, HttpServletRequest httpServletRequest) {
        if (bindingResult.hasErrors()) {
            populateEditForm(uiModel, movie);
            return "movies/update";
        }
        uiModel.asMap().clear();
        movie.merge();
        return "redirect:/movies/" + encodeUrlPathSegment(movie.getId().toString(), httpServletRequest);
    }

    @RequestMapping(value = "/{id}", params = "form", produces = "text/html")
    public String updateForm(@PathVariable("id") Long id, Model uiModel) {
        populateEditForm(uiModel, Movie.findMovie(id));
        return "movies/update";
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = "text/html")
    public String delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, Model uiModel) {
        Movie movie = Movie.findMovie(id);
        movie.remove();
        uiModel.asMap().clear();
        uiModel.addAttribute("page", (page == null) ? "1" : page.toString());
        uiModel.addAttribute("size", (size == null) ? "10" : size.toString());
        return "redirect:/movies";
    }

    void populateEditForm(Model uiModel, Movie movie) {
        uiModel.addAttribute("movie", movie);
    }

    String encodeUrlPathSegment(String pathSegment, HttpServletRequest httpServletRequest) {
        String enc = httpServletRequest.getCharacterEncoding();
        if (enc == null) {
            enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
        try {
            pathSegment = UriUtils.encodePathSegment(pathSegment, enc);
        } catch (UnsupportedEncodingException uee) {
        }
        return pathSegment;
    }
}
