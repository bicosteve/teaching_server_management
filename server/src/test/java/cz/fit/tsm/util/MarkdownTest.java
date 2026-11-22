package cz.fit.tsm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarkdownTest {

	@Test
	void toHtmlRendersBasicFormatting() {
		String html = Markdown.toHtml("Hello **world**");
		assertTrue(html.contains("<strong>world</strong>"));
	}

	@Test
	void toHtmlRendersGfmTables() {
		String markdown = """
				| Name | Year |
				| ---- | ---- |
				| TSM  | 2026 |
				""";
		String html = Markdown.toHtml(markdown);
		assertTrue(html.contains("<table>"));
		assertTrue(html.contains("<th>Name</th>"));
		assertTrue(html.contains("<td>TSM</td>"));
	}

	@Test
	void toHtmlEscapesRawHtml() {
		String html = Markdown.toHtml("<script>alert(1)</script>");
		assertFalse(html.contains("<script>"));
		assertTrue(html.contains("&lt;script&gt;"));
	}

	@Test
	void toPlainTextStripsMarkup() {
		assertEquals("Hello world", Markdown.toPlainText("Hello **world**"));
	}

	@Test
	void blankInputYieldsEmptyOutput() {
		assertEquals("", Markdown.toHtml("  "));
		assertEquals("", Markdown.toPlainText(null));
	}
}
