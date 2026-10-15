package app.receptnik.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoLinksTest {

    @Test fun ogVideoTag() {
        val html = """<head><meta property="og:video:secure_url" content="https://cdn.example.com/v.mp4?a=1&amp;b=2"></head>"""
        assertEquals("https://cdn.example.com/v.mp4?a=1&b=2", VideoLinks.fromHtml(html))
    }

    @Test fun ogVideoTagWithContentFirst() {
        val html = """<meta content="https://cdn.example.com/x.mp4" property="og:video">"""
        assertEquals("https://cdn.example.com/x.mp4", VideoLinks.fromHtml(html))
    }

    @Test fun embeddedJsonEscapes() {
        val html = """{"shortcode_media":{"video_url":"https:\/\/scontent.cdninstagram.com\/v\/reel.mp4?_nc=1&oh=2"}}"""
        assertEquals("https://scontent.cdninstagram.com/v/reel.mp4?_nc=1&oh=2", VideoLinks.fromHtml(html))
    }

    @Test fun noVideo() {
        assertNull(VideoLinks.fromHtml("<html><meta property=\"og:image\" content=\"https://x/y.jpg\"></html>"))
    }

    @Test fun authorPrefixIsRemoved() {
        assertEquals(
            "здесь простые рецепты от шефа",
            TextUtil.stripAuthorPrefix("@makc_galishnikov - здесь простые рецепты от шефа"),
        )
        assertEquals("Easy pasta", TextUtil.stripAuthorPrefix("Chef Max (@chef.max): Easy pasta"))
        assertEquals("No prefix here", TextUtil.stripAuthorPrefix("No prefix here"))
    }
}
