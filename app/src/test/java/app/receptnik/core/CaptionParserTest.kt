package app.receptnik.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionParserTest {

    @Test fun russianInstagramCaption() {
        val caption = """
            Сырники как в детстве 🥞😍
            Самый простой рецепт на завтрак!

            Ингредиенты:
            🔸 Творог 5% - 400 г
            🔸 Яйцо - 1 шт
            🔸 Сахар - 2 ст.л.
            🔸 Мука - 3 ст. л.

            Приготовление:
            1️⃣ Творог разомните вилкой, добавьте яйцо и сахар.
            2️⃣ Всыпьте муку и перемешайте.
            3️⃣ Обжарьте по 3 минуты с каждой стороны.

            Сохраняй рецепт, чтобы не потерять!
            #сырники #завтрак
        """.trimIndent()
        val d = CaptionParser.parse(caption)
        assertEquals("Сырники как в детстве", d.title)
        assertEquals(4, d.ingredients.size)
        assertEquals(3, d.steps.size)
        assertEquals(400.0, d.ingredients[0].quantity)
        assertTrue(d.steps.none { it.contains("Сохраняй") })
    }

    @Test fun englishCaptionWithoutHeaders() {
        val caption = """
            Easy Garlic Butter Pasta (serves 4)
            12 oz spaghetti
            4 tbsp butter
            6 cloves garlic, minced
            1. Boil pasta in salted water until al dente.
            2. Melt butter in a pan, add garlic and cook 1 minute.
            Follow for more easy recipes!
        """.trimIndent()
        val d = CaptionParser.parse(caption)
        assertEquals(4, d.servings)
        assertEquals(3, d.ingredients.size)
        assertEquals(2, d.steps.size)
    }

    @Test fun inlineHeaders() {
        val d = CaptionParser.parse(
            "Паста с курицей за 15 минут! Ингредиенты: паста 200 г, куриное филе 300 г, сливки 200 мл. " +
                "Приготовление: Обжарьте курицу. Добавьте сливки и пасту."
        )
        assertEquals(3, d.ingredients.size)
        assertEquals(15, d.totalTimeMin)
        assertTrue(d.steps.isNotEmpty())
    }

    @Test fun hebrewCaption() {
        val d = CaptionParser.parse("עוגה\nמצרכים:\n2 ביצים\n1 כוס סוכר\nאופן הכנה:\nמערבבים הכל.")
        assertEquals(2, d.ingredients.size)
        assertEquals(1, d.steps.size)
    }
}
