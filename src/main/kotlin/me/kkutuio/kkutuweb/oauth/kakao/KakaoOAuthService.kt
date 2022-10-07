/*
 * KKuTu-Web (https://github.com/KKuTuIO/KKuTu-Web)
 * Copyright (C) 2021 KKuTuIO <admin@kkutu.io>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.kkutuio.kkutuweb.oauth.kakao

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Verb
import me.kkutuio.kkutuweb.oauth.AuthVendor
import me.kkutuio.kkutuweb.oauth.OAuthService
import me.kkutuio.kkutuweb.oauth.OAuthUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class KakaoOAuthService(
    @Autowired private val objectMapper: ObjectMapper
) : OAuthService() {
    private val protectedResourceUrl = "https://kapi.kakao.com/v2/user/me"

    override fun init(apiKey: String, apiSecret: String, callbackUrl: String, _allowRegister: Boolean) {
        allowRegister = _allowRegister
        oAuth20Service = ServiceBuilder(apiKey)
            .callback(callbackUrl)
            .userAgent("KKuTu-Web (https://github.com/horyu1234/KKuTu-Web)")
            .build(KakaoApi)
    }

    override fun login(code: String): OAuthUser {
        val accessToken = oAuth20Service.getAccessToken(code)

        val request = OAuthRequest(Verb.GET, protectedResourceUrl)
        oAuth20Service.signRequest(accessToken, request)

        val response = oAuth20Service.execute(request)
        val jsonResponse = objectMapper.readTree(response.body)

        return OAuthUser(
            authVendor = AuthVendor.KAKAO,
            vendorId = jsonResponse["id"].longValue().toString(),
            name = jsonResponse["properties"]["nickname"].textValue(),
            profileImage = if (jsonResponse["properties"].has("profile_image")) jsonResponse["properties"]["profile_image"].textValue() else null,
            gender = null,
            minAge = null,
            maxAge = null
        )
    }
}