/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package br.com.mtanuri.liferay.rest.stagingconfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.liferay.portal.kernel.cache.CacheRegistryUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;

@Controller
@RequestMapping("/staging-target")
public class RESTController {
	
	@Autowired
	Authentication authentication;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping(method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> getShopInJSON(HttpServletRequest request,
            HttpServletResponse response) {
		
		if(!authentication.isAuthenticated(request)) {
			return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
		}

		try {
			String groupId = request.getParameter("groupId");
			String target = request.getParameter("target");
			Group group = GroupLocalServiceUtil.getGroup(Long.valueOf(groupId));
			String[] typeSettings = group.getTypeSettings().split(System.getProperty("line.separator"));
			
			for (String typeSetting : typeSettings) {
				if (typeSetting.startsWith("remoteAddress")) {
					group.setTypeSettings(group.getTypeSettings().replace(typeSetting, typeSetting.split("=")[0] + "=" + target));
					GroupLocalServiceUtil.updateGroup(group);
					CacheRegistryUtil.clear();
					break;
				}
			}
			return new ResponseEntity(group.getTypeSettings(), HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}

	}

}