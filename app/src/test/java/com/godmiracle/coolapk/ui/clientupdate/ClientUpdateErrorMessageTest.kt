package com.godmiracle.coolapk.ui.clientupdate

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ConnectException

class ClientUpdateErrorMessageTest {

    @Test
    fun `connection failure directs user to browser release page`() {
        assertEquals(
            "连接 GitHub 失败，请使用“浏览器下载 Release（推荐）”打开更新页",
            ConnectException("failed to connect to github.com:443").toClientUpdateUserMessage()
        )
    }
}
