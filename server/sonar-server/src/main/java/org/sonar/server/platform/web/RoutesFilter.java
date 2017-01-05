/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.web;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.lang.String.format;

public class RoutesFilter implements Filter {

  private static final String EMPTY = "";
  private static final String BATCH_WS = "/batch";
  private static final String API_SOURCES_WS = "/api/sources";

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    String path = request.getRequestURI().replaceFirst(request.getContextPath(), EMPTY);
    if (path.startsWith(BATCH_WS + "/") && path.endsWith(".jar")) {
      // Old scanners were using /batch/file.jar url (see SCANNERAPI-167)
      response.sendRedirect(format("%s%s/file?name=%s", request.getContextPath(), BATCH_WS, path.replace(BATCH_WS + "/", EMPTY)));
    } else if ("/batch_bootstrap/index".equals(path)) {
      // Old scanners were using /batch_bootstrap url (see SCANNERAPI-167)
      response.sendRedirect(format("%s%s/index", request.getContextPath(), BATCH_WS));
    } else if (API_SOURCES_WS.equals(path)) {
      // SONAR-7852 /api/sources?resource url is still used
      response.sendRedirect(format("%s%s/index?%s", request.getContextPath(), API_SOURCES_WS, request.getQueryString()));
    } else if (!path.equals("/api/properties/index") && path.startsWith("/api/properties")) {
      // api/properties is used in past version of SonarLint
      List<String> parameters = new ArrayList<>();
      String queryString = request.getQueryString();
      if (queryString != null) {
        parameters.add(queryString);
      }
      String key = path.replace("/api/properties", "");
      if (!key.isEmpty()) {
        parameters.add(String.format("key=%s", key.replaceFirst("/", "")));
      }
      if (parameters.isEmpty()) {
        response.sendRedirect(format("%s%s/index", request.getContextPath(), "/api/properties"));
      } else {
        response.sendRedirect(format("%s%s/index?%s", request.getContextPath(), "/api/properties", Joiner.on("&").join(parameters)));
      }
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing
  }

  @Override
  public void destroy() {
    // Nothing
  }
}
