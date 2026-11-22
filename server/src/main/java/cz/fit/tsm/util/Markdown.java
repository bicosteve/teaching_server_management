package cz.fit.tsm.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;

public final class Markdown {

	private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());

	private static final Parser PARSER = Parser.builder()
			.extensions(EXTENSIONS)
			.build();

	private static final HtmlRenderer HTML = HtmlRenderer.builder()
			.extensions(EXTENSIONS)
			.escapeHtml(true)
			.attributeProviderFactory(context -> Markdown::sanitizeUrlAttributes)
			.build();

	private static final TextContentRenderer PLAIN = TextContentRenderer.builder()
			.extensions(EXTENSIONS)
			.build();

	private Markdown() {
	}

	public static String toHtml(String markdown) {
		if (markdown == null || markdown.isBlank()) {
			return "";
		}
		return HTML.render(PARSER.parse(markdown));
	}

	public static String toPlainText(String markdown) {
		if (markdown == null || markdown.isBlank()) {
			return "";
		}
		return PLAIN.render(PARSER.parse(markdown)).trim();
	}

	private static void sanitizeUrlAttributes(Node node, String tagName, Map<String, String> attributes) {
		if ("a".equals(tagName)) {
			if (!isSafeUrl(attributes.get("href"))) {
				attributes.put("href", "#");
			}
			attributes.put("rel", "noopener noreferrer");
		}
		else if ("img".equals(tagName) && !isSafeUrl(attributes.get("src"))) {
			attributes.put("src", "");
		}
	}

	private static boolean isSafeUrl(String url) {
		if (url == null || url.isBlank()) {
			return false;
		}
		String value = url.trim().toLowerCase(Locale.ROOT);
		return value.startsWith("https://")
				|| value.startsWith("http://")
				|| value.startsWith("mailto:")
				|| value.startsWith("/")
				|| value.startsWith("#");
	}
}
