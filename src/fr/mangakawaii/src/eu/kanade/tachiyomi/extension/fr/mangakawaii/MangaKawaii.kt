package eu.kanade.tachiyomi.extension.fr.mangakawaii

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class MangaKawaii : ParsedHttpSource() {

    override val name = "MangaKawaii"

    override val baseUrl = "https://www.mangakawaii.com"

    override val lang = "fr"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

//    override fun popularMangaSelector() = "div.col-sm-12 > div.home-thumb > a.thumbnail"
    override fun popularMangaSelector() = "ul.manga-list.list-group > li.list-group-item"

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a.tips").first()

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain(urlElement.attr("href"))
        chapter.name = element.select("span.title.nowrap").first()?.text()?.let { urlElement.text() + " - " + it } ?: urlElement.text()
//        chapter.date_upload = element.select("span.date").first()?.text()?.let { parseChapterDate(it) } ?: 0
        return chapter
    }

    override fun chapterListSelector()= "ul.chapters"

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used")

    override fun latestUpdatesFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a.title").first().let {
            manga.setUrlWithoutDomain(it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun latestUpdatesNextPageSelector(): String? = "ul.pagination > li:last-child > a"

    override fun latestUpdatesRequest(page: Int): Request = throw Exception("Not used")

    override fun latestUpdatesSelector()= "ul.manga-list.list-group > li.list-group-item"

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select("div.tab-summary").first()
        val manga = SManga.create()
        manga.author = infoElement.select("ul.det > li:nth-child(4) > span.data > a")?.first()?.text()
        manga.artist = infoElement.select("ul.det > li:nth-child(5) > span.data > a")?.first()?.text()

        val genres = mutableListOf<String>()
        infoElement.select("ul.det > li:nth-child(6) > span.data > a").orEmpty().forEach{ id ->
            genres.add(id.text())
        }
        manga.genre = genres.joinToString(", ")

        manga.description = document.select("div#synopsis > p")?.first()?.text()
        manga.status = document.select("ul.det > li:nth-child(6) > span.data > span.label").first()?.text().orEmpty().let { parseStatus(it) }
        manga.thumbnail_url = document.select("div.cover-area.thumbnail img")?.first()?.absUrl("src")
        return manga
    }

    private fun parseStatus(status: String) = when {
        status.contains("En Cours", true) -> SManga.ONGOING
        status.contains("Terminé", true) -> SManga.COMPLETED
//        status.contains("Terminé", true) -> SManga.LICENSED
        else -> SManga.UNKNOWN
    }

    override fun pageListParse(document: Document): List<Page> {
        val doc = document.select("div.page-break img");

        val pages = mutableListOf<Page>()
        doc.forEach {
            // Create dummy element to resolve relative URL
            val absUrl = it.select("img").attr("src")
            pages.add(Page(pages.size, "", absUrl))
        }
        return pages
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()

        element.select("a.label.label-warning").first().let {
            manga.url = it.attr("href")
            manga.title = it.text()
        }

        element.select("a.label.label-warning + a > img").first()?.let {
            manga.thumbnail_url = it.absUrl("src")
        }

        return manga
    }

    override fun popularMangaNextPageSelector()= ""

    override fun popularMangaRequest(page: Int): Request = GET(baseUrl, headers)

    override fun searchMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
//        manga.setUrlWithoutDomain(titleElement.attr("href"))
//        manga.title = cleanTitle(titleElement.text())
//        manga.thumbnail_url = getThumbnail(thumbnailElement.attr("src"))
        return manga
    }

    override fun searchMangaNextPageSelector(): String? = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw Exception("Not used")

    override fun searchMangaSelector()= "input#autocomplete.form-control"
    
}
