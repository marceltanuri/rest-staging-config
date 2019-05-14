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
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;

@Controller
@RequestMapping("/staging-target")
public class RESTController {

	@Autowired
	Authentication authentication;

	@Autowired
	SecurityUtil securityUtil;

	private static Log _log = LogFactoryUtil.getLog(RESTController.class);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping(method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> updateStagingRemoteAddres(HttpServletRequest request,
			HttpServletResponse response) {

		_log.info(
				"Recieved request from: " + securityUtil.getIPAddress(request) + " for updating staging remote address.");
		_log.info("Request params: " + securityUtil.getRequestParameters(request));

		if (!authentication.isAuthenticated(request)) {
			String msg = "The request was reject due to invalid authentication";
			_log.info(msg);
			return new ResponseEntity<String>(msg, HttpStatus.UNAUTHORIZED);
		}

		try {
			String groupId = request.getParameter("groupId");
			String target = request.getParameter("target");

			if (groupId == null || groupId.isEmpty() || target == null || target.isEmpty()) {
				String msg = "The request was reject due to bad request";
				_log.info(msg);
				return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
			}
			
			if(!securityUtil.isValidTargetHost(target)) {
				String msg = "The request was reject. It is not a valid target host. Check the rest-staging-config project for more details (readme)";
				_log.info(msg);
				return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
			}

			Group group = GroupLocalServiceUtil.getGroup(Long.valueOf(groupId));
			String[] typeSettings = group.getTypeSettings().split(System.getProperty("line.separator"));

			for (String typeSetting : typeSettings) {
				if (typeSetting.startsWith("remoteAddress=")) {

					String newSettings = group.getTypeSettings().replace(typeSetting,
							"remoteAddress=" + target);
					
					group.setTypeSettings(newSettings);
					
					GroupLocalServiceUtil.updateGroup(group);
					
					CacheRegistryUtil.clear();
					
					_log.info("Staging remote address has been updated! The request was sent from: "
							+ securityUtil.getIPAddress(request) + ".");
					break;
				}
			}
			
			return new ResponseEntity(group.getTypeSettings(), HttpStatus.OK);
			
		} catch (Exception e) {
			String msg = "The request was reject due to bad request"+ ": " + e.getMessage();
			_log.info(msg);
			return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
		}
	}
}