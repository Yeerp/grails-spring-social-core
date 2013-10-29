/* Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugins.springsocial

import grails.plugins.springsocial.connect.web.GrailsConnectSupport
import grails.plugins.springsocial.signin.SpringSocialSimpleSignInAdapter
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.social.connect.Connection
import org.springframework.social.connect.web.ProviderSignInAttempt
import org.springframework.util.Assert
import org.springframework.web.context.request.RequestAttributes

class SpringSocialProviderSignInController {
  def connectionFactoryLocator
  def connectionRepository
  def signInService
  def usersConnectionRepository
  def requestCache
  GrailsConnectSupport webSupport = new GrailsConnectSupport(mapping: "springSocialSignIn")
  static allowedMethods = [signin: 'POST', oauthCallback: 'GET', disconnect: 'DELETE']

  def signin = {
    def providerId = params.providerId

    Assert.hasText(providerId, "The providerId is required")

    def connectionFactory = connectionFactoryLocator.getConnectionFactory(providerId)
    def nativeWebRequest = new GrailsWebRequest(request, response, servletContext)
    def url = webSupport.buildOAuthUrl(connectionFactory, nativeWebRequest)
    redirect url: url
  }

  def oauthCallback = {
    def providerId = params.providerId

    Assert.hasText(providerId, "The providerId is required")

    def nativeWebRequest = new GrailsWebRequest(request, response, servletContext)
    def config = SpringSocialUtils.config.get(providerId)

    def connectionFactory = connectionFactoryLocator.getConnectionFactory(providerId);
    def connection = webSupport.completeConnection(connectionFactory, nativeWebRequest);
    def url = handleSignIn(connection, nativeWebRequest, config);
    redirect url: url
  }

  private String handleSignIn(Connection connection, GrailsWebRequest request, config) {
    String result
    List<String> userIds = usersConnectionRepository.findUserIdsWithConnection(connection);
    if (userIds.size() == 0) {
      if (log.isDebugEnabled()) {
        log.debug("No user found in the repository, creating a new one...")
      }
      ProviderSignInAttempt signInAttempt = new ProviderSignInAttempt(connection, connectionFactoryLocator, usersConnectionRepository);
      request.setAttribute(ProviderSignInAttempt.SESSION_ATTRIBUTE, signInAttempt, RequestAttributes.SCOPE_SESSION)
      //TODO: Document this setting
      result = request.session.ss_oauth_redirect_on_signIn_attempt ?: config.page.handleSignIn
    } else if (userIds.size() == 1) {
      if (log.isDebugEnabled()) {
        log.debug("User found in the repository...")
      }
      usersConnectionRepository.createConnectionRepository(userIds.get(0)).updateConnection(connection)
      def signInAdapter = new SpringSocialSimpleSignInAdapter(requestCache)
      def originalUrl = signInAdapter.signIn(userIds.get(0), connection, request)
      if (log.isDebugEnabled()) {
        log.debug("originalUrl: ${originalUrl}")
      }
      //TODO: Document this setting
      result = originalUrl ?: config.postSignInUrl
    } else {
      if (log.isErrorEnabled()) {
        log.error("Multiple Users found in the repository...")
      }
      //TODO: handle redirect when multiple users found
      //result = redirect(URIBuilder.fromUri(signInUrl).queryParam("error", "multiple_users").build().toString());
    }
    result
  }
}