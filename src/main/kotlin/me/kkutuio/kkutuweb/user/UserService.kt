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

package me.kkutuio.kkutuweb.user

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import me.kkutuio.kkutuweb.extension.getOAuthUser
import me.kkutuio.kkutuweb.extension.isGuest
import me.kkutuio.kkutuweb.extension.toJson
import me.kkutuio.kkutuweb.shop.ShopDao
import me.kkutuio.kkutuweb.shop.ShopService
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.servlet.http.HttpSession
import kotlin.math.roundToInt

private val AVAIL_EQUIP = listOf(
    "NIK", "BDG1", "BDG2", "BDG3", "BDG4",
    "Mhead", "Meye", "Mmouth", "Mhand", "Mclothes", "Mshoes", "Mback"
)

@Service
class UserService(
    @Autowired private val userDao: UserDao,
    @Autowired private val shopDao: ShopDao,
    @Autowired private val shopService: ShopService
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    private val similarityRegex = "[-_ ]*".toRegex()

    fun getBox(session: HttpSession): String {
        if (session.isGuest()) return "{\"error\":400}"
        val oAuthUser = session.getOAuthUser()

        val userId = oAuthUser.getUserId()
        val user = userDao.getUser(userId) ?: return "{\"error\":400}"

        return user.box.toJson()
    }

    fun exordial(data: String, session: HttpSession): String {
        val maxExordialLength = 100
        if (session.isGuest()) return "{\"error\":400}"

        val oAuthUser = session.getOAuthUser()
        val userId = oAuthUser.getUserId()

        val resultData =
            data.substring(0, if (data.length > maxExordialLength) maxExordialLength else data.length).trim()

        userDao.updateUser(
            userId, mapOf(
                "exordial" to if (resultData.isEmpty()) null else resultData
            )
        )

        logger.info("$userId 님이 프로필을 수정했습니다. 소개 한마디: $resultData")
        return "{\"text\":\"$resultData\"}"
    }

    fun equip(id: String, isLeft: Boolean, session: HttpSession): String {
        return "{\"error\":555}"
        /* 기존 코드, 시스템 개선으로 인해 장착은 게임서버에서 처리합니다.
        if (session.isGuest()) return "{\"error\":400}"
        val oAuthUser = session.getOAuthUser()

        val userId = oAuthUser.getUserId()
        val user = userDao.getUser(userId) ?: return "{\"error\":400}"

        val good = shopDao.getGood(id) ?: return "{\"error\":430}"
        if (!AVAIL_EQUIP.contains(good.group)) return "{\"error\":400}"

        var part = good.group
        if (part.substring(0, 3) == "BDG") part = "BDG"
        else if (part == "Mhand") {
            part = if (isLeft) "Mlhand" else "Mrhand"
            val equipingGood = user.box.get(id)
            if(user.equip.has(part)) {
                val isUnequip = user.equip.has(part) && user.equip.get(part).toString() == id
                if (isUnequip) {
                    // 장착 해제
                } else {
                    try {
                        val equipingGoodChecker = equipingGood["value"].intValue() == 0 && equipingGood.intValue() <= 0
                        if (equipingGoodChecker) return "{\"error\":439}"
                    } catch(e: Exception) {
                        if (equipingGood["value"].intValue() <= 0) return "{\"error\":439}"
                    }
                }
            }
        }

        val equip: JsonNode = user.equip
        if (equip.has(part)) {
            val equipingGood = user.box.get(id)
            if (equipingGood != null && (equipingGood.has("expire") && equipingGood["expire"].intValue() > 0)) {
                var expire = equipingGood["expire"].intValue()
                if (expire == 2147483647) expire = 0
                shopService.obtainGood(
                    user.box,
                    equip.get(part).textValue(),
                    1,
                    expire,
                    true
                )
            } else {
                val currentTime = (System.currentTimeMillis() * 0.001).roundToInt()
                shopService.obtainGood(user.box, equip.get(part).textValue(), 1, currentTime + good.term, true)
            }
        }

        val equipObjectNode = equip as ObjectNode
        if (equip.has(part) && equip.get(part).textValue() == good.id) {
            equipObjectNode.remove(part)
        } else {
            if (!user.box.has(good.id)) return "{\"error\":430}"
            shopService.consumeGood(user.box, good.id, 1)
            equipObjectNode.put(part, good.id)
        }

        val userBoxJsonObj = PGobject()
        userBoxJsonObj.type = "json"
        userBoxJsonObj.value = user.box.toJson()

        val userEquipJsonObj = PGobject()
        userEquipJsonObj.type = "json"
        userEquipJsonObj.value = user.equip.toJson()

        userDao.updateUser(
            user.id, mapOf(
                "box" to userBoxJsonObj,
                "equip" to userEquipJsonObj
            )
        )
        return "{\"result\":200,\"box\":${user.box.toJson()},\"equip\":${user.equip.toJson()}}"
        */
    }

    fun getUserData(id: String): String {
        val user = userDao.getUser(id) ?: return "{\"error\":405}"
        return "{\"result\":200,\"id\":\"${user.id}\",\"data\":${user.kkutu.toJson()},\"equip\":${user.equip.toJson()},\"exordial\":\"${user.exordial ?: ""}\",\"profile\":{\"authtype\":\"offline\",\"id\":\"${user.id}\",\"title\":\"${user.nickname}\"}}"
        // NekoP - /user/{id}로 요청하여 회원의 정보를 받아 올 수 있게 함
    }

    fun getIdFromNick(nick: String): String {
        val similarityNick = similarityRegex.replace(nick, "")
        val user = userDao.getUserFromNick(similarityNick) ?: return "{\"error\":405}"
        return "{\"result\":200,\"id\":\"${user.id}\"}"
    }
}