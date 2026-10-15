package app.receptnik.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TikTokLinksTest {

    @Test fun videoIdFromFullUrl() {
        assertEquals("7412345678901234567", TikTokLinks.videoId("https://www.tiktok.com/@chef.max/video/7412345678901234567?is_from_webapp=1"))
        assertEquals("7412345678901234567", TikTokLinks.videoId("https://m.tiktok.com/v/7412345678901234567.html"))
        assertEquals("7412345678901234567", TikTokLinks.videoId("https://www.tiktok.com/t/abc/?item_id=7412345678901234567"))
        assertNull(TikTokLinks.videoId("https://vt.tiktok.com/ZSgRwvV7u/"))
        assertNull(TikTokLinks.videoId("https://www.tiktok.com/"))
    }

    @Test fun canonicalUrl() {
        assertEquals(
            "https://www.tiktok.com/@chef.max/video/7412345678901234567",
            TikTokLinks.canonical("https://www.tiktok.com/@chef.max/video/7412345678901234567?_r=1&_t=x"),
        )
        assertEquals(
            "https://www.tiktok.com/@_/video/7412345678901234567",
            TikTokLinks.canonical("https://m.tiktok.com/v/7412345678901234567.html"),
        )
    }

    @Test fun genericPreviewsAreRejected() {
        assertTrue(TikTokLinks.isGeneric("TikTok | Делает будни ярче"))
        assertTrue(TikTokLinks.isGeneric("TikTok - Make Your Day"))
        assertTrue(TikTokLinks.isGeneric(""))
        assertFalse(TikTokLinks.isGeneric("Котлеты: фарш 500 г, лук 1 шт, яйцо 1 шт"))
    }
}
