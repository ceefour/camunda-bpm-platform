/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.run;

import org.apache.catalina.filters.CorsFilter;
import org.camunda.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.camunda.bpm.run.property.CamundaBpmRunAuthenticationProperties;
import org.camunda.bpm.run.property.CamundaBpmRunCorsProperty;
import org.camunda.bpm.run.property.CamundaBpmRunProperties;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;

@EnableConfigurationProperties(CamundaBpmRunProperties.class)
@Configuration
@AutoConfigureAfter({ CamundaBpmAutoConfiguration.class })
@ConditionalOnClass(CamundaBpmRestInitializer.class)
public class CamundaBpmRunRestConfiguration {

  @Autowired
  CamundaBpmRunProperties camundaBpmRunProperties;

  @Bean
  @ConditionalOnProperty(name = "enabled", havingValue = "true", prefix = CamundaBpmRunAuthenticationProperties.PREFIX)
  public FilterRegistrationBean<Filter> processEngineAuthenticationFilter(JerseyApplicationPath applicationPath) {
    FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
    registration.setName("camunda-auth");
    registration.setFilter(new ProcessEngineAuthenticationFilter());

    String restApiPathPattern = applicationPath.getUrlMapping();
    registration.addUrlPatterns(restApiPathPattern);

    // if nothing is set, use Http Basic authentication
    CamundaBpmRunAuthenticationProperties properties = camundaBpmRunProperties.getAuth();
    if (properties.getAuthentication() == null || CamundaBpmRunAuthenticationProperties.DEFAULT_AUTH.equals(properties.getAuthentication())) {
      registration.addInitParameter("authentication-provider", "org.camunda.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider");
    }
    return registration;
  }

  @Bean
  @ConditionalOnProperty(name = "enabled", havingValue = "true", prefix = CamundaBpmRunCorsProperty.PREFIX)
  public FilterRegistrationBean<Filter> corsFilter(JerseyApplicationPath applicationPath) {
    FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
    registration.setName("camunda-cors");
    CorsFilter corsFilter = new CorsFilter();
    registration.setFilter(corsFilter);

    String restApiPathPattern = applicationPath.getUrlMapping();
    registration.addUrlPatterns(restApiPathPattern);

    registration.addInitParameter(CorsFilter.PARAM_CORS_ALLOWED_ORIGINS, camundaBpmRunProperties.getCors().getAllowedOrigins());
    if (!"*".equals(camundaBpmRunProperties.getCors().getAllowedOrigins())) {
      registration.addInitParameter(CorsFilter.PARAM_CORS_SUPPORT_CREDENTIALS, "true");
      registration.addInitParameter(CorsFilter.PARAM_CORS_ALLOWED_HEADERS,
        "Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization,X-XSRF-Token,X-CSRF-Token");
    }
    return registration;
  }

}
