/* ====================================================================
 *
 * Copyright (C) 2007 - 2012 GeoSolutions S.A.S.
 * http://www.geo-solutions.it
 *
 * GPLv3 + Classpath exception
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. 
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by developers
 * of GeoSolutions.  For more information on GeoSolutions, please see
 * <http://www.geo-solutions.it/>.
 *
 */
package it.geosolutions.fra2015.services.rest.impl;

import it.geosolutions.fra2015.server.model.user.User;
import it.geosolutions.fra2015.services.UserService;
import it.geosolutions.fra2015.services.exception.BadRequestServiceEx;
import it.geosolutions.fra2015.services.exception.NotFoundServiceEx;
import it.geosolutions.fra2015.services.rest.RESTUserService;
import it.geosolutions.fra2015.services.rest.exception.BadRequestWebEx;
import it.geosolutions.fra2015.services.rest.exception.InternalErrorWebEx;
import it.geosolutions.fra2015.services.rest.exception.NotFoundWebEx;
import it.geosolutions.fra2015.services.rest.model.RESTUser;
import it.geosolutions.fra2015.services.rest.model.UserList;
import it.geosolutions.fra2015.services.rest.utils.Fra2015Principal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.SecurityContext;
import org.apache.log4j.Logger;

/**
 * Class RESTUserServiceImpl.
 *
 * @author Tobia di Pisa (tobia.dipisa at geo-solutions.it)
 * @author Emanuele Tajariol (etj at geo-solutions.it)
 *
 */
public class RESTUserServiceImpl implements RESTUserService {

    private final static Logger LOGGER = Logger.getLogger(RESTUserServiceImpl.class);
    private UserService userService;

    /**
     * @param userService the userService to set
     */
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /*
     * (non-Javadoc) @see
     * it.geosolutions.fra2015.services.rest.RESTUserInterface#insert(it.geosolutions.fra2015.server.model.user.User)
     */
    @Override
    public User insert(SecurityContext sc, User user) {

        try {

            return userService.insert(user);

        } catch (NotFoundServiceEx e) {
            throw new NotFoundWebEx(e.getMessage());
        } catch (BadRequestServiceEx e) {
            throw new BadRequestWebEx(e.getMessage());
        }

    }

    /*
     * (non-Javadoc) @see it.geosolutions.fra2015.services.rest.RESTUserInterface#update(long,
     * it.geosolutions.fra2015.server.model.user.User)
     */
    @Override
    public User update(SecurityContext sc, long id, User user) {
        try {

            return userService.update(user);

        } catch (NotFoundServiceEx e) {
            throw new NotFoundWebEx(e.getMessage());
        } catch (BadRequestServiceEx e) {
            throw new BadRequestWebEx(e.getMessage());
        }

    }

    /**
     *
     * @param sc
     * @param id
     */
    @Override
    public void delete(SecurityContext sc, long id) {
        userService.deleteById(id);
    }

    /*
     * (non-Javadoc) @see it.geosolutions.fra2015.services.rest.RESTUserInterface#get(long)
     */
    @Override
    public User get(SecurityContext sc, long id) throws NotFoundWebEx {
        if (id == -1) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Retriving dummy data !");
            }

            //
            // return test instance
            //
            User user = new User();
            user.setName("dummy name");
            return user;
        }

        User ret = userService.get(id);
        if (ret == null) {
            throw new NotFoundWebEx("User not found");
        }

        return ret;
    }

    /*
     * (non-Javadoc) @see it.geosolutions.fra2015.services.rest.RESTUserService#get(java.lang.String)
     */
    @Override
    public User get(SecurityContext sc, String name) throws NotFoundWebEx {
        if (name == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("User Name is null !");
            }
            throw new BadRequestWebEx("User name is null");
        }

        User ret;
        try {
            ret = userService.get(name);
        } catch (NotFoundServiceEx e) {
            throw new NotFoundWebEx("User not found");
        }

        return ret;
    }

    /*
     * (non-Javadoc) @see it.geosolutions.fra2015.services.rest.RESTUserInterface#getAll(java.lang.Integer, java.lang.Integer)
     */
    @Override
    public UserList getAll(SecurityContext sc, Integer page, Integer entries) throws BadRequestWebEx {
        try {
            List<User> userList = userService.getAll(page, entries);
            Iterator<User> iterator = userList.iterator();

            List<RESTUser> restUSERList = new ArrayList<RESTUser>();
            while (iterator.hasNext()) {
                User user = iterator.next();

                RESTUser restUser = new RESTUser(user.getId(), user.getName());
                restUSERList.add(restUser);
            }

            return new UserList(restUSERList);
        } catch (BadRequestServiceEx ex) {
            throw new BadRequestWebEx(ex.getMessage());
        }
    }

    /*
     * (non-Javadoc) @see it.geosolutions.fra2015.services.rest.RESTUserInterface#getCount(java.lang.String)
     */
    @Override
    public long getCount(SecurityContext sc, String nameLike) {
        nameLike = nameLike.replaceAll("[*]", "%");
        return userService.getCount(nameLike);
    }

    /*
     * (non-Javadoc)
     *
     * @see it.geosolutions.fra2015.services.rest.RESTUserService#getAuthUserDetails (javax.ws.rs.core.SecurityContext)
     */
    @Override
    public User getAuthUserDetails(SecurityContext sc) {
        User authUser = extractAuthUser(sc);

        User ret = null;
        try {
            authUser = userService.get(authUser.getName());

            if (authUser != null) {
                ret = new User();
                ret.setId(authUser.getId());
                ret.setName(authUser.getName());
            }

        } catch (NotFoundServiceEx e) {
            throw new NotFoundWebEx("User not found");
        }

        return ret;
    }

    /**
     * @return User - The authenticated user that is accessing this service, or
     * null if guest access.
     */
    private User extractAuthUser(SecurityContext sc) throws InternalErrorWebEx {
        if (sc == null) {
            throw new InternalErrorWebEx("Missing auth info");
        } else {
            Principal principal = sc.getUserPrincipal();
            if (principal == null) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Missing auth principal");
                }
                throw new InternalErrorWebEx("Missing auth principal");
            }

            if (!(principal instanceof Fra2015Principal)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Mismatching auth principal");
                }
                throw new InternalErrorWebEx("Mismatching auth principal ("
                        + principal.getClass() + ")");
            }

            Fra2015Principal gsp = (Fra2015Principal) principal;

            //
            // may be null if guest
            //
            User user = gsp.getUser();

            LOGGER.info("Accessing service with user "
                    + (user == null ? "GUEST" : user.getName()));
            return user;
        }
    }
}
