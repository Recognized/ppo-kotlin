package crawler

import crawler.UnfurlCrawler.asProps

interface HtmlMetadata

interface MetadataFactory {
    fun from(attributes: MetaAttributes): HtmlMetadata?
}

object UnfurlCrawler {
    private val factories = listOf(
        OpenGraph.Companion,
        TwitterPlayerCard.Companion,
        TwitterSummaryCard.Companion,
        GeneralPageHtmlMetadata.Companion
    )

    fun createUnfurlFromMeta(attributes: MetaAttributes): Sequence<HtmlMetadata> {
        return factories.asSequence().mapNotNull {
            it.from(attributes)
        }
    }

    fun asProps(metaAttributes: MetaAttributes): Map<String, String> {
        return metaAttributes.flatMap {
            val name = it["name"] ?: it["property"]
            val content = it["content"]
            val title = it["title"]
            if (name != null && content != null) {
                listOf(name to content)
            } else {
                listOf()
            } + if (title != null) {
                listOf("title" to title)
            } else {
                listOf()
            }
        }.toMap()
    }
}


data class OpenGraphVideo(
    val url: String,
    val mime: String?,
    val width: Int?,
    val height: Int?
) : OpenGraphType()

data class OpenGraphImage(
    val url: String,
    val mime: String?,
    val width: Int?,
    val height: Int?,
    val alt: String?
) : OpenGraphType()

data class OpenGraphAudio(
    val url: String,
    val mime: String?
)

class OpenGraphWebsite : OpenGraphType() {
    override fun equals(other: Any?) = other is OpenGraphWebsite
    override fun hashCode(): Int = 0
    override fun toString(): String = "website"
}

sealed class OpenGraphType

data class OpenGraph(
    val url: String,
    val title: String,
    val image: OpenGraphImage,
    val description: String?,
    val determiner: String?,
    val siteName: String?,
    val video: OpenGraphVideo?,
    val audio: OpenGraphAudio?
) : HtmlMetadata {
    companion object : MetadataFactory {
        override fun from(attributes: List<Map<String, String>>): OpenGraph? {
            val props = asProps(attributes)
            val url = props["og:url"] ?: return null
            val title = props["og:title"] ?: props["title"] ?: return null
            val image = props["og:image"] ?: props["og:image:secure_url"] ?: props["og:image:url"] ?: return null
            val openGraphImage = OpenGraphImage(
                url = image,
                mime = props["og:image:type"],
                width = props["og:image:width"]?.toIntOrNull(),
                height = props["og:image:height"]?.toIntOrNull(),
                alt = props["og:image:alt"]
            )
            val openGraphVideo = (props["og:video"] ?: props["og:video:secure_url"] ?: props["og:video:url"])?.let {
                OpenGraphVideo(
                    url = it,
                    mime = props["og:video:type"],
                    width = props["og:video:width"]?.toIntOrNull(),
                    height = props["og:video:height"]?.toIntOrNull()
                )
            }
            val openGraphAudio = (props["og:audio"] ?: props["og:audio:secure_url"] ?: props["og:audio:url"])?.let {
                OpenGraphAudio(
                    url = it,
                    mime = props["og:audio:type"]
                )
            }
            return OpenGraph(
                url = url,
                title = title,
                image = openGraphImage,
                description = props["og:description"],
                determiner = props["og:determiner"],
                siteName = props["og:site_name"],
                video = openGraphVideo,
                audio = openGraphAudio
            )
        }
    }
}

data class GeneralPageHtmlMetadata(
    val title: String,
    val description: String?,
    val image: String?
) : HtmlMetadata {
    companion object : MetadataFactory {
        override fun from(attributes: MetaAttributes): GeneralPageHtmlMetadata? {
            val props = asProps(attributes)
            val title = props["title"] ?: return null
            val description = props["description"]
            val image = props["image"]
            if (description == null && image == null) return null
            return GeneralPageHtmlMetadata(title, description, image)
        }
    }
}

data class TwitterSummaryCard(
    val card: String,
    val title: String,
    val description: String?,
    val site: String?,
    val image: String?,
    val imageAlt: String?
) : HtmlMetadata {
    companion object : MetadataFactory {
        override fun from(attributes: List<Map<String, String>>): TwitterSummaryCard? {
            val props = asProps(attributes)
            val card = props["twitter:card"]?.takeIf { it == "summary" || it == "summary_large_image" } ?: return null
            val title = props["twitter:title"] ?: return null
            return TwitterSummaryCard(
                card = card,
                title = title,
                description = props["twitter:description"],
                site = props["twitter:site"],
                image = props["twitter:image"],
                imageAlt = props["twitter:image:alt"]
            )
        }
    }
}

typealias MetaAttributes = List<Map<String, String>>

data class TwitterPlayerCard(
    val title: String,
    val site: String,
    val player: String,
    val playerWidth: Int,
    val playerHeight: Int,
    val image: String,
    val playerStream: String?,
    val description: String?,
    val imageAlt: String?
) : HtmlMetadata {
    companion object : MetadataFactory {
        override fun from(attributes: MetaAttributes): TwitterPlayerCard? {
            val props = asProps(attributes)
            props["twitter:card"].takeIf { it == "player" } ?: return null
            val title = props["twitter:title"] ?: return null
            val site = props["twitter:site"] ?: return null
            val player = props["twitter:player"] ?: return null
            val playerWidth = props["twitter:player:width"]?.toIntOrNull() ?: return null
            val playerHeight = props["twitter:player:height"]?.toIntOrNull() ?: return null
            val image = props["twitter:image"] ?: return null
            return TwitterPlayerCard(
                title = title,
                site = site,
                player = player,
                playerWidth = playerWidth,
                playerHeight = playerHeight,
                image = image,
                description = props["twitter:description"],
                imageAlt = props["twitter:image:alt"],
                playerStream = props["twitter:player:stream"]
            )
        }
    }
}