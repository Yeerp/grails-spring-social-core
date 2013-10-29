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
package grails.plugins.springsocial.config.core

import javax.inject.Inject
import javax.sql.DataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.social.connect.ConnectionFactoryLocator
import org.springframework.social.connect.ConnectionRepository
import org.springframework.social.connect.UsersConnectionRepository
import org.springframework.social.connect.jdbc.JdbcUsersConnectionRepository
import org.springframework.social.connect.support.ConnectionFactoryRegistry

@Configuration
class SpringSocialCoreConfig {
  @Inject
  DataSource dataSource

  @Bean
  TextEncryptor textEncryptor() {
    Encryptors.noOpText()
  }

  @Bean
  @Scope(value = "singleton")
  ConnectionFactoryLocator connectionFactoryLocator() {
    new ConnectionFactoryRegistry()
  }

  @Bean
  @Scope(value = "singleton", proxyMode = ScopedProxyMode.INTERFACES)
  UsersConnectionRepository usersConnectionRepository() {
    new JdbcUsersConnectionRepository(dataSource, connectionFactoryLocator(), textEncryptor())
  }

  @Bean
  @Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)

  ConnectionRepository connectionRepository() {
    def authentication = SecurityContextHolder.getContext().getAuthentication()
    if (!authentication) {
      throw new IllegalStateException("Unable to get a ConnectionRepository: no user signed in")
    }
    usersConnectionRepository().createConnectionRepository(authentication.getName())
  }

}