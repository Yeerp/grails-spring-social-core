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
package grails.plugins.springsocial.signin

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import org.springframework.security.web.WebAttributes
import org.springframework.security.web.savedrequest.RequestCache
import org.springframework.social.connect.Connection
import org.springframework.social.connect.web.SignInAdapter
import org.springframework.web.context.request.NativeWebRequest

class SpringSocialSimpleSignInAdapter implements SignInAdapter {
  private RequestCache requestCache

  SpringSocialSimpleSignInAdapter(RequestCache requestCache) {
    this.requestCache = requestCache;
  }

  String signIn(String localUserId, Connection<?> connection, NativeWebRequest request) {
    SignInUtils.signin localUserId
    extractOriginalUrl request
  }

  private String extractOriginalUrl(NativeWebRequest request) {
    def result = null
    def nativeReq = request.getNativeRequest(HttpServletRequest)
    def nativeRes = request.getNativeResponse(HttpServletResponse)
    def saved = requestCache.getRequest(nativeReq, nativeRes)

    if (saved) {
      requestCache.removeRequest(nativeReq, nativeRes)
      removeAutheticationAttributes(nativeReq.getSession(false))
      result = saved.getRedirectUrl()
    }

    result
  }

  private void removeAutheticationAttributes(HttpSession session) {
    if (session) {
      session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)
    }
  }
}