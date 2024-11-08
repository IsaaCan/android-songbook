package mwongela.songbook.custom

import mwongela.songbook.mock.ShareSongServiceMock
import mwongela.songbook.persistence.general.model.Category
import mwongela.songbook.persistence.general.model.CategoryType
import mwongela.songbook.persistence.general.model.Song
import mwongela.songbook.persistence.general.model.SongStatus
import mwongela.songbook.settings.chordsnotation.ChordsNotation
import org.assertj.core.api.Assertions
import org.junit.Test
import java.net.URLEncoder

class ShareSongServiceTest {

    @Test
    fun test_encodeDecodeSong() {
        val shareSongEncoder = ShareSongServiceMock()
        val song = Song(
            id = "1",
            title = "Epitafium dla Włodzimierza Wysockiego",
            categories = mutableListOf(
                Category(1, type = CategoryType.ARTIST, name = "Jacek Kaczmarski")
            ),
            customCategoryName = "Kaczmarski",
            status = SongStatus.PUBLISHED,
            chordsNotation = ChordsNotation.ENGLISH,
            content = """
Do piekła! Do piekła! Do piekła! [a e]
Nie mam czasu na przejażdżki wiedźmo wściekła! [a C G G]
"""
        )

        var encoded = shareSongEncoder.encodeSong(song)
        Assertions.assertThat(encoded)
            .startsWith("H4sIAAAAAAAA")

        // encoded song is escaped url
        val encodedUrl: String = URLEncoder.encode(encoded, "utf-8")
        Assertions.assertThat(encodedUrl).isEqualTo(encoded)

        val decodedBack = shareSongEncoder.decodeSong(encoded)
        Assertions.assertThat(decodedBack.title).isEqualTo("Epitafium dla Włodzimierza Wysockiego")
        Assertions.assertThat(decodedBack.customCategoryName).isEqualTo("Kaczmarski")
        Assertions.assertThat(decodedBack.content).isEqualTo("""
Do piekła! Do piekła! Do piekła! [a e]
Nie mam czasu na przejażdżki wiedźmo wściekła! [a C G G]
""")
        Assertions.assertThat(decodedBack.chordsNotation).isEqualTo(ChordsNotation.ENGLISH)

        // ensure backwards compatiblity
        encoded = "H4sIAAAAAAAA_3WLIQ4CMRBFBa4SifoYLgJkBQkWQVZM2oEMZWizZbOhcq_BTcjauRcrMbj3kvfcZp_lSRfpFeFOONmYQhUV7upsr5J8FL6mpbhdQhaONtIa__hM4NYdhaGk8JVKjwchd5VvZFOwKQoG4WAfTRjs7X_OLRo0rVu5A_mq1JW5XXwB.YnGPKEAAAA-"

        val decodedBack2 = shareSongEncoder.decodeSong(encoded)
        Assertions.assertThat(decodedBack2.title).isEqualTo("Epitafium dla Włodzimierza Wysockiego")
        Assertions.assertThat(decodedBack2.customCategoryName).isEqualTo("Kaczmarski")
        Assertions.assertThat(decodedBack2.content).isEqualTo("""
Do piekła! Do piekła! Do piekła! [a e]
Nie mam czasu na przejażdżki wiedźmo wściekła! [a C G G]
""")
        Assertions.assertThat(decodedBack2.chordsNotation).isEqualTo(ChordsNotation.ENGLISH)
    }

    @Test
    fun test_marshalUnmarshal() {
        val shareSongEncoder = ShareSongServiceMock()
        val song = Song(
            id = "1",
            title = "Epitafium dla Włodzimierza Wysockiego",
            categories = mutableListOf(
                Category(1, type = CategoryType.ARTIST, name = "Jacek Kaczmarski")
            ),
            customCategoryName = "Kaczmarski",
            status = SongStatus.PUBLISHED,
            chordsNotation = ChordsNotation.ENGLISH,
            content = """
Do piekła! Do piekła! Do piekła! [a e]
Nie mam czasu na przejażdżki wiedźmo wściekła! [a C G G]
""",
        )

        val marshaled = shareSongEncoder.marshal(song)
        Assertions.assertThat(String(marshaled))
            .isEqualTo(
                "\n" +
                        "&Epitafium dla Włodzimierza Wysockiego\u0012i\n" +
                        "Do piekła! Do piekła! Do piekła! [a e]\n" +
                        "Nie mam czasu na przejażdżki wiedźmo wściekła! [a C G G]\n" +
                        "\u001A\n" +
                        "Kaczmarski \u0003"
            )

        val decodedBack = shareSongEncoder.unmarshal(marshaled)
        Assertions.assertThat(decodedBack.title).isEqualTo("Epitafium dla Włodzimierza Wysockiego")
        Assertions.assertThat(decodedBack.customCategoryName).isEqualTo("Kaczmarski")
        Assertions.assertThat(decodedBack.content).isEqualTo(
            """
Do piekła! Do piekła! Do piekła! [a e]
Nie mam czasu na przejażdżki wiedźmo wściekła! [a C G G]
"""
        )
        Assertions.assertThat(decodedBack.chordsNotation).isEqualTo(ChordsNotation.ENGLISH)
    }
}